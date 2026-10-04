package com.bugtracker.service;

import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.RoleName;
import com.bugtracker.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Row level authorization rules for bug operations.
 *
 * <table>
 *   <caption>Permission matrix</caption>
 *   <tr><th>Operation</th><th>ADMIN</th><th>TESTER</th><th>DEVELOPER</th></tr>
 *   <tr><td>Create bug</td><td>yes</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>Edit bug fields</td><td>yes</td><td>yes</td><td>assigned bugs only</td></tr>
 *   <tr><td>Change status</td><td>any transition</td><td>verify (close/reopen)</td><td>assigned bugs only</td></tr>
 *   <tr><td>Change priority / severity</td><td>yes</td><td>yes</td><td>no</td></tr>
 *   <tr><td>Assign developer</td><td>yes</td><td>no</td><td>no</td></tr>
 *   <tr><td>Delete bug</td><td>yes</td><td>no</td><td>no</td></tr>
 *   <tr><td>Comment / upload evidence</td><td>yes</td><td>yes</td><td>yes</td></tr>
 * </table>
 */
@Service
public class BugAuthorizationService {

    public void assertCanEdit(Bug bug, UserPrincipal actor) {
        requireAuthenticated(actor);
        if (isAdmin(actor) || isTester(actor)) {
            return;
        }
        if (isDeveloper(actor) && isAssignedTo(actor, bug)) {
            return;
        }
        throw denied("edit", bug);
    }

    public void assertCanDelete(Bug bug, UserPrincipal actor) {
        requireAuthenticated(actor);
        if (isAdmin(actor)) {
            return;
        }
        throw denied("delete", bug);
    }

    public void assertCanAssign(Bug bug, UserPrincipal actor) {
        requireAuthenticated(actor);
        if (isAdmin(actor)) {
            return;
        }
        throw denied("assign", bug);
    }

    public void assertCanChangePriorityOrSeverity(Bug bug, UserPrincipal actor) {
        requireAuthenticated(actor);
        if (isAdmin(actor) || isTester(actor)) {
            return;
        }
        throw denied("change priority or severity of", bug);
    }

    /**
     * Developers move their own bugs through the workflow (for example
     * IN_PROGRESS to RESOLVED). Testers verify by closing or reopening a bug.
     * Administrators are unrestricted.
     */
    public void assertCanChangeStatus(Bug bug, BugStatus targetStatus, UserPrincipal actor) {
        requireAuthenticated(actor);
        if (isAdmin(actor)) {
            return;
        }
        if (isDeveloper(actor) && isAssignedTo(actor, bug)) {
            return;
        }
        if (isTester(actor)
                && bug.getStatus() == BugStatus.RESOLVED
                && (targetStatus == BugStatus.CLOSED || targetStatus == BugStatus.REOPENED)) {
            return;
        }
        throw new AccessDeniedException(
                "Access denied. Only the assigned developer (or a tester verifying a resolved bug) "
                        + "may move " + bug.getBugCode() + " to " + targetStatus + ".");
    }

    public void assertCanComment(Bug bug, UserPrincipal actor) {
        requireAuthenticated(actor);
    }

    public boolean isAdmin(UserPrincipal actor) {
        return actor != null && actor.getRole() == RoleName.ROLE_ADMIN;
    }

    public boolean isTester(UserPrincipal actor) {
        return actor != null && actor.getRole() == RoleName.ROLE_TESTER;
    }

    public boolean isDeveloper(UserPrincipal actor) {
        return actor != null && actor.getRole() == RoleName.ROLE_DEVELOPER;
    }

    public boolean isAssignedTo(UserPrincipal actor, Bug bug) {
        return bug.getAssignedDeveloper() != null
                && actor != null
                && actor.getId().equals(bug.getAssignedDeveloper().getId());
    }

    private static void requireAuthenticated(UserPrincipal actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication is required to access this resource.");
        }
    }

    private static AccessDeniedException denied(String action, Bug bug) {
        return new AccessDeniedException(
                "Access denied. Your role is not allowed to " + action + " " + bug.getBugCode() + ".");
    }
}
