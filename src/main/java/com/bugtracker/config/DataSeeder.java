package com.bugtracker.config;

import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugHistory;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Comment;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.User;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.service.BugCodeGenerator;
import com.bugtracker.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates the development accounts and demonstration data on a fresh database.
 *
 * <p>Passwords below are development-only credentials and are documented in the
 * README. Disable seeding with {@code APP_SEED_DATA=false}.</p>
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // Development-only credentials.
    private static final String ADMIN_EMAIL = "admin@bugtracker.dev";
    private static final String ADMIN_PASSWORD = "Admin@12345";
    private static final String TESTER_EMAIL = "tester@bugtracker.dev";
    private static final String TESTER_PASSWORD = "Tester@12345";
    private static final String TESTER_TWO_EMAIL = "tester2@bugtracker.dev";
    private static final String TESTER_TWO_PASSWORD = "Tester2@12345";
    private static final String DEVELOPER_EMAIL = "developer@bugtracker.dev";
    private static final String DEVELOPER_PASSWORD = "Developer@12345";
    private static final String DEVELOPER_TWO_EMAIL = "developer2@bugtracker.dev";
    private static final String DEVELOPER_TWO_PASSWORD = "Developer2@12345";

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final BugRepository bugRepository;
    private final RoleService roleService;
    private final BugCodeGenerator bugCodeGenerator;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppProperties appProperties,
                      UserRepository userRepository,
                      BugRepository bugRepository,
                      RoleService roleService,
                      BugCodeGenerator bugCodeGenerator,
                      PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.bugRepository = bugRepository;
        this.roleService = roleService;
        this.bugCodeGenerator = bugCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.isSeedData()) {
            log.info("Seed data is disabled (app.seed-data=false).");
            return;
        }
        User admin = ensureUser("Priya Sharma", ADMIN_EMAIL, ADMIN_PASSWORD, RoleName.ROLE_ADMIN);
        User tester = ensureUser("Rahul Verma", TESTER_EMAIL, TESTER_PASSWORD, RoleName.ROLE_TESTER);
        User testerTwo = ensureUser("Neha Gupta", TESTER_TWO_EMAIL, TESTER_TWO_PASSWORD, RoleName.ROLE_TESTER);
        User developer = ensureUser("Arjun Mehta", DEVELOPER_EMAIL, DEVELOPER_PASSWORD, RoleName.ROLE_DEVELOPER);
        User developerTwo = ensureUser("Sara Khan", DEVELOPER_TWO_EMAIL, DEVELOPER_TWO_PASSWORD, RoleName.ROLE_DEVELOPER);

        if (bugRepository.count() > 0) {
            log.info("Bug data already present, skipping bug seeding.");
            return;
        }
        log.info("Seeding demonstration bugs...");

        createBug("Login button does not respond on the sign-in page",
                "Clicking the Login button after entering valid credentials produces no navigation and no error message.",
                "1. Open the login page\n2. Enter a valid username\n3. Enter a valid password\n4. Click Login",
                "The user should be signed in and redirected to the dashboard.",
                "The login request is never sent; the page stays on the sign-in screen.",
                "Chrome 124 / Windows 11",
                Severity.HIGH, Priority.URGENT, BugStatus.OPEN, tester, developer, null);

        createBug("Password reset email is never delivered",
                "Requesting a password reset for a registered account does not deliver any email.",
                "1. Open Forgot Password\n2. Enter a registered email address\n3. Submit the form",
                "A reset link email should arrive within a few minutes.",
                "No email is received and no entry appears in the outbound mail log.",
                "Firefox 125 / macOS 14",
                Severity.CRITICAL, Priority.URGENT, BugStatus.IN_PROGRESS, tester, developer, null);

        createBug("Bug list pagination resets the selected filters",
                "Changing the page while filters are applied clears every filter selection.",
                "1. Open the bug list\n2. Filter by severity HIGH\n3. Click page 2",
                "The HIGH filter should remain applied on page 2.",
                "All filters are cleared and the unfiltered list is shown.",
                "Edge 124 / Windows 11",
                Severity.MEDIUM, Priority.HIGH, BugStatus.IN_PROGRESS, testerTwo, developerTwo, null);

        createBug("Attachment preview shows a broken image icon",
                "PNG screenshots attached to a bug render as a broken image in the details page.",
                "1. Open any bug with a PNG attachment\n2. Expand the Evidence section",
                "The screenshot should be rendered inline.",
                "A broken image placeholder is displayed.",
                "Chrome 124 / Windows 11",
                Severity.MEDIUM, Priority.MEDIUM, BugStatus.RESOLVED, tester, developer,
                "Fixed the download endpoint so that the stored content type is honoured.");

        createBug("Dashboard total ignores closed bugs",
                "The Total Bugs card does not include bugs whose status is CLOSED.",
                "1. Close a bug\n2. Reload the dashboard",
                "The total should include every bug regardless of status.",
                "The total decreases by one after closing a bug.",
                "Safari 17 / macOS 14",
                Severity.HIGH, Priority.HIGH, BugStatus.CLOSED, testerTwo, developerTwo,
                "Statistics query now counts all rows; verified against the database.");

        createBug("Special characters in the title break the search filter",
                "Searching for a title containing an ampersand returns no results.",
                "1. Create a bug titled 'Export & Download fails'\n2. Search for 'Export & Download'",
                "The matching bug should be listed.",
                "The result table is empty.",
                "Chrome 124 / Windows 11",
                Severity.LOW, Priority.MEDIUM, BugStatus.OPEN, tester, null, null);

        createBug("Session expires immediately after signing in",
                "Users are returned to the login page a few seconds after a successful login.",
                "1. Sign in with a valid account\n2. Wait for five seconds\n3. Open the bug list",
                "The session should stay valid for the configured expiry window.",
                "The user is redirected to the login page almost immediately.",
                "Chrome 124 / Android 14",
                Severity.CRITICAL, Priority.URGENT, BugStatus.REOPENED, tester, developer,
                "Token lifetime read from the wrong configuration key.");

        createBug("Comment count is not refreshed after posting a comment",
                "The comment badge on the bug list keeps the previous count until a manual reload.",
                "1. Open a bug\n2. Add a comment\n3. Return to the bug list",
                "The comment badge should increase immediately.",
                "The badge still shows the previous number.",
                "Firefox 125 / Windows 11",
                Severity.LOW, Priority.LOW, BugStatus.OPEN, testerTwo, developer, null);

        log.info("Seeded {} demonstration bugs. Administrator account: {}", bugRepository.count(), ADMIN_EMAIL);
    }

    private User ensureUser(String fullName, String email, String rawPassword, RoleName roleName) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User(fullName, email, passwordEncoder.encode(rawPassword));
            user.addRole(roleService.getOrCreate(roleName));
            return userRepository.save(user);
        });
    }

    private void createBug(String title,
                           String description,
                           String steps,
                           String expected,
                           String actual,
                           String environment,
                           Severity severity,
                           Priority priority,
                           BugStatus status,
                           User reporter,
                           User assignee,
                           String resolution) {
        Bug bug = new Bug();
        bug.setBugCode(bugCodeGenerator.nextBugCode());
        bug.setTitle(title);
        bug.setDescription(description);
        bug.setStepsToReproduce(steps);
        bug.setExpectedResult(expected);
        bug.setActualResult(actual);
        bug.setEnvironment(environment);
        bug.setSeverity(severity);
        bug.setPriority(priority);
        bug.setStatus(status);
        bug.setReporter(reporter);
        bug.setAssignedDeveloper(assignee);
        bug.setResolution(resolution);

        bug.addHistory(new BugHistory("CREATED", null, "Bug reported with status OPEN", reporter));
        if (assignee != null) {
            bug.addHistory(new BugHistory("ASSIGNEE", null, assignee.getFullName(), reporter));
        }
        if (status != BugStatus.OPEN) {
            bug.addHistory(new BugHistory("STATUS", BugStatus.OPEN.name(), status.name(),
                    assignee == null ? reporter : assignee));
        }
        bug.addComment(new Comment(reporter, "Reproduced consistently on the latest build."));
        if (assignee != null) {
            bug.addComment(new Comment(assignee, "Investigating now, will update the status shortly."));
        }
        bugRepository.save(bug);
    }
}
