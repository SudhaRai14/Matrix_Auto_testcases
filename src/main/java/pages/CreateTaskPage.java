package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class CreateTaskPage {

    private final Page page;

    public CreateTaskPage(Page page) {

        this.page = page;
    }

    private Locator createTaskModal() {
        Locator modal = page.locator(".ant-modal:visible").last();
        modal.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
        return modal;
    }

    private Locator field(String fieldName) {
        return createTaskModal()
                .locator(String.format("#%s, input[name='%s'], textarea[name='%s']",
                        fieldName, fieldName, fieldName))
                .first();
    }

    private void clickDropdown(String fieldName) {
        field(fieldName)
                .locator("xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' ant-select ')][1]")
                .click();
    }

    private void selectDropdownOption(String fieldName, String optionText) {
        clickDropdown(fieldName);

        Locator option = optionsFor(fieldName)
                .filter(new Locator.FilterOptions().setHasText(optionText))
                .last();

        option.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private Locator optionsFor(String fieldName) {
        return page.locator(String.format(
                "//div[@id=%s]/following-sibling::div"
                        + "//*[contains(@class,'ant-select-item-option-content')]",
                xpathLiteral(fieldName + "_list")));
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        StringBuilder literal = new StringBuilder("concat(");
        String[] parts = value.split("'");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                literal.append(", \"'\", ");
            }
            literal.append("'").append(parts[i]).append("'");
        }
        literal.append(")");
        return literal.toString();
    }

    public void enterTaskName(String taskName) {

        field("task").fill(taskName);
    }

    public void selectCategory(String category) {

        selectDropdownOption("category", category);

        page.waitForLoadState();
    }

    public void selectSubCategory(String subCategory) {

        selectDropdownOption("asset", subCategory);
    }

    public void selectSkills(String skills) {

        clickDropdown("skills");

        Locator options = optionsFor("skills");

        Locator option = isBlank(skills) || "__FIRST__".equalsIgnoreCase(skills)
                ? options.first()
                : options.filter(new Locator.FilterOptions().setHasText(skills)).first();

        option.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void selectUnitOfMeasure(String unitOfMeasure) {

        selectDropdownOption("unitOfMeasure", unitOfMeasure);
    }

    public void enterMeasure(String measure) {

        field("measure").fill(measure);
    }

    public void enterTimeInMinutes(String timeInMinutes) {

        field("timeInMinutes").fill(timeInMinutes);
    }

    public void clickSave() {

        createTaskModal()
                .getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Save"))
                .click();
    }

    public void createTask(
            String taskName,
            String category,
            String subCategory,
            String skills,
            String unitOfMeasure,
            String measure,
            String timeInMinutes) {

        enterTaskName(taskName);

        selectCategory(category);

        selectSubCategory(subCategory);

        selectSkills(skills);

        selectUnitOfMeasure(unitOfMeasure);

        enterMeasure(measure);

        enterTimeInMinutes(timeInMinutes);

        clickSave();
    }

    public String getSuccessMessage() {

        Locator successModal = page.locator(".ant-modal:visible")
                .filter(new Locator.FilterOptions().setHasText("Task created successfully"))
                .last();

        successModal.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));

        return successModal.textContent();
    }

    public void closeSuccessMessage() {

        page.locator(".ant-modal:visible")
                .filter(new Locator.FilterOptions().setHasText("Task created successfully"))
                .last()
                .getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("OK"))
                .click();
    }

    public String getValidationMessage() {

        return page.locator(".ant-form-item-explain-error:visible")
                .first()
                .textContent();
    }
}
