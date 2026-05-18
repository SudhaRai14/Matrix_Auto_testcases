package tests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.annotations.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import pages.AuditPage;
import pages.ScheduleInspectionPage;
import utils.BaseTest;

public class ViewReportTest extends BaseTest {

    @Test(description = "Verify user can view completed audit report from schedule list")
    public void shouldViewCompletedAuditReport() {

        loginWithValidCredentials();

        auditPage = new AuditPage(page);
        scheduleInspectionPage = new ScheduleInspectionPage(page);

        // ============================================
        // Open Schedule Tab
        // ============================================

        auditPage.openScheduleTab();

        auditPage.selectFirstBuildingFromTopBar();

        scheduleInspectionPage.waitForScheduleGrid();

        // ============================================
        // Select Date Range
        // ============================================

        scheduleInspectionPage.setScheduleListDateRange(
                "2026-05-01",
                "2026-05-31");

        scheduleInspectionPage.waitForScheduleGrid();

        // ============================================
        // Filter Status = Completed
        // ============================================

        Locator statusFilterButton = page.getByRole(
                AriaRole.CELL,
                new Page.GetByRoleOptions()
                        .setName("Status filter"))
                .getByRole(AriaRole.BUTTON);

        assertThat(statusFilterButton).isVisible();

        statusFilterButton.click();

        Locator completedOption = page.getByText(
                "Completed",
                new Page.GetByTextOptions().setExact(true));

        assertThat(completedOption).isVisible();

        completedOption.click();

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("OK"))
                .click();

        scheduleInspectionPage.waitForScheduleGrid();

        Page reportPage = scheduleInspectionPage.openFirstViewReportPage();

        // ============================================
        // Validate Report Opened
        // ============================================

        scheduleInspectionPage.assertReportPageLoaded(reportPage);

        // ============================================
        // Validate Report Content
        // ============================================

        scheduleInspectionPage.assertReportSummaryVisible(reportPage);

        // ============================================
        // Validate Report is Read-only
        // ============================================

        scheduleInspectionPage.assertReportReadOnly(reportPage);

        // ============================================
        // Close Report Page
        // ============================================

        reportPage.close();

        assertThat(page).hasURL(
                java.util.regex.Pattern.compile(".*schedule.*"));
    }
}
