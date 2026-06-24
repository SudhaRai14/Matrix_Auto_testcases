package pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

public class TaskGroupPage {

    private static final String DEFAULT_ORG = "SMARTCLEAN";
    private static final String DEFAULT_PROPERTY_ID = "3b749a681d14446292b6c79b48403bbd";
    private static final String DEFAULT_ACCOUNT_ID = "0397f1af95604770a6148231aaf5c6f9";

    private final Page page;

    public TaskGroupPage(Page page) {
        this.page = page;
    }

    public void openTaskGroupModule() {
        openWorkorderModule();
        openActivitiesTab();

        Locator taskGroupsTab = page.getByRole(
                AriaRole.TAB,
                new Page.GetByRoleOptions().setName(Pattern.compile("task\\s+groups?", Pattern.CASE_INSENSITIVE)))
                .first();

        if (taskGroupsTab.count() == 0) {
            taskGroupsTab = page.getByText("Task Groups", new Page.GetByTextOptions().setExact(true)).first();
        }

        assertThat(taskGroupsTab).isVisible();
        taskGroupsTab.click();

        assertThat(page.getByText("Task Groups", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
        assertThat(createTaskGroupButton()).isVisible();
    }

    public void clickCreateTaskGroup() {
        Locator createButton = createTaskGroupButton();

        assertThat(createButton).isVisible();
        createButton.click();

        assertThat(createForm()).isVisible();
    }

    public void enterTaskGroupName(String name) {
        Locator input = taskGroupNameInput();

        assertThat(input).isVisible();
        input.fill(name);
        assertThat(input).hasValue(name);
    }

    public void clearTaskGroupName() {
        taskGroupNameInput().fill("");
    }

    public void enterDescription(String description) {
        Locator descriptionInput = descriptionInput();

        if (descriptionInput.count() == 0) {
            return;
        }

        assertThat(descriptionInput).isVisible();
        descriptionInput.fill(description);
        assertThat(descriptionInput).hasValue(description);
    }

    public void addTask(String taskName) {
        selectTask(taskName);

        assertThat(createForm()).isVisible();
    }

    public void addMultipleTasks(List<String> tasks) {
        if (!isAssociateTasksModalOpen()) {
            openTaskSelector();
        }

        for (String task : tasks) {
            selectTaskInOpenModal(task);
        }

        Locator modal = associateTasksModal();
        closeAssociateTasksModalAfterSave(modal);
        assertThat(createForm()).isVisible();
    }

    public void fillRequiredDefaults() {
        selectFirstZoneCategory();
        selectFrequency("Daily");
    }

    public void clickSave() {
        Locator saveButton = createForm().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("save|create", Pattern.CASE_INSENSITIVE)))
                .last();

        assertThat(saveButton).isVisible();
        saveButton.click();
    }

