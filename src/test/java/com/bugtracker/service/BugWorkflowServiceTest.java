package com.bugtracker.service;

import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.User;
import com.bugtracker.exception.BadRequestException;
import com.bugtracker.exception.InvalidStatusTransitionException;
import com.bugtracker.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the lifecycle state machine. No Spring context is required,
 * which keeps the transition matrix verifiable in isolation.
 */
class BugWorkflowServiceTest {

    private final BugWorkflowService workflowService = new BugWorkflowService();

    private final UserPrincipal admin = principal(1L, RoleName.ROLE_ADMIN);
    private final UserPrincipal tester = principal(2L, RoleName.ROLE_TESTER);
    private final UserPrincipal developer = principal(3L, RoleName.ROLE_DEVELOPER);

    @ParameterizedTest(name = "{0} -> {1} is accepted")
    @CsvSource({
            "OPEN,IN_PROGRESS",
            "OPEN,RESOLVED",
            "IN_PROGRESS,OPEN",
            "IN_PROGRESS,RESOLVED",
            "RESOLVED,CLOSED",
            "RESOLVED,REOPENED",
            "REOPENED,IN_PROGRESS",
            "REOPENED,RESOLVED",
            "CLOSED,REOPENED"
    })
    @DisplayName("every documented transition is accepted for every role")
    void acceptsDocumentedTransitions(BugStatus from, BugStatus to) {
        assertThatCode(() -> workflowService.validateTransition(from, to, developer))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflowService.validateTransition(from, to, tester))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflowService.validateTransition(from, to, admin))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "IN_PROGRESS,CLOSED",
            "CLOSED,OPEN",
            "CLOSED,IN_PROGRESS",
            "RESOLVED,OPEN",
            "RESOLVED,IN_PROGRESS",
            "REOPENED,OPEN",
            "REOPENED,CLOSED"
    })
    @DisplayName("transitions outside the matrix raise a conflict (HTTP 409)")
    void rejectsTransitionsOutsideTheMatrix(BugStatus from, BugStatus to) {
        assertThatThrownBy(() -> workflowService.validateTransition(from, to, developer))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Invalid status transition from " + from + " to " + to + ".");
    }

    @Test
    @DisplayName("only administrators may close a bug straight from OPEN")
    void onlyAdministratorsMayCloseDirectlyFromOpen() {
        assertThatCode(() -> workflowService.validateTransition(BugStatus.OPEN, BugStatus.CLOSED, admin))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> workflowService.validateTransition(BugStatus.OPEN, BugStatus.CLOSED, tester))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> workflowService.validateTransition(BugStatus.OPEN, BugStatus.CLOSED, developer))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("no-op and missing targets are bad requests (HTTP 400)")
    void rejectsNoOpAndMissingTargets() {
        assertThatThrownBy(() -> workflowService.validateTransition(BugStatus.OPEN, BugStatus.OPEN, admin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in status OPEN");

        assertThatThrownBy(() -> workflowService.validateTransition(BugStatus.OPEN, null, admin))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Please select a status.");
    }

    @ParameterizedTest(name = "target {0} does not require a resolution")
    @EnumSource(value = BugStatus.class, names = {"OPEN", "IN_PROGRESS", "REOPENED"})
    void resolutionIsOnlyRequiredForResolvedAndClosed(BugStatus target) {
        assertThatCode(() -> workflowService.validateResolutionRequirement(target, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("resolving and closing require a resolution summary")
    void resolutionIsRequiredToResolveAndClose() {
        assertThatThrownBy(() -> workflowService.validateResolutionRequirement(BugStatus.RESOLVED, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A resolution summary is required to resolve a bug.");
        assertThatThrownBy(() -> workflowService.validateResolutionRequirement(BugStatus.CLOSED, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A bug can only be closed after it has been resolved.");

        // A resolution captured while resolving, or supplied with this request,
        // satisfies the requirement.
        assertThatCode(() -> workflowService.validateResolutionRequirement(BugStatus.RESOLVED, "Fixed in 1.2.3", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflowService.validateResolutionRequirement(BugStatus.RESOLVED, null, "Fixed in 1.2.3"))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflowService.validateResolutionRequirement(BugStatus.CLOSED, "  ", "Fixed in 1.2.3"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("allowedTargetStatuses mirrors the transition matrix")
    void allowedTargetStatusesMirrorsTheMatrix() {
        assertThat(workflowService.allowedTargetStatuses(BugStatus.OPEN))
                .containsExactlyInAnyOrder(BugStatus.IN_PROGRESS, BugStatus.RESOLVED);
        assertThat(workflowService.allowedTargetStatuses(BugStatus.IN_PROGRESS))
                .containsExactlyInAnyOrder(BugStatus.OPEN, BugStatus.RESOLVED);
        assertThat(workflowService.allowedTargetStatuses(BugStatus.RESOLVED))
                .containsExactlyInAnyOrder(BugStatus.CLOSED, BugStatus.REOPENED);
        assertThat(workflowService.allowedTargetStatuses(BugStatus.REOPENED))
                .containsExactlyInAnyOrder(BugStatus.IN_PROGRESS, BugStatus.RESOLVED);
        assertThat(workflowService.allowedTargetStatuses(BugStatus.CLOSED))
                .containsExactly(BugStatus.REOPENED);
    }

    @Test
    @DisplayName("isAdmin only recognises ROLE_ADMIN and tolerates an anonymous actor")
    void isAdminRecognisesAdministratorsOnly() {
        assertThat(workflowService.isAdmin(admin)).isTrue();
        assertThat(workflowService.isAdmin(tester)).isFalse();
        assertThat(workflowService.isAdmin(developer)).isFalse();
        assertThat(workflowService.isAdmin(null)).isFalse();
    }

    private static UserPrincipal principal(Long id, RoleName roleName) {
        User user = new User("Test " + roleName, roleName.name().toLowerCase() + "@workflow.test", "irrelevant");
        user.setId(id);
        Role role = new Role(roleName, roleName.name());
        role.setId((long) roleName.ordinal() + 1);
        user.addRole(role);
        return UserPrincipal.from(user);
    }
}
