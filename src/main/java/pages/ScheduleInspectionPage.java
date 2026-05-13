package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import models.FrequencyConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import com.microsoft.playwright.Locator.GetByRoleOptions;


public class ScheduleInspectionPage 
{
    private static final double DEFAULT_TIMEOUT_MS = 20000;
    private static final DateTimeFormatter UI_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter UI_TIME_FORMAT_12_HOUR = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final Logger LOGGER = Logger.getLogger(ScheduleInspectionPage.class.getName());
    private final Page page;
    private final Locator modal;
    private final Locator modalTitle;
    private final Locator modalBody;
    private final Locator customRadio;
    private final Locator standardRadio;
    private final Locator saveButton;
    private final Locator cancelButton;
    private final Locator copyConfigurationButton;
    private final Locator loadingIndicator;
    private final Locator toastMessage;
    private final Locator validationMessages;
    private final Locator rows;
    private String lastFeedbackMessage = "";
    private final List<String> lastValidationMessages = new ArrayList<>();
    public ScheduleInspectionPage(Page page) 
    {
        this.page = page;

        this.modal = page.locator(
                ".ant-modal-root [role='dialog']:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active), " +
                        ".ant-modal-root .ant-modal.newSchedule:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active)")
                .first();

        this.modalTitle = page.locator(".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-title:visible")
                .filter(new Locator.FilterOptions().setHasText("Schedule Audit")).first();

        this.modalBody = page.locator(
                ".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-body:visible:has-text('Template:'), " +
                        ".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-content:visible:has-text('Template:'), " +
                        ".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-content:visible:has-text('Buildings:'), " +
                        ".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-content:visible:has-text('Zone Category:'), " +
                        ".ant-modal-root .ant-modal:not(.ant-zoom-leave):not(.ant-zoom-leave-active) .ant-modal-content:visible:has-text('StartTime'), " +
                        ".ant-modal-root [role='dialog']:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active):has-text('Template:')").first();
                        
        this.customRadio = modal.getByLabel("Custom", new Locator.GetByLabelOptions().setExact(true)).first()
                .or(modal.getByText("Custom", new Locator.GetByTextOptions().setExact(true))).first();
        this.standardRadio = modal.getByLabel("Standard", new Locator.GetByLabelOptions().setExact(true)).first()
                .or(modal.getByText("Standard", new Locator.GetByTextOptions().setExact(true))).first();
        this.saveButton = modal.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("^save$", Pattern.CASE_INSENSITIVE))).first();
        this.cancelButton = modal.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("^cancel$", Pattern.CASE_INSENSITIVE))).first();
        this.copyConfigurationButton = modal.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("copy configuration", Pattern.CASE_INSENSITIVE))).first();
        this.loadingIndicator = modal.locator(".ant-spin-spinning, [role='progressbar'], .loading, .skeleton");
        this.toastMessage = page.locator(
                ".ant-notification-notice:visible, " +
                        ".ant-message-notice:visible, " +
                        ".swal-overlay--show-modal .swal-modal:visible, " +
                        ".swal2-container .swal2-popup:visible, " +
                        "[role='alert']:visible").first();
        this.validationMessages = page.locator(
                ".ant-modal-content .ant-form-item-explain-error:visible, " +
                        ".ant-modal-content .error:visible, " +
                        ".ant-modal-content [role='alert']:visible, " +
                        ".swal-overlay--show-modal .swal-modal:visible");
        this.rows = modal.locator("tbody tr:not(.ant-table-placeholder)");
    }

     public void closeSuccessPopupIfPresent() {
    Locator popup = page.locator(".swal2-container");
    Locator okButton = page.locator(".swal2-confirm");

    if (popup.isVisible()) {
        okButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));

        okButton.click();

        // Wait until popup disappears
        popup.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10000));
    }
}
    public void waitForModal() 
    {
        Locator visibleModalWrap = page.locator(".ant-modal-root .ant-modal-wrap:visible").first();
        Locator visibleModalDialog = page.locator(
                ".ant-modal-root .ant-modal.newSchedule:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active), " +
                        ".ant-modal-root [role='dialog']:visible:not(.ant-zoom-leave):not(.ant-zoom-leave-active)")
                .first();
        Locator attachedModalDialog = page.locator(
                ".ant-modal-root .ant-modal.newSchedule:not(.ant-zoom-leave):not(.ant-zoom-leave-active), " +
                        ".ant-modal-root [role='dialog']:not(.ant-zoom-leave):not(.ant-zoom-leave-active)")
                .first();

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) 
        {
            try 
            {
                if (modalTitle.count() > 0 && modalTitle.isVisible()) {
                    return;
                }
                if (modalBody.count() > 0 && modalBody.isVisible()) {
                    return;
                }
                if (modal.count() > 0 && modal.isVisible()) {
                    return;
                }
                if (visibleModalWrap.count() > 0 && visibleModalWrap.isVisible() && visibleModalDialog.count() > 0) {
                    return;
                }
                if (visibleModalDialog.count() > 0 && isDomVisible(visibleModalDialog)) {
                    return;
                }
            } 
            catch (RuntimeException ignored) 
            {
                // Allow the dialog time to finish rendering.
            }

            page.waitForTimeout(250);
        }

        Locator root = page.locator("#root").first();
        if (root.count() > 0 && root.innerText().isBlank()) 
            {
            throw new IllegalStateException(
                    "Schedule Inspection did not open because the page rendered blank after the button click.");
        }

        if (attachedModalDialog.count() > 0) {
            String display = safeAttribute(attachedModalDialog, "style");
            String wrapDisplay = visibleModalWrap.count() > 0 ? safeAttribute(visibleModalWrap, "style") : "";
            throw new IllegalStateException(
                    "Schedule Audit dialog was attached to the DOM but never became visible. dialogStyle="
                            + display + ", wrapStyle=" + wrapDisplay);
        }

        throw new IllegalStateException("Schedule Audit dialog did not attach to the DOM.");
    }

    public boolean isModalOpen() 
    {
        return modalTitle.isVisible() || modalBody.isVisible() || modal.isVisible();
    }

    public void assertModalOpen() {
        waitForModal();
        PlaywrightAssertions.assertThat(modalTitle.or(modalBody).or(modal)).isVisible();
    }

    public void assertModalClosed() {
        PlaywrightAssertions.assertThat(modal).isHidden();
    }

    public void chooseCustomOption() 
    {
        selectRadio(customRadio);
    }

    public void chooseStandardOption() 
    {
        selectRadio(standardRadio);
    }

    public boolean isCustomSelected() 
    {
        return isRadioSelected(customRadio);
    }

    public void assertCustomSelected() {
        if (!isRadioSelected(customRadio)) {
            throw new AssertionError("Custom radio should be selected.");
        }
    }

    public boolean isStandardSelected() 
    {
        return isRadioSelected(standardRadio);
    }

    public void assertStandardSelected() {
        if (!isRadioSelected(standardRadio)) {
            throw new AssertionError("Standard radio should be selected.");
        }
    }

    public void selectTemplate(String templateName) 
    {
        selectDropdownOption("Template", templateName);
        waitForLoadingToFinish();
    }

    public String selectAnyTemplate() 
    {
        String selectedTemplate = selectFirstDropdownOption("Template");
        waitForLoadingToFinish();
        return selectedTemplate;
    }

    public String selectAnyTemplateWithRows() 
    {
        List<String> templateOptions = getVisibleDropdownOptions("Template");
        Optional<String> selectedBuilding = trySelectAnyBuilding();

        for (String templateOption : templateOptions) {
            try {
                selectTemplate(templateOption);
                if (getZoneRowCount() > 0) {
                    return templateOption;
                }
            } catch (RuntimeException ex) {
                LOGGER.info(() -> "Skipping template that could not be selected: " + templateOption
                        + ". Reason: " + ex.getMessage());
            }
        }

        if (selectedBuilding.isEmpty()) {
            selectedBuilding = trySelectAnyBuilding();
        }

        if (selectedBuilding.isPresent()) {
            for (String templateOption : templateOptions) {
                try {
                    selectTemplate(templateOption);
                    if (getZoneRowCount() > 0) {
                        return templateOption;
                    }
                } catch (RuntimeException ex) {
                    LOGGER.info(() -> "Skipping template after building selection: " + templateOption
                            + ". Reason: " + ex.getMessage());
                }
            }
        }

        throw new IllegalStateException("Unable to find an available template that loads schedule rows. Tried templates: "
                + templateOptions + selectedBuilding.map(building -> ", building=" + building).orElse(""));
    }

    public void selectBuilding(String buildingName) 
    {
        selectDropdownOption(new String[]{"Buildings", "Building"}, buildingName);
        waitForLoadingToFinish();
    }

    public String selectAnyBuilding()
    {
        String selectedBuilding = selectFirstDropdownOption(new String[]{"Buildings", "Building"});
        waitForLoadingToFinish();
        return selectedBuilding;
    }

    private Optional<String> trySelectAnyBuilding()
    {
        try {
            return Optional.of(selectAnyBuilding());
        } catch (RuntimeException ex) {
            LOGGER.info(() -> "Building dropdown was not selectable while preparing template rows: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public void selectZoneCategory(String zoneCategory) 
    {
        if (trySelectZoneCategory(zoneCategory)) {
            waitForLoadingToFinish();
            return;
        }
        selectDropdownOption(new String[]{"Zone Category", "Zone Category:"}, zoneCategory);
        waitForLoadingToFinish();
    }

    public String selectAnyZoneCategory() 
    {
        String selectedZoneCategory = selectFirstDropdownOption(new String[]{"Zone Category", "Zone Category:"});
        waitForLoadingToFinish();
        return selectedZoneCategory;
    }

    public void selectFloor(String floorName) 
    {
        selectDropdownOption(new String[]{"Floor", "Floor:"}, floorName);
        waitForLoadingToFinish();
    }

    public String selectAnyFloor() 
    {
        String selectedFloor = selectFirstDropdownOption(new String[]{"Floor", "Floor:"});
        waitForLoadingToFinish();
        return selectedFloor;
    }

    public void fillScheduleForm(ScheduleFormData data) 
    {
        if (data.template != null) {
            selectTemplate(data.template);
        }
        if (data.building != null) {
            selectBuilding(data.building);
        }
        // if (data.zoneCategory != null) {
        //     selectZoneCategory(data.zoneCategory);
        // }
        // if (data.floor != null) {
        //     selectFloor(data.floor);
        // }
        fillFirstZoneRow(data);
    }

    public void fillFirstZoneRow(ScheduleFormData data) 
    {
        Locator row = getRow(data.rowIndex);
        selectRowIfPossible(row);
        if (data.zone != null) 
        {
            fillTableField(row, "Zone", data.zone);
        }
        // if (data.criticality != null) {
        //     fillTableField(row, "Criticality", data.criticality);
        // }
        if (data.frequency != null) 
        {
            fillTableField(row, "How Often", data.frequency);
        }
        if (data.startDate != null) 
        {
            fillTableField(row, "Start Date", data.startDate);
        }
        if (data.endDate != null) 
        {
            fillTableField(row, "End Date", data.endDate);
        }
        if (data.startTime != null) 
        {
            fillTableField(row, "Start Time", data.startTime);
        }
        if (data.auditor != null) 
        {
            fillTableField(row, "Auditor", data.auditor);
        }
    }

    public boolean isZoneConfigurationMatching(
        String zone,
        String auditor,
        String startDate,
        String endDate,
        String time) 
    {
        return rowHasAuditor(zone, auditor)
                && rowHasDateRange(zone, startDate, endDate)
                && rowHasStartTime(zone, time);
    }

    public boolean isRowConfigurationMatching(
            int rowIndex,
            String auditor,
            String startDate,
            String endDate,
            String time) 
    {
        Locator row = getRow(rowIndex);
        return rowHasAuditor(row, auditor)
                && rowHasDateRange(row, startDate, endDate)
                && rowHasStartTime(row, time);
    }
    
    public void setRowStartDate(String zoneName, String date) 
    {
        setRowField(zoneName, "Start Date", date);
    }

    public void setRowStartDate(int rowIndex, String date) 
    {
        fillTableField(getRow(rowIndex), "Start Date", date);
    }

    public void setRowEndDate(String zoneName, String date) 
    {
        setRowField(zoneName, "End Date", date);
    }

    public void setRowEndDate(int rowIndex, String date) 
    {
        fillTableField(getRow(rowIndex), "End Date", date);
    }

    public boolean isRowDateDisabled(int rowIndex, String fieldName, String date)
    {
        Locator row = getRow(rowIndex);
        int columnIndex = getColumnIndex(fieldName);
        Locator cell = row.locator("td").nth(columnIndex);
        Locator picker = cell.locator(".ant-picker").first();
        picker.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        picker.click();

        try {
            return isDateDisabledInOpenPicker(date);
        } finally {
            closeVisibleDatePicker();
        }
    }

    public void setRowStartTime(String zoneName, String time) 
    {
        setRowField(zoneName, "Start Time", time);
    }

    public void setRowStartTime(int rowIndex, String time) 
    {
        fillTableField(getRow(rowIndex), "Start Time", time);
    }

    public void setRowAuditor(String zoneName, String auditor) 
    {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zoneName)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        fillAuditorField(row, auditor);
    }

    public void setRowAuditor(int rowIndex, String auditor) 
    {
        Locator row = getRow(rowIndex);
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        fillAuditorField(row, auditor);
    }

    public String getZoneByIndex(int index) 
    {
        Locator row = getRow(index);
        int columnIndex = getColumnIndex("Zone");
        return row.locator("td").nth(columnIndex).innerText().trim();
    }

    public void selectFrequency(int rowIndex, FrequencyConfig config) {

    Locator row = getRow(rowIndex);
    int columnIndex = getColumnIndex("How Often");
    Locator cell = row.locator("td").nth(columnIndex);

    cell.click(new Locator.ClickOptions().setForce(true));

    Locator popup = waitForRecurrencePopup();

    // Select type
    selectFrequencyType(popup, config.getType());

    // Set interval
    setEveryValue(popup, config.getEvery());

    switch (config.getType()) {

        case DAILY:
            break;

        case WEEKLY:
            if (config.getDaysOfWeek() != null) {
                for (String day : config.getDaysOfWeek()) {
                    clickTextChoice(popup, day);
                }
            }
            break;

        case MONTHLY:
            if (config.getDates() != null) {
                clickTextChoice(popup, "On the Dates");
                for (Integer date : config.getDates()) {
                    selectMonthlyDate(popup, date);
                }
            }
            break;
    }

    clickPopupOk(popup);

    waitForLoadingToFinish();
    }

    private void clickDatePickerOk() {

    Locator activePicker = page.locator(
            ".ant-picker-dropdown.ant-picker-dropdown-range:not(.ant-picker-dropdown-hidden), " +
                    ".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)"
    ).last();

    activePicker.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));

    Locator okBtn = activePicker.locator(
            ".ant-picker-ok button:visible, " +
                    ".ant-picker-footer button:has-text('OK'):visible, " +
                    "button:has-text('OK'):visible"
    ).last();

    okBtn.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));
    okBtn.scrollIntoViewIfNeeded();

    page.waitForTimeout(200); // allow animation

    try {
        okBtn.click(new Locator.ClickOptions().setForce(true));
    } catch (RuntimeException ex) {
        okBtn.evaluate("element => element.click()");
    }

    activePicker.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(5000));
    
    // Optional stabilization (recommended)
    page.waitForTimeout(300);
    }

    public void selectDates(int rowIndex, String startDate, String endDate, String time)
    {
        LOGGER.info(() -> "Setting row dates/time for row index " + rowIndex
                + " start=" + startDate + " end=" + endDate + " time=" + time);
        setRowStartDate(rowIndex, startDate);
        setRowEndDate(rowIndex, endDate);
        setRowStartTime(rowIndex, time);
    }

    public String getFrequencySummary(int rowIndex)
    {
        Locator row = getRow(rowIndex);
        int columnIndex = getColumnIndex("How Often");
        return row.locator("td").nth(columnIndex).innerText().trim();
    }

    public void setScheduleListDateRange(String startDate, String endDate)
    {
        LOGGER.info(() -> "Applying schedule list date range " + startDate + " -> " + endDate);
        Locator rangePicker = page.locator("main .ant-picker-range").first();
        rangePicker.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

        rangePicker.click();
        selectDateFromPicker(startDate, false);
        selectDateFromPicker(endDate, false);

        clickDatePickerOk();

        waitForScheduleListToRefresh();
        // page.waitForFunction(
        // "() => !document.querySelector('.ant-picker-dropdown')"
        // );

        System.out.println("Date filter applied: " + startDate + " -> " + endDate);
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();
    }

    public void searchScheduleList(String query)
    {
        Locator searchInput = page.locator("main input[type='search'][placeholder='Search ...']").first();
        if (searchInput.count() == 0) {
            return;
        }

        searchInput.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        searchInput.fill(query);
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();
    }
    

    public List<String> getScheduledAuditDates(String zone, String auditor)
    {
        List<String> dates = new ArrayList<>();
        Locator rows = visibleScheduleRows(zone, auditor);
        int rowCount = rows.count();
        int auditDateColumnIndex = getSchedulePageColumnIndex("Audit Date");

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            if (!row.isVisible()) {
                continue;
            }

            Locator cell = row.locator("td").nth(auditDateColumnIndex);
            String text = cell.innerText().trim();
            if (!text.isBlank()) {
                dates.add(text);
            }
        }

        return dates;
    }

    public int countScheduledEvents(String zone, String auditor, List<String> expectedDates)
    {
        int matches = 0;
        List<String> actualDates = getScheduledAuditDates(zone, auditor);
        for (String expectedDate : expectedDates) {
            if (actualDates.stream().anyMatch(actualDate -> dateValuesMatch(expectedDate, actualDate))) {
                matches++;
            }
        }
        return matches;
    }


    public int waitForScheduledEvents(String zone, String auditor, List<String> expectedDates)
    {
        int expectedCount = expectedDates.size();
        long start = System.currentTimeMillis();
        int latestMatchCount = 0;
        List<String> latestActualDates = List.of();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            latestActualDates = getScheduledAuditDatesAcrossPages(zone, auditor, expectedDates, expectedCount);
            latestMatchCount = countMatchingDates(latestActualDates, expectedDates);
            if (latestMatchCount >= expectedCount) {
                return latestMatchCount;
            }

            page.waitForTimeout(500);
            waitForScheduleListToRefresh();
        }

        throw new IllegalStateException("Expected " + expectedCount + " scheduled events for zone=" + zone
                + ", auditor=" + auditor + ", expectedDates=" + expectedDates
                + ", matched=" + latestMatchCount + ", actualDates=" + latestActualDates + ".");
    }

    public ScheduledAuditRecord deleteScheduledAudit(String template,
                                                    String building,
                                                    String level,
                                                    String zone,
                                                    String auditDate,
                                                    String auditor)
    {
        ScheduledAuditRecord record = findScheduledAuditAcrossPages(template, building, level, zone, auditDate, auditor)
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to find scheduled audit for template=" + template
                                + ", building=" + building
                                + ", level=" + level
                                + ", zone=" + zone
                                + ", auditDate=" + auditDate
                                + ", auditor=" + auditor + "."));

        clickScheduledAuditActions(template, building, level, zone, record.auditDate(), auditor);
        clickDeleteAuditMenuItem();
        waitForDeleteConfirmationModal();
        clickConfirmDelete();
        
        page.waitForTimeout(200);

        String feedback = captureDeleteSuccessBanner();
        System.out.println("Delete feedback: " + feedback);
        
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();

        return new ScheduledAuditRecord(
                record.template(),
                record.building(),
                record.level(),
                record.zone(),
                record.auditor(),
                record.auditDate(),
                feedback);
    }

    public boolean hasScheduledAuditAcrossPages(String template,
                                                String building,
                                                String level,
                                                String zone,
                                                String auditDate,
                                                String auditor)
    {
        return findScheduledAuditAcrossPages(template, building, level, zone, auditDate, auditor).isPresent();
    }

    public Optional<ScheduledAuditGridRecord> findScheduledAuditGridRecordAcrossPages(String template,
                                                                                     String building,
                                                                                     String level,
                                                                                     String zone,
                                                                                     String auditDate,
                                                                                     String auditor)
    {
        goToFirstSchedulePage();
        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {
            waitForScheduleListToRefresh();
            String pageSignature = currentSchedulePageSignature();
            if (!visitedPages.add(pageSignature)) {
                return Optional.empty();
            }

            Optional<ScheduledAuditGridRecord> record = findScheduledAuditGridRecordOnCurrentPage(
                    template,
                    building,
                    level,
                    zone,
                    auditDate,
                    auditor);
            if (record.isPresent()) {
                return record;
            }

            if (!goToNextSchedulePage()) {
                return Optional.empty();
            }
        }
    }

    public Optional<ScheduledAuditRecord> findFirstScheduledAuditWithStatusAcrossPages(String status)
    {
        goToFirstSchedulePage();
        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {
            waitForScheduleListToRefresh();
            String pageSignature = currentSchedulePageSignature();
            if (!visitedPages.add(pageSignature)) {
                return Optional.empty();
            }

            Optional<ScheduledAuditRecord> record = findFirstScheduledAuditWithStatusOnCurrentPage(status);
            if (record.isPresent()) {
                return record;
            }

            if (!goToNextSchedulePage()) {
                return Optional.empty();
            }
        }
    }

    public ScheduledAuditRecord editScheduledAuditDateAndAuditor(ScheduledAuditRecord record,
                                                                 String newAuditDate,
                                                                 String preferredAuditor)
    {
        clickScheduledAuditActions(
                record.template(),
                record.building(),
                record.level(),
                record.zone(),
                record.auditDate(),
                record.auditor(),
                false);
        clickEditAuditMenuItem();
        waitForEditAuditDrawer();

        if (newAuditDate != null && !newAuditDate.isBlank()) {
            fillEditAuditDate(newAuditDate);
        }

        String updatedAuditor = record.auditor();
        if (preferredAuditor != null && !preferredAuditor.isBlank()) {
            fillEditAuditAuditor(preferredAuditor);
            updatedAuditor = preferredAuditor;
        } else {
            updatedAuditor = fillEditAuditWithAnyDifferentAuditor(record.auditor());
        }

        clickSaveEditAudit();
        String feedback = captureEditAuditFeedback();
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();

        return new ScheduledAuditRecord(
                record.template(),
                record.building(),
                record.level(),
                record.zone(),
                updatedAuditor,
                newAuditDate == null || newAuditDate.isBlank() ? record.auditDate() : newAuditDate,
                feedback);
    }

    public ScheduledAuditRecord reAuditScheduledAuditDateAndAuditor(ScheduledAuditRecord record,
                                                                    String newAuditDate,
                                                                    String preferredAuditor)
    {
        openReAuditDrawer(record);

        if (newAuditDate != null && !newAuditDate.isBlank()) {
            fillEditAuditDate(newAuditDate);
        }

        String updatedAuditor = record.auditor();
        if (preferredAuditor != null && !preferredAuditor.isBlank()) {
            fillEditAuditAuditor(preferredAuditor);
            updatedAuditor = preferredAuditor;
        } else {
            updatedAuditor = fillEditAuditWithAnyDifferentAuditor(record.auditor());
        }

        fillReAuditReason(System.getProperty("matrix.reaudit.reason",
                System.getenv().getOrDefault("MATRIX_REAUDIT_REASON", "Automation Re-Audit")));

        clickSaveEditAudit();
        String feedback = captureEditAuditFeedback();
        closeVisibleAuditDrawerIfPresent();
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();

        return new ScheduledAuditRecord(
                record.template(),
                record.building(),
                record.level(),
                record.zone(),
                updatedAuditor,
                newAuditDate == null || newAuditDate.isBlank() ? record.auditDate() : newAuditDate,
                feedback);
    }

    public String getAnyDifferentAuditor(String currentAuditor) {
        Locator trigger = resolveEditAuditAuditorTrigger();
        clickDropdownTrigger(trigger);
        Locator dropdown = waitForVisibleSelectDropdown();
        Locator options = dropdown.locator(".ant-select-item-option, [role='option']");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String text = option.innerText().trim();
                if (!text.isBlank()
                        && !"No data".equalsIgnoreCase(text)
                        && !text.equalsIgnoreCase(currentAuditor)) {
                    page.keyboard().press("Escape");
                    page.waitForTimeout(200);
                    return text;
                }
            }
            page.waitForTimeout(250);
        }

        page.keyboard().press("Escape");
        throw new IllegalStateException("Unable to find different auditor.");
    }

    public boolean isEditAuditOptionAvailable(
        ScheduledAuditRecord audit) {

        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);

        Locator activeDropdown = page.locator(
                ".ant-dropdown:not(.ant-dropdown-hidden)")
                .last();

        activeDropdown.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(3000));

        Locator editOption =
                activeDropdown.locator("text=Edit Audit");

        boolean visible =
                editOption.count() > 0
                && editOption.first().isVisible();

        page.keyboard().press("Escape");

        return visible;
    }

    public void assertEditAuditOptionAvailable(ScheduledAuditRecord audit) {
        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);

        Locator activeDropdown = page.locator(".ant-dropdown:not(.ant-dropdown-hidden)").last();
        activeDropdown.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(3000));
        PlaywrightAssertions.assertThat(activeDropdown.locator("text=Edit Audit").first()).isVisible();
        page.keyboard().press("Escape");
    }

    public boolean isReAuditOptionAvailable(
        ScheduledAuditRecord audit) {

        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);

        boolean visible = isVisible(visibleReAuditMenuItem());

        page.keyboard().press("Escape");

        return visible;
    }

    public void assertReAuditOptionAvailable(ScheduledAuditRecord audit) {
        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);

        PlaywrightAssertions.assertThat(visibleReAuditMenuItem()).isVisible();
        page.keyboard().press("Escape");
    }
    
    public Locator findScheduledAuditRowAcrossPages(
            String template,
            String building,
            String level,
            String zone,
            String auditDate,
            String auditor) {

        goToFirstSchedulePage();

        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {

            waitForScheduleListToRefresh();

            String pageSignature = currentSchedulePageSignature();

            if (!visitedPages.add(pageSignature)) {
                return null;
            }

            Locator rows =
                    page.locator("main tbody tr:not(.ant-table-placeholder)");

            int rowCount = rows.count();

            for (int i = 0; i < rowCount; i++) {

                Locator row = rows.nth(i);

                if (!row.isVisible()) {
                    continue;
                }

                String rowText =
                        row.innerText().toLowerCase(Locale.ENGLISH);

                boolean matches =
                        rowText.contains(template.toLowerCase())
                        && rowText.contains(building.toLowerCase())
                        && rowText.contains(level.toLowerCase())
                        && rowText.contains(zone.toLowerCase())
                        && rowText.contains(auditor.toLowerCase());

                if (matches) {
                    return row;
                }
            }

            if (!goToNextSchedulePage()) {
                return null;
            }
        }
    }










    private Optional<ScheduledAuditRecord> findScheduledAuditAcrossPages(String template,
                                                                         String building,
                                                                         String level,
                                                                         String zone,
                                                                         String auditDate,
                                                                         String auditor)
    {
        goToFirstSchedulePage();
        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {
            waitForScheduleListToRefresh();
            String pageSignature = currentSchedulePageSignature();
            if (!visitedPages.add(pageSignature)) {
                return Optional.empty();
            }

            Optional<ScheduledAuditRecord> record = findScheduledAuditOnCurrentPage(
                    template,
                    building,
                    level,
                    zone,
                    auditDate,
                    auditor);
            if (record.isPresent()) {
                return record;
            }

            if (!goToNextSchedulePage()) {
                return Optional.empty();
            }
        }
    }

    private Optional<ScheduledAuditRecord> findScheduledAuditOnCurrentPage(String template,
                                                                          String building,
                                                                          String level,
                                                                          String zone,
                                                                          String auditDate,
                                                                          String auditor)
    {
        int templateColumnIndex = getSchedulePageColumnIndex("Template");
        int buildingColumnIndex = getSchedulePageColumnIndex("Building");
        int levelColumnIndex = getSchedulePageColumnIndex("Level");
        int zoneColumnIndex = getSchedulePageColumnIndex("Zone");
        int auditorColumnIndex = getSchedulePageColumnIndex("Assigned To");
        int auditDateColumnIndex = getSchedulePageColumnIndex("Audit Date");
        Locator rows = page.locator("main tbody tr:not(.ant-table-placeholder)");
        int rowCount = rows.count();

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            if (!row.isVisible()) {
                continue;
            }

            String actualTemplate = cellText(row, templateColumnIndex);
            String actualBuilding = cellText(row, buildingColumnIndex);
            String actualLevel = cellText(row, levelColumnIndex);
            String actualZone = cellText(row, zoneColumnIndex);
            String actualAuditor = cellText(row, auditorColumnIndex);
            String actualAuditDate = cellText(row, auditDateColumnIndex);

            if (actualTemplate.equalsIgnoreCase(template)
                    && actualBuilding.equalsIgnoreCase(building)
                    && actualLevel.equalsIgnoreCase(level)
                    && actualZone.equalsIgnoreCase(zone)
                    && dateValuesMatch(auditDate, actualAuditDate)
                    && actualAuditor.equalsIgnoreCase(auditor)) {
                return Optional.of(new ScheduledAuditRecord(
                        actualTemplate,
                        actualBuilding,
                        actualLevel,
                        actualZone,
                        actualAuditor,
                        actualAuditDate,
                        ""));
            }
        }

        return Optional.empty();
    }

    private Optional<ScheduledAuditGridRecord> findScheduledAuditGridRecordOnCurrentPage(String template,
                                                                                        String building,
                                                                                        String level,
                                                                                        String zone,
                                                                                        String auditDate,
                                                                                        String auditor)
    {
        int templateColumnIndex = getSchedulePageColumnIndex("Template");
        int buildingColumnIndex = getSchedulePageColumnIndex("Building");
        int levelColumnIndex = getSchedulePageColumnIndex("Level");
        int zoneColumnIndex = getSchedulePageColumnIndex("Zone");
        int auditorColumnIndex = getSchedulePageColumnIndex("Assigned To");
        int auditDateColumnIndex = getSchedulePageColumnIndex("Audit Date");
        int statusColumnIndex = getSchedulePageColumnIndex("Status");
        Locator rows = page.locator("main tbody tr:not(.ant-table-placeholder)");
        int rowCount = rows.count();

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            if (!row.isVisible()) {
                continue;
            }

            String actualTemplate = cellText(row, templateColumnIndex);
            String actualBuilding = cellText(row, buildingColumnIndex);
            String actualLevel = cellText(row, levelColumnIndex);
            String actualZone = cellText(row, zoneColumnIndex);
            String actualAuditor = cellText(row, auditorColumnIndex);
            String actualAuditDate = cellText(row, auditDateColumnIndex);
            String actualStatus = cellText(row, statusColumnIndex);

            if (actualTemplate.equalsIgnoreCase(template)
                    && actualBuilding.equalsIgnoreCase(building)
                    && actualLevel.equalsIgnoreCase(level)
                    && actualZone.equalsIgnoreCase(zone)
                    && dateValuesMatch(auditDate, actualAuditDate)
                    && actualAuditor.equalsIgnoreCase(auditor)) {
                return Optional.of(new ScheduledAuditGridRecord(
                        actualTemplate,
                        actualBuilding,
                        actualLevel,
                        actualZone,
                        actualAuditor,
                        actualAuditDate,
                        actualStatus));
            }
        }

        return Optional.empty();
    }

    private Optional<ScheduledAuditRecord> findFirstScheduledAuditWithStatusOnCurrentPage(String status)
    {
        int templateColumnIndex = getSchedulePageColumnIndex("Template");
        int buildingColumnIndex = getSchedulePageColumnIndex("Building");
        int levelColumnIndex = getSchedulePageColumnIndex("Level");
        int zoneColumnIndex = getSchedulePageColumnIndex("Zone");
        int auditorColumnIndex = getSchedulePageColumnIndex("Assigned To");
        int auditDateColumnIndex = getSchedulePageColumnIndex("Audit Date");
        int statusColumnIndex = getSchedulePageColumnIndex("Status");
        Locator rows = page.locator("main tbody tr:not(.ant-table-placeholder)");
        int rowCount = rows.count();

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            if (!row.isVisible()) {
                continue;
            }

            if (cellText(row, statusColumnIndex).equalsIgnoreCase(status)) {
                return Optional.of(new ScheduledAuditRecord(
                        cellText(row, templateColumnIndex),
                        cellText(row, buildingColumnIndex),
                        cellText(row, levelColumnIndex),
                        cellText(row, zoneColumnIndex),
                        cellText(row, auditorColumnIndex),
                        cellText(row, auditDateColumnIndex),
                        ""));
            }
        }

        return Optional.empty();
    }

    private List<String> getScheduledAuditDatesAcrossPages(String zone,
                                                           String auditor,
                                                           List<String> expectedDates,
                                                           int expectedCount)
    {
        goToFirstSchedulePage();

        List<String> dates = new ArrayList<>();
        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {
            waitForScheduleListToRefresh();
            String pageSignature = currentSchedulePageSignature();
            if (!visitedPages.add(pageSignature)) {
                break;
            }

            dates.addAll(getScheduledAuditDates(zone, auditor));
            if (countMatchingDates(dates, expectedDates) >= expectedCount) {
                break;
            }

            if (!goToNextSchedulePage()) {
                break;
            }
        }

        return dates;
    }

    public void openEditAuditDrawer(ScheduledAuditRecord audit) {
        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);
        clickEditAuditMenuItem();
        waitForEditAuditDrawer();
    }

    public void openReAuditDrawer(ScheduledAuditRecord audit) {
        clickScheduledAuditActions(
                audit.template(),
                audit.building(),
                audit.level(),
                audit.zone(),
                audit.auditDate(),
                audit.auditor(),
                false);
        clickReAuditMenuItem();
        waitForEditAuditDrawer();
    }

    public void updateAuditDate(String date) {
        fillEditAuditDate(date);
    }

    public void updateAuditAuditor(String auditor) {
        fillEditAuditAuditor(auditor);
    }

    public void cancelEditAudit() {
        Locator cancelButton = page.locator(
                ".ant-drawer:visible button:has-text('Cancel'), " +
                        ".ant-drawer:visible .ant-drawer-footer button:not(.ant-btn-primary):visible")
                .last();
        cancelButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        cancelButton.scrollIntoViewIfNeeded();
        try {
            cancelButton.click(new Locator.ClickOptions().setForce(true));
        } catch (RuntimeException ex) {
            cancelButton.evaluate("element => element.click()");
        }
    }

    public void waitForEditDrawerToClose() {
        visibleEditAuditDrawer().waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private void closeVisibleAuditDrawerIfPresent() {
        Locator drawer = page.locator(".ant-drawer:visible").last();
        if (!isVisible(drawer)) {
            return;
        }

        Locator closeButton = drawer.locator(".ant-drawer-close:visible, button[aria-label='Close']:visible").first();
        if (isVisible(closeButton)) {
            try {
                closeButton.click(new Locator.ClickOptions().setForce(true).setTimeout(3000));
            } catch (RuntimeException ex) {
                closeButton.evaluate("element => element.click()");
            }
        } else {
            page.keyboard().press("Escape");
        }

        try {
            drawer.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(5000));
        } catch (RuntimeException ignored) {
            page.keyboard().press("Escape");
            page.waitForTimeout(500);
        }
    }

    public void waitForScheduledEvents(String zone, String auditor, int expectedCount)
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (visibleScheduleRows(zone, auditor).count() >= expectedCount) {
                return;
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Expected at least " + expectedCount
                + " scheduled rows for zone=" + zone + ", auditor=" + auditor + ".");
    }

    private void selectAuditorInput(Locator input, String auditor) 
    {
        input.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        input.click();
        input.fill(auditor);

        Locator option = page.locator("li:has-text('" + auditor + "'), .ant-select-item-option:has-text('" + auditor + "')")
                .last();
        option.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        option.click(new Locator.ClickOptions().setForce(true));
        waitForLoadingToFinish();
    }

    private void fillAuditorField(Locator row, String auditor) 
    {
        int columnIndex = getColumnIndex("Auditor");
        Locator cell = row.locator("td").nth(columnIndex);
        if (cell.count() > 0) {
            try {
                cell.click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(200);
            } catch (RuntimeException ignored) {
                // Some rows already expose the input without clicking the cell.
            }
        }

        Locator input = row.locator("input[placeholder='Select Auditor']").first();
        if (input.count() > 0) {
            try {
                if (input.isVisible()) {
                    selectAuditorInput(input, auditor);
                    return;
                }
            } catch (RuntimeException ignored) {
                // Fall back to the combobox-style selector below.
            }
        }

        Locator combobox = cell.locator("nz-select, .ant-select, [role='combobox']").first();
        if (combobox.count() > 0) {
            selectComboboxValue(cell, combobox, auditor);
            return;
        }

        if (input.count() > 0) {
            selectAuditorInput(input, auditor);
            return;
        }

        throw new IllegalStateException("Unable to locate auditor control for the selected row.");
    }

    private void selectFrequencyType(Locator popup, FrequencyConfig.Type type) {
        String label = switch (type) {
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
        };

        try {
            clickTextChoice(popup, label);
            return;
        } catch (RuntimeException ignored) {
            // Recurrence type options can render in a detached Ant dropdown.
        }

        Locator typeSelect = popup.locator(".ant-select:visible, [role='combobox']:visible").first();
        typeSelect.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        typeSelect.click(new Locator.ClickOptions().setForce(true));
        findVisibleDropdownOptionByText(label).click(new Locator.ClickOptions().setForce(true));
    }

    // private void configureMonthlyRecurrence(Locator popup, FrequencyConfig config) {
    //     if (config.monthlyMode == MonthlyMode.DATES) {
    //         clickTextChoice(popup, "On the Dates");
    //         for (Integer dayOfMonth : config.monthlyDates) {
    //             clickTextChoice(popup, String.valueOf(dayOfMonth));
    //         }
    //         return;
    //     }

    //     clickTextChoice(popup, "On the Days");
    //     selectPopupDropdownValue(popup, config.monthlyWeekOrder);
    //     selectPopupDropdownValue(popup, config.monthlyDayName);
    // }

   

    private void setEveryValue(Locator popup, int every) {
        Locator inputs = popup.locator(
                "input[type='number']:not([readonly]), " +
                        "input[role='spinbutton']:not([readonly]), " +
                        ".ant-input-number-input:not([readonly]), " +
                        "input:not([readonly]):not([type='search']):not([type='radio']):not([type='checkbox']):not([type='hidden'])");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = inputs.count();
            for (int index = 0; index < count; index++) {
                Locator input = inputs.nth(index);
                if (input.isVisible() && input.isEnabled()) {
                    input.fill(String.valueOf(every));
                    input.press("Tab");
                    return;
                }
            }
            page.waitForTimeout(250);
        }

        if (every == 1) {
            return;
        }

        throw new IllegalStateException("Unable to locate editable recurrence interval input.");
    }

    private Locator waitForRecurrencePopup() {
        Locator popup = page.locator(
                ".ant-popover:visible, " +
                        ".ant-modal-root .ant-modal-wrap:visible, " +
                        ".ant-dropdown:visible")
                .filter(new Locator.FilterOptions().setHasText("OK"))
                .last();

        popup.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return popup;
    }

    private void clickPopupOk(Locator popup) {
        Locator okButton = popup.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("^ok$", Pattern.CASE_INSENSITIVE))).first();
        if (okButton.count() > 0 && okButton.isVisible()) {
            okButton.click();
            return;
        }

        clickTextChoice(popup, "OK");
    }

    private void clickTextChoice(Locator container, String text) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (clickVisibleText(container, text)) {
                return;
            }

            Locator visibleOverlay = page.locator(
                    ".ant-popover:visible, " +
                            ".ant-dropdown:visible, " +
                            ".ant-select-dropdown:not(.ant-select-dropdown-hidden), " +
                            ".ant-modal-root .ant-modal-wrap:visible")
                    .last();
            if (visibleOverlay.count() > 0 && clickVisibleText(visibleOverlay, text)) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to click popup choice: " + text);
    }

    private boolean clickVisibleText(Locator container, String text) {
        Locator exactMatch = container.getByText(text, new Locator.GetByTextOptions().setExact(true)).last();
        if (exactMatch.count() > 0 && exactMatch.isVisible()) {
            exactMatch.click(new Locator.ClickOptions().setForce(true));
            return true;
        }

        Locator containsMatch = container.getByText(text).last();
        if (containsMatch.count() > 0 && containsMatch.isVisible()) {
            containsMatch.click(new Locator.ClickOptions().setForce(true));
            return true;
        }

        return false;
    }

    private void selectPopupDropdownValue(Locator popup, String value) {
        Locator dropdown = popup.locator(".ant-select:visible, [role='combobox']:visible").first();
        dropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        dropdown.click(new Locator.ClickOptions().setForce(true));
        findVisibleDropdownOptionByText(value).click(new Locator.ClickOptions().setForce(true));
    }

    private void selectMonthlyDate(Locator popup, int date) {
        String ordinalDate = ordinal(date);
        Locator datesSelect = popup.locator(".ant-select:visible").last();

        datesSelect.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        datesSelect.click(new Locator.ClickOptions().setForce(true));
        findVisibleDropdownOptionByText(ordinalDate).click(new Locator.ClickOptions().setForce(true));
    }

    private String ordinal(int value) {
        if (value % 100 >= 11 && value % 100 <= 13) {
            return value + "th";
        }

        return switch (value % 10) {
            case 1 -> value + "st";
            case 2 -> value + "nd";
            case 3 -> value + "rd";
            default -> value + "th";
        };
    }
    public void selectRowByZone(String zoneName, boolean multiSelect) 
    {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zoneName)).first();
        row.waitFor();

        if (!multiSelect) 
        {
        clearSelectedRowsExcept(row);
        }

        selectRowIfPossible(row);
    }

    public void selectRowByIndex(int rowIndex, boolean multiSelect) 
    {
        Locator row = getRow(rowIndex);
        if (!multiSelect) {
            clearSelectedRowsExcept(row);
        }
        selectRowIfPossible(row);
    }

    public void clickCopyConfiguration() 
    {
        copyConfigurationButton.click();
        waitForLoadingToFinish();
    }

    public void clickSave() 
    {
        waitForLoadingToFinish();
        clearFeedbackSnapshot();
        try 
        {
            saveButton.click(new Locator.ClickOptions().setTimeout(5000));
        } 
        catch (RuntimeException ex) 
        {
            clickVisibleModalButton("save");
        }
        waitForLoadingToFinish();
        waitForPostSaveFeedback();
        closeSuccessPopupIfPresent();
    }

    public void clickCancel() 
    {
        cancelButton.click();
        modal.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(DEFAULT_TIMEOUT_MS));
    }

    public void assertSaveButtonDisabled() {
        PlaywrightAssertions.assertThat(saveButton).isDisabled();
    }

    public boolean isSaveButtonDisabled() 
    {
        return !saveButton.isEnabled();
    }

    public String getToastMessage() 
    {
        if (!lastFeedbackMessage.isBlank()) {
            return lastFeedbackMessage;
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            String feedback = readVisibleFeedbackText();
            if (!feedback.isBlank()) {
                return feedback;
            }
            page.waitForTimeout(250);
        }
        throw new IllegalStateException("Unable to find a visible feedback message.");
    }

    public String getToastMessageIfPresent(double timeoutMs) 
    {
        if (!lastFeedbackMessage.isBlank()) {
            return lastFeedbackMessage;
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String feedback = readVisibleFeedbackText();
            if (!feedback.isBlank()) {
                return feedback;
            }
            page.waitForTimeout(250);
        }
        return "";
    }

    public boolean hasScheduledInspectionForAuditor(String auditor) 
    {
        Locator scheduledRows = page.locator(".custom-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText("Scheduled"))
                .filter(new Locator.FilterOptions().setHasText(auditor));
        return scheduledRows.count() > 0;
    }

    public void assertScheduledInspectionForAuditorVisible(String auditor) {
        Locator scheduledRows = page.locator(".custom-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText("Scheduled"))
                .filter(new Locator.FilterOptions().setHasText(auditor))
                .first();
        PlaywrightAssertions.assertThat(scheduledRows).isVisible();
    }

    public boolean rowHasAuditor(String zone, String auditor) 
    {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zone)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return rowHasAuditor(row, auditor);
    }

    public boolean rowHasDateRange(String zone, String startDate, String endDate) 
    {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zone)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return rowHasDateRange(row, startDate, endDate);
    }

    public boolean rowHasStartTime(String zone, String time) 
    {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zone)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return rowHasStartTime(row, time);
    }

    private boolean rowHasAuditor(Locator row, String auditor) 
    {
        Locator input = row.locator("input[placeholder='Select Auditor']").first();
        if (input.count() > 0) {
            String value = input.inputValue().trim();
            if (!value.isBlank()) {
                return value.equalsIgnoreCase(auditor);
            }
        }

        return row.innerText().toLowerCase().contains(auditor.toLowerCase());
    }

    private boolean rowHasDateRange(Locator row, String startDate, String endDate) 
    {
        Locator dateInputs = row.locator(".ant-picker input");
        if (dateInputs.count() >= 2) {
            return dateValuesMatch(startDate, dateInputs.nth(0).inputValue().trim())
                    && dateValuesMatch(endDate, dateInputs.nth(1).inputValue().trim());
        }

        String rowText = row.innerText();
        return rowTextContainsNormalizedDate(rowText, startDate)
                && rowTextContainsNormalizedDate(rowText, endDate);
    }

    private boolean rowHasStartTime(Locator row, String time) 
    {
        Locator timeInput = row.locator("input[placeholder='Select time']").first();
        if (timeInput.count() > 0) {
            return timeValuesMatch(time, timeInput.inputValue().trim());
        }

        return rowTextContainsNormalizedTime(row.innerText(), time);
    }

    public List<String> getValidationMessages() 
    {
        List<String> messages = new ArrayList<>();
        int count = validationMessages.count();
        for (int index = 0; index < count; index++) {
            String text = validationMessages.nth(index).innerText().trim();
            if (!text.isBlank()) {
                messages.add(text);
            }
        }
        if (!messages.isEmpty()) {
            return messages;
        }
        return new ArrayList<>(lastValidationMessages);
    }

    public boolean hasValidationMessageContaining(String expectedText) {
        String normalizedExpectedText = expectedText.toLowerCase(Locale.ENGLISH);
        if (getValidationMessages().stream()
                .anyMatch(message -> message.toLowerCase(Locale.ENGLISH).contains(normalizedExpectedText))) {
            return true;
        }

        if (!lastFeedbackMessage.isBlank()
                && lastFeedbackMessage.toLowerCase(Locale.ENGLISH).contains(normalizedExpectedText)) {
            return true;
        }

        if (isModalOpen()) {
            String modalText = modal.innerText().toLowerCase(Locale.ENGLISH);
            return modalText.contains(normalizedExpectedText);
        }

        return false;
    }

    public int getZoneRowCount() {
        waitForLoadingToFinish();
        return rows.count();
    }

    public void assertZoneRowsLoaded() {
        waitForLoadingToFinish();
        PlaywrightAssertions.assertThat(rows.first()).isVisible();
    }

    public boolean isDropdownValueSelected(String fieldLabel, String expectedValue) {
        Locator field = findFieldContainer(fieldLabel);
        return field.innerText().toLowerCase().contains(expectedValue.toLowerCase());
    }

    public void assertDropdownValueSelected(String fieldLabel, String expectedValue) {
        Locator field = findFieldContainer(fieldLabel);
        PlaywrightAssertions.assertThat(field).containsText(Pattern.compile(Pattern.quote(expectedValue), Pattern.CASE_INSENSITIVE));
    }

    public boolean isModalTitleVisible() {
        return modalTitle.isVisible() || modalBody.isVisible();
    }

    public void assertModalTitleVisible() {
        PlaywrightAssertions.assertThat(modalTitle.or(modalBody)).isVisible();
    }

    public boolean isConfigurationCopied(String expectedZone, String expectedAuditor) {
        Locator firstRow = getRow(0);
        String rowText = firstRow.innerText().toLowerCase();
        return rowText.contains(expectedZone.toLowerCase()) && rowText.contains(expectedAuditor.toLowerCase());
    }

    public boolean areTableFieldsCleared() {
        if (getZoneRowCount() == 0) {
            return true;
        }
        Locator firstRow = getRow(0);
        return firstRow.locator("input").all().stream()
                .map(Locator::inputValue)
                .allMatch(String::isBlank);
    }

    public void assertTableFieldsCleared() {
        if (!areTableFieldsCleared()) {
            throw new AssertionError("Expected table fields to be cleared.");
        }
    }

    public void waitForLoadingToFinish() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        if (loadingIndicator.count() > 0 && loadingIndicator.first().isVisible()) {
            loadingIndicator.first().waitFor(
                    new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(DEFAULT_TIMEOUT_MS));
        }
    }

    private void selectRadio(Locator radio) {
        radio.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        radio.click();
        waitForLoadingToFinish();
    }

    private boolean isRadioSelected(Locator radio) {
        String checked = radio.getAttribute("aria-checked");
        if (checked != null) {
            return Boolean.parseBoolean(checked);
        }
        return radio.isChecked();
    }

    private void selectDropdownOption(String fieldLabel, String optionText) {
        selectDropdownOption(new String[]{fieldLabel}, optionText);
    }

    private List<String> getVisibleDropdownOptions(String fieldLabel) {
        Locator container = findFieldContainer(fieldLabel);
        Locator trigger = resolveDropdownTrigger(container);
        clickDropdownTrigger(trigger);
        Locator visibleDropdown = waitForVisibleSelectDropdown();

        List<String> options = new ArrayList<>();
        Locator optionLocators = visibleDropdown.locator(".ant-select-item-option, [role='option']");
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = optionLocators.count();
            for (int index = 0; index < count; index++) {
                Locator option = optionLocators.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String text = option.innerText().trim();
                if (!text.isBlank() && !"No data".equalsIgnoreCase(text) && !options.contains(text)) {
                    options.add(text);
                }
            }

            if (!options.isEmpty()) {
                page.keyboard().press("Escape");
                return options;
            }
            page.waitForTimeout(250);
        }

        page.keyboard().press("Escape");
        throw new IllegalStateException("Unable to find visible options for dropdown: " + fieldLabel);
    }

    private String selectFirstDropdownOption(String fieldLabel) {
        return selectFirstDropdownOption(new String[]{fieldLabel});
    }

    private String selectFirstDropdownOption(String[] fieldLabels) {
        Locator container = null;
        for (String fieldLabel : fieldLabels) {
            try {
                container = findFieldContainer(fieldLabel);
                if (container.isVisible()) {
                    break;
            } }
            catch (RuntimeException ignored) {
                container = null;
            }
        }
        if (container == null) {
            throw new IllegalStateException("Unable to find dropdown field for labels: " + String.join(", ", fieldLabels));
        }

        Locator trigger = resolveDropdownTrigger(container);
        clickDropdownTrigger(trigger);
        Locator visibleDropdown = waitForVisibleSelectDropdown();

        Locator option = findFirstVisibleDropdownOption(visibleDropdown);
        String optionText = option.innerText().trim();
        option.click(new Locator.ClickOptions().setForce(true));
        return optionText;
    }

    private void selectDropdownOption(String[] fieldLabels, String optionText) {
        Locator container = null;
        for (String fieldLabel : fieldLabels) {
            try {
                container = findFieldContainer(fieldLabel);
                if (container.isVisible()) {
                    break;
                }
            } catch (RuntimeException ignored) {
                container = null;
            }
        }
        if (container == null) {
            throw new IllegalStateException("Unable to find dropdown field for labels: " + String.join(", ", fieldLabels));
        }

        if (container.innerText().toLowerCase().contains(optionText.toLowerCase())) {
            return;
        }

        Locator trigger = resolveDropdownTrigger(container);
        clickDropdownTrigger(trigger);
        waitForVisibleSelectDropdown();
        Locator option = findVisibleDropdownOptionByText(optionText);
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private Locator resolveDropdownTrigger(Locator container) {
        Locator selectors = container.locator(
                ".ant-select-selector, " +
                        "nz-select .ant-select-selector, " +
                        "[role='combobox']:visible");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = selectors.count();
            for (int index = 0; index < count; index++) {
                Locator selector = selectors.nth(index);
                if (selector.isVisible()) {
                    return selector;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to locate visible dropdown trigger in field: " + container.innerText());
    }

    private void clickDropdownTrigger(Locator trigger) {
        try {
            trigger.click(new Locator.ClickOptions().setForce(true).setTimeout(5000));
        } catch (RuntimeException ignored) {
            trigger.evaluate("element => element.click()");
        }
    }

    private boolean trySelectZoneCategory(String zoneCategory) {
        try {
            Locator container = findFieldContainer("Zone Category");
            if (container.innerText().toLowerCase().contains(zoneCategory.toLowerCase())) {
                return true;
            }

            Locator trigger = resolveDropdownTrigger(container);
            trigger.click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(500);

            Locator titledOption = page.getByTitle(zoneCategory).last();
            if (titledOption.count() > 0 && titledOption.isVisible()) {
                titledOption.click(new Locator.ClickOptions().setForce(true));
                return true;
            }

            Locator textOption = page.getByText(zoneCategory, new Page.GetByTextOptions().setExact(true)).last();
            if (textOption.count() > 0 && textOption.isVisible()) {
                textOption.click(new Locator.ClickOptions().setForce(true));
                return true;
            }
        } catch (RuntimeException ignored) {
            // Fall back to the generic dropdown helper below.
        }

        return false;
    }

    private Locator waitForVisibleSelectDropdown() {
        Locator visibleDropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        visibleDropdown.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        return visibleDropdown;
    }

    private Locator findFirstVisibleDropdownOption(Locator visibleDropdown) {
        Locator options = visibleDropdown.locator(".ant-select-item-option, [role='option']");
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String text = option.innerText().trim();
                if (!text.isBlank() && !"No data".equalsIgnoreCase(text)) {
                    return option;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find a visible dropdown option.");
    }

    private Locator findVisibleDropdownOptionByText(String optionText) {
        Locator visibleDropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (visibleDropdown.count() > 0 && visibleDropdown.isVisible()) {
                Locator exactText = visibleDropdown.getByText(optionText, new Locator.GetByTextOptions().setExact(true)).first();
                if (exactText.count() > 0 && exactText.isVisible()) {
                    return exactText;
                }

                Locator containsText = visibleDropdown.getByText(optionText).first();
                if (containsText.count() > 0 && containsText.isVisible()) {
                    return containsText;
                }
            }

            Locator options = visibleDropdown.locator(".ant-select-item-option, [role='option']");
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String text = option.innerText().trim();
                if (text.equalsIgnoreCase(optionText) || text.contains(optionText)) {
                    return option;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find visible dropdown option: " + optionText);
    }

    private void fillTableField(Locator row, String fieldName, String value) {
        if ("Auditor".equalsIgnoreCase(fieldName)) {
            fillAuditorField(row, value);
            return;
        }

        int columnIndex = getColumnIndex(fieldName);
        Locator cell = row.locator("td").nth(columnIndex);
        Locator input = cell.locator("input, textarea").first();
        Locator picker = cell.locator(".ant-picker").first();
        Locator combobox = cell.locator("nz-select, .ant-select, [role='combobox']").first();

        if (picker.count() > 0 && picker.isVisible()) {
            fillPickerField(picker, fieldName, value);
            return;
        }

        if (input.count() > 0 && input.isVisible()) {
            if (input.getAttribute("readonly") != null) {
                setInputValueWithEvents(input, value);
                return;
            } else {
                input.fill(value);
            }
            input.press("Tab");
            return;
        }

        if (combobox.count() == 0 || !combobox.isVisible()) {
            return;
        }

        selectComboboxValue(cell, combobox, value);
    }

    private void setRowField(String zoneName, String fieldName, String value) {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zoneName)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        fillTableField(row, fieldName, value);
    }

    private Locator getRow(int rowIndex) {
        rows.first().waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return rows.nth(rowIndex);
    }

    private void selectRowIfPossible(Locator row) {
        Locator checkbox = row.locator("input[type='checkbox']").first();
        if (checkbox.count() == 0 || checkbox.isChecked()) {
            return;
        }

        try {
            checkbox.check(new Locator.CheckOptions().setForce(true));
        } catch (RuntimeException ignored) {
            row.locator(".ant-checkbox-wrapper, .ant-checkbox").first()
                    .click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void clearSelectedRowsExcept(Locator targetRow) {
        String targetText = targetRow.innerText().trim();
        int rowCount = rows.count();

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            Locator checkbox = row.locator("input[type='checkbox']").first();
            if (checkbox.count() == 0 || !checkbox.isChecked()) {
                continue;
            }

            if (row.innerText().trim().equals(targetText)) {
                continue;
            }

            try {
                checkbox.uncheck(new Locator.UncheckOptions().setForce(true));
            } catch (RuntimeException ignored) {
                row.locator(".ant-checkbox-wrapper, .ant-checkbox").first()
                        .click(new Locator.ClickOptions().setForce(true));
            }
        }
    }

    private void waitForPostSaveFeedback() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            captureFeedbackSnapshot();
            if (!lastFeedbackMessage.isBlank()) {
                return;
            }
            if (!lastValidationMessages.isEmpty()) {
                return;
            }
            if (!isModalOpen()) {
                return;
            }
            page.waitForTimeout(250);
        }
    }

    private void clearFeedbackSnapshot() {
        lastFeedbackMessage = "";
        lastValidationMessages.clear();
    }

    private void captureFeedbackSnapshot() {
        String feedback = readVisibleFeedbackText();
        if (!feedback.isBlank()) {
            lastFeedbackMessage = feedback;
        }

        int count = validationMessages.count();
        for (int index = 0; index < count; index++) {
            String text = validationMessages.nth(index).innerText().trim();
            if (!text.isBlank() && !lastValidationMessages.contains(text)) {
                lastValidationMessages.add(text);
            }
        }
    }

    private void fillPickerField(Locator picker, String fieldName, String value) {
        picker.click();

        if ("Start Time".equalsIgnoreCase(fieldName)) {
            selectTimeFromPicker(value);
        } else {
            selectDateFromPicker(value);
        }

        page.waitForTimeout(300);
    }

    private void selectComboboxValue(Locator cell, Locator combobox, String value) {
        combobox.click();
        String resolvedValue = resolveDropdownValue(value);
        Locator selectedValue = cell.locator(".ant-select-selection-item, .ant-select-selection-overflow").first();

        Locator searchInput = cell.locator(".ant-select-selection-search-input, input[role='combobox']").first();
        if (searchInput.count() > 0 && searchInput.isVisible()) {
            try {
                searchInput.fill("");
                searchInput.fill(resolvedValue);
                searchInput.press("ArrowDown");
                searchInput.press("Enter");
                if (waitForSelectedComboboxValue(selectedValue, resolvedValue, 1500)) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // Some select controls are readonly-search; fallback is option click only.
            }
        }

        Locator visibleDropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        visibleDropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        Locator option = visibleDropdown.locator(".ant-select-item-option, [role='option']")
                .filter(new Locator.FilterOptions().setHasText(resolvedValue))
                .first();
        option.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        option.click(new Locator.ClickOptions().setForce(true));
        waitForSelectedComboboxValue(selectedValue, resolvedValue, DEFAULT_TIMEOUT_MS);
    }

    private String resolveDropdownValue(String requestedValue) {
        if ("Daily".equalsIgnoreCase(requestedValue)) {
            return "Every day";
        }
        if ("Weekly".equalsIgnoreCase(requestedValue)) {
            return "Every week";
        }
        return requestedValue;
    }

    private void selectDateFromPicker(String value) {
        selectDateFromPicker(value, true);
    }

    private void selectDateFromPicker(String value, boolean clickOk) {
        Locator visibleDropdown = page.locator(".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)").last();
        visibleDropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

        LocalDate targetDate = LocalDate.parse(value);
        String dateTitle = targetDate.toString();
        Locator targetCell = visibleDropdown.locator(
                String.format("td.ant-picker-cell[title='%s'], .ant-picker-cell[title='%s']", dateTitle, dateTitle));

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (targetCell.count() > 0 && targetCell.first().isVisible()) {
                targetCell.first().click();
                if (clickOk) {
                    clickPickerOkIfVisible(visibleDropdown);
                }
                return;
            }

            Locator nextButton = visibleDropdown.locator(".ant-picker-header-next-btn, button[aria-label='Next month']").first();
            if (nextButton.count() == 0 || !nextButton.isVisible()) {
                break;
            }
            nextButton.click();
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to select date from picker: " + value);
    }

    private boolean isDateDisabledInOpenPicker(String value) {
        Locator visibleDropdown = page.locator(".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)").last();
        visibleDropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

        LocalDate targetDate = LocalDate.parse(value);
        String dateTitle = targetDate.toString();
        Locator targetCell = visibleDropdown.locator(
                String.format("td.ant-picker-cell[title='%s'], .ant-picker-cell[title='%s']", dateTitle, dateTitle));

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (targetCell.count() > 0 && targetCell.first().isVisible()) {
                String classes = targetCell.first().getAttribute("class");
                return classes != null && classes.contains("ant-picker-cell-disabled");
            }

            Locator nextButton = visibleDropdown.locator(".ant-picker-header-next-btn, button[aria-label='Next month']").first();
            if (nextButton.count() == 0 || !nextButton.isVisible()) {
                break;
            }
            nextButton.click();
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find date in picker: " + value);
    }

    private void closeVisibleDatePicker() {
        Locator visibleDropdown = page.locator(".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)").last();
        if (visibleDropdown.count() > 0 && visibleDropdown.isVisible()) {
            page.keyboard().press("Escape");
            page.waitForTimeout(200);
        }
    }

    private void selectTimeFromPicker(String value) {
        Locator visibleDropdown = page.locator(".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)").last();
        visibleDropdown.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

        LocalTime targetTime = parseUiTime(value);
        String hour = String.format("%02d", targetTime.getHour());
        String minute = String.format("%02d", targetTime.getMinute());

        clickTimeCell(visibleDropdown, 0, hour);
        clickTimeCell(visibleDropdown, 1, minute);
        clickPickerOkIfVisible(visibleDropdown);
    }

    private void clickTimeCell(Locator dropdown, int columnIndex, String cellText) {
        Locator column = dropdown.locator(".ant-picker-time-panel-column").nth(columnIndex);
        Locator targetCell = column.locator(".ant-picker-time-panel-cell-inner")
                .filter(new Locator.FilterOptions().setHasText(cellText))
                .first();
        targetCell.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        targetCell.click();
    }

    private void clickPickerOkIfVisible(Locator dropdown) {
        Locator okButton = dropdown.locator(".ant-picker-ok button, .ant-picker-ok .ant-btn").first();
        if (okButton.count() > 0 && okButton.isVisible()) {
            okButton.click();
        }
    }

    private void clickVisiblePickerOkIfPresent() {
        Locator okButtons = page.locator(
                ".ant-picker-dropdown:not(.ant-picker-dropdown-hidden) .ant-picker-ok button, " +
                        ".ant-picker-dropdown:not(.ant-picker-dropdown-hidden) .ant-picker-ok .ant-btn");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            int count = okButtons.count();
            for (int index = count - 1; index >= 0; index--) {
                Locator okButton = okButtons.nth(index);
                if (okButton.isVisible()) {
                    okButton.click(new Locator.ClickOptions().setForce(true));
                    return;
                }
            }
            page.waitForTimeout(200);
        }

        Locator visibleDropdown = page.locator(".ant-picker-dropdown:not(.ant-picker-dropdown-hidden)").last();
        if (visibleDropdown.count() > 0 && visibleDropdown.isVisible()) {
            page.keyboard().press("Escape");
        }
    }

    private LocalTime parseUiTime(String value) {
        try {
            return LocalTime.parse(value, UI_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(value.toUpperCase(Locale.ENGLISH), UI_TIME_FORMAT_12_HOUR);
        }
    }

    private boolean dateValuesMatch(String expected, String actual) {
        Optional<LocalDate> expectedDate = parseUiDate(expected);
        Optional<LocalDate> actualDate = parseUiDate(actual);
        if (expectedDate.isPresent() && actualDate.isPresent()) {
            return expectedDate.get().equals(actualDate.get());
        }
        return expected.equalsIgnoreCase(actual);
    }

    private boolean timeValuesMatch(String expected, String actual) {
        Optional<LocalTime> expectedTime = parseFlexibleTime(expected);
        Optional<LocalTime> actualTime = parseFlexibleTime(actual);
        if (expectedTime.isPresent() && actualTime.isPresent()) {
            return expectedTime.get().equals(actualTime.get());
        }
        return expected.equalsIgnoreCase(actual);
    }

    private boolean rowTextContainsNormalizedDate(String rowText, String expectedDate) {
        Optional<LocalDate> parsedExpected = parseUiDate(expectedDate);
        if (parsedExpected.isEmpty()) {
            return rowText.contains(expectedDate);
        }

        String isoDate = parsedExpected.get().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String slashDate = parsedExpected.get().format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH));
        return rowText.contains(isoDate) || rowText.contains(slashDate);
    }

    private boolean rowTextContainsNormalizedTime(String rowText, String expectedTime) {
        Optional<LocalTime> parsedExpected = parseFlexibleTime(expectedTime);
        if (parsedExpected.isEmpty()) {
            return rowText.toLowerCase(Locale.ENGLISH).contains(expectedTime.toLowerCase(Locale.ENGLISH));
        }

        String twentyFourHour = parsedExpected.get().format(UI_TIME_FORMAT);
        String twelveHour = parsedExpected.get().format(UI_TIME_FORMAT_12_HOUR);
        String normalizedRowText = rowText.toLowerCase(Locale.ENGLISH);
        return normalizedRowText.contains(twentyFourHour.toLowerCase(Locale.ENGLISH))
                || normalizedRowText.contains(twelveHour.toLowerCase(Locale.ENGLISH));
    }

    private Optional<LocalDate> parseUiDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));

        for (String candidate : dateCandidates(value)) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return Optional.of(LocalDate.parse(candidate, formatter));
                } catch (DateTimeParseException ignored) {
                    // Try the next supported UI format.
                }
            }
        }
        return Optional.empty();
    }

    private List<String> dateCandidates(String value) {
        String trimmed = value.trim().replaceAll("\\s+", " ");
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);

        List<Pattern> patterns = List.of(
                Pattern.compile("\\d{4}-\\d{2}-\\d{2}"),
                Pattern.compile("\\d{4}/\\d{2}/\\d{2}"),
                Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}"),
                Pattern.compile("\\d{1,2}\\s+[A-Za-z]{3,}\\s+\\d{4}"),
                Pattern.compile("[A-Za-z]{3,}\\s+\\d{1,2},\\s+\\d{4}"));

        for (Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(trimmed);
            while (matcher.find()) {
                String candidate = matcher.group();
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }

        return candidates;
    }

    private Optional<LocalTime> parseFlexibleTime(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseUiTime(value.trim()));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private boolean waitForSelectedComboboxValue(Locator selectedValue, String expectedValue, double timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (selectedValue.count() > 0) {
                String text = selectedValue.innerText().trim();
                if (text.toLowerCase(Locale.ENGLISH).contains(expectedValue.toLowerCase(Locale.ENGLISH))) {
                    return true;
                }
            }
            page.waitForTimeout(250);
        }
        return false;
    }

    private void setInputValueWithEvents(Locator input, String value) {
        input.evaluate("(element, newValue) => {" +
                "const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "nativeInputValueSetter.call(element, newValue);" +
                "element.dispatchEvent(new Event('input', { bubbles: true }));" +
                "element.dispatchEvent(new Event('change', { bubbles: true }));" +
                "element.dispatchEvent(new Event('blur', { bubbles: true }));" +
                "}", value);
    }

    private void clickVisibleModalButton(String buttonText) {
        page.evaluate("text => {" +
                "const normalizedText = String(text).trim().toLowerCase();" +
                "const buttons = Array.from(document.querySelectorAll('.ant-modal-content button'));" +
                "const button = buttons.find(element => {" +
                "const label = (element.textContent || '').trim().toLowerCase();" +
                "const rect = element.getBoundingClientRect();" +
                "return label === normalizedText && rect.width > 0 && rect.height > 0;" +
                "});" +
                "if (!button) throw new Error(`Unable to find visible modal button: `);" +
                "button.click();" +
                "}", buttonText);
    }

    private boolean isDomVisible(Locator locator) {
        return Boolean.TRUE.equals(locator.evaluate("element => {" +
                "const rect = element.getBoundingClientRect();" +
                "const style = window.getComputedStyle(element);" +
                "return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;" +
                "}"));
    }

    private String safeAttribute(Locator locator, String name) {
        try {
            String value = locator.getAttribute(name);
            return value == null ? "" : value;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private String readVisibleFeedbackText() {
        Locator notification = page.locator(".ant-notification-notice:visible, .ant-message-notice:visible").first();
        if (notification.count() > 0 && notification.isVisible()) {
            return notification.innerText().trim();
        }

        Locator sweetAlert = page.locator(".swal-overlay--show-modal .swal-modal:visible").first();
        if (sweetAlert.count() > 0 && sweetAlert.isVisible()) {
            return sweetAlert.innerText().trim();
        }

        Locator sweetAlert2Title = page.locator(".swal2-container .swal2-title:visible").first();
        Locator sweetAlert2Body = page.locator(".swal2-container .swal2-html-container:visible").first();
        if (sweetAlert2Title.count() > 0 && sweetAlert2Title.isVisible()) {
            String titleText = sweetAlert2Title.innerText().trim();
            String bodyText = "";
            if (sweetAlert2Body.count() > 0 && sweetAlert2Body.isVisible()) {
                bodyText = sweetAlert2Body.innerText().trim();
            }
            return (titleText + " " + bodyText).trim();
        }

        Locator alert = page.locator("[role='alert']:visible").first();
        if (alert.count() > 0 && alert.isVisible()) {
            return alert.innerText().trim();
        }

        return "";
    }

    private Locator findFieldContainer(String label) {
        Locator root = modalBody.count() > 0 && modalBody.isVisible() ? modalBody : modal;
        String normalizedLabel = label.endsWith(":") ? label.substring(0, label.length() - 1) : label;

        for (String exactLabel : new String[]{label, normalizedLabel + ":", normalizedLabel}) {
            Locator exactContainers = root.locator("xpath=.//*[normalize-space()='" + exactLabel + "']/ancestor::*[self::div or self::nz-form-item][1]");
            Optional<Locator> visibleDropdownContainer = firstVisibleWithDropdown(exactContainers);
            if (visibleDropdownContainer.isPresent()) {
                return visibleDropdownContainer.get();
            }
        }

        Locator partialContainers = root.locator("xpath=.//*[contains(normalize-space(), '" + normalizedLabel + "')]/ancestor::*[self::div or self::nz-form-item][1]");
        Optional<Locator> visiblePartialContainer = firstVisibleWithDropdown(partialContainers);
        if (visiblePartialContainer.isPresent()) {
            return visiblePartialContainer.get();
        }

        visiblePartialContainer = firstVisible(partialContainers);
        if (visiblePartialContainer.isPresent()) {
            return visiblePartialContainer.get();
        }

        Locator container = partialContainers.first();
        container.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return container;
    }

    private Optional<Locator> firstVisibleWithDropdown(Locator locators) {
        Optional<Locator> visibleLocator = firstVisible(locators);
        while (visibleLocator.isPresent()) {
            Locator locator = visibleLocator.get();
            if (locator.locator(".ant-select-selector:visible, nz-select .ant-select-selector:visible, [role='combobox']:visible").count() > 0) {
                return visibleLocator;
            }

            int count = locators.count();
            for (int index = 0; index < count; index++) {
                Locator candidate = locators.nth(index);
                if (candidate.isVisible()
                        && candidate.locator(".ant-select-selector:visible, nz-select .ant-select-selector:visible, [role='combobox']:visible").count() > 0) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Locator> firstVisible(Locator locators) {
        if (locators.count() == 0) {
            return Optional.empty();
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = locators.count();
            for (int index = 0; index < count; index++) {
                Locator locator = locators.nth(index);
                if (locator.isVisible()) {
                    return Optional.of(locator);
                }
            }
            page.waitForTimeout(250);
        }
        return Optional.empty();
    }

    // private int getColumnIndex(String columnName) {
    //     int headerCount = modal.locator("thead th").count();
    //     for (int index = 0; index < headerCount; index++) {
    //         String headerText = modal.locator("thead th").nth(index).innerText().trim();
    //         if (headerText.equalsIgnoreCase(columnName)) {
    //             return index;
    //         }
    //         if (columnName.equalsIgnoreCase("Start Time") && headerText.equalsIgnoreCase("StartTime")) {
    //             return index;
    //         }
    //     }
    //     throw new IllegalStateException("Unable to find table column: " + columnName);
    // }

    private int getColumnIndex(String columnName) {
        Locator modalHeaders = modal.locator("table thead th");
        if (modalHeaders.count() > 0 && modal.isVisible()) {
            Optional<Integer> modalIndex = findColumnIndex(modalHeaders, columnName);
            if (modalIndex.isPresent()) {
                return modalIndex.get();
            }
        }

        return findColumnIndex(page.locator("main table thead th"), columnName)
                .orElseThrow(() -> new IllegalStateException("Column not found: " + columnName));
    }

    private Optional<Integer> findColumnIndex(Locator headers, String columnName) {
        String target = normalizeColumnName(columnName);
        int count = headers.count();

        for (int i = 0; i < count; i++) {
            if (normalizeColumnName(headers.nth(i).innerText()).equals(target)) {
                return Optional.of(i);
            }
        }

        for (int i = 0; i < count; i++) {
            String headerText = normalizeColumnName(headers.nth(i).innerText());
            if (headerText.contains(target) || target.contains(headerText)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }

    private String normalizeColumnName(String value) {
        String normalized = value.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        if (normalized.equals("start time")) {
            return "starttime";
        }
        if (normalized.equals("date")) {
            return "audit date";
        }
        return normalized;
    }

    // private void waitForFrequencySummary(int rowIndex, List<String> expectedTokens) {
    //     long start = System.currentTimeMillis();
    //     while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
    //         String summary = getFrequencySummary(rowIndex).toLowerCase(Locale.ENGLISH);
    //         boolean allPresent = summaryMatchesExpectedTokens(summary, expectedTokens);
    //         if (allPresent) {
    //             return;
    //         }
    //         page.waitForTimeout(250);
    //     }

    //     throw new IllegalStateException(
    //             "Frequency summary did not contain expected tokens: " + expectedTokens
    //                     + ". Actual summary: '" + getFrequencySummary(rowIndex) + "'");
    // }

    // private boolean summaryMatchesExpectedTokens(String summary, List<String> expectedTokens) {
    //     for (String expectedToken : expectedTokens) {
    //         String normalizedToken = expectedToken.toLowerCase(Locale.ENGLISH);
    //         if (summary.contains(normalizedToken)) {
    //             continue;
    //         }

    //         if (isSingularIntervalToken(normalizedToken) && summaryContainsFrequencyWord(summary, expectedTokens)) {
    //             continue;
    //         }

    //         if (matchesNaturalLanguageFrequency(summary, normalizedToken)) {
    //             continue;
    //         }

    //         return false;
    //     }
    //     return true;
    // }

    // private boolean isSingularIntervalToken(String normalizedToken) {
    //     return "1".equals(normalizedToken);
    // }

    // private boolean summaryContainsFrequencyWord(String summary, List<String> expectedTokens) {
    //     return expectedTokens.stream()
    //             .map(token -> token.toLowerCase(Locale.ENGLISH))
    //             .filter(token -> !"1".equals(token))
    //             .anyMatch(summary::contains);
    // }

    // private boolean matchesNaturalLanguageFrequency(String summary, String normalizedToken) {
    //     return switch (normalizedToken) {
    //         case "daily" -> summary.contains("every day");
    //         case "monthly" -> summary.contains("every month");
    //         // case "yearly" -> summary.contains("every year");
    //         default -> false;
    //     };
    // }

    public void waitForScheduleListToRefresh() 
    {
    Locator rows = page.locator("table tbody tr");

    // Wait for at least one row to appear
    rows.first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(10000));

    // Optional stabilization
    page.waitForTimeout(500);
    }

    private Locator visibleScheduleRows(String zone, String auditor) {
        waitForScheduleListToRefresh();
        return page.locator("main tbody tr")
                .filter(new Locator.FilterOptions().setHasText(zone))
                .filter(new Locator.FilterOptions().setHasText(auditor));
    }

    private void clickScheduledAuditActions(String template,
                                            String building,
                                            String level,
                                            String zone,
                                            String auditDate,
                                            String auditor)
    {
        clickScheduledAuditActions(template, building, level, zone, auditDate, auditor, true);
    }

    private void clickScheduledAuditActions(String template,
                                            String building,
                                            String level,
                                            String zone,
                                            String auditDate,
                                            String auditor,
                                            boolean waitForDeleteOption)
    {
        goToFirstSchedulePage();
        Set<String> visitedPages = new LinkedHashSet<>();

        while (true) {
            waitForScheduleListToRefresh();
            String pageSignature = currentSchedulePageSignature();
            if (!visitedPages.add(pageSignature)) {
                break;
            }

            Optional<Locator> row = findScheduledAuditRowOnCurrentPage(
                    template,
                    building,
                    level,
                    zone,
                    auditDate,
                    auditor);
            if (row.isPresent()) {
                int actionsColumnIndex = getSchedulePageColumnIndex("Actions");
                Locator actionsCell = row.get().locator("td").nth(actionsColumnIndex);
                // Locator menuButton = actionsCell.locator(
                //         ".ant-dropdown-trigger:visible, [aria-label='ellipsis']:visible, button:visible")
                //         .first();

                Locator menuButton = actionsCell.locator(
                        ".ant-dropdown-trigger, " +
                        ".ant-btn-icon-only, " +
                        "button, " +
                        "[role='button']")
                        .first();
                menuButton.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(DEFAULT_TIMEOUT_MS));

                menuButton.scrollIntoViewIfNeeded();

                menuButton.click(new Locator.ClickOptions()
                        .setForce(true));

                // menuButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
                // menuButton.click(new Locator.ClickOptions().setForce(true));
                if (waitForDeleteOption) {
                    waitForDeleteAuditMenu();
                } else {
                    waitForAuditActionMenu();
                }
                return;
            }

            if (!goToNextSchedulePage()) {
                break;
            }
        }

        throw new IllegalStateException("Unable to find action menu for scheduled audit template=" + template
                + ", building=" + building
                + ", level=" + level
                + ", zone=" + zone
                + ", auditDate=" + auditDate
                + ", auditor=" + auditor + ".");
    }

    private Optional<Locator> findScheduledAuditRowOnCurrentPage(String template,
                                                                String building,
                                                                String level,
                                                                String zone,
                                                                String auditDate,
                                                                String auditor)
    {
        int templateColumnIndex = getSchedulePageColumnIndex("Template");
        int buildingColumnIndex = getSchedulePageColumnIndex("Building");
        int levelColumnIndex = getSchedulePageColumnIndex("Level");
        int zoneColumnIndex = getSchedulePageColumnIndex("Zone");
        int auditorColumnIndex = getSchedulePageColumnIndex("Assigned To");
        int auditDateColumnIndex = getSchedulePageColumnIndex("Audit Date");
        Locator rows = page.locator("main tbody tr:not(.ant-table-placeholder)");
        int rowCount = rows.count();

        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            if (!row.isVisible()) {
                continue;
            }

            if (cellText(row, templateColumnIndex).equalsIgnoreCase(template)
                    && cellText(row, buildingColumnIndex).equalsIgnoreCase(building)
                    && cellText(row, levelColumnIndex).equalsIgnoreCase(level)
                    && cellText(row, zoneColumnIndex).equalsIgnoreCase(zone)
                    && dateValuesMatch(auditDate, cellText(row, auditDateColumnIndex))
                    && cellText(row, auditorColumnIndex).equalsIgnoreCase(auditor)) {
                return Optional.of(row);
            }
        }

        return Optional.empty();
    }

    private void clickDeleteAuditMenuItem()
    {
        Locator deleteAudit = visibleDeleteAuditMenuItem();
        deleteAudit.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        deleteAudit.click(new Locator.ClickOptions().setForce(true));
    }

    private void clickEditAuditMenuItem()
    {
        Locator editAudit = visibleEditAuditMenuItem();
        editAudit.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        editAudit.click(new Locator.ClickOptions().setForce(true));
    }

    private void clickReAuditMenuItem()
    {
        Locator reAudit = visibleReAuditMenuItem();
        reAudit.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        reAudit.click(new Locator.ClickOptions().setForce(true));
    }

    private void waitForDeleteConfirmationModal()
    {
        visibleDeleteConfirmationButton().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
    }


    private void clickConfirmDelete() {

    // Step 1: Identify visible drawer
    Locator drawer = page.locator(".ant-drawer-content:visible").last();
    drawer.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

    // Step 2: Locate Delete button inside drawer ONLY
    Locator deleteBtn = drawer.getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Delete"));

        deleteBtn.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));

    // Step 3: Scroll INSIDE drawer (critical)
        deleteBtn.scrollIntoViewIfNeeded();

    // Step 4: Stabilize UI (drawer animation / footer render)
        page.waitForTimeout(300);

    // Step 5: Click safely
        try {
            deleteBtn.click();
        } catch (Exception e) {
            // fallback for tricky overlay/scroll issues
            deleteBtn.evaluate("el => el.click()");
        }
    }

    private void waitForDeleteAuditMenu()
    {
        visibleDeleteAuditMenuItem().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private void waitForAuditActionMenu()
    {
        page.locator(".ant-dropdown:not(.ant-dropdown-hidden):visible, .ant-dropdown-menu:visible, [role='menu']:visible")
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private Locator visibleDeleteAuditMenuItem()
    {
        return page.locator(
                ".ant-dropdown:not(.ant-dropdown-hidden):visible .ant-dropdown-menu-title-content:text-is('Delete Audit'), " +
                        ".ant-dropdown-menu:visible .ant-dropdown-menu-title-content:text-is('Delete Audit'), " +
                        ".ant-dropdown:visible .ant-dropdown-menu-item:has-text('Delete Audit'), " +
                        "[role='menu']:visible :text-is('Delete Audit')")
                .first();
    }

    private Locator visibleEditAuditMenuItem()
    {
        return page.locator(
                ".ant-dropdown:not(.ant-dropdown-hidden):visible .ant-dropdown-menu-title-content:text-is('Edit Audit'), " +
                        ".ant-dropdown-menu:visible .ant-dropdown-menu-title-content:text-is('Edit Audit'), " +
                        ".ant-dropdown:visible .ant-dropdown-menu-item:has-text('Edit Audit'), " +
                        "[role='menu']:visible :text-is('Edit Audit')")
                .first();
    }

    private Locator visibleReAuditMenuItem()
    {
        return page.locator(
                ".ant-dropdown:not(.ant-dropdown-hidden):visible .ant-dropdown-menu-title-content:text-is('Re-Audit'), " +
                        ".ant-dropdown:not(.ant-dropdown-hidden):visible .ant-dropdown-menu-title-content:text-is('Re Audit'), " +
                        ".ant-dropdown-menu:visible .ant-dropdown-menu-title-content:text-is('Re-Audit'), " +
                        ".ant-dropdown-menu:visible .ant-dropdown-menu-title-content:text-is('Re Audit'), " +
                        ".ant-dropdown:visible .ant-dropdown-menu-item:has-text('Re-Audit'), " +
                        ".ant-dropdown:visible .ant-dropdown-menu-item:has-text('Re Audit'), " +
                        "[role='menu']:visible :text-is('Re-Audit'), " +
                        "[role='menu']:visible :text-is('Re Audit')")
                .first();
    }

    private Locator visibleDeleteConfirmationButton()
    {
        return page.locator(
                ".ant-modal:visible button:has-text('Delete'), " +
                        ".ant-popover:visible button:has-text('Delete'), " +
                        ".ant-popconfirm:visible button:has-text('Delete'), " +
                        ".swal2-container .swal2-confirm:visible, " +
                        ".swal-overlay--show-modal button:visible:has-text('Delete'), " +
                        "[role='dialog']:visible button:has-text('Delete')")
                .last();
    }

    public String captureDeleteSuccessBanner() {

    // Target banner near header (based on your UI)
    Locator banner = page.locator("text=Audit successfully deleted");

    banner.waitFor(new Locator.WaitForOptions()
            .setTimeout(5000));

    return banner.first().innerText().trim();
   
    }

    public void openDeleteAuditDrawer(String template, String building, String level,
                                  String zone, String auditDate, String auditor) 
    {

        clickScheduledAuditActions(template, building, level, zone, auditDate, auditor);
        clickDeleteAuditMenuItem();
        waitForDeleteConfirmationModal(); // your existing method
    }

    public void clickCancelDelete() {

        // Target ONLY delete drawer
        Locator drawer = page.locator(".ant-drawer-content").last();

        // Scoped cancel button
        Locator cancelBtn = drawer.getByRole(AriaRole.BUTTON,
                new GetByRoleOptions().setName("Cancel"));

        cancelBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));

        cancelBtn.scrollIntoViewIfNeeded();

        cancelBtn.click();

        // CRITICAL WAIT (this is missing in your code)
        drawer.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(5000));

        page.waitForTimeout(300); // animation buffer
    }
    public boolean isDeleteDrawerVisible() 
    {
        Locator drawer = page.locator(".ant-drawer-content").last();

        return drawer.count() > 0 && drawer.first().isVisible();
    }

    public void assertDeleteDrawerHidden() {
        Locator drawer = page.locator(".ant-drawer-content").last();
        PlaywrightAssertions.assertThat(drawer).isHidden();
    }

    private void waitForEditAuditDrawer()
    {
        visibleEditAuditDrawer().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private Locator visibleEditAuditDrawer()
    {
        return page.locator(
                ".ant-drawer-content:visible:has-text('Edit Audit'), " +
                        ".ant-drawer-content:visible:has-text('Audit Date'), " +
                        ".ant-drawer-content:visible:has-text('Auditor')")
                .last();
    }

    private void fillEditAuditDate(String auditDate)
    {
        Locator drawer = visibleEditAuditDrawer();
        Locator field = findFieldContainerInRoot(drawer, "Audit Date");
        Locator picker = field.locator(".ant-picker:visible").first();
        if (picker.count() == 0) {
            picker = field.locator("input:visible").first();
        }

        picker.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        try {
            picker.scrollIntoViewIfNeeded();
            picker.click(new Locator.ClickOptions().setForce(true));
        } catch (RuntimeException ex) {
            picker.evaluate("element => element.click()");
        }
        selectDateFromPicker(auditDate);
        page.waitForTimeout(300);
    }

    private void fillEditAuditAuditor(String auditor)
    {
        Locator trigger = resolveEditAuditAuditorTrigger();
        clickDropdownTrigger(trigger);
        waitForVisibleSelectDropdown();
        Locator option = findVisibleDropdownOptionByText(auditor);
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private String fillEditAuditWithAnyDifferentAuditor(String currentAuditor)
    {
        Locator trigger = resolveEditAuditAuditorTrigger();
        clickDropdownTrigger(trigger);
        Locator dropdown = waitForVisibleSelectDropdown();
        Locator options = dropdown.locator(".ant-select-item-option, [role='option']");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String optionText = option.innerText().trim();
                if (!optionText.isBlank()
                        && !"No data".equalsIgnoreCase(optionText)
                        && !optionText.equalsIgnoreCase(currentAuditor)) {
                    option.click(new Locator.ClickOptions().setForce(true));
                    return optionText;
                }
            }
            page.waitForTimeout(250);
        }

        page.keyboard().press("Escape");
        throw new IllegalStateException("Unable to find a different auditor option for Edit Audit.");
    }

    private Locator resolveEditAuditAuditorTrigger()
    {
        Locator drawer = visibleEditAuditDrawer();
        for (String label : new String[]{"Auditor", "Auditor Name", "Assigned To"}) {
            try {
                Locator field = findFieldContainerInRoot(drawer, label);
                return resolveDropdownTrigger(field);
            } catch (RuntimeException ignored) {
                // Try the next label or the generic drawer-level select fallback.
            }
        }

        Locator selectors = drawer.locator(".ant-select-selector:visible, nz-select .ant-select-selector:visible, [role='combobox']:visible");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            int count = selectors.count();
            for (int index = count - 1; index >= 0; index--) {
                Locator selector = selectors.nth(index);
                if (selector.isVisible()) {
                    return selector;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to locate auditor selector in the Edit Audit drawer.");
    }

    private void fillReAuditReason(String reason)
    {
        if (reason == null || reason.isBlank()) {
            return;
        }

        Locator drawer = visibleEditAuditDrawer();
        Locator reasonControl = drawer.locator("textarea:visible").first();

        if (!isVisible(reasonControl)) {
            try {
                Locator field = findFieldContainerInRoot(drawer, "Reason");
                reasonControl = field.locator("textarea:visible, input:visible").last();
            } catch (RuntimeException ignored) {
                reasonControl = drawer.locator("input:visible, textarea:visible").last();
            }
        }

        if (!isVisible(reasonControl)) {
            throw new IllegalStateException("Unable to locate Reason field in the Re-Audit drawer.");
        }

        reasonControl.scrollIntoViewIfNeeded();
        reasonControl.fill(reason);
        reasonControl.press("Tab");
    }

    private void clickSaveEditAudit()
    {
        Locator saveButton = page.locator(
                ".ant-drawer:visible button:has-text('Save'), " +
                        ".ant-drawer:visible button:has-text('Schedule'), " +
                        ".ant-drawer:visible button:has-text('Re-Audit'), " +
                        ".ant-drawer:visible button:has-text('Re Audit'), " +
                        ".ant-drawer:visible button:has-text('Update'), " +
                        ".ant-drawer:visible button:has-text('Submit'), " +
                        ".ant-drawer:visible .ant-drawer-footer button.ant-btn-primary:visible, " +
                        ".ant-drawer:visible button.ant-btn-primary:visible")
                .last();
        saveButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        saveButton.scrollIntoViewIfNeeded();
        try {
            saveButton.click(new Locator.ClickOptions().setForce(true));
        } catch (RuntimeException ex) {
            saveButton.evaluate("element => element.click()");
        }
    }

    private String captureEditAuditFeedback()
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            String feedback = readVisibleFeedbackText();
            if (!feedback.isBlank()) {
                return feedback;
            }
            page.waitForTimeout(250);
        }

        return "";
    }

    private Locator findFieldContainerInRoot(Locator root, String label)
    {
        String normalizedLabel = label.endsWith(":") ? label.substring(0, label.length() - 1) : label;

        for (String exactLabel : new String[]{label, normalizedLabel + ":", normalizedLabel}) {
            Locator exactContainers = root.locator("xpath=.//*[normalize-space()='" + exactLabel + "']/ancestor::*[self::div or self::nz-form-item][1]");
            Optional<Locator> visibleDropdownContainer = firstVisibleWithDropdown(exactContainers);
            if (visibleDropdownContainer.isPresent()) {
                return visibleDropdownContainer.get();
            }

            Optional<Locator> visibleContainer = firstVisible(exactContainers);
            if (visibleContainer.isPresent()) {
                return visibleContainer.get();
            }
        }

        Locator partialContainers = root.locator("xpath=.//*[contains(normalize-space(), '" + normalizedLabel + "')]/ancestor::*[self::div or self::nz-form-item][1]");
        Optional<Locator> visiblePartialContainer = firstVisibleWithDropdown(partialContainers);
        if (visiblePartialContainer.isPresent()) {
            return visiblePartialContainer.get();
        }

        visiblePartialContainer = firstVisible(partialContainers);
        if (visiblePartialContainer.isPresent()) {
            return visiblePartialContainer.get();
        }

        Locator container = partialContainers.first();
        container.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return container;
    }

    private boolean isVisible(Locator locator)
    {
        try {
            return locator.count() > 0 && locator.first().isVisible();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String cellText(Locator row, int columnIndex)
    {
        Locator cells = row.locator("td");
        if (cells.count() <= columnIndex) {
            return "";
        }
        return cells.nth(columnIndex).innerText().trim();
    }

    private void goToFirstSchedulePage()
    {
        Locator previousButton = schedulePaginationButton(".ant-pagination-prev");
        int guard = 0;
        while (previousButton.count() > 0 && isPaginationButtonEnabled(previousButton) && guard++ < 100) {
            previousButton.click();
            waitForScheduleListToRefresh();
            waitForLoadingToFinish();
        }
    }

    private boolean goToNextSchedulePage()
    {
        Locator nextButton = schedulePaginationButton(".ant-pagination-next");
        if (nextButton.count() == 0 || !isPaginationButtonEnabled(nextButton)) {
            return false;
        }

        nextButton.click();
        waitForScheduleListToRefresh();
        waitForLoadingToFinish();
        return true;
    }

    private Locator schedulePaginationButton(String selector)
    {
        return page.locator("main .ant-pagination " + selector + ":visible").first();
    }

    private boolean isPaginationButtonEnabled(Locator button)
    {
        if (button.count() == 0 || !button.isVisible()) {
            return false;
        }

        String className = button.getAttribute("class");
        String ariaDisabled = button.getAttribute("aria-disabled");
        return (className == null || !className.contains("ant-pagination-disabled"))
                && !"true".equalsIgnoreCase(ariaDisabled);
    }

    private String currentSchedulePageSignature()
    {
        String activePage = "";
        Locator activePageItem = page.locator("main .ant-pagination .ant-pagination-item-active").first();
        if (activePageItem.count() > 0) {
            activePage = activePageItem.innerText().trim();
        }

        Locator rows = page.locator("main tbody tr");
        List<String> rowTexts = new ArrayList<>();
        int rowCount = rows.count();
        for (int index = 0; index < rowCount; index++) {
            rowTexts.add(rows.nth(index).innerText().trim());
        }

        return activePage + "|" + String.join("|", rowTexts);
    }

    private int countMatchingDates(List<String> actualDates, List<String> expectedDates) {
        int matches = 0;
        for (String expectedDate : expectedDates) {
            if (actualDates.stream().anyMatch(actualDate -> dateValuesMatch(expectedDate, actualDate))) {
                matches++;
            }
        }
        return matches;
    }

    private int getSchedulePageColumnIndex(String columnName) {
        Locator headers = page.locator("main thead th");
        int count = headers.count();
        for (int index = 0; index < count; index++) {
            String headerText = headers.nth(index).innerText().trim();
            if (headerText.equalsIgnoreCase(columnName)) {
                return index;
            }
        }
        throw new IllegalStateException("Unable to find schedule page column: " + columnName);
    }

    public static class ScheduleFormData {
        private String template;
        private String building;
        private String zoneCategory;
        private String floor;
        private String zone;
        private String criticality;
        private String frequency;
        private String startDate;
        private String endDate;
        private String startTime;
        private String auditor;
        private int rowIndex;

        public ScheduleFormData withTemplate(String template) {
            this.template = template;
            return this;
        }

        public ScheduleFormData withBuilding(String building) {
            this.building = building;
            return this;
        }

        public ScheduleFormData withZoneCategory(String zoneCategory) {
            this.zoneCategory = zoneCategory;
            return this;
        }

        public ScheduleFormData withFloor(String floor) {
            this.floor = floor;
            return this;
        }

        public ScheduleFormData withZone(String zone) {
            this.zone = zone;
            return this;
        }

        public ScheduleFormData withCriticality(String criticality) {
            this.criticality = criticality;
            return this;
        }

        public ScheduleFormData withFrequency(String frequency) {
            this.frequency = frequency;
            return this;
        }

        public ScheduleFormData withStartDate(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public ScheduleFormData withEndDate(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public ScheduleFormData withStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public ScheduleFormData withAuditor(String auditor) {
            this.auditor = auditor;
            return this;
        }

        public ScheduleFormData withRowIndex(int rowIndex) 
        {
            this.rowIndex = rowIndex;
            return this;
        }
        public String getZone() 
        {
            return zone;
        }

        public String getAuditor() 
        {
            return auditor;
        }

        public String getStartDate() 
        {
            return startDate;
        }

        public String getEndDate() 
        {
            return endDate;
        }

        public String getStartTime() 
        {
            return startTime;
        }
    }

    public enum FrequencyType {
        DAILY("Daily"),
        MONTHLY("Monthly"),
        YEARLY("Yearly");

        private final String label;

        FrequencyType(String label) {
            this.label = label;
        }
    }

    public enum MonthlyMode {
        DATES,
        DAYS
    }

    public record ScheduledAuditRecord(String template,
                                       String building,
                                       String level,
                                       String zone,
                                       String auditor,
                                       String auditDate,
                                       String feedback) {
    }

    public record ScheduledAuditGridRecord(String template,
                                           String building,
                                           String level,
                                           String zone,
                                           String auditor,
                                           String auditDate,
                                           String status) {
    }
    
    public enum RecurrenceMonth {
        JANUARY("January", Month.JANUARY),
        FEBRUARY("February", Month.FEBRUARY),
        MARCH("March", Month.MARCH),
        APRIL("April", Month.APRIL),
        MAY("May", Month.MAY),
        JUNE("June", Month.JUNE),
        JULY("July", Month.JULY),
        AUGUST("August", Month.AUGUST),
        SEPTEMBER("September", Month.SEPTEMBER),
        OCTOBER("October", Month.OCTOBER),
        NOVEMBER("November", Month.NOVEMBER),
        DECEMBER("December", Month.DECEMBER);

        private final String label;
        private final Month month;

        RecurrenceMonth(String label, Month month) {
            this.label = label;
            this.month = month;
        }

        public Month month() {
            return month;
        }
    }
    
    // 1. Wait for modal
public void waitForModalVisible() {
    Locator modal = page.locator(".ant-modal:visible").last();

    modal.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(15000));

    page.waitForTimeout(400);
}

// 2. Handle success popup
public void handleSuccessPopup() {
    Locator ok = page.locator(".swal2-container button:has-text('OK')");
    if (ok.isVisible()) {
        ok.click();
        ok.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN));
    }
}

// 3. Wait for schedule grid
public void waitForScheduleGrid() {
    Locator rows = page.locator("table tbody tr");
    rows.first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE));
    page.waitForTimeout(800);
}

public void assertScheduleGridVisible() {
    PlaywrightAssertions.assertThat(page.locator("table tbody tr").first()).isVisible();
}
}

 
