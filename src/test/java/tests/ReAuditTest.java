package tests;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import pages.ScheduleInspectionPage;
import pages.ScheduleInspectionPage.ScheduledAuditGridRecord;
import pages.ScheduleInspectionPage.ScheduledAuditRecord;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ReAuditTest extends BaseTest {
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter UI_AUDIT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a, MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter UI_AUDIT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final String COMPLETED_STATUS = "Completed";
    private static final String EXPECTED_REAUDIT_STATUS = "Pending";

    @Test(description = "Verify completed audit can be re-audited and newly scheduled audit is shown")
    public void shouldReAuditForCompletedAudit() {
        openScheduleList();

        LocalDate completedAuditDate = resolveCompletedAuditDate();
        String completedAuditDateText = completedAuditDate.format(ISO_DATE_FORMAT);

        scheduleInspectionPage.setScheduleListDateRange(completedAuditDateText, completedAuditDateText);
        scheduleInspectionPage.searchScheduleList(COMPLETED_STATUS);
        scheduleInspectionPage.waitForScheduleGrid();

        ScheduledAuditRecord completedAudit = firstAuditWithStatus(COMPLETED_STATUS);
        LocalDate reAuditDate = parseAuditDate(completedAudit.auditDate()).plusDays(1);
        String reAuditDateText = reAuditDate.format(ISO_DATE_FORMAT);

        Assert.assertTrue(
                scheduleInspectionPage.isReAuditOptionAvailable(completedAudit),
                "Re-Audit option should be available for Completed audits.");

        ScheduledAuditRecord reAudit = scheduleInspectionPage.reAuditScheduledAuditDateAndAuditor(
                completedAudit,
                reAuditDateText,
                System.getProperty("matrix.edit.audit.auditor", System.getenv("MATRIX_EDIT_AUDIT_AUDITOR")));

        Assert.assertTrue(
                reAudit.feedback().toLowerCase(Locale.ENGLISH)
                        .matches(".*(success|scheduled|re[- ]?audit|created).*"),
                "Re-Audit should show a success/scheduled message. Feedback=" + reAudit.feedback());

        scheduleInspectionPage.setScheduleListDateRange(reAuditDateText, reAuditDateText);
        scheduleInspectionPage.searchScheduleList(reAudit.zone());
        scheduleInspectionPage.waitForScheduleGrid();

        ScheduledAuditGridRecord scheduledReAudit = scheduleInspectionPage.findScheduledAuditGridRecordAcrossPages(
                        reAudit.template(),
                        reAudit.building(),
                        reAudit.level(),
                        reAudit.zone(),
                        reAudit.auditDate(),
                        reAudit.auditor())
                .orElseThrow(() -> new AssertionError(
                        "Newly scheduled Re-Audit should exist in schedule grid for template="
                                + reAudit.template() + ", zone=" + reAudit.zone()
                                + ", auditor=" + reAudit.auditor()
                                + ", auditDate=" + reAudit.auditDate()));

        Assert.assertEquals(scheduledReAudit.template(), reAudit.template(), "Template should match.");
        Assert.assertEquals(scheduledReAudit.zone(), reAudit.zone(), "Zone should match.");
        Assert.assertEquals(scheduledReAudit.auditor(), reAudit.auditor(), "Auditor should match.");
        Assert.assertTrue(
                parseAuditDate(scheduledReAudit.auditDate()).equals(reAuditDate),
                "Audit Date should match. Actual=" + scheduledReAudit.auditDate());
        Assert.assertEquals(
                scheduledReAudit.status().toLowerCase(Locale.ENGLISH),
                EXPECTED_REAUDIT_STATUS.toLowerCase(Locale.ENGLISH),
                "Newly scheduled Re-Audit status should be Pending.");
    }

    @Test(description = "Verify clicking Cancel does not update auditor or audit date")
    public void shouldNotUpdateAuditWhenReAuditCancelled() {
        openScheduleList();

        LocalDate completedAuditDate = resolveCompletedAuditDate();
        String completedAuditDateText = completedAuditDate.format(ISO_DATE_FORMAT);

        scheduleInspectionPage.setScheduleListDateRange(completedAuditDateText, completedAuditDateText);
        scheduleInspectionPage.searchScheduleList(COMPLETED_STATUS);
        scheduleInspectionPage.waitForScheduleGrid();

        ScheduledAuditRecord completedAudit = firstAuditWithStatus(COMPLETED_STATUS);
        
        Assert.assertTrue(
                scheduleInspectionPage.isReAuditOptionAvailable(completedAudit),
                "Re-Audit option should be available for Completed audits.");

        String originalAuditDate = completedAudit.auditDate();
        String originalAuditor = completedAudit.auditor();
        String updatedDateText = parseAuditDate(originalAuditDate).plusDays(2).format(ISO_DATE_FORMAT);

        scheduleInspectionPage.openReAuditDrawer(completedAudit);
        String updatedAuditor = scheduleInspectionPage.getAnyDifferentAuditor(originalAuditor);
        scheduleInspectionPage.updateAuditDate(updatedDateText);
        scheduleInspectionPage.updateAuditAuditor(updatedAuditor);

        scheduleInspectionPage.cancelEditAudit();
        scheduleInspectionPage.waitForEditDrawerToClose();

        String originalDateText = parseAuditDate(originalAuditDate).format(ISO_DATE_FORMAT);
        scheduleInspectionPage.setScheduleListDateRange(originalDateText, originalDateText);
        scheduleInspectionPage.searchScheduleList(completedAudit.zone());
        scheduleInspectionPage.waitForScheduleGrid();

        Assert.assertTrue(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        completedAudit.template(),
                        completedAudit.building(),
                        completedAudit.level(),
                        completedAudit.zone(),
                        completedAudit.auditDate(),
                        completedAudit.auditor()
                ),
                "Audit should retain original values after clicking Cancel."
        );

        scheduleInspectionPage.setScheduleListDateRange(updatedDateText, updatedDateText);
        scheduleInspectionPage.searchScheduleList(completedAudit.zone());
        scheduleInspectionPage.waitForScheduleGrid();

        Assert.assertFalse(
                scheduleInspectionPage.hasScheduledAuditAcrossPages(
                        completedAudit.template(),
                        completedAudit.building(),
                        completedAudit.level(),
                        completedAudit.zone(),
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
    }

    private ScheduledAuditRecord firstAuditWithStatus(String status) {
        return scheduleInspectionPage.findFirstScheduledAuditWithStatusAcrossPages(status)
                .orElseThrow(() -> new SkipException("No scheduled audit found with status: " + status));
    }

    private LocalDate resolveCompletedAuditDate() {
        String configuredDate = System.getProperty("matrix.reaudit.completed.date",
                System.getenv("MATRIX_REAUDIT_COMPLETED_DATE"));
        if (configuredDate != null && !configuredDate.isBlank()) {
            return parseAuditDate(configuredDate);
        }
        return LocalDate.now();
    }

    private LocalDate parseAuditDate(String auditDate) {
        try {
            return java.time.LocalDateTime
                    .parse(auditDate, UI_AUDIT_DATE_TIME_FORMAT)
                    .toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(auditDate, UI_AUDIT_DATE_FORMAT);
            } catch (DateTimeParseException ignoredAgain) {
                return LocalDate.parse(auditDate, ISO_DATE_FORMAT);
            }
        }
    }
}
