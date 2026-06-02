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
        Locator input = row.locator(
                "input.ant-input[maxlength='250']:visible, " +
                        "textarea.ant-input[maxlength='250']:visible")
                .first();
        assertThat(input).isVisible();
        fillTextInput(input, newQuestion);
    }

    public void editAnswerType(String question, String questionType) {
        Locator row = questionRow(question);
        if (tryOpenQuestionEditor(row)) {
            selectModalAnswerFormat(questionType);
            clickModalOk();
            return;
        }

        Locator disabledAnswerFormat = row.locator(".ant-select-disabled .ant-select-selection-item:visible").first();
        if (disabledAnswerFormat.count() > 0 && disabledAnswerFormat.isVisible()) {
            return;
        }

        selectInlineAnswerFormat(row, questionType);
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
        Locator row = questionRow(question);
        if (tryOpenQuestionEditor(row)) {
            fillNumberNearLabel("Min", min);
            fillNumberNearLabel("Max", max);
            clickModalOk();
            return;
        }

        fillNumberNearLabel(row, "Min", min);
        fillNumberNearLabel(row, "Max", max);
    }

    public void editMandatoryFlag(String question, boolean mandatory) {
        Locator row = questionRow(question);
        if (tryOpenQuestionEditor(row)) {
            setModalCheckbox("Mandatory", mandatory);
            clickModalOk();
            return;
        }

        setCheckbox(row, "Mandatory", mandatory);
    }

    public void editScoring(String question, boolean enabled, Integer firstScore) {
        Locator row = questionRow(question);
        if (tryOpenQuestionEditor(row)) {
            setSwitchNearText("Score", enabled);
            if (firstScore != null) {
                Locator scoreInput = page.locator(".ant-modal-content:visible .ant-input-number-input:visible").first();
                if (scoreInput.count() > 0 && scoreInput.isVisible() && scoreInput.isEnabled()) {
                    scoreInput.fill(String.valueOf(firstScore));
                }
            }
            clickModalOk();
            return;
        }

        Locator scoreSwitch = row.locator("span:has-text('Score') + button[role='switch']:visible").first();
        if (scoreSwitch.count() > 0 && scoreSwitch.isVisible()) {
            boolean current = "true".equals(scoreSwitch.getAttribute("aria-checked"));
            if (current != enabled) {
                scoreSwitch.click(new Locator.ClickOptions().setForce(true));
            }
        }
        if (firstScore != null) {
            Locator scoreInput = row.locator(".ant-input-number-input:visible").first();
            if (scoreInput.count() > 0 && scoreInput.isVisible() && scoreInput.isEnabled()) {
                scoreInput.fill(String.valueOf(firstScore));
            }
        }
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
        expandVisibleSections();
        if (isQuestionPresent(question)) {
            return;
        }
        throw new AssertionError("Question was not visible: " + question + ". Page text: " + page.locator("body").innerText());
    }

    public void verifyQuestionAbsent(String question) {
        expandVisibleSections();
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
        expandVisibleSections();
        Locator row = questionRow(question);
        if (!tryOpenQuestionEditor(row)) {
            throw new AssertionError("Unable to open question editor for '" + question + "'. Page text: "
                    + page.locator("body").innerText());
        }
    }

    private boolean tryOpenQuestionEditor(Locator row) {
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
        try {
            assertThat(modal).isVisible();
            return true;
        } catch (AssertionError ignored) {
            return false;
        }
    }

    private Locator questionRow(String question) {
        expandVisibleSections();
        Locator questionInputs = page.locator(
                "input.ant-input[maxlength='250']:visible, " +
                        "textarea.ant-input[maxlength='250']:visible");
        for (int index = 0; index < questionInputs.count(); index++) {
            Locator input = questionInputs.nth(index);
            if (question.equals(safeInputValue(input))) {
                return input.locator("xpath=ancestor::div[contains(@style,'border-bottom')][1]");
            }
        }

        Locator row = page.locator("form div:visible")
                .filter(new Locator.FilterOptions().setHas(page.locator("input, textarea")))
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile(Pattern.quote(question))))
                .last();
        if (row.count() > 0 && row.isVisible()) {
            return row;
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

    private void expandVisibleSections() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            Locator collapsedHeaders = page.locator(".ant-collapse-header[aria-expanded='false']:visible");
            if (collapsedHeaders.count() == 0) {
                return;
            }

            Locator header = collapsedHeaders.first();
            header.scrollIntoViewIfNeeded();
            try {
                header.click(new Locator.ClickOptions().setForce(true).setTimeout(2000));
            } catch (RuntimeException ex) {
                header.evaluate("element => element.click()");
            }
            page.waitForTimeout(500);
        }
    }

    private void clickQuestionPlusControl() {
        Locator plus = page.locator("span[aria-label='plus']:visible").last();
        assertThat(plus).isVisible();
        plus.scrollIntoViewIfNeeded();
        plus.click(new Locator.ClickOptions().setForce(true));
    }

    private void fillModalQuestion(String question) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator input = modal.locator(
                "input.ant-input[maxlength='250']:visible, " +
                        "textarea.ant-input[maxlength='250']:visible")
                .first();
        if (input.count() == 0 || !input.isVisible()) {
            input = modal.locator("input.ant-input:visible, textarea.ant-input:visible").first();
        }
        assertThat(input).isVisible();
        fillTextInput(input, question);
    }

    private void selectModalAnswerFormat(String questionType) {
        String answerFormat = answerFormatLabel(questionType);
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator select = modal.locator("text=Answer Format")
                .locator("xpath=following::div[contains(@class,'ant-select-selector')][1]")
                .first();
        if (select.count() == 0 || !select.isVisible()) {
            select = modal.locator(".ant-select:not(.ant-select-disabled) .ant-select-selector:visible").first();
        }
        assertThat(select).isVisible();
        openAnswerFormatDropdown(select);

        Locator option = visibleAnswerFormatOption(answerFormat);
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private void openAnswerFormatDropdown(Locator select) {
        select.scrollIntoViewIfNeeded();
        select.click(new Locator.ClickOptions().setForce(true));
        if (hasVisibleAnswerFormatOptions()) {
            return;
        }

        Locator combobox = select.locator("input[role='combobox']").first();
        if (combobox.count() > 0) {
            combobox.click(new Locator.ClickOptions().setForce(true));
            if (hasVisibleAnswerFormatOptions()) {
                return;
            }
            combobox.press("ArrowDown");
            if (hasVisibleAnswerFormatOptions()) {
                return;
            }
        }

        select.evaluate("element => element.click()");
        if (!hasVisibleAnswerFormatOptions()) {
            page.keyboard().press("ArrowDown");
        }
    }

    private boolean hasVisibleAnswerFormatOptions() {
        Locator options = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option");
        for (int index = 0; index < options.count(); index++) {
            if (options.nth(index).isVisible()) {
                return true;
            }
        }
        return false;
    }

    private Locator visibleAnswerFormatOption(String answerFormat) {
        String expected = normalizeAnswerFormat(answerFormat);
        Locator options = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                if (!option.isVisible()) {
                    continue;
                }

                String text = option.innerText().trim();
                String title = option.getAttribute("title");
                if (expected.equals(normalizeAnswerFormat(text))
                        || expected.equals(normalizeAnswerFormat(title))) {
                    return option;
                }
            }
            page.waitForTimeout(250);
        }
        throw new AssertionError("Unable to select answer format '" + answerFormat + "'. Page text: "
                + page.locator("body").innerText());
    }

    private void selectInlineAnswerFormat(Locator row, String questionType) {
        String answerFormat = answerFormatLabel(questionType);
        Locator select = row.locator(".ant-select:not(.ant-select-disabled) .ant-select-selector:visible").first();
        assertThat(select).isVisible();
        openAnswerFormatDropdown(select);
        visibleAnswerFormatOption(answerFormat).click(new Locator.ClickOptions().setForce(true));
    }

    private String normalizeAnswerFormat(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return normalized.endsWith("responses")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
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

    private void fillNumberNearLabel(Locator scope, String label, Integer value) {
        if (value == null) {
            return;
        }

        Locator input = scope.locator("span:has-text('" + label + "'), label:has-text('" + label + "')")
                .last()
                .locator("xpath=following::input[contains(@class,'ant-input-number-input')][1]");
        if (input.count() > 0 && input.isVisible() && input.isEnabled()) {
            input.fill(String.valueOf(value));
        }
    }

    private void setModalCheckbox(String label, boolean checked) {
        Locator checkbox = page.locator(".ant-modal-content:visible label:has-text('" + label + "') input[type='checkbox']")
                .last();
        if (checkbox.count() > 0 && checkbox.isVisible() && checkbox.isChecked() != checked) {
            checkbox.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void setCheckbox(Locator scope, String label, boolean checked) {
        Locator checkbox = scope.locator("label:has-text('" + label + "') input[type='checkbox']:visible").first();
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

    private void fillTextInput(Locator input, String value) {
        input.scrollIntoViewIfNeeded();
        input.click(new Locator.ClickOptions().setForce(true));
        input.fill(value);
        input.evaluate("(element, value) => {"
                + "const prototype = element.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;"
                + "const setter = Object.getOwnPropertyDescriptor(prototype, 'value').set;"
                + "setter.call(element, value);"
                + "element.dispatchEvent(new Event('input', { bubbles: true }));"
                + "element.dispatchEvent(new Event('change', { bubbles: true }));"
                + "element.blur();"
                + "}", value);
        page.waitForTimeout(150);
    }
}
