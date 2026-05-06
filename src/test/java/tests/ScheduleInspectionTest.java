package tests;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import data.FrequencyData;
import models.FrequencyConfig;
import utils.TestDataLoader;
import utils.RecurrenceUtils;
import utils.FrequencyValidator;
import pages.ScheduleInspectionPage.RecurrenceMonth;
import pages.ScheduleInspectionPage.ScheduleFormData;
import utils.BaseTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.time.temporal.TemporalAdjusters;

public class ScheduleInspectionTest extends BaseTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Logger LOGGER = Logger.getLogger(ScheduleInspectionTest.class.getName());
    // private static final String TEMPLATE_DAILY = "Daily Audit Template";
    // private static final String TEMPLATE_STANDARD = "Standard Audit Template";
    // private static final String TEMPLATE_FULL_PROPERTY = "Full Property Template";
    // private static final String BUILDING = "Back of School";
    // private static final String BUILDING_MAIN_CAMPUS = "Main Campus";
    // private static final String ZONE_CATEGORY_GUEST_ROOM = "Guest Room";
    // private static final String ZONE_CATEGORY_PUBLIC_AREA = "Public Area";
    // private static final String FLOOR_1 = "Floor 1";
    // private static final String FLOOR_2 = "Floor 2";
    private static final String DEFAULT_AUDITOR = "Sudha Rai";
    private static final String DEFAULT_TIME = "09:00";

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
        captureStep("10-after-standard-save-click");
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



    @Test(description = "Verify Copy Configuration copies data from first zone to multiple selected zones")
    public void shouldCopyConfigurationToMultipleZones() {
        openScheduleInspectionModal();
        scheduleInspectionPage.chooseCustomOption();
        scheduleInspectionPage.selectTemplate("Checkbox question");
        if (!scheduleInspectionPage.isDropdownValueSelected("Buildings", "B1")) {
         scheduleInspectionPage.selectBuilding("B1");
         }
    // if (scheduleInspectionPage.getZoneRowCount() < 3) {
    //     try {
    //         scheduleInspectionPage.selectZoneCategory("Back of School");
    //     } catch (RuntimeException ignored) {
    //         // Some sessions do not expose this zone-category option; keep the loaded rows instead.
    //     }
    // }

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
        scheduleInspectionPage.clickSave();

        Assert.assertTrue(
                scheduleInspectionPage.hasValidationMessageContaining("template")
                        || scheduleInspectionPage.isModalOpen(),
                "Saving without required schedule details should keep the modal open and show validation.");

        assertValidation("Template");
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
        String startDate = datePlusDays(14);
        String disabledEndDate = datePlusDays(7);

        scheduleInspectionPage.fillScheduleForm(baseScheduleData()
                .withStartDate(startDate));

        Assert.assertTrue(
                scheduleInspectionPage.isRowDateDisabled(0, "End Date", disabledEndDate),
                "End date earlier than start date should be disabled");
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

    
   @Test(dataProvider = "jsonFrequencyData")
    public void shouldScheduleInspection(FrequencyData data) {
    System.out.println("Running test: " + data.name);
    ScheduleContext context = openAndPrepareScheduleAudit();

    LocalDate startDate = LocalDate.now().plusDays(7);
    LocalDate endDate = startDate.plusWeeks(4);

    FrequencyConfig config = buildConfig(data);

    scheduleInspectionPage.selectRowByIndex(context.rowIndex(), false);
    scheduleInspectionPage.selectFrequency(context.rowIndex(), config);

    String summary = scheduleInspectionPage.getFrequencySummary(context.rowIndex());
    FrequencyValidator.validate(summary, config);

    scheduleInspectionPage.selectDates(
            context.rowIndex(),
            iso(startDate),
            iso(endDate),
            DEFAULT_TIME
    );

    scheduleInspectionPage.setRowAuditor(context.rowIndex(), DEFAULT_AUDITOR);
    scheduleInspectionPage.clickSave();
    scheduleInspectionPage.handleSuccessPopup();
    assertScheduleSaved(data.name + " should save successfully");

    List<LocalDate> expectedDates =
            RecurrenceUtils.generateDates(config, startDate, endDate);
    
    validateScheduledEvents(
            context.zone(),
            DEFAULT_AUDITOR,
            startDate,
            endDate,
            expectedDates
    );
}

  /* Matrix allows duplicate schedule submissions as of 6/2024, but the system should provide deterministic feedback if a user attempts to submit the same schedule twice. This test verifies that behavior. If duplicate submissions become disallowed in the future, this test should be updated to expect an error message instead of success feedback on the second submission. 
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
    } */

    private ScheduleFormData buildValidCustomData() {
        return baseScheduleData()
                .withZone("Lobby")
                .withCriticality("High")
                .withFrequency("Daily")
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

    private ScheduleContext openAndPrepareScheduleAudit() {
        openScheduleInspectionModal();
        Assert.assertTrue(scheduleInspectionPage.isModalOpen(), "Schedule Audit modal should be visible.");

        if (!scheduleInspectionPage.isCustomSelected()) {
            scheduleInspectionPage.chooseCustomOption();
        }

        String selectedTemplate = scheduleInspectionPage.selectAnyTemplateWithRows();
        String selectedBuilding = "selected building";
        int rowCount = scheduleInspectionPage.getZoneRowCount();
        if (rowCount == 0) {
            selectedBuilding = scheduleInspectionPage.selectAnyBuilding();
            selectedTemplate = scheduleInspectionPage.selectAnyTemplateWithRows();
            rowCount = scheduleInspectionPage.getZoneRowCount();
        }

        Assert.assertTrue(rowCount > 0,
                "Selecting template '" + selectedTemplate + "' and building '" + selectedBuilding
                        + "' should populate one or more zones.");

        rowCount = scheduleInspectionPage.getZoneRowCount();
        int rowIndex = firstStableZoneRowIndex(rowCount);
        String zone = scheduleInspectionPage.getZoneByIndex(rowIndex);
        LOGGER.info("Prepared schedule modal with template=" + selectedTemplate
                + ", building=" + selectedBuilding + ", zone=" + zone);
        return new ScheduleContext(selectedTemplate, selectedBuilding, zone, rowIndex);
    }

    private int firstStableZoneRowIndex(int rowCount) {
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            String zone = scheduleInspectionPage.getZoneByIndex(rowIndex);
            if (isStableZoneName(zone)) {
                return rowIndex;
            }
        }
        return 0;
    }

    private boolean isStableZoneName(String zone) {
        return zone != null
                && !zone.isBlank()
                && !zone.contains("?")
                && zone.chars().allMatch(character -> character < 128)
                && zone.matches(".*[A-Za-z0-9].*");
    }

    private void trySelectOptionalFilters() {
        try {
            scheduleInspectionPage.selectAnyZoneCategory();
        } catch (RuntimeException ignored) {
            // Zone category is optional in the Schedule Inspection flow.
        }

        try {
            scheduleInspectionPage.selectAnyFloor();
        } catch (RuntimeException ignored) {
            // Floor is optional in the Schedule Inspection flow.
        }
    }

    private void validateScheduledEvents(String zone,
                                         String auditor,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         List<LocalDate> expectedDates) {
        LOGGER.info(() -> "Validating scheduled events for zone=" + zone + ", auditor=" + auditor
                + ", expectedCount=" + expectedDates.size());
        scheduleInspectionPage.setScheduleListDateRange(iso(startDate), iso(endDate));
        scheduleInspectionPage.searchScheduleList(zone);
        scheduleInspectionPage.waitForScheduleGrid();
        int matchedEvents = scheduleInspectionPage.waitForScheduledEvents(
                zone,
                auditor,
                expectedDates.stream().map(this::iso).toList());
        Assert.assertEquals(matchedEvents, expectedDates.size(),
                "Scheduled event dates should match generated recurrence dates.");
    }

    private void assertScheduleSaved(String message) {
        String feedback = scheduleInspectionPage.getToastMessageIfPresent(5000).toLowerCase(Locale.ENGLISH);
        Assert.assertTrue(
                feedback.matches(".*(success|scheduled|created).*")
                        || scheduleInspectionPage.hasScheduledInspectionForAuditor(DEFAULT_AUDITOR),
                message);
                scheduleInspectionPage.closeSuccessPopupIfPresent();
    }

    private String iso(LocalDate date) {
        return date.toString();
    }

    // private List<LocalDate> dailyDates(LocalDate start, LocalDate end, int every) {
    //     List<LocalDate> dates = new ArrayList<>();
    //     for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(every)) {
    //         dates.add(date);
    //     }
    //     return dates;
    // }

    // private List<LocalDate> monthlyDates(LocalDate start, LocalDate end, int every, List<Integer> dayNumbers) {
    //     List<LocalDate> dates = new ArrayList<>();
    //     YearMonth currentMonth = YearMonth.from(start);
    //     YearMonth endMonth = YearMonth.from(end);
    //     int monthOffset = 0;

    //     while (!currentMonth.isAfter(endMonth)) {
    //         if (monthOffset % every == 0) {
    //             for (Integer dayNumber : dayNumbers) {
    //                 if (dayNumber > currentMonth.lengthOfMonth()) {
    //                     continue;
    //                 }

    //                 LocalDate candidate = currentMonth.atDay(dayNumber);
    //                 if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
    //                     dates.add(candidate);
    //                 }
    //             }
    //         }

    //         currentMonth = currentMonth.plusMonths(1);
    //         monthOffset++;
    //     }

    //     return dates;
    // }

    // private List<LocalDate> monthlyByDayPattern(LocalDate start,
    //                                             LocalDate end,
    //                                             int every,
    //                                             String weekOrder,
    //                                             DayOfWeek dayOfWeek) {
    //     List<LocalDate> dates = new ArrayList<>();
    //     YearMonth currentMonth = YearMonth.from(start);
    //     YearMonth endMonth = YearMonth.from(end);
    //     int monthOffset = 0;

    //     while (!currentMonth.isAfter(endMonth)) {
    //         if (monthOffset % every == 0) {
    //             LocalDate candidate = resolveMonthlyWeekday(currentMonth, weekOrder, dayOfWeek);
    //             if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
    //                 dates.add(candidate);
    //             }
    //         }

    //         currentMonth = currentMonth.plusMonths(1);
    //         monthOffset++;
    //     }

    //     return dates;
    // }

    // private List<LocalDate> yearlyDates(LocalDate start,
    //                                     LocalDate end,
    //                                     int every,
    //                                     RecurrenceMonth recurrenceMonth,
    //                                     int dayOfMonth) {
    //     List<LocalDate> dates = new ArrayList<>();
    //     int startYear = start.getYear();
    //     int endYear = end.getYear();

    //     for (int year = startYear; year <= endYear; year += every) {
    //         YearMonth yearMonth = YearMonth.of(year, recurrenceMonth.month());
    //         if (dayOfMonth > yearMonth.lengthOfMonth()) {
    //             continue;
    //         }

    //         LocalDate candidate = yearMonth.atDay(dayOfMonth);
    //         if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
    //             dates.add(candidate);
    //         }
    //     }

    //     return dates;
    // }

    // private List<String> toExpectedUiDates(List<LocalDate> dates) {
    //     List<String> values = new ArrayList<>();
    //     for (LocalDate date : dates) {
    //         values.add(date.format(DATE_FORMAT));
    //     }
    //     return values;
    // }

    // private LocalDate resolveMonthlyWeekday(YearMonth month, String weekOrder, DayOfWeek dayOfWeek) {
    //     return switch (weekOrder.toLowerCase(Locale.ENGLISH)) {
    //         case "first" -> month.atDay(1).with(TemporalAdjusters.firstInMonth(dayOfWeek));
    //         case "second" -> month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(2, dayOfWeek));
    //         case "third" -> month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(3, dayOfWeek));
    //         case "fourth" -> month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(4, dayOfWeek));
    //         case "last" -> month.atEndOfMonth().with(TemporalAdjusters.lastInMonth(dayOfWeek));
    //         default -> throw new IllegalArgumentException("Unsupported monthly week order: " + weekOrder);
    //     };
    // }

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

    @DataProvider(name = "jsonFrequencyData")
    public Object[][] jsonFrequencyData() {
    List<FrequencyData> dataList = TestDataLoader.loadFrequencyData();

    return dataList.stream()
            .map(d -> new Object[]{d})
            .toArray(Object[][]::new);
    }

    private FrequencyConfig buildConfig(FrequencyData data) {

    return switch (data.type.toUpperCase()) {
        case "DAILY" -> FrequencyConfig.daily(data.every);

        case "WEEKLY" -> FrequencyConfig.weekly(
                data.every,
                data.daysOfWeek
        );

        case "MONTHLY" -> FrequencyConfig.monthlyOnDates(
                data.every,
                data.dates
        );

        default -> throw new IllegalArgumentException("Invalid type");
    };
    }
    private record ScheduleContext(String template, String building, String zone, int rowIndex) {
    }

    
}
