package pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import models.QuestionData;
import models.TemplateData;

public class TemplatePage {

    private final Page page;

    public TemplatePage(Page page) {
        this.page = page;
    }

    // =========================================================
    // OPEN TEMPLATE MODULE
    // =========================================================

    public void openTemplateModule() {

        try {
            page.getByText(
                    "Audits",
                    new Page.GetByTextOptions()
                            .setExact(true))
                    .click(new Locator.ClickOptions().setTimeout(5000));

            page.getByText(
                    "Templates",
                    new Page.GetByTextOptions()
                            .setExact(true))
                    .click(new Locator.ClickOptions().setTimeout(10000));
        } catch (RuntimeException ex) {
            navigateDirectlyToTemplates();
        }

        waitForTemplatePage();
    }

    private void navigateDirectlyToTemplates() {
        String currentUrl = page.url();
        String org = extractQueryValue(currentUrl, "org", "SMARTCLEANHQ");
        String propertyId = extractQueryValue(currentUrl, "propId", "8cb0777846c64dc0a2d69cc080aaf8c7");
        String accountId = System.getProperty("matrix.account.id",
                System.getenv().getOrDefault("MATRIX_ACCOUNT_ID", "0397f1af95604770a6148231aaf5c6f9"));
        page.navigate(String.format(
                "https://www.smartclean.io/matrix/audits/v48_7/#/templates/%s/%s/%s",
                org,
                propertyId,
                accountId));
    }

    private String extractQueryValue(String url, String key, String fallback) {
        Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(key) + "=([^&#]+)");
        java.util.regex.Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    public void waitForTemplatePage() {

        Locator title = page.getByText(
                "Templates",
                new Page.GetByTextOptions()
                        .setExact(true));

        assertThat(title).isVisible();
    }

    // =========================================================
    // CREATE TEMPLATE
    // =========================================================

