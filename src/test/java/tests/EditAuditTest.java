package tests;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import pages.ScheduleInspectionPage;
import pages.ScheduleInspectionPage.ScheduledAuditRecord;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class EditAuditTest extends BaseTest {
    
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter UI_AUDIT_DATE_TIME_FORMAT =DateTimeFormatter.ofPattern("hh:mm a, MMM dd, yyyy", Locale.ENGLISH);
    private static final String PENDING_STATUS = "Pending";

    @Test(description = "Verify pending audit allows editing auditor and audit date")
    public void shouldEditAuditorAndAuditDateForPendingAudit() {
        openScheduleList();
        ScheduledAuditRecord pendingAudit = firstAuditWithStatus(PENDING_STATUS);
        LocalDate updatedAuditDate = parseAuditDate(pendingAudit.auditDate()).plusDays(1);
        String updatedAuditDateText = updatedAuditDate.format(ISO_DATE_FORMAT);

        scheduleInspectionPage.assertEditAuditOptionAvailable(pendingAudit);

        ScheduledAuditRecord updatedAudit = scheduleInspectionPage.editScheduledAuditDateAndAuditor(
                pendingAudit,
                updatedAuditDateText,
                System.getProperty("matrix.edit.audit.auditor", System.getenv("MATRIX_EDIT_AUDIT_AUDITOR")));

        scheduleInspectionPage.setScheduleListDateRange(updatedAuditDateText, updatedAuditDateText);
        scheduleInspectionPage.searchScheduleList(updatedAudit.zone());
        scheduleInspectionPage.waitForScheduleGrid();

        Assert.assertTrue(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        updatedAudit.template(),
                        updatedAudit.building(),
                        updatedAudit.level(),
                        updatedAudit.zone(),
                        updatedAudit.auditDate(),
                        updatedAudit.auditor()),
                "Updated pending audit should be visible with the edited auditor and audit date.");
    }

    @Test(description = "Verify clicking Cancel does not update auditor or audit date")
    public void shouldNotUpdateAuditWhenEditCancelled() {

        openScheduleList();

        ScheduledAuditRecord originalAudit =
                firstAuditWithStatus(PENDING_STATUS);

        // Original values
        String originalAuditDate = originalAudit.auditDate();
        String originalAuditor = originalAudit.auditor();

        // New values for edit attempt
        LocalDate updatedDate =
                parseAuditDate(originalAuditDate).plusDays(2);

        String updatedDateText =
                updatedDate.format(ISO_DATE_FORMAT);

        // Open edit drawer
        scheduleInspectionPage.openEditAuditDrawer(originalAudit);

        String updatedAuditor =
                scheduleInspectionPage.getAnyDifferentAuditor(originalAuditor);

        // Modify values
        scheduleInspectionPage.updateAuditDate(updatedDateText);

        scheduleInspectionPage.updateAuditAuditor(updatedAuditor);

        // Click Cancel
        scheduleInspectionPage.cancelEditAudit();

        // Wait for drawer close
        scheduleInspectionPage.waitForEditDrawerToClose();

        // Reapply original filters
        LocalDate originalDate =
                parseAuditDate(originalAuditDate);

        String originalDateText =
                originalDate.format(ISO_DATE_FORMAT);

        scheduleInspectionPage.setScheduleListDateRange(
                originalDateText,
                originalDateText);

        scheduleInspectionPage.searchScheduleList(originalAudit.zone());

        scheduleInspectionPage.waitForScheduleGrid();

        // Verify ORIGINAL audit still exists
        Assert.assertTrue(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        originalAudit.template(),
                        originalAudit.building(),
                        originalAudit.level(),
                        originalAudit.zone(),
                        originalAudit.auditDate(),
                        originalAudit.auditor()
                ),
                "Audit should retain original values after clicking Cancel."
        );

        // Verify UPDATED audit does NOT exist
        Assert.assertFalse(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        originalAudit.template(),
                        originalAudit.building(),
                        originalAudit.level(),
                        originalAudit.zone(),
                        updatedDateText,
                        updatedAuditor
                ),
                "Audit should NOT be updated when Cancel is clicked."
        );
    }

    private void openScheduleList() {
        loginWithValidCredentials();
        if (scheduleInspectionPage == null) {
            scheduleInspectionPage = new ScheduleInspectionPage(page);
        }

        auditPage.openScheduleTab();
        auditPage.selectFirstBuildingFromTopBar();
        scheduleInspectionPage.waitForScheduleGrid();
        scheduleInspectionPage.assertScheduleGridVisible();
    }

    private ScheduledAuditRecord firstAuditWithStatus(String status) {
        return scheduleInspectionPage.findFirstScheduledAuditWithStatusAcrossPages(status)
                .orElseThrow(() -> new SkipException("No scheduled audit found with status: " + status));
    }

    private LocalDate parseAuditDate(String auditDate) {

    try {
        // UI format: 10:30 AM, May 08, 2026
        return java.time.LocalDateTime
                .parse(auditDate, UI_AUDIT_DATE_TIME_FORMAT)
                .toLocalDate();

    } catch (DateTimeParseException ignored) {

        try {
            // UI format without time
            return LocalDate.parse(
                    auditDate,
                    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));

        } catch (DateTimeParseException ignoredAgain) {

            // ISO fallback
            return LocalDate.parse(auditDate, ISO_DATE_FORMAT);
        }
    }
    }
}
