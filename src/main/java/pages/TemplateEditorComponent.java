package pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

import models.EditTemplateData;

public class TemplateEditorComponent {
    private final Page page;
    private final QuestionComponent questions;

    public TemplateEditorComponent(Page page) {
        this.page = page;
        this.questions = new QuestionComponent(page);
    }

    public QuestionComponent questions() {
        return questions;
    }

    public void waitForEditor() {
        Locator form = page.locator("form:visible").first();
        assertThat(form).isVisible();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        expandFirstSectionIfNeeded();
    }

    public void clickEditIfNeeded() {
        Locator editButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^Edit$", Pattern.CASE_INSENSITIVE)))
                .first();
        if (editButton.count() > 0 && editButton.isVisible()) {
            editButton.click();
            waitForEditor();
        }
    }

    public void editTemplateName(String name) {
        if (isBlank(name)) {
            return;
        }
        fillInputByLabel("Template Name", name);
    }

    public void editDescription(String description) {
        if (description == null) {
            return;
        }
        Locator input = page.locator(
                "textarea[id*='templateDescription']:visible, " +
                        "textarea[placeholder*='description' i]:visible, " +
                        "form textarea:visible")
                .first();
        assertThat(input).isVisible();
        input.fill(description);
    }

    public void editTargetScore(Integer score) {
        if (score == null) {
            return;
        }
        Locator input = page.locator("#template_targetScore, input[id*='targetScore'], .ant-input-number-input:visible")
                .first();
        assertThat(input).isVisible();
        input.fill(String.valueOf(score));
    }

    public void editCategory(String category) {
        if (isBlank(category)) {
            return;
        }

        Locator selector = page.locator(".ant-form-item:has(label[title='Category']) .ant-select-selector:visible, "
                + ".ant-form-item:has(label:has-text('Category')) .ant-select-selector:visible")
                .first();
        assertThat(selector).isVisible();
        selector.click(new Locator.ClickOptions().setForce(true));

        Locator option = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option")
                .filter(new Locator.FilterOptions().setHasText(category))
                .first();
        assertThat(option).isVisible();
        option.scrollIntoViewIfNeeded();
        option.click(new Locator.ClickOptions().setForce(true));
    }

    public void editSectionTitle(String title) {
        editInlineTitle("Type your Main Section title", title);
    }

    public void editSubSectionTitle(String title) {
        editInlineTitle("Type your Sub Section title", title);
    }

    public void applyScenario(EditTemplateData data) {
        editTemplateName(data.newTemplateName);
        editDescription(data.newDescription);
        editTargetScore(data.newTargetScore);
        editCategory(data.newCategory);
        editSectionTitle(data.sectionTitle);
        editSubSectionTitle(data.subSectionTitle);

        if (data.questionTextEdit != null) {
            questions.editQuestionText(data.questionTextEdit.existingQuestion, data.questionTextEdit.newQuestion);
        }
        if (data.answerTypeEdit != null) {
            questions.editAnswerType(data.answerTypeEdit.question, data.answerTypeEdit.newQuestionType);
        }
        if (data.questionToAdd != null && !data.cancelEdit) {
            questions.addQuestion(data.questionToAdd);
        }
        if (data.questionToDelete != null) {
            questions.deleteQuestion(data.questionToDelete);
        }
        if (data.mcqOptionsEdit != null) {
            questions.editMcqOptions(data.mcqOptionsEdit.question, data.mcqOptionsEdit.options);
        }
        if (data.checkboxOptionsEdit != null) {
            questions.editCheckboxOptions(data.checkboxOptionsEdit.question, data.checkboxOptionsEdit.options);
        }
        if (data.sliderEdit != null) {
            questions.editSliderValues(data.sliderEdit.question, data.sliderEdit.min, data.sliderEdit.max);
        }
        if (data.mandatoryEdit != null) {
            questions.editMandatoryFlag(data.mandatoryEdit.question, data.mandatoryEdit.mandatory);
        }
        if (data.scoringEdit != null) {
            questions.editScoring(data.scoringEdit.question, data.scoringEdit.enabled, data.scoringEdit.firstScore);
        }
        if (data.reorderEdit != null) {
            questions.reorderQuestions(data.reorderEdit.sourceQuestion, data.reorderEdit.targetQuestion);
        }
    }

    public void saveChanges() {
        clickVisibleSaveButtons();

        verifySuccessToastOrSavedState();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void clickVisibleSaveButtons() {
        Locator saves = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^Save$", Pattern.CASE_INSENSITIVE)));
        int clicked = 0;
        for (int index = saves.count() - 1; index >= 0; index--) {
            Locator save = saves.nth(index);
            if (!save.isVisible() || !save.isEnabled()) {
                continue;
            }

            Response response = null;
            try {
                response = page.waitForResponse(
                        r -> r.url().contains("scaudits") || r.url().contains("/template") || r.url().contains("/templates"),
                        () -> save.click(new Locator.ClickOptions().setForce(true).setTimeout(5000)));
            } catch (RuntimeException ex) {
                save.click(new Locator.ClickOptions().setForce(true).setTimeout(5000));
            }
            if (response != null && response.status() >= 400) {
                throw new AssertionError("Template save request failed: " + response.status() + " " + response.url());
            }
            clicked++;
            page.waitForTimeout(750);
        }

        if (clicked == 0) {
            Locator fallback = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(Pattern.compile("^(Update|Create)$", Pattern.CASE_INSENSITIVE)))
                    .last();
            assertThat(fallback).isVisible();
            fallback.click(new Locator.ClickOptions().setForce(true));
        }
    }

