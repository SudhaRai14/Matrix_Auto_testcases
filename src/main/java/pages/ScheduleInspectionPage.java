package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ScheduleInspectionPage {
    private static final double DEFAULT_TIMEOUT_MS = 20000;
    private static final DateTimeFormatter UI_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter UI_TIME_FORMAT_12_HOUR = DateTimeFormatter.ofPattern("hh:mm a");

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

    public ScheduleInspectionPage(Page page) {
        this.page = page;

        this.modal = page.getByRole(AriaRole.DIALOG,
                new Page.GetByRoleOptions().setName("Schedule Audit"));

        this.modalTitle = page.locator(".ant-modal-title")
                .filter(new Locator.FilterOptions().setHasText("Schedule Audit")).first();

        this.modalBody = page.locator(
                ".ant-modal-body:has-text('Template:'), " +
                        ".ant-modal-content:has-text('Template:'), " +
                        ".ant-modal-content:has-text('Buildings:'), " +
                        ".ant-modal-content:has-text('Zone Category:'), " +
                        ".ant-modal-content:has-text('StartTime'), " +
                        "[role='dialog']:has-text('Template:')")
                .first();
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
                        "[role='alert']:visible").first();
        this.validationMessages = page.locator(
                ".ant-modal-content .ant-form-item-explain-error:visible, " +
                        ".ant-modal-content .error:visible, " +
                        ".ant-modal-content [role='alert']:visible, " +
                        ".swal-overlay--show-modal .swal-modal:visible");
        this.rows = modal.locator("tbody tr:not(.ant-table-placeholder)");
    }

    public void waitForModal() {
        Locator modalWrap = page.locator(".ant-modal-root .ant-modal-wrap").filter(
                new Locator.FilterOptions().setHas(page.locator(".ant-modal.newSchedule, [role='dialog']"))).first();
        Locator modalDialog = page.locator(".ant-modal-root .ant-modal.newSchedule, .ant-modal-root [role='dialog']").first();

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            try {
                if (modalTitle.count() > 0 && modalTitle.isVisible()) {
                    return;
                }
                if (modalBody.count() > 0 && modalBody.isVisible()) {
                    return;
                }
                if (modal.count() > 0 && modal.isVisible()) {
                    return;
                }
                if (modalWrap.count() > 0 && modalWrap.isVisible() && modalDialog.count() > 0) {
                    return;
                }
                if (modalDialog.count() > 0 && isDomVisible(modalDialog)) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // Allow the dialog time to finish rendering.
            }

            page.waitForTimeout(250);
        }

        Locator root = page.locator("#root").first();
        if (root.count() > 0 && root.innerText().isBlank()) {
            throw new IllegalStateException(
                    "Schedule Inspection did not open because the page rendered blank after the button click.");
        }

        if (modalDialog.count() > 0) {
            String display = safeAttribute(modalDialog, "style");
            String wrapDisplay = modalWrap.count() > 0 ? safeAttribute(modalWrap, "style") : "";
            throw new IllegalStateException(
                    "Schedule Audit dialog was attached to the DOM but never became visible. dialogStyle="
                            + display + ", wrapStyle=" + wrapDisplay);
        }

        throw new IllegalStateException("Schedule Audit dialog did not attach to the DOM.");
    }

    public boolean isModalOpen() {
        return modalTitle.isVisible() || modalBody.isVisible() || modal.isVisible();
    }

    public void chooseCustomOption() {
        selectRadio(customRadio);
    }

    public void chooseStandardOption() {
        selectRadio(standardRadio);
    }

    public boolean isCustomSelected() {
        return isRadioSelected(customRadio);
    }

    public boolean isStandardSelected() {
        return isRadioSelected(standardRadio);
    }

    public void selectTemplate(String templateName) {
        selectDropdownOption("Template", templateName);
        waitForLoadingToFinish();
    }

    public String selectAnyTemplate() {
        String selectedTemplate = selectFirstDropdownOption("Template");
        waitForLoadingToFinish();
        return selectedTemplate;
    }

    public String selectAnyTemplateWithRows() {
        List<String> templateOptions = getVisibleDropdownOptions("Template");
        for (String templateOption : templateOptions) {
            selectTemplate(templateOption);
            if (getZoneRowCount() > 0) {
                return templateOption;
            }
        }
        throw new IllegalStateException("Unable to find an available template that loads schedule rows.");
    }

    public void selectBuilding(String buildingName) {
        selectDropdownOption(new String[]{"Buildings", "Building"}, buildingName);
        waitForLoadingToFinish();
    }

    public void selectZoneCategory(String zoneCategory) {
        if (trySelectZoneCategory(zoneCategory)) {
            waitForLoadingToFinish();
            return;
        }
        selectDropdownOption(new String[]{"Zone Category", "Zone Category:"}, zoneCategory);
        waitForLoadingToFinish();
    }

    public String selectAnyZoneCategory() {
        String selectedZoneCategory = selectFirstDropdownOption(new String[]{"Zone Category", "Zone Category:"});
        waitForLoadingToFinish();
        return selectedZoneCategory;
    }

    public void selectFloor(String floorName) {
        selectDropdownOption(new String[]{"Floor", "Floor:"}, floorName);
        waitForLoadingToFinish();
    }

    public String selectAnyFloor() {
        String selectedFloor = selectFirstDropdownOption(new String[]{"Floor", "Floor:"});
        waitForLoadingToFinish();
        return selectedFloor;
    }

    public void fillScheduleForm(ScheduleFormData data) {
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

    public void fillFirstZoneRow(ScheduleFormData data) {
        Locator row = getRow(data.rowIndex);
        selectRowIfPossible(row);
        if (data.zone != null) {
            fillTableField(row, "Zone", data.zone);
        }
        // if (data.criticality != null) {
        //     fillTableField(row, "Criticality", data.criticality);
        // }
        if (data.frequency != null) {
            fillTableField(row, "How Often", data.frequency);
        }
        if (data.startDate != null) {
            fillTableField(row, "Start Date", data.startDate);
        }
        if (data.endDate != null) {
            fillTableField(row, "End Date", data.endDate);
        }
        if (data.startTime != null) {
            fillTableField(row, "Start Time", data.startTime);
        }
        if (data.auditor != null) {
            fillTableField(row, "Auditor", data.auditor);
        }
    }

    public void selectRowByZone(String zoneName) {
        Locator row = rows.filter(new Locator.FilterOptions().setHasText(zoneName)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        clearSelectedRowsExcept(row);
        selectRowIfPossible(row);
    }

    public void setRowStartDate(String zoneName, String date) {
        setRowField(zoneName, "Start Date", date);
    }

    public void setRowEndDate(String zoneName, String date) {
        setRowField(zoneName, "End Date", date);
    }

    public void setRowStartTime(String zoneName, String time) {
        setRowField(zoneName, "Start Time", time);
    }

    public void setRowAuditor(String zoneName, String auditor) {
        Locator input = page.locator("tr:has-text('" + zoneName + "') input[placeholder='Select Auditor']").first();
        selectAuditorInput(input, auditor);
    }

    private void selectAuditorInput(Locator input, String auditor) {
        input.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        input.click();
        input.fill(auditor);

        Locator option = page.locator("li:has-text('" + auditor + "'), .ant-select-item-option:has-text('" + auditor + "')")
                .last();
        option.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        option.click(new Locator.ClickOptions().setForce(true));
        waitForLoadingToFinish();
    }

    public void clickCopyConfiguration() {
        copyConfigurationButton.click();
        waitForLoadingToFinish();
    }

    public void clickSave() {
        waitForLoadingToFinish();
        try {
            saveButton.click(new Locator.ClickOptions().setTimeout(5000));
        } catch (RuntimeException ex) {
            clickVisibleModalButton("save");
        }
        waitForLoadingToFinish();
        waitForPostSaveFeedback();
    }

    public void clickCancel() {
        cancelButton.click();
        modal.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(DEFAULT_TIMEOUT_MS));
    }

    public boolean isSaveButtonDisabled() {
        return !saveButton.isEnabled();
    }

    public String getToastMessage() {
        toastMessage.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return toastMessage.innerText().trim();
    }

    public String getToastMessageIfPresent(double timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (toastMessage.count() > 0 && toastMessage.isVisible()) {
                return toastMessage.innerText().trim();
            }
            page.waitForTimeout(250);
        }
        return "";
    }

    public boolean hasScheduledInspectionForAuditor(String auditor) {
        Locator scheduledRows = page.locator(".custom-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText("Scheduled"))
                .filter(new Locator.FilterOptions().setHasText(auditor));
        return scheduledRows.count() > 0;
    }

    public List<String> getValidationMessages() {
        List<String> messages = new ArrayList<>();
        int count = validationMessages.count();
        for (int index = 0; index < count; index++) {
            String text = validationMessages.nth(index).innerText().trim();
            if (!text.isBlank()) {
                messages.add(text);
            }
        }
        return messages;
    }

    public boolean hasValidationMessageContaining(String expectedText) {
        return getValidationMessages().stream()
                .anyMatch(message -> message.toLowerCase().contains(expectedText.toLowerCase()));
    }

    public int getZoneRowCount() {
        waitForLoadingToFinish();
        return rows.count();
    }

    public boolean isDropdownValueSelected(String fieldLabel, String expectedValue) {
        Locator field = findFieldContainer(fieldLabel);
        return field.innerText().toLowerCase().contains(expectedValue.toLowerCase());
    }

    public boolean isModalTitleVisible() {
        return modalTitle.isVisible() || modalBody.isVisible();
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
        trigger.click();

        List<String> options = new ArrayList<>();
        Locator optionLocators = page.locator(".ant-select-item-option, [role='option']");
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
                }
            } catch (RuntimeException ignored) {
                container = null;
            }
        }
        if (container == null) {
            throw new IllegalStateException("Unable to find dropdown field for labels: " + String.join(", ", fieldLabels));
        }

        Locator trigger = resolveDropdownTrigger(container);
        trigger.click();

        Locator option = findFirstVisibleDropdownOption();
        String optionText = option.innerText().trim();
        option.click();
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
        trigger.click();
        Locator option = findVisibleDropdownOptionByText(optionText);
        option.click();
    }

    private Locator resolveDropdownTrigger(Locator container) {
        Locator selector = container.locator(
                ".ant-select-selector, " +
                        ".ant-select-selection-overflow, " +
                        ".ant-select-selection-search, " +
                        "nz-select .ant-select-selector")
                .first();

        if (selector.count() > 0) {
            return selector;
        }

        return container.locator("nz-select, .ant-select, [role='combobox']").first();
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

    private Locator findFirstVisibleDropdownOption() {
        Locator options = page.locator(".ant-select-item-option, [role='option']");
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
            Locator auditorInput = row.locator("input[placeholder='Select Auditor']").first();
            if (auditorInput.count() > 0) {
                selectAuditorInput(auditorInput, value);
                return;
            }
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
                checkbox.uncheck(new Locator.CheckOptions().setForce(true));
            } catch (RuntimeException ignored) {
                row.locator(".ant-checkbox-wrapper, .ant-checkbox").first()
                        .click(new Locator.ClickOptions().setForce(true));
            }
        }
    }

    private void waitForPostSaveFeedback() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT_MS) {
            if (toastMessage.count() > 0 && toastMessage.isVisible()) {
                return;
            }
            if (validationMessages.count() > 0) {
                return;
            }
            if (!isModalOpen()) {
                return;
            }
            page.waitForTimeout(250);
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
                clickPickerOkIfVisible(visibleDropdown);
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

    private LocalTime parseUiTime(String value) {
        try {
            return LocalTime.parse(value, UI_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(value.toUpperCase(), UI_TIME_FORMAT_12_HOUR);
        }
    }

    private boolean waitForSelectedComboboxValue(Locator selectedValue, String expectedValue, double timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                if (selectedValue.count() > 0
                        && selectedValue.isVisible()
                        && selectedValue.innerText().toLowerCase().contains(expectedValue.toLowerCase())) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Allow the control time to settle after selection.
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
                "if (!button) throw new Error(`Unable to find visible modal button: ${text}`);" +
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

    private Locator findFieldContainer(String label) {
        Locator container = modal.locator("xpath=.//*[normalize-space()='" + label + "']/ancestor::*[self::div or self::nz-form-item][1]").first();
        if (container.count() == 0) {
            container = modal.locator("xpath=.//*[contains(normalize-space(), '" + label + "')]/ancestor::*[self::div or self::nz-form-item][1]").first();
        }
        container.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return container;
    }

    private int getColumnIndex(String columnName) {
        int headerCount = modal.locator("thead th").count();
        for (int index = 0; index < headerCount; index++) {
            String headerText = modal.locator("thead th").nth(index).innerText().trim();
            if (headerText.equalsIgnoreCase(columnName)) {
                return index;
            }
            if (columnName.equalsIgnoreCase("Start Time") && headerText.equalsIgnoreCase("StartTime")) {
                return index;
            }
        }
        throw new IllegalStateException("Unable to find table column: " + columnName);
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

        public ScheduleFormData withRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
            return this;
        }
    }
}

 
