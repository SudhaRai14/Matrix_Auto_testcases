package pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import models.QuestionData;

public class QuestionComponent {
    private final Page page;

    public QuestionComponent(Page page) {
        this.page = page;
    }

    public void editQuestionText(String existingQuestion, String newQuestion) {
        Locator row = questionRow(existingQuestion);
        row.scrollIntoViewIfNeeded();
        Locator input = row.locator("input:visible, textarea:visible").first();
        assertThat(input).isVisible();
        input.fill(newQuestion);
    }

    public void editAnswerType(String question, String questionType) {
        openQuestionEditor(question);
        selectModalAnswerFormat(questionType);
        clickModalOk();
    }

    public void addQuestion(QuestionData question) {
        clickQuestionPlusControl();
        configureQuestionModal(question);
    }

    public void deleteQuestion(String question) {
        Locator row = questionRow(question);
        row.scrollIntoViewIfNeeded();
        row.hover();

        Locator delete = row.locator("span[aria-label='delete']:visible, img[src*='Delete']:visible").last();
        assertThat(delete).isVisible();
        delete.click(new Locator.ClickOptions().setForce(true));

        Locator confirm = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^(Delete|OK|Yes)$", Pattern.CASE_INSENSITIVE)))
                .last();
        if (confirm.count() > 0 && confirm.isVisible()) {
            confirm.click(new Locator.ClickOptions().setForce(true));
        }
    }

    public void editMcqOptions(String question, List<String> options) {
        openQuestionEditor(question);
        replaceVisibleOptionTexts(options);
        clickModalOk();
    }

    public void editCheckboxOptions(String question, List<String> options) {
        openQuestionEditor(question);
        replaceVisibleOptionTexts(options);
        clickModalOk();
    }

    public void editSliderValues(String question, Integer min, Integer max) {
        openQuestionEditor(question);
        fillNumberNearLabel("Min", min);
        fillNumberNearLabel("Max", max);
        clickModalOk();
    }

    public void editMandatoryFlag(String question, boolean mandatory) {
        openQuestionEditor(question);
        setModalCheckbox("Mandatory", mandatory);
        clickModalOk();
    }

    public void editScoring(String question, boolean enabled, Integer firstScore) {
        openQuestionEditor(question);
        setSwitchNearText("Score", enabled);
        if (firstScore != null) {
            Locator scoreInput = page.locator(".ant-modal-content:visible .ant-input-number-input:visible").first();
            if (scoreInput.count() > 0 && scoreInput.isVisible() && scoreInput.isEnabled()) {
                scoreInput.fill(String.valueOf(firstScore));
            }
        }
        clickModalOk();
    }

    public void reorderQuestions(String sourceQuestion, String targetQuestion) {
        Locator source = questionRow(sourceQuestion);
        Locator target = questionRow(targetQuestion);
        source.scrollIntoViewIfNeeded();
        target.scrollIntoViewIfNeeded();

        Locator handle = source.locator("[aria-label='drag']:visible, .anticon-drag:visible").first();
        if (handle.count() == 0 || !handle.isVisible()) {
            handle = source;
        }
        handle.dragTo(target);
    }

    public void verifyQuestionPresent(String question) {
        if (isQuestionPresent(question)) {
            return;
        }
        throw new AssertionError("Question was not visible: " + question + ". Page text: " + page.locator("body").innerText());
    }

    public void verifyQuestionAbsent(String question) {
        if (isQuestionPresent(question)) {
            throw new AssertionError("Question should not be visible after edit: " + question);
        }
    }

    private void configureQuestionModal(QuestionData data) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        assertThat(modal).isVisible();

