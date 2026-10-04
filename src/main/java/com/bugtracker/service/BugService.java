package com.bugtracker.service;

import com.bugtracker.dto.BugAssignRequest;
import com.bugtracker.dto.BugDetailResponse;
import com.bugtracker.dto.BugFilterRequest;
import com.bugtracker.dto.BugPriorityUpdateRequest;
import com.bugtracker.dto.BugRequest;
import com.bugtracker.dto.BugSeverityUpdateRequest;
import com.bugtracker.dto.BugStatusUpdateRequest;
import com.bugtracker.dto.BugSummaryResponse;
import com.bugtracker.dto.BugUpdateRequest;
import com.bugtracker.dto.PageResponse;
import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugHistory;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.User;
import com.bugtracker.exception.BadRequestException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.BugMapper;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.BugSpecifications;
import com.bugtracker.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business operations for bugs: creation, editing, searching, workflow
 * transitions, assignment and deletion. Every mutation records an audit trail row.
 */
@Service
public class BugService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "bugCode", "severity", "priority", "status",
            "assignedDeveloper.fullName", "reporter.fullName");

    private final BugRepository bugRepository;
    private final UserService userService;
    private final BugMapper bugMapper;
    private final BugCodeGenerator bugCodeGenerator;
    private final BugWorkflowService workflowService;
    private final BugAuthorizationService authorizationService;
    private final FileStorageService fileStorageService;

    public BugService(BugRepository bugRepository,
                      UserService userService,
                      BugMapper bugMapper,
                      BugCodeGenerator bugCodeGenerator,
                      BugWorkflowService workflowService,
                      BugAuthorizationService authorizationService,
                      FileStorageService fileStorageService) {
        this.bugRepository = bugRepository;
        this.userService = userService;
        this.bugMapper = bugMapper;
        this.bugCodeGenerator = bugCodeGenerator;
        this.workflowService = workflowService;
        this.authorizationService = authorizationService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Loads the entity or fails with HTTP 404.
     */
    @Transactional(readOnly = true)
    public Bug getEntity(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug", id));
    }

    @Transactional
    public BugDetailResponse createBug(BugRequest request, UserPrincipal actor) {
        requireActor(actor);
        User reporter = userService.getEntityById(actor.getId());

        Bug bug = bugMapper.toEntity(request);
        bug.setBugCode(bugCodeGenerator.nextBugCode());
        bug.setReporter(reporter);
        bug.setStatus(BugStatus.OPEN);

        User assignee = request.assignedDeveloperId() == null
                ? null
                : resolveAssignee(request.assignedDeveloperId());
        bug.setAssignedDeveloper(assignee);

        bug.addHistory(new BugHistory("CREATED", null, "Bug reported with status OPEN", reporter));
        if (assignee != null) {
            bug.addHistory(new BugHistory("ASSIGNEE", null, assignee.getFullName(), reporter));
        }

        // saveAndFlush guarantees generated identifiers and timestamps are present
        // in the response payload.
        Bug saved = bugRepository.saveAndFlush(bug);
        return bugMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public BugDetailResponse getBug(Long id, UserPrincipal actor) {
        requireActor(actor);
        return bugMapper.toDetail(getEntity(id));
    }

    /**
     * Search + filter + sort + paginate in a single call.
     */
    @Transactional(readOnly = true)
    public PageResponse<BugSummaryResponse> searchBugs(BugFilterRequest filter, UserPrincipal actor) {
        requireActor(actor);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), resolveSort(filter));
        Page<Bug> page = bugRepository.findAll(BugSpecifications.withFilter(filter), pageable);
        List<BugSummaryResponse> content = page.getContent().stream().map(bugMapper::toSummary).toList();
        return PageResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                sortKey(filter),
                filter.isDescending() ? "desc" : "asc"
        );
    }

    /**
     * Full edit of a bug. Priority / severity may only be changed by testers and
     * administrators; the assignee only by administrators.
     */
    @Transactional
    public BugDetailResponse updateBug(Long id, BugUpdateRequest request, UserPrincipal actor) {
        Bug bug = getEntity(id);
        authorizationService.assertCanEdit(bug, actor);
        User editor = userService.getEntityById(actor.getId());

        boolean triageChanged = bug.getPriority() != request.priority() || bug.getSeverity() != request.severity();
        if (triageChanged && !authorizationService.isAdmin(actor) && !authorizationService.isTester(actor)) {
            throw new AccessDeniedException(
                    "Access denied. Your role is not allowed to change priority or severity.");
        }

        Long currentAssigneeId = bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getId();
        boolean assigneeChanged = !Objects.equals(currentAssigneeId, request.assignedDeveloperId());
        if (assigneeChanged && !authorizationService.isAdmin(actor)) {
            throw new AccessDeniedException("Access denied. Only administrators may change the assignee.");
        }

        String oldTitle = bug.getTitle();
        String oldDescription = bug.getDescription();
        String oldSteps = bug.getStepsToReproduce();
        String oldExpected = bug.getExpectedResult();
        String oldActual = bug.getActualResult();
        String oldEnvironment = bug.getEnvironment();
        Priority oldPriority = bug.getPriority();
        Severity oldSeverity = bug.getSeverity();
        String oldResolution = bug.getResolution();

        bugMapper.applyUpdate(bug, request);

        recordIfChanged(bug, editor, "TITLE", oldTitle, bug.getTitle());
        recordIfChanged(bug, editor, "DESCRIPTION", oldDescription, bug.getDescription());
        recordIfChanged(bug, editor, "STEPS_TO_REPRODUCE", oldSteps, bug.getStepsToReproduce());
        recordIfChanged(bug, editor, "EXPECTED_RESULT", oldExpected, bug.getExpectedResult());
        recordIfChanged(bug, editor, "ACTUAL_RESULT", oldActual, bug.getActualResult());
        recordIfChanged(bug, editor, "ENVIRONMENT", oldEnvironment, bug.getEnvironment());
        recordIfChanged(bug, editor, "PRIORITY", name(oldPriority), name(bug.getPriority()));
        recordIfChanged(bug, editor, "SEVERITY", name(oldSeverity), name(bug.getSeverity()));
        recordIfChanged(bug, editor, "RESOLUTION", oldResolution, bug.getResolution());

        if (assigneeChanged) {
            User assignee = request.assignedDeveloperId() == null
                    ? null
                    : resolveAssignee(request.assignedDeveloperId());
            recordIfChanged(bug, editor, "ASSIGNEE",
                    bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getFullName(),
                    assignee == null ? null : assignee.getFullName());
            bug.setAssignedDeveloper(assignee);
        }

        return bugMapper.toDetail(bugRepository.saveAndFlush(bug));
    }

    /**
     * Moves a bug through the workflow, enforcing both the transition matrix and
     * the resolution requirements.
     */
    @Transactional
    public BugDetailResponse changeStatus(Long id, BugStatusUpdateRequest request, UserPrincipal actor) {
        Bug bug = getEntity(id);
        BugStatus current = bug.getStatus();
        BugStatus target = request.status();

        authorizationService.assertCanChangeStatus(bug, target, actor);
        workflowService.validateTransition(current, target, actor);
        workflowService.validateResolutionRequirement(target, request.resolution(), bug.getResolution());

        User editor = userService.getEntityById(actor.getId());
        recordIfChanged(bug, editor, "STATUS", name(current), name(target));

        String resolution = request.resolution() == null ? bug.getResolution() : request.resolution().trim();
        if (target == BugStatus.REOPENED) {
            recordIfChanged(bug, editor, "RESOLUTION", bug.getResolution(), null);
            resolution = null;
        }
        bug.setResolution(resolution);
        bug.setStatus(target);

        return bugMapper.toDetail(bugRepository.saveAndFlush(bug));
    }

    @Transactional
    public BugDetailResponse changePriority(Long id, BugPriorityUpdateRequest request, UserPrincipal actor) {
        Bug bug = getEntity(id);
        authorizationService.assertCanChangePriorityOrSeverity(bug, actor);
        User editor = userService.getEntityById(actor.getId());
        if (bug.getPriority() == request.priority()) {
            throw new BadRequestException("Bug already has priority " + request.priority() + ".");
        }
        recordIfChanged(bug, editor, "PRIORITY", name(bug.getPriority()), name(request.priority()));
        bug.setPriority(request.priority());
        return bugMapper.toDetail(bugRepository.saveAndFlush(bug));
    }

    @Transactional
    public BugDetailResponse changeSeverity(Long id, BugSeverityUpdateRequest request, UserPrincipal actor) {
        Bug bug = getEntity(id);
        authorizationService.assertCanChangePriorityOrSeverity(bug, actor);
        User editor = userService.getEntityById(actor.getId());
        if (bug.getSeverity() == request.severity()) {
            throw new BadRequestException("Bug already has severity " + request.severity() + ".");
        }
        recordIfChanged(bug, editor, "SEVERITY", name(bug.getSeverity()), name(request.severity()));
        bug.setSeverity(request.severity());
        return bugMapper.toDetail(bugRepository.saveAndFlush(bug));
    }

    @Transactional
    public BugDetailResponse assignDeveloper(Long id, BugAssignRequest request, UserPrincipal actor) {
        Bug bug = getEntity(id);
        authorizationService.assertCanAssign(bug, actor);
        User editor = userService.getEntityById(actor.getId());

        User assignee = request.assignedDeveloperId() == null
                ? null
                : resolveAssignee(request.assignedDeveloperId());
        String oldAssignee = bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getFullName();
        String newAssignee = assignee == null ? null : assignee.getFullName();

        if (Objects.equals(oldAssignee, newAssignee)) {
            throw new BadRequestException(newAssignee == null
                    ? "Bug is already unassigned."
                    : "Bug is already assigned to " + newAssignee + ".");
        }
        recordIfChanged(bug, editor, "ASSIGNEE", oldAssignee, newAssignee);
        bug.setAssignedDeveloper(assignee);
        return bugMapper.toDetail(bugRepository.saveAndFlush(bug));
    }

    /**
     * Deletes the bug together with its comments, attachments (rows and files on
     * disk) and history entries.
     */
    @Transactional
    public void deleteBug(Long id, UserPrincipal actor) {
        Bug bug = getEntity(id);
        authorizationService.assertCanDelete(bug, actor);
        bug.getAttachments().forEach(attachment -> fileStorageService.delete(attachment.getStoredFileName()));
        bugRepository.delete(bug);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static Sort resolveSort(BugFilterRequest filter) {
        return Sort.by(filter.isDescending() ? Sort.Direction.DESC : Sort.Direction.ASC, sortKey(filter));
    }

    private static String sortKey(BugFilterRequest filter) {
        String requested = filter.getSortBy();
        if (requested != null && ALLOWED_SORT_FIELDS.contains(requested)) {
            return requested;
        }
        return "createdAt";
    }

    private static void requireActor(UserPrincipal actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication is required to access this resource.");
        }
    }

    private User resolveAssignee(Long userId) {
        User assignee = userService.getEntityById(userId);
        if (!assignee.isEnabled()) {
            throw new BadRequestException("The selected developer account is disabled.");
        }
        return assignee;
    }

    private static void recordIfChanged(Bug bug, User actor, String field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            bug.addHistory(new BugHistory(field, oldValue, newValue, actor));
        }
    }
}
