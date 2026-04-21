package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import pages.AuditPage;
import pages.LoginPage;
import pages.ScheduleInspectionPage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

public class BaseTest {
    protected static final String DEFAULT_URL = "https://www.smartclean.io/matrix/sso/#/login";
    private static final int NAVIGATION_RETRIES = 2;
    private static final int SCHEDULE_MODAL_RETRIES = 2;
    private static final DateTimeFormatter SCREENSHOT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected LoginPage loginPage;
    protected AuditPage auditPage;
    protected ScheduleInspectionPage scheduleInspectionPage;
    protected Path screenshotDirectory;
    protected final List<String> browserDiagnostics = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        System.out.println("Setup: creating Playwright");
        playwright = PlaywrightFactory.createPlaywright();
        System.out.println("Setup: launching browser");
        browser = PlaywrightFactory.launchBrowser(playwright);
        System.out.println("Setup: creating context");
        context = PlaywrightFactory.createContext(browser);
        System.out.println("Setup: creating page");
        page = PlaywrightFactory.createPage(context);
        registerPageDiagnostics();
        loginPage = new LoginPage(page);
        auditPage = new AuditPage(page);
        screenshotDirectory = Paths.get("target", "screenshots");
        try {
            Files.createDirectories(screenshotDirectory);
            System.out.println("Setup: screenshot directory ready at " + screenshotDirectory.toAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create screenshot directory: " + screenshotDirectory, ex);
        }
        System.out.println("Setup: navigating to login");
        navigateWithRetry(System.getProperty("matrix.base.url", DEFAULT_URL));
        System.out.println("Setup: navigation complete");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    protected void loginWithValidCredentials() {
        String username = System.getProperty("matrix.valid.username", System.getenv("MATRIX_VALID_USERNAME"));
        String password = System.getProperty("matrix.valid.password", System.getenv("MATRIX_VALID_PASSWORD"));
        String propertyName = System.getProperty("matrix.property.name", System.getenv("MATRIX_PROPERTY_NAME"));

        if (isBlank(username) || isBlank(password)) {
            throw new SkipException("Valid login credentials are not configured for this run.");
        }

        captureStep("01-login-page");
        loginPage.login(username, password);
        captureStep("02-after-login-submit");

        if (loginPage.isPropertySelectionDisplayed()) {
            captureStep("03-property-selection");
            if (isBlank(propertyName)) {
                loginPage.selectFirstPropertyAndLogin();
            } else {
                loginPage.selectPropertyAndLogin(propertyName);
            }
            captureStep("04-after-property-selection");
        }

        loginPage.waitForHomePage();
        loginPage.waitForHomeContentToRender();
        captureStep("05-post-login");
    }

    protected void openScheduleInspectionModal() {
        loginWithValidCredentials();
        if (scheduleInspectionPage == null) {
            scheduleInspectionPage = new ScheduleInspectionPage(page);
        }

        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= SCHEDULE_MODAL_RETRIES; attempt++) {
            try {
                openScheduleInspectionModalOnce();
                return;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt == SCHEDULE_MODAL_RETRIES) {
                    throw ex;
                }
                recoverSchedulePageAfterModalFailure(attempt);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    protected void captureStep(String stepName) {
        Path screenshot = screenshotDirectory.resolve(
                SCREENSHOT_STAMP.format(LocalDateTime.now()) + "_" + stepName + ".png");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshot)
                .setFullPage(true)
                .setType(ScreenshotType.PNG));
        Path htmlSnapshot = screenshotDirectory.resolve(
                screenshot.getFileName().toString().replace(".png", ".html"));
        Path textSnapshot = screenshotDirectory.resolve(
                screenshot.getFileName().toString().replace(".png", ".txt"));
        Path diagnosticsSnapshot = screenshotDirectory.resolve(
                screenshot.getFileName().toString().replace(".png", ".log"));
        try {
            Files.writeString(htmlSnapshot, page.content(), StandardCharsets.UTF_8);
            Files.writeString(textSnapshot, page.locator("body").innerText(), StandardCharsets.UTF_8);
            Files.writeString(diagnosticsSnapshot, String.join(System.lineSeparator(), browserDiagnostics), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to capture DOM snapshot for step: " + stepName, ex);
        }
        System.out.println("Screenshot captured: " + screenshot.toAbsolutePath());
        System.out.println("HTML captured: " + htmlSnapshot.toAbsolutePath());
        System.out.println("Text captured: " + textSnapshot.toAbsolutePath());
        System.out.println("Diagnostics captured: " + diagnosticsSnapshot.toAbsolutePath());
        System.out.println("Current URL: " + page.url());
    }

    private void registerPageDiagnostics() {
        page.onConsoleMessage(this::recordConsoleMessage);
        page.onPageError(error -> browserDiagnostics.add("PAGEERROR :: " + error));
        page.onRequestFailed(request -> browserDiagnostics.add(
                "REQUESTFAILED :: " + request.method() + " " + request.url() + " :: " + request.failure()));
    }

    private void recordConsoleMessage(ConsoleMessage message) {
        browserDiagnostics.add("CONSOLE " + message.type() + " :: " + message.text());
    }

    private void navigateWithRetry(String url) {
        PlaywrightException lastException = null;

        for (int attempt = 1; attempt <= NAVIGATION_RETRIES; attempt++) {
            try {
                loginPage.navigate(url);
                return;
            } catch (PlaywrightException ex) {
                lastException = ex;
                if (attempt == NAVIGATION_RETRIES) {
                    throw ex;
                }
                page.waitForTimeout(2000L * attempt);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void openScheduleInspectionModalOnce() {
        auditPage.openScheduleTab();
        auditPage.waitForSchedulePageReady();
        captureStep("06-schedule-tab");
        auditPage.selectFirstBuildingFromTopBar();
        captureStep("06b-schedule-building-selected");
        auditPage.clickScheduleInspectionButton();
        captureStep("07-after-schedule-inspection-click");
        scheduleInspectionPage.waitForModal();
        captureStep("08-schedule-audit-modal");
    }

    private void recoverSchedulePageAfterModalFailure(int attempt) {
        browserDiagnostics.add("RETRY :: Schedule Inspection modal open attempt " + attempt + " failed. Reloading schedule page.");
        page.reload();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        auditPage = new AuditPage(page);
        scheduleInspectionPage = new ScheduleInspectionPage(page);
        page.waitForTimeout(1000L * attempt);
    }
}
