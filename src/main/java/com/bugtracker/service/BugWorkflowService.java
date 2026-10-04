package com.bugtracker.service;

import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.RoleName;
import com.bugtracker.exception.BadRequestException;
import com.bugtracker.exception.InvalidStatusTransitionException;
import com.bugtracker.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Central definition of the bug lifecycle.
 *
 * <pre>
 * OPEN ──▶ IN_PROGRESS ──▶ RESOLVED ──▶ CLOSED
 *   │            ▲             │
 *   │            │             ▼
 *   └────────────┴──────── REOPENED
 * </pre>
 *
 * <p>Only administrators may short-circuit the workflow (for example closing a
 * duplicate right away). Every rejected transition produces HTTP 409.</p>
 */
@Service
public class BugWorkflowService {

    private static final Map<BugStatus, Set<BugStatus>> TRANSITIONS = Map.of(
            BugStatus.OPEN, EnumSet.of(BugStatus.IN_PROGRESS, BugStatus.RESOLVED),
            BugStatus.IN_PROGRESS, EnumSet.of(BugStatus.OPEN, BugStatus.RESOLVED),
            BugStatus.RESOLVED, EnumSet.of(BugStatus.CLOSED, BugStatus.REOPENED),
            BugStatus.REOPENED, EnumSet.of(BugStatus.IN_PROGRESS, BugStatus.RESOLVED),
            BugStatus.CLOSED, EnumSet.of(BugStatus.REOPENED)
    );

    /**
     * @param actor the user requesting the transition
     * @param from  current status
     * @param to    requested status
     * @throws InvalidStatusTransitionException when the workflow forbids the transition
     */
    public void validateTransition(BugStatus from, BugStatus to, UserPrincipal actor) {
        if (to == null) {
            throw new BadRequestException("Please select a status.");
        }
        if (from == to) {
            throw new BadRequestException("Bug is already in status " + from + ".");
        }

        Set<BugStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (allowed.contains(to)) {
            return;
        }

        // Administrators may close a bug directly from OPEN (e.g. duplicate report).
        if (isAdmin(actor) && to == BugStatus.CLOSED && from == BugStatus.OPEN) {
            return;
        }

        throw new InvalidStatusTransitionException(from, to);
    }

    /**
     * Resolving a bug requires a resolution summary; closing requires that a
     * resolution is already recorded on the bug.
     */
    public void validateResolutionRequirement(BugStatus targetStatus,
                                              String requestedResolution,
                                              String existingResolution) {
        boolean hasResolution = (requestedResolution != null && !requestedResolution.isBlank())
                || (existingResolution != null && !existingResolution.isBlank());
        if (targetStatus == BugStatus.RESOLVED && !hasResolution) {
            throw new BadRequestException("A resolution summary is required to resolve a bug.");
        }
        if (targetStatus == BugStatus.CLOSED && !hasResolution) {
            throw new BadRequestException("A bug can only be closed after it has been resolved.");
        }
    }

    public boolean isAdmin(UserPrincipal actor) {
        return actor != null && actor.getRole() == RoleName.ROLE_ADMIN;
    }

    public Set<BugStatus> allowedTargetStatuses(BugStatus currentStatus) {
        return TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }
}
