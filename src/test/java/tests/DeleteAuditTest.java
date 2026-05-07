package tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import pages.ScheduleInspectionPage;
import pages.ScheduleInspectionPage.ScheduledAuditRecord;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DeleteAuditTest extends BaseTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter UI_AUDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final String TARGET_TEMPLATE = "All question Template";
    private static final String TARGET_BUILDING = "B1";
    private static final String TARGET_LEVEL = "G";
    private static final String TARGET_ZONE = "Zone F";
    private static final String TARGET_AUDIT_DATE = "Jun 09, 2026";
    private static final String TARGET_AUDITOR = "Sudha Rai";

    @Test(description = "Verify deleting a scheduled audit from the paginated schedule grid")
    public void shouldDeleteScheduledAuditFromPaginatedGrid() {
        loginWithValidCredentials();
        if (scheduleInspectionPage == null) {
            scheduleInspectionPage = new ScheduleInspectionPage(page);
        }

        auditPage.openScheduleTab();
        auditPage.selectFirstBuildingFromTopBar();
        scheduleInspectionPage.waitForScheduleGrid();

        LocalDate targetDate = LocalDate.parse(TARGET_AUDIT_DATE, UI_AUDIT_DATE_FORMAT);
        scheduleInspectionPage.setScheduleListDateRange(format(targetDate), format(targetDate));
        scheduleInspectionPage.searchScheduleList(TARGET_ZONE);
        scheduleInspectionPage.waitForScheduleGrid();

        ScheduledAuditRecord deletedAudit = scheduleInspectionPage.deleteScheduledAudit(
                TARGET_TEMPLATE,
                TARGET_BUILDING,
                TARGET_LEVEL,
                TARGET_ZONE,
                TARGET_AUDIT_DATE,
                TARGET_AUDITOR);
        System.out.println("Deleted Audit Details: " + deletedAudit);
        
       Assert.assertTrue(
        deletedAudit.feedback().toLowerCase(Locale.ENGLISH)
        .contains("audit successfully deleted"),
        "Deleting a scheduled audit should show a deleted successfully message. Feedback="
        + deletedAudit.feedback());

        //Refresh and verify the deleted audit no longer appears in the schedule grid
        page.waitForTimeout(1000);
        page.reload();
        scheduleInspectionPage.waitForScheduleGrid();

        scheduleInspectionPage.setScheduleListDateRange(
        format(targetDate), format(targetDate));

        scheduleInspectionPage.searchScheduleList(TARGET_ZONE);

        scheduleInspectionPage.waitForScheduleGrid();

        Assert.assertFalse(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        deletedAudit.template(),
                        deletedAudit.building(),
                        deletedAudit.level(),
                        deletedAudit.zone(),
                        deletedAudit.auditDate(),
                        deletedAudit.auditor()),
                "Deleted scheduled audit should not exist on any paginated schedule-grid page.");
    }

    @Test(description = "Verify Cancel closes the Delete Audit drawer and does not delete audit")
    public void shouldCancelDeleteAudit() {

    loginWithValidCredentials();

    if (scheduleInspectionPage == null) {
        scheduleInspectionPage = new ScheduleInspectionPage(page);
    }

    auditPage.openScheduleTab();
    auditPage.selectFirstBuildingFromTopBar();
    scheduleInspectionPage.waitForScheduleGrid();

    // Apply filter
    LocalDate targetDate = LocalDate.parse(TARGET_AUDIT_DATE, UI_AUDIT_DATE_FORMAT);

    scheduleInspectionPage.setScheduleListDateRange(
            format(targetDate), format(targetDate));

    scheduleInspectionPage.searchScheduleList(TARGET_ZONE);
    scheduleInspectionPage.waitForScheduleGrid();

    // Step 1: Open delete drawer
    scheduleInspectionPage.openDeleteAuditDrawer(
            TARGET_TEMPLATE,
            TARGET_BUILDING,
            TARGET_LEVEL,
            TARGET_ZONE,
            TARGET_AUDIT_DATE,
            TARGET_AUDITOR
    );

    // Step 2: Click Cancel
    scheduleInspectionPage.clickCancelDelete();

    // Step 3: Verify drawer closed
    Assert.assertFalse(
            scheduleInspectionPage.isDeleteDrawerVisible(),
            "Delete drawer should be closed after clicking Cancel"
    );

    // Step 4: Reapply filters (UI resets sometimes)
    scheduleInspectionPage.setScheduleListDateRange(
            format(targetDate), format(targetDate));

    scheduleInspectionPage.searchScheduleList(TARGET_ZONE);
    scheduleInspectionPage.waitForScheduleGrid();

    // Step 5: Verify audit STILL EXISTS
    Assert.assertTrue(
            scheduleInspectionPage.hasScheduledAuditAcrossPages(
                    TARGET_TEMPLATE,
                    TARGET_BUILDING,
                    TARGET_LEVEL,
                    TARGET_ZONE,
                    TARGET_AUDIT_DATE,
                    TARGET_AUDITOR
            ),
            "Audit should NOT be deleted when Cancel is clicked"
    );
}

    private String format(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
