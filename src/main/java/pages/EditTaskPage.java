package pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Page object for editing tasks from the Activities > Tasks view.
 *
 * <p>The Matrix UI uses Ant Design tables and modals.  Selectors are deliberately
 * scoped to visible controls and, for edit actions, to the row whose task-name
 * cell exactly matches the requested task.</p>
 */
public class EditTaskPage {

    private static final int UI_TIMEOUT_MS = 15_000;

    private final Page page;

    public EditTaskPage(Page page) {
        this.page = page;
    }

    public void openTaskModule() {
        if (!page.url().contains("/matrix/workorders/")) {
            // The home page contains both an image alt text and a module title named
            // Workorders. Click the visible module card, not the first text match.
            Locator workorders = workorderModuleCard();
            workorders.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
            workorders.click();

            page.waitForURL("**/workorders/**",
                    new Page.WaitForURLOptions().setTimeout(UI_TIMEOUT_MS));
        }

        Locator activities = page.locator("span.sidenav-element:visible")
                .filter(new Locator.FilterOptions().setHasText("Activities"))
                .first();
        activities.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
        activities.click();

        Locator taskTab = page.getByRole(AriaRole.TAB,
                new Page.GetByRoleOptions().setName(Pattern.compile("tasks?", Pattern.CASE_INSENSITIVE)))
                .first();
        if (taskTab.count() == 0) {
            taskTab = page.getByText("Tasks", new Page.GetByTextOptions().setExact(true)).first();
        }
        // Some Matrix deployments show the task listing directly under Activities rather
        // than in a dedicated Tasks tab.
        if (taskTab.count() > 0) {
            taskTab.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
            taskTab.click();
        }

        taskTableRows().first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(UI_TIMEOUT_MS));
    }

    private Locator workorderModuleCard() {
        return page.locator("xpath=//app-module-item[.//div["
                        + "contains(concat(' ', normalize-space(@class), ' '), ' module_name ')"
                        + " and normalize-space()='Workorders']]"
                        + "//div[contains(concat(' ', normalize-space(@class), ' '), ' module ')]")
                .first();
    }

    public void searchTask(String taskName) {
        Locator search = page.locator(
                "input[placeholder*='search' i]:visible, input[type='search']:visible")
                .last();

        if (search.count() == 0) {
            System.out.println("DEBUG: Task search input was not found; waiting for exact row: " + taskName);
        } else {
            search.fill(taskName);
        }

        // Wait for either the matching row or Ant Design's empty state.  The latter is
        // important when callers intentionally verify that a renamed task is absent.
        page.waitForCondition(() -> exactTaskRow(taskName).count() > 0 || isEmptyTaskResultVisible(),
                new Page.WaitForConditionOptions().setTimeout(UI_TIMEOUT_MS));
    }

    public void openEditTask(String taskName) {
        searchTask(taskName);
        Locator row = exactTaskRow(taskName);
        if (row.count() == 0) {
            logTaskRowNotFound(taskName);
            throw new IllegalStateException("Task row was not found for edit: " + taskName);
        }

        // The Actions column in the current UI exposes a pencil icon directly, rather
        // than an action menu.
        Locator edit = row.locator("[aria-label='Edit']:visible, [aria-label='edit']:visible, "
                + ".anticon-edit:visible, .anticon-form:visible, [data-testid*='edit' i]:visible")
                .last();
        if (edit.count() == 0) {
            // TODO: Replace this fallback with the task edit icon's data-testid if one becomes available.
            edit = row.locator("td:last-child button:visible, td:last-child [role='button']:visible").last();
        }
        if (edit.count() == 0) {
            System.out.println("DEBUG: Edit pencil was not found for task: " + taskName
                    + "; row text: " + row.innerText());
            throw new IllegalStateException("Edit action was not found for task: " + taskName);
        }
        edit.click();

        taskForm().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
    }

    public void enterTaskName(String taskName) {
        Locator input = taskNameInput();
        input.fill(taskName);
    }

    public void clearTaskName() {
        taskNameInput().fill("");
    }

    public void clickSave() {
        Locator save = taskForm().getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("save|update", Pattern.CASE_INSENSITIVE)))
                .last();
        save.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
        save.click();
    }

    public void clickCancel() {
        Locator cancel = taskForm().getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("cancel|close", Pattern.CASE_INSENSITIVE)))
                .first();
        cancel.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
        cancel.click();
        taskForm().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN).setTimeout(UI_TIMEOUT_MS));
    }

    public boolean isSuccessMessageVisible() {
        return isVisible(page.locator(
                ".ant-message-notice-content:visible, .ant-notification-notice:visible, "
                        + ".ant-modal:visible, .swal-modal:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("success|updated", Pattern.CASE_INSENSITIVE)))
                .last(), UI_TIMEOUT_MS);
    }

    /** Closes the blocking success dialog when the application displays one. */
    public void closeSuccessMessageIfPresent() {
        Locator successDialog = page.locator(".ant-modal:visible, .swal-modal:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("success|updated", Pattern.CASE_INSENSITIVE)))
                .last();
        if (successDialog.count() == 0 || !successDialog.isVisible()) {
            return;
        }

        Locator close = successDialog.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("ok|close", Pattern.CASE_INSENSITIVE)))
                .last();
        if (close.count() > 0 && close.isVisible()) {
            close.click();
            successDialog.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(UI_TIMEOUT_MS));
        }
    }

    public boolean isValidationMessageVisible(String text) {
        Pattern expected = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
        try {
            page.waitForCondition(() -> {
                Locator invalidTaskName = page.locator(
                        "form#editable-form-task #task[aria-invalid='true']").last();
                Locator inlineTaskError = page.locator(
                        "form#editable-form-task #task_help .ant-form-item-explain-error").last();

                return invalidTaskName.count() > 0
                        && inlineTaskError.count() > 0
                        && expected.matcher(inlineTaskError.innerText()).find();
            }, new Page.WaitForConditionOptions().setTimeout(UI_TIMEOUT_MS));
            return true;
        } catch (RuntimeException ignored) {
            // Fall through to support validation messages on other task fields.
        }

        return isVisible(page.locator(
                ".ant-form-item-explain-error:visible, .ant-alert:visible")
                .filter(new Locator.FilterOptions().setHasText(expected))
                .first(), 2_000);
    }

    public boolean isTaskVisible(String taskName) {
        try {
            page.waitForCondition(() -> exactTaskRow(taskName).count() > 0 || isEmptyTaskResultVisible(),
                    new Page.WaitForConditionOptions().setTimeout(UI_TIMEOUT_MS));
            return exactTaskRow(taskName).count() > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean isEditFormOpen() {
        return isTaskFormOpen();
    }

    private Locator taskForm() {
        Locator editTaskForm = page.locator("form#editable-form-task:visible").last();
        if (editTaskForm.count() > 0) {
            return editTaskForm;
        }
        return page.locator(".ant-modal:visible, .ant-drawer-content:visible, form:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("task", Pattern.CASE_INSENSITIVE)))
                .last();
    }

    private boolean isTaskFormOpen() {
        Locator form = taskForm();
        return form.count() > 0 && form.isVisible();
    }

    private Locator taskNameInput() {
        Locator input = taskForm().locator(
                "#task:visible, input[name='task']:visible, input[name='taskName']:visible, "
                        + "input[placeholder*='task name' i]:visible")
                .first();
        input.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(UI_TIMEOUT_MS));
        return input;
    }

    private Locator taskTableRows() {
        return page.locator("tr:visible");
    }

    private void waitForExactTaskRow(String taskName) {
        page.waitForCondition(() -> exactTaskRow(taskName).count() > 0,
                new Page.WaitForConditionOptions().setTimeout(UI_TIMEOUT_MS));
    }

    private boolean isEmptyTaskResultVisible() {
        Locator emptyResult = page.locator(".ant-empty:visible, tr:visible")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("no\\s+(data|results|task|custom\\s+tasks)", Pattern.CASE_INSENSITIVE)))
                .first();
        return emptyResult.count() > 0 && emptyResult.isVisible();
    }

    private Locator exactTaskRow(String taskName) {
        // Ant Design tables render a row per task. Start with the requested hasText locator,
        // then retain only rows that have a cell equal to the requested task name.
        Locator candidates = page.locator("tr")
                .filter(new Locator.FilterOptions().setHasText(taskName));
        int count = candidates.count();
        for (int index = 0; index < count; index++) {
            Locator row = candidates.nth(index);
            Locator cells = row.locator("td");
            for (int cell = 0; cell < cells.count(); cell++) {
                if (taskName.equals(cells.nth(cell).innerText().trim())) {
                    return row;
                }
            }
        }
        return page.locator(".__task-row-not-found__");
    }

    private boolean isVisible(Locator locator, int timeoutMs) {
        try {
            locator.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutMs));
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void logTaskRowNotFound(String taskName) {
        System.out.println("DEBUG: Task row not found for '" + taskName + "'. Visible table text: "
                + page.locator("tr:visible").allInnerTexts());
    }
}
