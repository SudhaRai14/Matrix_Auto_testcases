package tests;

import org.testng.annotations.Test;
import pages.AuditPage;
import pages.ScheduleInspectionPage;
import utils.BaseTest;

public class AuditDetailsTest extends BaseTest {

    @Test(description = "Verify Audit Details drawer opens correctly from Schedule Inspection list")
    public void shouldViewAuditDetailsOfScheduledInspection() {

        loginWithValidCredentials();

        auditPage = new AuditPage(page);
        scheduleInspectionPage = new ScheduleInspectionPage(page);

        // Open Schedule tab
        auditPage.openScheduleTab();

        // Select building/property
        auditPage.selectFirstBuildingFromTopBar();

        // Wait for schedule grid
        scheduleInspectionPage.waitForScheduleGrid();

        scheduleInspectionPage.openFirstAuditDetailsDrawer();
        scheduleInspectionPage.assertAuditDetailsDrawerVisible();
        scheduleInspectionPage.assertAuditDetailsTabsVisible();
        scheduleInspectionPage.switchAuditDetailsTab("Activity log");
        scheduleInspectionPage.switchAuditDetailsTab("Details");
        scheduleInspectionPage.closeAuditDetailsDrawer();
    }
}
