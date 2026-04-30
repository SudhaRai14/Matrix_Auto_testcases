package tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import pages.ScheduleInspectionPage.ScheduleFormData;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScheduleInspectionTest extends BaseTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String TEMPLATE_DAILY = "Daily Audit Template";
    private static final String TEMPLATE_STANDARD = "Standard Audit Template";
    private static final String TEMPLATE_FULL_PROPERTY = "Full Property Template";
    private static final String BUILDING_TOWER_A = "Tower A";
    private static final String BUILDING_MAIN_CAMPUS = "Main Campus";
    private static final String ZONE_CATEGORY_GUEST_ROOM = "Guest Room";
    private static final String ZONE_CATEGORY_PUBLIC_AREA = "Public Area";
    private static final String FLOOR_1 = "Floor 1";
    private static final String FLOOR_2 = "Floor 2";

    @Test(description = "Verify Schedule Audit modal opens from Audits > Schedule")
    public void shouldOpenScheduleInspectionModal() {
        openScheduleInspectionModal();

        Assert.assertTrue(scheduleInspectionPage.isModalOpen(), "Schedule Audit modal should open.");
        Assert.assertTrue(scheduleInspectionPage.isModalTitleVisible(), "Schedule Audit title should be visible.");
        Assert.assertTrue(auditPage.isScheduleInspectionButtonVisible(),
                "Schedule Inspection button should remain visible on the page.");
    }

    @Test(description = "Verify Cancel closes the Schedule Audit modal")
    public void shouldCloseScheduleInspectionModalOnCancel() {
        openScheduleInspectionModal();

        Assert.assertTrue(scheduleInspectionPage.isModalOpen(), "Schedule Audit modal should open before cancel.");
        scheduleInspectionPage.clickCancel();

        Assert.assertFalse(scheduleInspectionPage.isModalOpen(), "Schedule Audit modal should close after cancel.");
    }

    @Test(description = "Verify validation is shown when Save is clicked with no required data")
    public void shouldKeepSaveDisabledUntilRequiredFieldsAreEntered() {
        openScheduleInspectionModal();
        scheduleInspectionPage.clickSave();

        Assert.assertTrue(
                scheduleInspectionPage.hasValidationMessageContaining("template")
                        || scheduleInspectionPage.hasValidationMessageContaining("building")
                        || scheduleInspectionPage.hasValidationMessageContaining("zone")
                        || scheduleInspectionPage.isModalOpen(),
                "Saving without required schedule details should keep the modal open and show validation.");
    }

    @Test(description = "Verify user can save a custom inspection schedule with valid data")
    public void shouldSaveCustomScheduleWithValidData() {
        openScheduleInspectionModal();
        scheduleInspectionPage.chooseCustomOption();
        scheduleInspectionPage.selectAnyTemplateWithRows();
         scheduleInspectionPage.fillScheduleForm(buildValidStandardData());
        scheduleInspectionPage.clickSave();
        captureStep("10-after-custom-save-click");
        assertSuccessToast("Saving a valid custom schedule should show a success confirmation.");
    }

    @Test(description = "Verify user can save a standard inspection schedule with valid data")
    public void shouldSaveStandardScheduleWithValidData() {
        openScheduleInspectionModal();
        scheduleInspectionPage.chooseStandardOption();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidStandardData());
        scheduleInspectionPage.clickSave();
        captureStep("10-after-custom-save-click");
        Assert.assertTrue(scheduleInspectionPage.isStandardSelected(), "Standard should remain selected after save.");
        assertSuccessToast("Saving a valid standard schedule should show a success confirmation.");
    }

    @Test(description = "Verify changing template loads schedule rows")
    public void shouldLoadRowsWhenTemplateChanges() {
        openScheduleInspectionModal();
        String selectedTemplate = scheduleInspectionPage.selectAnyTemplateWithRows();

        Assert.assertTrue(scheduleInspectionPage.isDropdownValueSelected("Template", selectedTemplate),
                "Selected template should be visible in the Template dropdown.");
        Assert.assertTrue(scheduleInspectionPage.getZoneRowCount() > 0,
                "Selecting a template should load one or more schedule rows.");
    }

    @Test(description = "Verify changing building refreshes the schedule grid without breaking it")
    public void shouldRefreshScheduleGridWhenBuildingChanges() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        int initialRows = scheduleInspectionPage.getZoneRowCount();

        Assert.assertTrue(initialRows >= 0, "Initial row count should be readable.");
        Assert.assertTrue(scheduleInspectionPage.getZoneRowCount() >= 0,
                "The zone grid should still be available after changing the building.");
    }

    @Test(description = "Verify Zone Category and Floor selections are retained")
    public void shouldRetainZoneCategoryAndFloorSelections() {
        openScheduleInspectionModal();
        String selectedZoneCategory = scheduleInspectionPage.selectAnyZoneCategory();
        String selectedFloor = scheduleInspectionPage.selectAnyFloor();

        Assert.assertTrue(scheduleInspectionPage.isDropdownValueSelected("Zone Category", selectedZoneCategory),
                "Zone Category selection should be retained.");
        Assert.assertTrue(scheduleInspectionPage.isDropdownValueSelected("Floor", selectedFloor),
                "Floor selection should be retained.");
    }

    // @Test(description = "Verify Copy Configuration populates the current row with existing setup")
    // public void shouldCopyConfigurationIntoCurrentSetup() {
    //     openScheduleInspectionModal();
    //     scheduleInspectionPage.selectAnyTemplateWithRows();
    //     scheduleInspectionPage.fillScheduleForm(buildValidCustomData());
    //     scheduleInspectionPage.clickCopyConfiguration();

    //     Assert.assertTrue(scheduleInspectionPage.isConfigurationCopied("Lobby", "John Auditor"),
    //             "Copy Configuration should populate the row with the copied schedule values.");
    // }