    public void clickCancel() {
        Locator cancelButton = createForm().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("cancel|close", Pattern.CASE_INSENSITIVE)))
                .first();

        assertThat(cancelButton).isVisible();
        cancelButton.click();

        assertThat(createForm()).not().isVisible();
    }

    public void openEditTaskGroup(String taskGroupName) {
        searchTaskGroup(taskGroupName);
        Locator row = exactTaskGroupRow(taskGroupName);
        if (row.count() == 0) {
            System.out.println("DEBUG: Task group row was not found for edit: " + taskGroupName);
            throw new IllegalStateException("Task group row was not found: " + taskGroupName);
        }

        // TODO: Replace this selector with the task-group edit action data-testid when available.
        Locator edit = row.locator("[aria-label='Edit']:visible, [aria-label='edit']:visible, "
                + ".anticon-edit:visible, .anticon-form:visible, [data-testid*='edit' i]:visible")
                .last();
        if (edit.count() == 0) {
            edit = row.locator("td:last-child button:visible, td:last-child [role='button']:visible").last();
        }
        if (edit.count() == 0) {
            System.out.println("DEBUG: Edit action was not found for task group '" + taskGroupName
                    + "'; row text: " + row.innerText());
            throw new IllegalStateException("Task group edit action was not found: " + taskGroupName);
        }
        clickReliably(edit);
        createForm().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
    }

    public boolean isEditFormOpen() {
        return isCreateFormOpen();
    }

    public void removeTask(String taskName) {
        Locator taskEntry = createForm().locator("tr:visible, .ant-tag:visible, .ant-list-item:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile(Pattern.quote(taskName), Pattern.CASE_INSENSITIVE)))
                .first();
        if (taskEntry.count() == 0) {
            throw new IllegalStateException("Associated task was not found for removal: " + taskName);
        }

        // TODO: Replace with the task-association remove data-testid if the UI exposes one.
        Locator remove = taskEntry.locator("[aria-label*='remove' i]:visible, [aria-label*='delete' i]:visible, "
                + ".anticon-delete:visible, .anticon-close:visible, button:visible").last();
        if (remove.count() == 0) {
            throw new IllegalStateException("Remove action was not found for associated task: " + taskName);
        }
        clickReliably(remove);
        page.waitForCondition(() -> !isTaskAssociatedWithGroup(taskName),
                new Page.WaitForConditionOptions().setTimeout(10000));
    }

    public boolean isTaskAssociatedWithGroup(String taskName) {
        Locator taskText = createForm().getByText(taskName, new Locator.GetByTextOptions().setExact(true)).first();
        return taskText.count() > 0 && taskText.isVisible();
    }

    public boolean isSuccessMessageVisible() {
        Locator successMessage = page.locator(
                ".ant-message-notice-content:visible, " +
                        ".ant-modal:visible, " +
                        ".swal-modal:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("success|created|updated", Pattern.CASE_INSENSITIVE)))
                .first();

        try {
            successMessage.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(15000));
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean hasValidationMessageContaining(String text) {
        Pattern expected = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
        Locator validationMessage = page.locator(
                ".ant-form-item-explain-error:visible, " +
                        ".ant-form-item-extra:visible, " +
                        ".ant-alert:visible, " +
                        ".ant-message-notice-content:visible, " +
                        ".swal-modal:visible")
                .filter(new Locator.FilterOptions().setHasText(expected))
                .first();

        try {
            validationMessage.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));
            return true;
        } catch (RuntimeException ex) {
            return page.locator("body").innerText().toLowerCase().contains(text.toLowerCase());
        }
    }

    public void searchTaskGroup(String name) {
        Locator search = visibleTaskGroupsPanel().locator(
                "input[placeholder*='search' i]:visible, input[type='search']:visible").last();
        if (search.count() > 0 && search.isVisible()) {
            search.fill(name);
        } else {
            System.out.println("DEBUG: Task group search input was not found; waiting for row: " + name);
        }
        page.waitForCondition(() -> exactTaskGroupRow(name).count() > 0 || isEmptyTaskGroupResultVisible(),
                new Page.WaitForConditionOptions().setTimeout(15000));
    }

    public boolean isTaskGroupVisible(String name) {
        try {
            page.waitForCondition(() -> exactTaskGroupRow(name).count() > 0 || isEmptyTaskGroupResultVisible(),
                    new Page.WaitForConditionOptions().setTimeout(15000));
            return exactTaskGroupRow(name).count() > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean isCreateFormOpen() {
        return createForm().count() > 0 && createForm().isVisible();
    }

    public void openTaskDropdown() {
        if (!isAssociateTasksModalOpen()) {
            openTaskSelector();
        }
        waitForAssociateTasksModal();
    }

    public String getFirstAvailableTaskName() {
        List<String> taskNames = getAvailableTaskNamesFromDropdown(1);

        if (taskNames.isEmpty()) {
            throw new RuntimeException("No task options are available in Create Task Group dropdown.");
        }

        return taskNames.get(0);
    }

    public List<String> getAvailableTaskNamesFromDropdown(int limit) {
        Locator rows = taskRows();

        page.waitForCondition(() -> rows.count() > 0,
                new Page.WaitForConditionOptions().setTimeout(10000));

        List<String> taskNames = new ArrayList<>();
        int count = rows.count();
        for (int i = 0; i < count && taskNames.size() < limit; i++) {
            String optionText = taskNameFromRow(rows.nth(i));
            if (!optionText.isBlank() && !"No data".equalsIgnoreCase(optionText)) {
                taskNames.add(optionText);
            }
        }

        return taskNames;
    }

    public void selectTask(String taskName) {
        if (!isAssociateTasksModalOpen()) {
            openTaskSelector();
        }

        selectTaskInOpenModal(taskName);
        Locator modal = associateTasksModal();
        closeAssociateTasksModalAfterSave(modal);
    }

    private void selectTaskInOpenModal(String taskName) {
        Locator modal = associateTasksModal();
        Locator searchInput = modal.locator("input[placeholder*='search' i]:visible, input[type='search']:visible").first();

        if (searchInput.count() > 0 && searchInput.isVisible()) {
            searchInput.fill(taskName);
        }

        Locator rows = taskRows();
        page.waitForCondition(() -> rows.count() > 0,
                new Page.WaitForConditionOptions().setTimeout(10000));

        Locator option = findTaskRow(taskName, true);

        if (option.count() == 0) {
            option = findTaskRow(taskName, false);
        }

        if (option.count() == 0) {
            printAvailableTaskOptions();
            throw new RuntimeException("Task option not found: " + taskName);
        }

        clickTaskCheckbox(option.first());
    }

    public void closeSuccessMessageIfPresent() {
        Locator successModal = page.locator(".ant-modal:visible, .swal-modal:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("success|created|updated", Pattern.CASE_INSENSITIVE)))
                .last();

        if (successModal.count() > 0 && successModal.isVisible()) {
            Locator okButton = successModal.getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName(Pattern.compile("ok|close", Pattern.CASE_INSENSITIVE)))
                    .last();

            if (okButton.count() > 0 && okButton.isVisible()) {
                clickReliably(okButton);
                try {
                    successModal.waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5000));
                } catch (RuntimeException ignored) {
                }
            }
        }

        Locator okButton = page.locator(".ant-modal:visible button:visible, .swal-modal:visible button:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("ok|close", Pattern.CASE_INSENSITIVE)))
                .last();

        if (okButton.count() > 0 && okButton.isVisible()) {
            clickReliably(okButton);
        }
    }

    private void openWorkorderModule() {
        closeOpenOverlays();

        if (page.url().contains("/matrix/workorders/")) {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            return;
        }

        Locator workorders = page.locator("xpath=//app-module-item[.//div["
                        + "contains(concat(' ', normalize-space(@class), ' '), ' module_name ')"
                        + " and normalize-space()='Workorders']]"
                        + "//div[contains(concat(' ', normalize-space(@class), ' '), ' module ')]")
                .first();

        assertThat(workorders).isVisible();
        workorders.click();

        try {
            page.waitForURL("**/workorders/**", new Page.WaitForURLOptions().setTimeout(8000));
        } catch (RuntimeException ex) {
            navigateDirectlyToActivities();
        }

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        closeOpenOverlays();
    }

    private void navigateDirectlyToActivities() {
        String currentUrl = page.url();
        String org = extractQueryValue(currentUrl, "org", DEFAULT_ORG);
        String propertyId = extractQueryValue(currentUrl, "propId", DEFAULT_PROPERTY_ID);
        String accountId = System.getProperty("matrix.account.id",
                System.getenv().getOrDefault("MATRIX_ACCOUNT_ID", DEFAULT_ACCOUNT_ID));

        page.navigate(String.format(
                "https://www.smartclean.io/matrix/workorders/v124_6/#/activities/%s/%s/%s",
                org,
                propertyId,
                accountId));
        page.waitForURL("**/workorders/**");
    }

    private String extractQueryValue(String url, String key, String fallback) {
        java.util.regex.Matcher matcher = Pattern.compile("[?&]" + Pattern.quote(key) + "=([^&#]+)").matcher(url);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private void openActivitiesTab() {
        closeOpenOverlays();

        Locator activitiesTab = page.locator("span.sidenav-element")
                .filter(new Locator.FilterOptions().setHasText("Activities"))
                .first();

        assertThat(activitiesTab).isVisible();
        activitiesTab.click();

        assertThat(page.getByText("Activities", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
        closeOpenOverlays();
    }

    private Locator createTaskGroupButton() {
        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("create\\s+task\\s+group", Pattern.CASE_INSENSITIVE)))
                .first();
    }

    private Locator visibleTaskGroupsPanel() {
        return page.locator(".ant-tabs-tabpane:not(.ant-tabs-tabpane-hidden):visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("task\\s+groups?", Pattern.CASE_INSENSITIVE)))
                .last();
    }

    private Locator taskGroupListingCell(String name) {
        return page.locator("main .ant-tabs-tabpane:not(.ant-tabs-tabpane-hidden) .ant-table-tbody tr:visible td:visible, "
                + "main .ant-table-tbody tr:visible td:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE)));
    }

    private Locator exactTaskGroupRow(String name) {
        Locator rows = visibleTaskGroupsPanel().locator(".ant-table-tbody tr:visible");
        int rowCount = rows.count();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Locator row = rows.nth(rowIndex);
            Locator cells = row.locator("td");
            for (int cellIndex = 0; cellIndex < cells.count(); cellIndex++) {
                if (name.equals(cells.nth(cellIndex).innerText().trim())) {
                    return row;
                }
            }
        }
        return page.locator(".__task-group-row-not-found__");
    }

    private boolean isEmptyTaskGroupResultVisible() {
        Locator empty = visibleTaskGroupsPanel().locator(".ant-empty:visible, tr:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("no\\s+(data|results|task\\s+groups?)", Pattern.CASE_INSENSITIVE)))
                .first();
        return empty.count() > 0 && empty.isVisible();
    }

    private boolean visiblePageTextContains(String text) {
        return page.locator("body").innerText().toLowerCase().contains(text.toLowerCase());
    }

    private Locator createForm() {
        Locator taskGroupForm = page.locator("form:has(#taskGroupName):visible").last();
        if (taskGroupForm.count() > 0) {
            return taskGroupForm;
        }
        return page.locator(".ant-modal:visible, .ant-drawer-content:visible, form:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("create\\s+task\\s+group|edit\\s+task\\s+group|task\\s+group", Pattern.CASE_INSENSITIVE)))
                .last();
    }

    private Locator taskGroupNameInput() {
        // TODO: Replace selector with exact task-group name id/data-testid when the app exposes one.
        return createForm().locator(
                "input#taskGroupName:visible, " +
                        "input#task_group_name:visible, " +
                        "input[name='taskGroupName']:visible, " +
                        "input[placeholder*='task group name' i]:visible, " +
                        "input[placeholder*='group name' i]:visible, " +
                        "input[placeholder*='name' i]:visible")
                .first();
    }

    private Locator descriptionInput() {
        return createForm().locator(
                "textarea#taskGroupDescription:visible, input#taskGroupDescription:visible, " +
                        "textarea#description:visible, " +
                        "textarea[name='description']:visible, " +
                        "textarea[placeholder*='description' i]:visible, " +
                        "input[placeholder*='description' i]:visible, " +
                        "textarea:visible")
                .first();
    }

    private void selectFirstZoneCategory() {
        Locator form = createForm();
        Locator selectedZone = form.locator(
                ".ant-form-item:has(label[for='zoneCategory']) .ant-select-selection-item-content:visible, "
                        + ".ant-form-item:has(label[for='zoneCategory']) .ant-select-selection-item:visible")
                .first();

        if (selectedZone.count() > 0 && selectedZone.isVisible()
                && !selectedZone.innerText().trim().isBlank()) {
            return;
        }

        Locator zoneSelect = form.locator(
                ".ant-form-item:has(label[for='zoneCategory']) .ant-select-selector:visible, "
                        + "#zoneCategory")
                .first();

        assertThat(zoneSelect).isVisible();
        openAntSelect(zoneSelect);

        Locator options = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) "
                + ".ant-select-item-option-content:visible");

        if (!waitForOptions(options, 3000)) {
            Locator zoneInput = form.locator("#zoneCategory").first();
            clickReliably(zoneInput);
            zoneInput.fill("ALL");
        }

        if (!waitForOptions(options, 5000)) {
            Locator zoneInput = form.locator("#zoneCategory").first();
            zoneInput.press("ArrowDown");
            zoneInput.press("Enter");
            if (selectedZone.count() > 0 && selectedZone.isVisible()
                    && !selectedZone.innerText().trim().isBlank()) {
                return;
            }
            throw new RuntimeException("Zone category options did not open for Create Task Group.");
        }

        Locator allOption = options.filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^\\s*ALL\\s*$", Pattern.CASE_INSENSITIVE)))
                .first();
        clickReliably(allOption.count() > 0 ? allOption : options.first());
        closeOpenDropdown();
    }

    private void selectFrequency(String frequency) {
        Locator frequencyControl = createForm().locator(".frequency-add:visible").first();

        assertThat(frequencyControl).isVisible();
        clickReliably(frequencyControl);

        Locator frequencyModal = page.locator(".ant-modal:visible")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("frequency\\s+calculation", Pattern.CASE_INSENSITIVE)))
                .last();

        frequencyModal.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));

        Locator frequencySelect = frequencyModal.locator(
                ".ant-form-item:has(label[for='frequencySelector']) .ant-select-selector:visible, "
                        + "#frequencySelector")
                .first();

        Locator selectedFrequency = frequencyModal.locator(".ant-select-selection-item:visible").first();
        if (selectedFrequency.count() == 0
                || !Pattern.compile(Pattern.quote(frequency), Pattern.CASE_INSENSITIVE)
                        .matcher(selectedFrequency.innerText()).find()) {
            openAntSelect(frequencySelect);
            Locator preferredOptions = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) "
                    + ".ant-select-item-option-content:visible")
                    .filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile(Pattern.quote(frequency), Pattern.CASE_INSENSITIVE)));
            Locator fallbackOptions = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) "
                    + ".ant-select-item-option-content:visible");
            Locator options = preferredOptions.count() > 0 ? preferredOptions : fallbackOptions;

            page.waitForCondition(() -> options.count() > 0,
                    new Page.WaitForConditionOptions().setTimeout(10000));

            clickReliably(options.first());
            closeOpenDropdown();
        }

        Locator saveButton = frequencyModal.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("save", Pattern.CASE_INSENSITIVE)))
                .last();
        clickReliably(saveButton);

        frequencyModal.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10000));
    }

    private void openTaskSelector() {
        closeOpenDropdown();

        Locator form = createForm();

        Locator addTaskButton = form.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("add\\s+task|select\\s+task", Pattern.CASE_INSENSITIVE)))
                .first();

        if (addTaskButton.count() > 0 && addTaskButton.isVisible()) {
            clickReliably(addTaskButton);
        } else {
            Locator plusIcon = form.locator("span[aria-label='plus-circle']:visible").last();
            assertThat(plusIcon).isVisible();
            Locator plusButton = plusIcon.locator("xpath=ancestor::button[1]");
            if (plusButton.count() > 0) {
                clickReliably(plusButton);
            } else {
                clickReliably(plusIcon);
            }
        }

        waitForAssociateTasksModal();
    }

    private void clickReliably(Locator locator) {
        try {
            locator.click(new Locator.ClickOptions().setForce(true).setTimeout(3000));
        } catch (RuntimeException ex) {
            locator.evaluate("element => element.click()");
        }
    }

    private void openAntSelect(Locator selectOrSelector) {
        clickReliably(selectOrSelector);

        Locator openDropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        if (openDropdown.count() > 0 && openDropdown.isVisible()) {
            return;
        }

        selectOrSelector.evaluate("element => {"
                + "const target = element.closest('.ant-select') || element;"
                + "['mousedown', 'mouseup', 'click'].forEach(type => target.dispatchEvent(new MouseEvent(type, {"
                + "bubbles: true, cancelable: true, view: window"
                + "})));"
                + "}");
    }

    private boolean waitForOptions(Locator options, int timeoutMs) {
        try {
            page.waitForCondition(() -> options.count() > 0,
                    new Page.WaitForConditionOptions().setTimeout(timeoutMs));
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Locator associateTasksModal() {
        return page.locator("xpath=//div[@role='dialog'][.//div["
                        + "contains(concat(' ', normalize-space(@class), ' '), ' title-styles ')"
                        + " and normalize-space()='Associate Tasks To Task Groups']]")
                .last();
    }

    private boolean isAssociateTasksModalOpen() {
        Locator modal = associateTasksModal();
        return modal.count() > 0 && modal.isVisible();
    }

    private void waitForAssociateTasksModal() {
        associateTasksModal().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));
    }

    private Locator taskRows() {
        return associateTasksModal().locator(".ant-table-tbody tr:visible")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("\\S")));
    }

    private Locator findTaskRow(String taskName, boolean exactMatch) {
        Pattern pattern = exactMatch
                ? Pattern.compile("^\\s*" + Pattern.quote(taskName) + "\\s*$", Pattern.CASE_INSENSITIVE)
                : Pattern.compile(Pattern.quote(taskName), Pattern.CASE_INSENSITIVE);

        Locator rows = taskRows();
        int count = rows.count();
        for (int i = 0; i < count; i++) {
            Locator row = rows.nth(i);
            if (pattern.matcher(taskNameFromRow(row)).find()) {
                return row;
            }
        }

        return page.locator(".__task-row-not-found__");
    }

    private String taskNameFromRow(Locator row) {
        Locator cells = row.locator("td");
        if (cells.count() > 1) {
            return cells.nth(1).innerText().trim();
        }

        return row.innerText().trim();
    }

    private void clickTaskCheckbox(Locator row) {
        Locator checkbox = row.locator(".ant-checkbox-input, .ant-checkbox-wrapper").first();
        clickReliably(checkbox);
    }

    private void clickAssociateTasksSave() {
        Locator saveButton = associateTasksModal().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("save", Pattern.CASE_INSENSITIVE)))
                .last();
        clickReliably(saveButton);
    }

    private void closeAssociateTasksModalAfterSave(Locator modal) {
        clickAssociateTasksSave();

        try {
            modal.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(5000));
            return;
        } catch (RuntimeException ignored) {
        }

        Locator saveButton = associateTasksModal().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("save", Pattern.CASE_INSENSITIVE)))
                .last();
        saveButton.evaluate("element => element.click()");
        modal.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10000));
    }

    private void printAvailableTaskOptions() {
        System.out.println("Available task options:");
        Locator rows = taskRows();
        int count = rows.count();
        for (int i = 0; i < count; i++) {
            System.out.println(taskNameFromRow(rows.nth(i)));
        }
    }

    private void closeOpenOverlays() {
        closeSuccessMessageIfPresent();

        Locator visibleModal = page.locator(".ant-modal:visible, .ant-drawer-content:visible").last();
        if (visibleModal.count() > 0 && visibleModal.isVisible()) {
            page.keyboard().press("Escape");
            try {
                visibleModal.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(3000));
            } catch (RuntimeException ignored) {
            }
        }

        closeOpenDropdown();
    }

    private void closeOpenDropdown() {
        Locator openDropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        if (openDropdown.count() > 0 && openDropdown.isVisible()) {
            Locator formTitle = page.locator(".ant-drawer-content:visible .title-styles:visible, "
                    + ".ant-modal:visible .title-styles:visible").first();
            if (formTitle.count() > 0 && formTitle.isVisible()) {
                formTitle.click(new Locator.ClickOptions().setForce(true));
            } else {
                page.mouse().click(5, 5);
            }
        }
    }
}