        fillModalQuestion(data.question);
        selectModalAnswerFormat(data.questionType);
        fillModalQuestion(data.question);
        configureAnswerDetails(data);
        setSwitchNearText("Enabled", true);
        setModalCheckbox("Mandatory", data.mandatory);
        clickModalOk();
    }

    private void configureAnswerDetails(QuestionData data) {
        if ("slider".equalsIgnoreCase(data.questionType)) {
            fillNumberNearLabel("Min", data.min);
            fillNumberNearLabel("Max", data.max);
        } else if ("mcq".equalsIgnoreCase(data.questionType) || "checkbox".equalsIgnoreCase(data.questionType)) {
            replaceVisibleOptionTexts(data.options);
        }
    }

    private void openQuestionEditor(String question) {
        Locator row = questionRow(question);
        row.scrollIntoViewIfNeeded();
        row.hover();

        Locator edit = row.locator("span[aria-label='form']:visible, span[aria-label='edit']:visible, .anticon-form:visible")
                .last();
        if (edit.count() > 0 && edit.isVisible()) {
            edit.click(new Locator.ClickOptions().setForce(true));
        } else {
            row.dblclick(new Locator.DblclickOptions().setForce(true));
        }

        Locator modal = page.locator(".ant-modal-content:visible").last();
        assertThat(modal).isVisible();
    }

    private Locator questionRow(String question) {
        Locator row = page.locator("form div:visible")
                .filter(new Locator.FilterOptions().setHas(page.locator("input, textarea")))
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile(Pattern.quote(question))))
                .last();
        if (row.count() > 0 && row.isVisible()) {
            return row;
        }

        Locator inputs = page.locator("input:visible, textarea:visible");
        for (int index = 0; index < inputs.count(); index++) {
            Locator input = inputs.nth(index);
            if (question.equals(safeInputValue(input))) {
                return input.locator("xpath=ancestor::div[contains(@style,'border-bottom') or contains(@class,'ant-collapse-content-box')][1]");
            }
        }

        throw new AssertionError("Unable to locate question row for '" + question + "'. Page text: "
                + page.locator("body").innerText());
    }

    private boolean isQuestionPresent(String question) {
        if (page.getByText(question).count() > 0 && page.getByText(question).first().isVisible()) {
            return true;
        }

        Locator inputs = page.locator("input:visible, textarea:visible");
        for (int index = 0; index < inputs.count(); index++) {
            if (question.equals(safeInputValue(inputs.nth(index)))) {
                return true;
            }
        }
        return false;
    }

    private void clickQuestionPlusControl() {
        Locator plus = page.locator("span[aria-label='plus']:visible").last();
        assertThat(plus).isVisible();
        plus.scrollIntoViewIfNeeded();
        plus.click(new Locator.ClickOptions().setForce(true));
    }

    private void fillModalQuestion(String question) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator input = modal.locator("input.ant-input:visible, textarea.ant-input:visible").first();
        assertThat(input).isVisible();
        input.fill(question);
    }

    private void selectModalAnswerFormat(String questionType) {
        String answerFormat = answerFormatLabel(questionType);
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator select = modal.locator(".ant-select-selector:visible").last();
        assertThat(select).isVisible();
        select.click(new Locator.ClickOptions().setForce(true));

        Locator option = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option")
                .filter(new Locator.FilterOptions().setHasText(answerFormat))
                .first();
        assertThat(option).isVisible();
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private String answerFormatLabel(String questionType) {
        return switch (questionType.toLowerCase()) {
            case "mcq" -> "Multiple Choice Responses";
            case "checkbox" -> "Checkbox";
            case "slider" -> "Slider";
            case "number" -> "Number";
            case "date" -> "Date";
            case "datetime" -> "Datetime";
            case "signature" -> "Signature";
            case "text" -> "Text";
            default -> throw new IllegalArgumentException("Unsupported question type: " + questionType);
        };
    }

    private void replaceVisibleOptionTexts(List<String> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator optionInputs = modal.locator("input.ant-input:visible").filter(new Locator.FilterOptions()
                .setHasNotText(Pattern.compile("^$")));
        int editableCount = Math.min(optionInputs.count(), options.size());
        for (int index = 1; index < editableCount; index++) {
            optionInputs.nth(index).fill(options.get(index - 1));
        }
    }

    private void fillNumberNearLabel(String label, Integer value) {
        if (value == null) {
            return;
        }

        Locator scoped = page.locator(".ant-modal-content:visible")
                .locator("span:has-text('" + label + "'), label:has-text('" + label + "')")
                .last()
                .locator("xpath=following::input[contains(@class,'ant-input-number-input')][1]");
        if (scoped.count() > 0 && scoped.isVisible() && scoped.isEnabled()) {
            scoped.fill(String.valueOf(value));
            return;
        }

        Locator fallback = page.locator(".ant-modal-content:visible .ant-input-number-input:visible").last();
        if (fallback.count() > 0 && fallback.isVisible() && fallback.isEnabled()) {
            fallback.fill(String.valueOf(value));
        }
    }

    private void setModalCheckbox(String label, boolean checked) {
        Locator checkbox = page.locator(".ant-modal-content:visible label:has-text('" + label + "') input[type='checkbox']")
                .last();
        if (checkbox.count() > 0 && checkbox.isVisible() && checkbox.isChecked() != checked) {
            checkbox.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void setSwitchNearText(String label, boolean enabled) {
        Locator switchButton = page.locator(".ant-modal-content:visible span:has-text('" + label + "') + button[role='switch']")
                .last();
        if (switchButton.count() == 0 || !switchButton.isVisible()) {
            return;
        }

        boolean current = "true".equals(switchButton.getAttribute("aria-checked"));
        if (current != enabled) {
            switchButton.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void clickModalOk() {
        Locator modalWrap = page.locator(".ant-modal-wrap:visible").last();
        Locator ok = modalWrap.locator("button:has-text('OK'), button:has-text('Save')").last();
        assertThat(ok).isVisible();
        ok.click(new Locator.ClickOptions().setForce(true));
        modalWrap.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    private String safeInputValue(Locator input) {
        try {
            return input.inputValue().trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }
}