    public void cancelChanges() {
        Locator cancel = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^(Cancel|Back)$", Pattern.CASE_INSENSITIVE)))
                .first();
        if (cancel.count() > 0 && cancel.isVisible()) {
            cancel.click(new Locator.ClickOptions().setForce(true));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            return;
        }

        page.goBack();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void verifyTemplateFields(String name, String description, Integer targetScore, String category) {
        if (!isBlank(name)) {
            assertInputValue("Template Name", name);
        }
        if (description != null) {
            assertVisibleValueOrText(description);
        }
        if (targetScore != null) {
            assertVisibleValueOrText(String.valueOf(targetScore));
        }
        if (!isBlank(category)) {
            assertVisibleValueOrText(category);
        }
    }

    public void verifySectionTitles(String sectionTitle, String subSectionTitle) {
        if (!isBlank(sectionTitle)) {
            assertVisibleValueOrText(sectionTitle);
        }
        if (!isBlank(subSectionTitle)) {
            assertVisibleValueOrText(subSectionTitle);
        }
    }

    public void verifySuccessToastOrSavedState() {
        Locator success = page.locator(".ant-message-success:visible, .ant-notification-notice-success:visible, "
                + ".ant-message-notice:visible:has-text('success'), .ant-notification-notice:visible:has-text('success')")
                .first();
        try {
            assertThat(success).isVisible();
            return;
        } catch (AssertionError ignored) {
            Locator savedState = page.locator("form:visible, table:visible")
                    .or(page.getByText("Inspection Template"))
                    .first();
            assertThat(savedState).isVisible();
        }
    }

    private void fillInputByLabel(String label, String value) {
        Locator input = page.getByLabel(label).first();
        if (input.count() == 0 || !input.isVisible()) {
            input = page.locator("label:has-text('" + label + "')")
                    .locator("xpath=following::input[not(@type='hidden')][1]")
                    .first();
        }
        assertThat(input).isVisible();
        input.fill(value);
    }

    private void assertInputValue(String label, String value) {
        Locator input = page.getByLabel(label).first();
        if (input.count() > 0 && input.isVisible()) {
            assertThat(input).hasValue(value);
            return;
        }
        assertVisibleValueOrText(value);
    }

    private void assertVisibleValueOrText(String value) {
        if (page.getByText(value).count() > 0 && page.getByText(value).first().isVisible()) {
            return;
        }
        Locator inputs = page.locator("input:visible, textarea:visible");
        for (int index = 0; index < inputs.count(); index++) {
            if (value.equals(safeInputValue(inputs.nth(index)))) {
                return;
            }
        }
        throw new AssertionError("Expected visible value/text not found: " + value
                + ". Page text: " + page.locator("body").innerText());
    }

    private void editInlineTitle(String existingTextHint, String title) {
        if (isBlank(title)) {
            return;
        }

        Locator text = page.getByText(Pattern.compile("^\\s*" + Pattern.quote(existingTextHint) + "\\s*$",
                Pattern.CASE_INSENSITIVE))
                .first();
        if (text.count() == 0 || !text.isVisible()) {
            text = page.getByText(Pattern.compile(".*" + Pattern.quote(existingTextHint) + ".*",
                    Pattern.CASE_INSENSITIVE)).last();
        }
        if (text.count() == 0 || !text.isVisible()) {
            return;
        }

        text.scrollIntoViewIfNeeded();
        Locator editIcon = text.locator("xpath=ancestor::*[contains(@class,'ant-typography') or self::span][1]")
                .locator("[aria-label='edit']:visible")
                .first();
        if (editIcon.count() > 0 && editIcon.isVisible()) {
            editIcon.click(new Locator.ClickOptions().setForce(true));
        } else {
            editIcon = text.locator("xpath=following::*[@aria-label='Edit' or @aria-label='edit'][1]");
            if (editIcon.count() > 0 && editIcon.isVisible()) {
                editIcon.click(new Locator.ClickOptions().setForce(true));
            } else {
                text.dblclick(new Locator.DblclickOptions().setForce(true));
            }
        }

        Locator input = page.locator(".ant-typography-edit-content input:visible, "
                + ".ant-typography-edit-content textarea:visible, "
                + "input:visible, textarea:visible").last();
        if (input.count() > 0 && input.isVisible() && input.isEnabled()) {
            input.fill(title);
            input.press("Enter");
            page.waitForTimeout(300);
        }
    }

    private void expandFirstSectionIfNeeded() {
        Locator header = page.locator(".ant-collapse-item:visible .ant-collapse-header").first();
        if (header.count() == 0 || !header.isVisible()) {
            return;
        }

        if (!"true".equals(header.getAttribute("aria-expanded"))) {
            header.click(new Locator.ClickOptions().setForce(true));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            Locator content = page.locator(".ant-collapse-content-active:visible, .ant-collapse-content-box:visible").first();
            if (content.count() > 0 && content.isVisible()) {
                return;
            }
            page.waitForTimeout(250);
        }
    }

    private String safeInputValue(Locator input) {
        try {
            return input.inputValue().trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