    public void clickCreateTemplate() {

        Locator createButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("create\\s+template", Pattern.CASE_INSENSITIVE)));

        assertThat(createButton).isVisible();

        createButton.click();
    }

    public void enterTemplateName(String templateName) {

        Locator templateNameInput = visibleTemplateNameInput();

        assertThat(templateNameInput).isVisible();

        templateNameInput.fill(templateName);
    }

    public void enterTemplateDescription(
            String description) {

        Locator descriptionInput = page.locator(
                "#template_templateDescription:visible, " +
                        "textarea[placeholder*='description' i]:visible, " +
                        "input[placeholder*='description' i]:visible, " +
                        ".ant-drawer-content:visible textarea, " +
                        ".ant-modal-content:visible textarea")
                .first();

        if (descriptionInput.count() > 0) {

            descriptionInput.fill(description);
        }
    }

    public void selectAnyCategory() {
        openCategoryDropdown();

        Locator dropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        Locator options = dropdown.locator(".ant-select-item-option");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            int count = options.count();
            for (int index = 0; index < count; index++) {
                Locator option = options.nth(index);
                String text = safeInnerText(option);
                if (!text.isBlank()
                        && !"No data".equalsIgnoreCase(text)) {
                    clickCategoryOption(option);
                    return;
                }
            }

            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to select a category. Page text: " + page.locator("body").innerText());
    }

    private void clickCategoryOption(Locator option) {
        try {
            option.click(new Locator.ClickOptions().setForce(true).setTimeout(1500));
        } catch (RuntimeException ex) {
            option.evaluate("element => element.click()");
        }

        if (isCategoryDropdownOpen()) {
            page.keyboard().press("Escape");
        }
    }

    private String safeInnerText(Locator locator) {
        try {
            return locator.innerText(new Locator.InnerTextOptions().setTimeout(1500)).trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private void openCategoryDropdown() {
        Locator templateCategorySelector = page.locator(
                ".ant-form-item:has(label[for='template_category']) .ant-select-selector")
                .first();
        if (templateCategorySelector.count() > 0) {
            templateCategorySelector.click(new Locator.ClickOptions().setForce(true));
            if (isCategoryDropdownOpen()) {
                return;
            }
        }

        throw new IllegalStateException("Unable to open category dropdown. Page text: " + page.locator("body").innerText());
    }

    private boolean isCategoryDropdownOpen() {
        Locator dropdown = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden)").last();
        return dropdown.count() > 0;
    }

    private void clickQuestionPlusControl() {
        Locator plus = page.locator("span[aria-label='plus']:visible").last();
        assertThat(plus).isVisible();
        plus.click(new Locator.ClickOptions().setForce(true));
    }

    private Locator visibleTemplateNameInput() {
        Locator preferredTemplateInput = page.locator("#template_templateName:visible").first();
        if (preferredTemplateInput.count() > 0
                && preferredTemplateInput.isVisible()
                && preferredTemplateInput.isEnabled()) {
            return preferredTemplateInput;
        }

        Locator candidates = page.locator(
                "input[placeholder*='template name' i]:visible, " +
                        "input[placeholder*='template' i]:visible, " +
                        "input[placeholder*='name' i]:visible, " +
                        ".ant-drawer-content:visible input:not([type='hidden']):not([readonly]), " +
                        ".ant-modal-content:visible input:not([type='hidden']):not([readonly]), " +
                        "form:visible input:not([type='hidden']):not([readonly])");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            int count = candidates.count();
            for (int index = 0; index < count; index++) {
                Locator candidate = candidates.nth(index);
                if (candidate.isVisible() && candidate.isEnabled()) {
                    return candidate;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find visible template name input. Page text: "
                + page.locator("body").innerText());
    }

    private Locator visibleButtonMatching(String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Locator candidates = page.locator("button:visible, [role='button']:visible");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            int count = candidates.count();
            for (int index = 0; index < count; index++) {
                Locator candidate = candidates.nth(index);
                if (!candidate.isVisible() || !candidate.isEnabled()) {
                    continue;
                }

                String ariaLabel = candidate.getAttribute("aria-label");
                String title = candidate.getAttribute("title");
                String haystack = String.join(" ",
                        candidate.innerText().trim(),
                        ariaLabel == null ? "" : ariaLabel,
                        title == null ? "" : title);

                if (pattern.matcher(haystack).find()) {
                    return candidate;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find button matching /" + regex + "/. Visible buttons: "
                + visibleButtonTexts() + ". Page text: " + page.locator("body").innerText());
    }

    private String visibleButtonTexts() {
        StringBuilder builder = new StringBuilder();
        Locator buttons = page.locator("button:visible, [role='button']:visible");
        int count = buttons.count();
        for (int index = 0; index < count; index++) {
            Locator button = buttons.nth(index);
            if (!button.isVisible()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(button.innerText().trim());
            String ariaLabel = button.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isBlank()) {
                builder.append(" [").append(ariaLabel).append("]");
            }
        }
        return builder.toString();
    }

    // =========================================================
    // CREATE COMPLETE TEMPLATE
    // =========================================================

    public void createTemplate(TemplateData templateData) {

        clickCreateTemplate();

        enterTemplateName(templateData.templateName);

        enterTemplateDescription(
                templateData.description);

        for (QuestionData question :
                templateData.questions) {

            addQuestion(question);
        }

        saveTemplate();
    }

    // =========================================================
    // ADD QUESTION
    // =========================================================

    public void addQuestion(QuestionData data) {

        clickAddQuestion();

        waitForQuestionDrawer();

        selectQuestionType(data.questionType);

        enterQuestion(data.question);

        switch (data.questionType.toLowerCase()) {

            case "mcq":
                addOptions(data.options);
                break;

            case "checkbox":
                addOptions(data.options);
                break;

            case "slider":
                configureSlider(data.min, data.max);
                break;

            case "number":
                configureNumberQuestion();
                break;

            case "date":
                configureDateQuestion();
                break;

            case "datetime":
                configureDateTimeQuestion();
                break;

            case "signature":
                configureSignatureQuestion();
                break;

            case "text":
                configureTextQuestion();
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported question type: "
                                + data.questionType);
        }

        setMandatory(data.mandatory);

        clickSaveQuestion();
    }

    public void configureInitialQuestion(QuestionData data) {
        if (page.getByText("No questions").count() > 0 && page.getByText("No questions").first().isVisible()) {
            clickQuestionPlusControl();
        }

        configureLatestInlineQuestion(data);
    }

    public void configureInlineQuestions(List<QuestionData> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Template must contain at least one question.");
        }

        ensureMainSectionExists();
        expandFirstMainSection();
        configureInitialQuestion(questions.get(0));
        for (int index = 1; index < questions.size(); index++) {
            clickQuestionPlusControl();
            configureQuestionModal(questions.get(index));
        }

        normalizeInlineQuestionTexts(questions);
    }

    private void ensureMainSectionExists() {
        Locator emptyMainSections = page.getByText(
                Pattern.compile("No\\s+main\\s+sections", Pattern.CASE_INSENSITIVE))
                .first();
        if (emptyMainSections.count() == 0 || !emptyMainSections.isVisible()) {
            return;
        }

        Locator mainSectionControl = page.locator("div[style*='cursor']")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*Main\\s+Section\\s*$",
                        Pattern.CASE_INSENSITIVE)))
                .last();
        if (mainSectionControl.count() == 0 || !mainSectionControl.isVisible()) {
            mainSectionControl = page.getByText(Pattern.compile("^\\s*Main\\s+Section\\s*$",
                    Pattern.CASE_INSENSITIVE)).last();
        }

        assertThat(mainSectionControl).isVisible();
        mainSectionControl.click(new Locator.ClickOptions().setForce(true));

        emptyMainSections.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    private void expandFirstMainSection() {
        Locator header = page.locator(".ant-collapse-item:visible .ant-collapse-header").first();
        if (header.count() == 0 || !header.isVisible()) {
            return;
        }

        if (!"true".equals(header.getAttribute("aria-expanded"))) {
            header.click(new Locator.ClickOptions().setForce(true));
        }

        fillInlineTitleIfPresent("Type your Main Section title", "Main Section");
        fillInlineTitleIfPresent("Type your Sub Section title", "Sub Section");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            Locator content = page.locator(".ant-collapse-content-active:visible, .ant-collapse-content-box:visible").first();
            if (content.count() > 0 && content.isVisible()) {
                return;
            }
            page.waitForTimeout(250);
        }
    }

    private void fillInlineTitleIfPresent(String placeholderText, String title) {
        Locator placeholder = page.getByText(
                Pattern.compile("^\\s*" + Pattern.quote(placeholderText) + "\\s*$", Pattern.CASE_INSENSITIVE))
                .first();
        if (placeholder.count() == 0 || !placeholder.isVisible()) {
            return;
        }

        Locator edit = placeholder.locator("xpath=ancestor::*[contains(@class,'ant-typography')][1]")
                .locator("[aria-label='Edit']:visible, [aria-label='edit']:visible")
                .first();
        if (edit.count() == 0 || !edit.isVisible()) {
            edit = placeholder.locator("xpath=following::*[@aria-label='Edit' or @aria-label='edit'][1]");
        }
        if (edit.count() == 0 || !edit.isVisible()) {
            return;
        }

        edit.click(new Locator.ClickOptions().setForce(true));

        Locator input = page.locator(
                ".ant-typography-edit-content input:visible, " +
                        ".ant-typography-edit-content textarea:visible, " +
                        "input:visible:not(#template_templateName):not(#template_targetScore), " +
                        "textarea:visible:not(#template_templateDescription)")
                .last();
        if (input.count() > 0 && input.isVisible() && input.isEnabled()) {
            input.fill(title);
            input.press("Enter");
            page.waitForTimeout(300);
        }
    }

    private void normalizeInlineQuestionTexts(List<QuestionData> questions) {
        Locator questionInputs = page.locator(
                "form input.ant-input:visible:not(#template_templateName), " +
                        "form textarea.ant-input:visible:not(#template_templateDescription)");

        int questionIndex = 0;
        for (int inputIndex = 0; inputIndex < questionInputs.count() && questionIndex < questions.size(); inputIndex++) {
            Locator input = questionInputs.nth(inputIndex);
            if (!input.isVisible() || !input.isEnabled()) {
                continue;
            }

            String id = input.getAttribute("id");
            if (id != null && id.startsWith("template_")) {
                continue;
            }

            input.scrollIntoViewIfNeeded();
            input.fill(questions.get(questionIndex).question);
            questionIndex++;
        }

        if (questionIndex < questions.size()) {
            throw new AssertionError("Expected " + questions.size() + " question inputs, but only found "
                    + questionIndex + ". Page text: " + page.locator("body").innerText());
        }
    }

    private void configureLatestInlineQuestion(QuestionData data) {
        saveQuestionModalIfOpen();
        enterInlineQuestion(data.question);
        selectInlineAnswerFormat(data.questionType);
        configureInlineAnswerDetails(data);
        saveQuestionModalIfOpen();
        setInlineMandatory(data.mandatory);
        saveQuestionModalIfOpen();
    }

    private void enterInlineQuestion(String question) {
        Locator questionInput = visibleInlineQuestionInput();
        assertThat(questionInput).isVisible();
        questionInput.fill(question);
    }

    private Locator visibleInlineQuestionInput() {
        Locator preferred = page.locator(
                "form input[placeholder*='question' i]:visible, " +
                        "form textarea[placeholder*='question' i]:visible")
                .last();
        if (preferred.count() > 0 && preferred.isVisible() && preferred.isEnabled()) {
            return preferred;
        }

        Locator candidates = page.locator(
                "form input[type='text']:visible:not([readonly]), " +
                        "form textarea:visible:not([readonly])");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            int count = candidates.count();
            for (int index = count - 1; index >= 0; index--) {
                Locator candidate = candidates.nth(index);
                if (!candidate.isVisible() || !candidate.isEnabled()) {
                    continue;
                }

                String id = candidate.getAttribute("id");
                if (id != null && id.startsWith("template_")) {
                    continue;
                }

                return candidate;
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find inline question input. Page text: "
                + page.locator("body").innerText());
    }

    private void selectInlineAnswerFormat(String questionType) {
        String answerFormat = inlineAnswerFormatLabel(questionType);
        if (isInlineAnswerFormatSelected(answerFormat)) {
            return;
        }

        Locator answerFormatSelect = page.locator(
                "form .ant-select:has(.ant-select-selection-placeholder:has-text('Select answer type')) .ant-select-selector, " +
                        "form .ant-select-selector:has-text('Select answer type')")
                .last();

        if (answerFormatSelect.count() == 0 || !answerFormatSelect.isVisible()) {
            answerFormatSelect = page.locator("form .ant-select-selector:visible").last();
        }

        assertThat(answerFormatSelect).isVisible();
        answerFormatSelect.click(new Locator.ClickOptions().setForce(true));

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            if (isInlineAnswerFormatSelected(answerFormat)) {
                return;
            }

            Locator option = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option")
                    .filter(new Locator.FilterOptions().setHasText(answerFormat))
                    .first();
            if (option.count() > 0 && option.isVisible()) {
                option.click(new Locator.ClickOptions().setForce(true));
                return;
            }

            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to select inline answer format '" + answerFormat
                + "'. Page text: " + page.locator("body").innerText());
    }

    private boolean isInlineAnswerFormatSelected(String answerFormat) {
        Locator selected = page.locator("form .ant-select-selection-item")
                .filter(new Locator.FilterOptions().setHasText(answerFormat))
                .first();
        return selected.count() > 0 && selected.isVisible();
    }

    private void configureInlineAnswerDetails(QuestionData data) {
        switch (data.questionType.toLowerCase()) {
            case "slider":
                fillLatestBlankNumberInput(data.min, "1");
                fillLatestBlankNumberInput(data.max, "10");
                break;
            case "mcq":
            case "checkbox":
            case "number":
            case "date":
            case "datetime":
            case "signature":
            case "text":
                break;
            default:
                throw new IllegalArgumentException("Unsupported question type: " + data.questionType);
        }
    }

    private void fillLatestBlankNumberInput(Integer value, String fallback) {
        Locator inputs = page.locator("form input[type='number']:visible, form .ant-input-number-input:visible");
        for (int index = inputs.count() - 1; index >= 0; index--) {
            Locator input = inputs.nth(index);
            if (!input.isVisible() || !input.isEnabled()) {
                continue;
            }

            if (safeInputValue(input).isBlank()) {
                input.fill(value == null ? fallback : String.valueOf(value));
                return;
            }
        }
    }

    private void setInlineMandatory(boolean mandatory) {
        Locator mandatoryCheckbox = page.locator("label:has-text('Mandatory') input[type='checkbox']:visible").last();
        if (mandatoryCheckbox.count() == 0 || !mandatoryCheckbox.isVisible()) {
            setMandatory(mandatory);
            return;
        }

        if (mandatoryCheckbox.isChecked() != mandatory) {
            mandatoryCheckbox.click(new Locator.ClickOptions().setForce(true));
        }
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

    private String inlineAnswerFormatLabel(String questionType) {
        return switch (questionType.toLowerCase()) {
            case "datetime" -> "Date Time";
            case "text" -> "Text Answer";
            default -> answerFormatLabel(questionType);
        };
    }

    private void configureQuestionModal(QuestionData data) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        assertThat(modal).isVisible();

        fillModalQuestion(modal, data.question);

        selectModalAnswerFormat(data.questionType);
        fillModalQuestion(modal, data.question);
        configureModalAnswerDetails(data);
        setModalEnabled(true);
        setModalMandatory(data.mandatory);
        clickModalOk(modal);
    }

    private void fillModalQuestion(Locator modal, String question) {
        Locator questionInput = modal.locator("input.ant-input:visible, textarea.ant-input:visible").first();
        assertThat(questionInput).isVisible();
        questionInput.fill(question);
    }

    private void selectModalAnswerFormat(String questionType) {
        String answerFormat = answerFormatLabel(questionType);
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator answerFormatSelect = modal.locator(".ant-select-selector:visible").last();
        assertThat(answerFormatSelect).isVisible();
        openAnswerFormatDropdown(answerFormatSelect);

        Locator option = visibleAnswerFormatOption(answerFormat);
        option.click(new Locator.ClickOptions().setForce(true));
    }

    private void openAnswerFormatDropdown(Locator answerFormatSelect) {
        answerFormatSelect.scrollIntoViewIfNeeded();
        answerFormatSelect.click(new Locator.ClickOptions().setForce(true));
        if (hasVisibleAnswerFormatOptions()) {
            return;
        }

        Locator combobox = answerFormatSelect.locator("input[role='combobox']").first();
        if (combobox.count() > 0) {
            combobox.click(new Locator.ClickOptions().setForce(true));
            if (hasVisibleAnswerFormatOptions()) {
                return;
            }

            combobox.press("Enter");
            if (hasVisibleAnswerFormatOptions()) {
                return;
            }

            combobox.press("ArrowDown");
            if (hasVisibleAnswerFormatOptions()) {
                return;
            }
        }

        answerFormatSelect.evaluate("element => element.click()");
        if (hasVisibleAnswerFormatOptions()) {
            return;
        }

        page.keyboard().press("ArrowDown");
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

        throw new AssertionError("Unable to select answer format '" + answerFormat
                + "'. Visible options: " + visibleAnswerFormatOptions());
    }

    private String visibleAnswerFormatOptions() {
        StringBuilder builder = new StringBuilder();
        Locator options = page.locator(".ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option");
        int count = options.count();
        for (int index = 0; index < count; index++) {
            Locator option = options.nth(index);
            if (!option.isVisible()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(option.innerText().trim());
        }
        return builder.toString();
    }

    private String normalizeAnswerFormat(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return normalized.endsWith("responses")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private void configureModalAnswerDetails(QuestionData data) {
        switch (data.questionType.toLowerCase()) {
            case "slider":
                fillLatestBlankNumberInput(data.min, "1");
                fillLatestBlankNumberInput(data.max, "10");
                break;
            case "mcq":
            case "checkbox":
            case "number":
            case "date":
            case "datetime":
            case "signature":
            case "text":
                break;
            default:
                throw new IllegalArgumentException("Unsupported question type: " + data.questionType);
        }
    }

    private void setModalMandatory(boolean mandatory) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator mandatoryCheckbox = modal.locator("label:has-text('Mandatory') input[type='checkbox']:visible").last();
        if (mandatoryCheckbox.count() > 0 && mandatoryCheckbox.isVisible()
                && mandatoryCheckbox.isChecked() != mandatory) {
            mandatoryCheckbox.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void setModalEnabled(boolean enabled) {
        Locator modal = page.locator(".ant-modal-content:visible").last();
        Locator enabledSwitch = modal.locator("span:has-text('Enabled') + button[role='switch']:visible").last();
        if (enabledSwitch.count() == 0 || !enabledSwitch.isVisible()) {
            return;
        }

        boolean isEnabled = "true".equals(enabledSwitch.getAttribute("aria-checked"));
        if (isEnabled != enabled) {
            enabledSwitch.click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void clickModalOk(Locator modal) {
        Locator okButton = modal.locator("button:has-text('OK')").last();
        assertThat(okButton).isVisible();
        okButton.click(new Locator.ClickOptions().setForce(true));

        page.locator(".ant-modal-wrap:visible").last().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    private void saveQuestionModalIfOpen() {
        Locator modal = page.locator(".ant-modal-wrap:visible").last();
        if (modal.count() == 0 || !modal.isVisible()) {
            return;
        }

        Locator button = modal.locator("button:has-text('OK'), button:has-text('Create'), button:has-text('Save')").last();
        if (button.count() == 0 || !button.isVisible()) {
            page.keyboard().press("Escape");
            return;
        }

        button.click(new Locator.ClickOptions().setForce(true));

        modal.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    private void closeQuestionModalIfOpen() {
        Locator modal = page.locator(".ant-modal-wrap:visible").last();
        if (modal.count() == 0 || !modal.isVisible()) {
            return;
        }

        Locator closeButton = modal.locator("button[aria-label='Close'], button:has-text('Cancel')").first();
        if (closeButton.count() > 0 && closeButton.isVisible()) {
            closeButton.click(new Locator.ClickOptions().setForce(true));
        } else {
            page.keyboard().press("Escape");
        }

        modal.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    // =========================================================
    // ADD QUESTION BUTTON
    // =========================================================

    public void clickAddQuestion() {

        Locator addQuestionButton = visibleButtonMatching("add\\s+question|question|\\+");

        assertThat(addQuestionButton).isVisible();

        addQuestionButton.click();
    }

    // =========================================================
    // QUESTION DRAWER
    // =========================================================

    public void waitForQuestionDrawer() {

        Locator drawer = page.locator(
                ".ant-drawer-content-wrapper");

        assertThat(drawer).isVisible();
    }

    public void waitForQuestionDrawerToClose() {

        Locator drawer = page.locator(
                ".ant-drawer-content-wrapper");

        assertThat(drawer).isHidden();
    }

    // =========================================================
    // QUESTION TYPE
    // =========================================================

    public void selectQuestionType(String type) {

        Locator dropdown = page.getByRole(
                AriaRole.COMBOBOX);

        dropdown.click();

        Locator option = page.getByRole(
                AriaRole.OPTION,
                new Page.GetByRoleOptions()
                        .setName(type));

        assertThat(option).isVisible();

        option.click();
    }

    // =========================================================
    // QUESTION TEXT
    // =========================================================

    public void enterQuestion(String question) {

        Locator questionInput = visibleQuestionInput();

        assertThat(questionInput).isVisible();

        questionInput.fill(question);
    }

    private Locator visibleQuestionInput() {
        Locator candidates = page.locator(
                ".ant-modal-content:visible input:not([type='hidden']):not([readonly]):visible, " +
                        ".ant-modal-content:visible textarea:visible, " +
                        ".ant-drawer-content:visible input:not([type='hidden']):not([readonly]):visible, " +
                        ".ant-drawer-content:visible textarea:visible, " +
                "textarea[placeholder*='question' i]:visible, " +
                        "input[placeholder*='question' i]:visible, " +
                        "textarea:visible, " +
                        "input:not([type='hidden']):not([readonly]):visible");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            int count = candidates.count();
            for (int index = 0; index < count; index++) {
                Locator candidate = candidates.nth(index);
                if (!candidate.isVisible() || !candidate.isEnabled()) {
                    continue;
                }

                String placeholder = candidate.getAttribute("placeholder");
                String value = safeInputValue(candidate);
                String text = String.join(" ",
                        placeholder == null ? "" : placeholder,
                        value);

                if (Pattern.compile("question", Pattern.CASE_INSENSITIVE).matcher(text).find()
                        || value.isBlank()) {
                    return candidate;
                }
            }
            page.waitForTimeout(250);
        }

        throw new IllegalStateException("Unable to find visible question input. Page text: "
                + page.locator("body").innerText());
    }

    private String safeInputValue(Locator input) {
        try {
            return input.inputValue().trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    // =========================================================
    // OPTIONS
    // =========================================================

    public void addOptions(List<String> options) {

        for (int i = 0; i < options.size(); i++) {

            Locator optionInput = page.getByPlaceholder(
                    "Enter option")
                    .last();

            optionInput.fill(options.get(i));

            if (i < options.size() - 1) {

                Locator addOptionButton = page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                                .setName("Add Option"));

                addOptionButton.click();
            }
        }
    }

    // =========================================================
    // SLIDER
    // =========================================================

    public void configureSlider(
            Integer min,
            Integer max) {

        Locator minInput = page.getByLabel(
                "Minimum");

        Locator maxInput = page.getByLabel(
                "Maximum");

        minInput.fill(String.valueOf(min));

        maxInput.fill(String.valueOf(max));
    }

    // =========================================================
    // NUMBER
    // =========================================================

    public void configureNumberQuestion() {

        Locator numberInput = page.locator(
                "input[type='number']");

        assertThat(numberInput).isVisible();
    }

    // =========================================================
    // DATE
    // =========================================================

    public void configureDateQuestion() {

        Locator datePicker = page.locator(
                ".ant-picker");

        assertThat(datePicker).isVisible();
    }

    // =========================================================
    // DATETIME
    // =========================================================

    public void configureDateTimeQuestion() {

        Locator dateTimePicker = page.locator(
                ".ant-picker");

        assertThat(dateTimePicker).isVisible();
    }

    // =========================================================
    // SIGNATURE
    // =========================================================

    public void configureSignatureQuestion() {

        Locator signatureCanvas = page.locator(
                "canvas");

        assertThat(signatureCanvas).isVisible();
    }

    // =========================================================
    // TEXT
    // =========================================================

    public void configureTextQuestion() {

        Locator textArea = page.locator(
                "textarea");

        assertThat(textArea).isVisible();
    }

    // =========================================================
    // MANDATORY
    // =========================================================

    public void setMandatory(boolean mandatory) {

        Locator toggle = page.locator(
                ".ant-switch:visible, input[type='checkbox']:visible")
                .first();

        if (toggle.count() == 0 || !toggle.isVisible()) {
            return;
        }

        boolean enabled;
        if ("checkbox".equalsIgnoreCase(toggle.getAttribute("type"))) {
            enabled = toggle.isChecked();
        } else {
            String classes = toggle.getAttribute("class");
            enabled = classes != null && classes.contains("ant-switch-checked");
        }

        if (mandatory != enabled) {

            toggle.click();
        }
    }

    // =========================================================
    // SAVE QUESTION
    // =========================================================

    public void clickSaveQuestion() {

        Locator saveButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Save"));

        assertThat(saveButton).isVisible();

        saveButton.click();

        waitForQuestionDrawerToClose();
    }

    // =========================================================
    // SAVE TEMPLATE
    // =========================================================

    public void saveTemplate() {

        fillRequiredNumericFields();

        Locator saveTemplateButton = visibleButtonMatching("save\\s+template|create");

        assertThat(saveTemplateButton).isVisible();

        saveTemplateButton.click();

        verifySuccessMessage();
        waitForTemplateSaveDestination();
    }

    private void waitForTemplateSaveDestination() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 20000) {
            if (isOnCustomTemplateDetailPage()) {
                return;
            }

            Locator templateList = page.locator("tbody tr:visible, table:visible").first();
            if (templateList.count() > 0 && templateList.isVisible()) {
                return;
            }

            page.waitForTimeout(250);
        }
    }

    private boolean isOnCustomTemplateDetailPage() {
        String url = page.url();
        return url.contains("/templates/")
                && !url.endsWith("/create")
                && !url.contains("/create?")
                && (page.getByText("Template Details").count() > 0
                        || page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions().setName(Pattern.compile("^Edit$", Pattern.CASE_INSENSITIVE)))
                                .count() > 0);
    }

    // =========================================================
    // SUCCESS MESSAGE
    // =========================================================

    public void verifySuccessMessage() {

        Locator successToast = page.locator(
                ".ant-message-success:visible, " +
                        ".ant-notification-notice-success:visible, " +
                        ".ant-message-notice:visible:has-text('success'), " +
                        ".ant-notification-notice:visible:has-text('success')");

        try {
            assertThat(successToast.first()).isVisible();
            return;
        } catch (AssertionError ignored) {
            // Some environments remove the toast quickly; the template list is the durable signal.
        }

        Locator templateList = page.locator("tbody tr:visible, table:visible")
                .or(page.getByText("Inspection Template"))
                .first();
        try {
            assertThat(templateList).isVisible();
        } catch (AssertionError ex) {
            throw new AssertionError("Template save did not return to a success/list state. Current URL="
                    + page.url() + ". Page text: " + page.locator("body").innerText(), ex);
        }
    }

    private void fillRequiredNumericFields() {
        Locator numericInputs = page.locator(
                "input[type='number']:visible, " +
                        ".ant-input-number-input:visible");

        int count = numericInputs.count();
        for (int index = 0; index < count; index++) {
            Locator input = numericInputs.nth(index);
            if (!input.isVisible() || !input.isEnabled()) {
                continue;
            }

            String value = safeInputValue(input);
            if (!value.isBlank()) {
                continue;
            }

            input.fill(index == 0 ? "100" : "1");
        }
    }

    // =========================================================
    // VALIDATE TEMPLATE
    // =========================================================

    public void verifyTemplateCreated(
            String templateName) {

        if (isOnCustomTemplateDetailPage()) {
            return;
        }

        long start = System.currentTimeMillis();
        AssertionError lastError = null;

        while (System.currentTimeMillis() - start < 45000) {
            openCustomTemplatesTab();
            resetCustomTemplatePaginationToFirstPage();
            searchTemplate(templateName);

            if (openVisibleTemplateRow(templateName) || openTemplateRowFromPagedList(templateName)) {
                return;
            }

            lastError = new AssertionError("Template row was not visible yet for '" + templateName + "'.");
            refreshCustomTemplatesTab();
        }

        throw new AssertionError("Template row was not visible for '" + templateName
                + "'. Current URL=" + page.url()
                + ". Page text: " + page.locator("body").innerText(), lastError);
    }

    public TemplateEditorComponent openTemplateForEdit(String templateName) {
        if (isOnCustomTemplateDetailPage()) {
            TemplateEditorComponent editor = new TemplateEditorComponent(page);
            editor.clickEditIfNeeded();
            return editor;
        }

        openCustomTemplatesTab();
        resetCustomTemplatePaginationToFirstPage();
        searchTemplate(templateName);
        Locator row = findTemplateRowAcrossPages(templateName);
        row.scrollIntoViewIfNeeded();
        row.hover();

        if (!clickRowActionMenu(row)) {
            row.getByText(templateName).first().click(new Locator.ClickOptions().setForce(true));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            TemplateEditorComponent editor = new TemplateEditorComponent(page);
            editor.clickEditIfNeeded();
            editor.waitForEditor();
            return editor;
        }

        Locator edit = page.getByText(
                Pattern.compile("Edit\\s+Template|^Edit$", Pattern.CASE_INSENSITIVE))
                .last();
        assertThat(edit).isVisible();
        edit.click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        TemplateEditorComponent editor = new TemplateEditorComponent(page);
        editor.waitForEditor();
        return editor;
    }

    public void reopenTemplate(String templateName) {
        openTemplateModule();
        openCustomTemplatesTab();
        resetCustomTemplatePaginationToFirstPage();
        searchTemplate(templateName);
        Locator row = findTemplateRowAcrossPages(templateName);
        row.scrollIntoViewIfNeeded();
        row.getByText(templateName).first().click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void openCustomTemplateList() {
        openCustomTemplatesTab();
    }

    private Locator findTemplateRowAcrossPages(String templateName) {
        AssertionError lastError = null;
        for (int pageIndex = 0; pageIndex < 10; pageIndex++) {
            Locator row = page.locator(".ant-tabs-tabpane-active tbody tr:visible, tbody tr:visible")
                    .filter(new Locator.FilterOptions().setHasText(templateName))
                    .first();
            if (row.count() > 0 && row.isVisible()) {
                return row;
            }

            lastError = new AssertionError("Template row was not visible on page " + (pageIndex + 1)
                    + " for '" + templateName + "'.");
            Locator nextButton = page.locator(
                    ".ant-tabs-tabpane-active .ant-pagination-next:not(.ant-pagination-disabled) button, "
                            + ".ant-pagination-next:not(.ant-pagination-disabled) button")
                    .first();
            if (nextButton.count() == 0 || !nextButton.isVisible() || !nextButton.isEnabled()) {
                break;
            }

            nextButton.scrollIntoViewIfNeeded();
            nextButton.click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.locator("tbody tr:visible").first().waitFor();
        }

        throw new AssertionError("Unable to find template row for '" + templateName
                + "'. Page text: " + page.locator("body").innerText(), lastError);
    }

    private boolean clickRowActionMenu(Locator row) {
        Locator action = row.locator("td").last().getByRole(AriaRole.BUTTON).first();
        if (action.count() == 0 || !action.isVisible()) {
            action = row.locator("button:visible, [role='button']:visible, "
                + "span[aria-label='more']:visible, span[aria-label='ellipsis']:visible, "
                + "span[aria-label='setting']:visible")
                .last();
        }
        if (action.count() == 0 || !action.isVisible()) {
            return false;
        }

        action.click(new Locator.ClickOptions().setForce(true));
        return page.locator(".ant-dropdown:not(.ant-dropdown-hidden):visible, .ant-popover:visible")
                .first()
                .count() > 0;
    }

    private boolean openTemplateRowFromPagedList(String templateName) {
        for (int pageIndex = 0; pageIndex < 10; pageIndex++) {
            if (openVisibleTemplateRow(templateName)) {
                return true;
            }

            Locator nextButton = page.locator(
                    ".ant-tabs-tabpane-active .ant-pagination-next:not(.ant-pagination-disabled) button")
                    .first();
            if (nextButton.count() == 0 || !nextButton.isVisible() || !nextButton.isEnabled()) {
                return false;
            }

            nextButton.click();
            page.waitForTimeout(1000);
        }

        return false;
    }

    private boolean openVisibleTemplateRow(String templateName) {
        Locator templateRow = page.locator(
                ".ant-tabs-tabpane-active tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(templateName))
                .first();

        if (templateRow.count() == 0 || !templateRow.isVisible()) {
            return false;
        }

        templateRow.getByText(templateName).first().click();
        return true;
    }

    public void verifyQuestionAdded(
            String question) {

        Locator questionText = page.getByText(question)
                .or(page.locator("input[value=\"" + cssAttributeValue(question) + "\"], " +
                        "textarea[value=\"" + cssAttributeValue(question) + "\"]"))
                .first();

        if (questionText.count() > 0 && questionText.isVisible()) {
            return;
        }

        Locator inputs = page.locator("input:visible, textarea:visible");
        int count = inputs.count();
        for (int index = 0; index < count; index++) {
            Locator input = inputs.nth(index);
            if (question.equals(safeInputValue(input))) {
                return;
            }
        }

        assertThat(questionText).isVisible();
    }

    private String cssAttributeValue(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    // =========================================================
    // SEARCH TEMPLATE
    // =========================================================

    public void searchTemplate(
            String templateName) {

        Locator searchInput = page.locator(
                ".ant-tabs-tabpane-active input[placeholder*='Search' i], " +
                        "input[placeholder*='Search' i]:visible")
                .first();

        assertThat(searchInput).isVisible();
        searchInput.scrollIntoViewIfNeeded();
        searchInput.click(new Locator.ClickOptions().setForce(true));
        searchInput.fill("");
        searchInput.type(templateName);

        Locator searchIcon = searchInput.locator("xpath=ancestor::*[contains(@class,'ant-input-affix-wrapper')][1]")
                .locator("span[aria-label='search']")
                .first();
        if (searchIcon.count() > 0 && searchIcon.isVisible()) {
            searchIcon.click(new Locator.ClickOptions().setForce(true));
        } else {
            searchInput.press("Enter");
        }

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator(".ant-tabs-tabpane-active table:visible, table:visible").first().waitFor();
    }

    private void openCustomTemplatesTab() {
        Locator customTab = page.getByRole(
                AriaRole.TAB,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("^Custom Templates$", Pattern.CASE_INSENSITIVE)))
                .first();

        assertThat(customTab).isVisible();
        if (!"true".equals(customTab.getAttribute("aria-selected"))) {
            customTab.click();
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            if ("true".equals(customTab.getAttribute("aria-selected"))
                    && page.locator(".ant-tabs-tabpane-active table:visible").count() > 0) {
                return;
            }
            page.waitForTimeout(250);
        }

        throw new AssertionError("Custom Templates tab did not become active. Page text: "
                + page.locator("body").innerText());
    }

    private void resetCustomTemplatePaginationToFirstPage() {
        Locator firstPage = page.locator(
                ".ant-tabs-tabpane-active .ant-pagination-item-1 a, " +
                        ".ant-tabs-tabpane-active .ant-pagination-item-1, " +
                        ".ant-pagination-item-1 a, " +
                        ".ant-pagination-item-1")
                .first();
        if (firstPage.count() == 0 || !firstPage.isVisible()) {
            return;
        }

        String parentClass = firstPage.locator("xpath=ancestor-or-self::li[contains(@class,'ant-pagination-item')][1]")
                .getAttribute("class");
        if (parentClass != null && parentClass.contains("ant-pagination-item-active")) {
            return;
        }

        firstPage.scrollIntoViewIfNeeded();
        firstPage.click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(500);
    }

    private void refreshCustomTemplatesTab() {
        Locator standardTab = page.getByRole(
                AriaRole.TAB,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("^Standard Template$", Pattern.CASE_INSENSITIVE)))
                .first();

        if (standardTab.count() > 0 && standardTab.isVisible()) {
            standardTab.click();
            page.waitForTimeout(500);
        }

        openCustomTemplatesTab();
        page.waitForTimeout(1500);
    }

    // =========================================================
    // DELETE TEMPLATE
    // =========================================================

    public void deleteTemplate(
            String templateName) {

        searchTemplate(templateName);

        Locator row = page.locator("tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(templateName))
                .first();

        assertThat(row).isVisible();

        row.hover();

        Locator actionButton = row.locator("td")
                .last()
                .getByRole(AriaRole.BUTTON)
                .first();

        actionButton.click();

        page.getByText(
                "Delete",
                new Page.GetByTextOptions()
                        .setExact(true))
                .click();

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete"))
                .click();

        verifySuccessMessage();
    }
}