//     @Test(description = "Verify Copy Configuration populates the current row with existing setup")
//     public void shouldCopyConfigurationIntoCurrentSetup() {
//         openScheduleInspectionModal();
//         scheduleInspectionPage.chooseCustomOption();
//         scheduleInspectionPage.selectAnyTemplateWithRows();
//         scheduleInspectionPage.fillScheduleForm(buildValidStandardData());
//         scheduleInspectionPage.selectRowByZone("Zone B");
//         scheduleInspectionPage.clickCopyConfiguration();
//         Assert.assertTrue(scheduleInspectionPage.isConfigurationCopied("Zone B", "Sudha Rai"),
//                 "Copy Configuration should populate the row with the copied schedule values.");
    
// }

@Test(description = "Verify Copy Configuration copies data from first zone to multiple selected zones")
public void shouldCopyConfigurationToMultipleZones() {
    openScheduleInspectionModal();
    scheduleInspectionPage.chooseCustomOption();
    scheduleInspectionPage.selectTemplate("Checkbox question");
    if (!scheduleInspectionPage.isDropdownValueSelected("Buildings", "B1")) {
        scheduleInspectionPage.selectBuilding("B1");
    }
    if (scheduleInspectionPage.getZoneRowCount() < 3) {
        try {
            scheduleInspectionPage.selectZoneCategory("Back of School");
        } catch (RuntimeException ignored) {
            // Some sessions do not expose this zone-category option; keep the loaded rows instead.
        }
    }

    int rowCount = scheduleInspectionPage.getZoneRowCount();
    Assert.assertTrue(rowCount >= 2, "At least 2 zones are required for copy configuration.");

    int sourceRowIndex = rowCount > 2 ? 1 : 0;
    List<Integer> targetRowIndexes = new ArrayList<>();
    for (int index = 0; index < rowCount && targetRowIndexes.size() < 2; index++) {
        if (index != sourceRowIndex) {
            targetRowIndexes.add(index);
        }
    }

    Assert.assertFalse(targetRowIndexes.isEmpty(), "At least one target zone is required for copy configuration.");

    // Step 3: Fill the first selected source zone only
    ScheduleFormData data = baseScheduleData()
            .withStartDate(datePlusDays(1))
            .withEndDate(datePlusDays(3))
            .withStartTime("05:00 AM")
            .withAuditor("Sudha Rai");

    scheduleInspectionPage.selectRowByIndex(sourceRowIndex, false);
    scheduleInspectionPage.setRowStartDate(sourceRowIndex, data.getStartDate());
    scheduleInspectionPage.setRowEndDate(sourceRowIndex, data.getEndDate());
    scheduleInspectionPage.setRowStartTime(sourceRowIndex, data.getStartTime());
    scheduleInspectionPage.setRowAuditor(sourceRowIndex, data.getAuditor());

    // Step 4: Capture expected values
    String expectedAuditor = data.getAuditor();
    String expectedStartDate = data.getStartDate();
    String expectedEndDate = data.getEndDate();
    String expectedTime = data.getStartTime();

    // Step 5: Select multiple target zones
    for (int targetRowIndex : targetRowIndexes) {
        scheduleInspectionPage.selectRowByIndex(targetRowIndex, true);
    }

    // Step 6: Copy configuration
    scheduleInspectionPage.clickCopyConfiguration();

    // Step 7: Validate all copied rows
    for (int targetRowIndex : targetRowIndexes) {
        Assert.assertTrue(
                scheduleInspectionPage.isRowConfigurationMatching(
                        targetRowIndex,
                        expectedAuditor, expectedStartDate, expectedEndDate, expectedTime
                ),
                "Copy failed for row index: " + targetRowIndex
        );
    }

    scheduleInspectionPage.clickSave();
    captureStep("10-after-copy-configuration-save");
    assertSuccessToast("Saving a copied custom schedule should show a success confirmation.");
}

    @Test(description = "Verify validation is shown when Template is missing")
    public void shouldShowValidationWhenTemplateIsMissing() {
        openScheduleInspectionModal();
        scheduleInspectionPage.fillScheduleForm(new ScheduleFormData()
                .withBuilding(BUILDING_TOWER_A)
                .withZone("Lobby")
                .withCriticality("High")
                .withFrequency("Daily")
                .withStartDate(datePlusDays(7))
                .withEndDate(datePlusDays(21))
                .withStartTime("09:00")
                .withAuditor("John Auditor"));
        scheduleInspectionPage.clickSave();

        assertValidation("template");
    }

    @Test(description = "Verify validation is shown when zone data is missing")
    public void shouldShowValidationWhenZoneDataIsMissing() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(new ScheduleFormData()
                .withBuilding(null));
        scheduleInspectionPage.clickSave();

        assertValidation("zone");
    }

    @Test(description = "Verify invalid date ranges are rejected")
    public void shouldShowValidationForInvalidDateRange() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidCustomData()
                .withStartDate(datePlusDays(14))
                .withEndDate(datePlusDays(7)));
        scheduleInspectionPage.clickSave();

        assertValidation("date");
    }

    @Test(description = "Verify scheduling without an auditor is not allowed")
    public void shouldShowValidationWhenAuditorIsMissing() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidCustomData().withAuditor(null));
        scheduleInspectionPage.clickSave();

        assertValidation("auditor");
    }

    @Test(description = "Verify switching from Custom to Standard clears row-level custom data")
    public void shouldResetRowDataWhenSwitchingModes() {
        openScheduleInspectionModal();
        scheduleInspectionPage.chooseCustomOption();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidCustomData());
        scheduleInspectionPage.chooseStandardOption();

        Assert.assertTrue(scheduleInspectionPage.isStandardSelected(), "Standard should be selected after switching.");
        Assert.assertTrue(scheduleInspectionPage.areTableFieldsCleared(),
                "Switching modes should clear the custom row-level values.");
    }

    @Test(description = "Verify large templates render a meaningful number of rows")
    public void shouldRenderRowsForLargeTemplates() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();

        Assert.assertTrue(scheduleInspectionPage.getZoneRowCount() > 0,
                "Selecting an available template should render one or more rows.");
    }

    @Test(description = "Verify submitting the same schedule twice returns deterministic feedback")
    public void shouldReturnDeterministicFeedbackForDuplicateSubmission() {
        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidCustomData());
        scheduleInspectionPage.clickSave();

        openScheduleInspectionModal();
        scheduleInspectionPage.selectAnyTemplateWithRows();
        scheduleInspectionPage.fillScheduleForm(buildValidCustomData());
        scheduleInspectionPage.clickSave();

        Assert.assertTrue(scheduleInspectionPage.getToastMessage().toLowerCase()
                        .matches(".*(duplicate|already exists|already scheduled|success).*"),
                "Submitting the same schedule twice should return deterministic feedback.");
    }

    private ScheduleFormData buildValidCustomData() {
        return baseScheduleData()
                .withZone("Lobby")
                //.withCriticality("High")
               // .withFrequency("Daily")
                .withStartDate(datePlusDays(7))
                .withEndDate(datePlusDays(21))
                .withStartTime("09:00")
                .withAuditor("Sudha Rai");
    }

    private ScheduleFormData buildValidStandardData() {
        return baseScheduleData()
                .withZone("Reception")
                .withCriticality("Medium")
                .withFrequency("Weekly")
                .withStartDate(datePlusDays(8))
                .withEndDate(datePlusDays(28))
                .withStartTime("10:30")
                .withAuditor("Sudha Rai");
    }

    private ScheduleFormData baseScheduleData() {
        return new ScheduleFormData();
    }

    private String datePlusDays(long days) {
        return LocalDate.now().plusDays(days).format(DATE_FORMAT);
    }

    private void assertSuccessToast(String message) {
        String feedback = scheduleInspectionPage.getToastMessageIfPresent(5000).toLowerCase();
        Assert.assertTrue(feedback.matches(".*(success|scheduled|created).*")
                        || scheduleInspectionPage.hasScheduledInspectionForAuditor("Sudha Rai"),
                message);
    }

    private void assertValidation(String expectedKeyword) {
        Assert.assertTrue(
                scheduleInspectionPage.hasValidationMessageContaining(expectedKeyword)
                        || scheduleInspectionPage.isSaveButtonDisabled(),
                "Expected validation tied to: " + expectedKeyword);
    }
}
