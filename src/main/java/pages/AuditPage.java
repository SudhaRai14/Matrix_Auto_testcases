package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditPage {
    private static final double DEFAULT_TIMEOUT_MS = 20000;
    private static final int OPEN_AUDITS_RETRIES = 2;
    private static final String DEFAULT_ACCOUNT_ID = "0fd3e86b6e424bf383a2e02cf1281bec";
    private static final Pattern HOME_ROUTE_PATTERN = Pattern.compile(".*/#/home\\?org=([^&]+)&propId=([^&]+).*");
    private static final Pattern DASHBOARD_ROUTE_PATTERN = Pattern.compile(".*/#/dashboard/([^/]+)/([^/]+)/([^/?#]+).*");
    private static final Pattern SCHEDULE_ROUTE_PATTERN = Pattern.compile(".*/#/schedule/([^/]+)/([^/]+)/([^/?#]+).*");

    private final Page page;
    private final Locator auditsModuleLink;
    private final Locator buildingDropdown;
    private final Locator buildingOptions;
    private final Locator dashboardTab;
    private final Locator scheduleTab;
    private final Locator scheduleInspectionButton;

    public AuditPage(Page page) {
        this.page = page;
        this.auditsModuleLink = page.getByText("AuditsAudits module can be")
        .or(page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Audits")))
        .or(page.getByText("Audits", new Page.GetByTextOptions().setExact(true)));


        this.buildingDropdown = page.locator(
                "span:visible:has-text('Building :') + * .ant-select-selector, " +
                        "span:visible:has-text('Building :') + * .ant-select-selection-item, " +
                        ".top-navbar:has-text('Building :') .ant-select-selector, " +
                        ".top-navbar:has-text('Building :') .ant-select-selection-item")
                .first();
        this.buildingOptions = page.locator(".ant-select-dropdown [role='option'], .ant-select-item-option");

        this.dashboardTab = page.locator(
                ".ant-menu-title-content:has-text('Dashboard'), " +
                        "a:visible:has-text('Dashboard'), " +
                        "button:visible:has-text('Dashboard'), " +
                        "[role='tab']:visible:has-text('Dashboard'), " +
                        ".ant-tabs-tab:visible:has-text('Dashboard')")
                .first()
                .or(page.getByRole(AriaRole.TAB,
                        new Page.GetByRoleOptions().setName(Pattern.compile("^dashboard$", Pattern.CASE_INSENSITIVE))).first())
                .or(page.getByRole(AriaRole.LINK,
                        new Page.GetByRoleOptions().setName(Pattern.compile("^dashboard$", Pattern.CASE_INSENSITIVE))).first());

        this.scheduleTab = page.locator(
                ".ant-menu-title-content:has-text('Schedule'), " +
                        "a:visible:has-text('Schedule'), " +
                        "button:visible:has-text('Schedule'), " +
                        "[role='tab']:visible:has-text('Schedule'), " +
                        ".ant-tabs-tab:visible:has-text('Schedule')")
                .first()
                .or(page.getByRole(AriaRole.TAB,
                        new Page.GetByRoleOptions().setName(Pattern.compile("^schedule$", Pattern.CASE_INSENSITIVE))).first())
                .or(page.getByRole(AriaRole.LINK,
                        new Page.GetByRoleOptions().setName(Pattern.compile("^schedule$", Pattern.CASE_INSENSITIVE))).first());
        this.scheduleInspectionButton = page.locator(
                "button.ant-btn.ant-btn-primary:visible:has-text('Schedule Inspection'), " +
                        "button:visible:has-text('Schedule Inspection'), " +
                        "a:visible:has-text('Schedule Inspection'), " +
                        "[role='button']:visible:has-text('Schedule Inspection')")
                .first()
                .or(page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(Pattern.compile("schedule inspection", Pattern.CASE_INSENSITIVE))).first());
    }

    public void navigateToScheduleTab() {
        openAuditsModule();
        openScheduleTab();
    }

    public void openAuditsModule() {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= OPEN_AUDITS_RETRIES; attempt++) {
            try {
                auditsModuleLink.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
                PlaywrightAssertions.assertThat(auditsModuleLink).isVisible();
                auditsModuleLink.click();
                waitForAuditsPage();
                return;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < OPEN_AUDITS_RETRIES) {
                    page.waitForTimeout(2000L * attempt);
                }
            }
        }

        navigateDirectlyToAudits();
    }

    public void openScheduleTab() {
        try {
            if (!page.url().contains("/auditsv2/")) {
                openAuditsModule();
            }

            scheduleTab.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
            scheduleTab.click();
            waitForSchedulePage();
        } catch (RuntimeException ex) {
            navigateDirectlyToSchedule();
        }
        if (!hasVisibleScheduleInspectionButton()) {
            throw new IllegalStateException("Schedule page loaded, but the Schedule Inspection button was not visible.");
        }
    }

    public void selectFirstBuildingFromTopBar() {
        try {
            buildingDropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
            buildingDropdown.click();
            Locator firstBuilding = buildingOptions.first();
            firstBuilding.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
            firstBuilding.click();
            page.waitForTimeout(1000);
        } catch (RuntimeException ignored) {
            // Some sessions may already have a default building applied.
        }
    }

    public void clickScheduleInspection() {
        navigateToScheduleTab();
        clickScheduleInspectionButton();
    }

    public void clickScheduleInspectionButton() {
        waitForSchedulePageReady();
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Locator button = findVisibleScheduleInspectionButton();
                button.click(new Locator.ClickOptions().setForce(true).setTimeout(DEFAULT_TIMEOUT_MS));
                if (waitForVisibleScheduleDialog(5000)) {
                    return;
                }

                button.evaluate("element => element.click()");
                if (waitForVisibleScheduleDialog(5000)) {
                    return;
                }
                page.waitForTimeout(1000L * attempt);
            } catch (RuntimeException ex) {
                lastException = ex;
                page.waitForTimeout(750L * attempt);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IllegalStateException("Schedule Inspection button was clicked, but the Schedule Audit dialog did not open.");
    }

    public boolean isScheduleInspectionButtonVisible() {
        return hasVisibleScheduleInspectionButton();
    }

    public void waitForSchedulePageReady() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        waitForSchedulePage();
        page.waitForTimeout(1000);
    }

    private void waitForAuditsPage() {
        page.waitForURL("**/auditsv2/**", new Page.WaitForURLOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        waitForAuditsNavigationToRender();
    }

    private void waitForSchedulePage() {
        page.waitForURL("**/#/schedule/**", new Page.WaitForURLOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (hasVisibleScheduleInspectionButton()) {
                return;
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Schedule page loaded, but the Schedule Inspection button never became visible.");
    }

    private void navigateDirectlyToAudits() {
        if (page.url().contains("/auditsv2/")) {
            return;
        }

        RouteContext routeContext = resolveRouteContext();
        String fallbackUrl = String.format(
                "https://www.smartclean.io/matrix/auditsv2/v3_9/#/dashboard/%s/%s/%s",
                routeContext.org(),
                routeContext.propertyId(),
                routeContext.accountId());
        page.navigate(fallbackUrl);
        waitForAuditsPage();
    }

    private void navigateDirectlyToSchedule() {
        RouteContext routeContext = resolveRouteContext();
        String scheduleUrl = String.format(
                "https://www.smartclean.io/matrix/auditsv2/v3_9/#/schedule/%s/%s/%s",
                routeContext.org(),
                routeContext.propertyId(),
                routeContext.accountId());
        page.navigate(scheduleUrl);
        waitForSchedulePage();
    }

    private void waitForAuditsNavigationToRender() {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (isVisible(dashboardTab) || isVisible(scheduleTab) || isVisible(scheduleInspectionButton)) {
                return;
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Audits page loaded, but Dashboard/Schedule navigation did not render.");
    }

    private boolean isVisible(Locator locator) {
        try {
            return locator.count() > 0 && locator.first().isVisible();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean hasAttachedScheduleDialog() {
        Locator dialog = page.locator(".ant-modal-root .ant-modal.newSchedule, .ant-modal-root [role='dialog']").first();
        try {
            return dialog.count() > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean hasVisibleScheduleDialog() {
        Locator dialog = page.locator(
                ".ant-modal-root .ant-modal.newSchedule:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active), " +
                        ".ant-modal-root [role='dialog']:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active)")
                .first();
        try {
            return dialog.count() > 0 && dialog.isVisible();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean waitForVisibleScheduleDialog(double timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (hasVisibleScheduleDialog()) {
                return true;
            }
            page.waitForTimeout(250);
        }
        return false;
    }

    private boolean hasVisibleScheduleInspectionButton() {
        try {
            return findVisibleScheduleInspectionButton().isVisible();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Locator findVisibleScheduleInspectionButton() {
        Locator candidates = page.locator(
                "button.ant-btn.ant-btn-primary:has-text('Schedule Inspection'), " +
                        "button:has-text('Schedule Inspection'), " +
                        "a:has-text('Schedule Inspection'), " +
                        "[role='button']:has-text('Schedule Inspection')");

        int count = candidates.count();
        for (int index = 0; index < count; index++) {
            Locator candidate = candidates.nth(index);
            if (candidate.isVisible()) {
                return candidate;
            }
        }

        Locator roleButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("schedule inspection", Pattern.CASE_INSENSITIVE))).first();
        roleButton.waitFor(new Locator.WaitForOptions().setTimeout(1000));
        return roleButton;
    }

    private RouteContext resolveRouteContext() {
        String currentUrl = page.url();
        return matchRoute(currentUrl, SCHEDULE_ROUTE_PATTERN)
                .or(() -> matchRoute(currentUrl, DASHBOARD_ROUTE_PATTERN))
                .or(() -> matchHomeRoute(currentUrl))
                .orElseThrow(() -> new IllegalStateException("Unable to derive audits route details from current URL: " + currentUrl));
    }

    private Optional<RouteContext> matchRoute(String currentUrl, Pattern pattern) {
        Matcher matcher = pattern.matcher(currentUrl);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new RouteContext(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3)));
    }

    private Optional<RouteContext> matchHomeRoute(String currentUrl) {
        Matcher matcher = HOME_ROUTE_PATTERN.matcher(currentUrl);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new RouteContext(
                matcher.group(1),
                matcher.group(2),
                resolveAccountId()));
    }

    private String resolveAccountId() {
        String accountId = System.getProperty("matrix.account.id", System.getenv("MATRIX_ACCOUNT_ID"));
        if (accountId == null || accountId.isBlank()) {
            return DEFAULT_ACCOUNT_ID;
        }
        return accountId;
    }

    private record RouteContext(String org, String propertyId, String accountId) {
    }
}
