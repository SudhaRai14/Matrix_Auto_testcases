package tests;

import models.QuestionData;
import models.TemplateData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import pages.TemplatePage;
import utils.BaseTest;
import utils.TemplateDataLoader;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CreateTemplateTest extends BaseTest {
    private static final DateTimeFormatter TEMPLATE_NAME_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test(dataProvider = "templateData")
    public void shouldCreateTemplateQuestion(TemplateData templateData) {
        loginWithValidCredentials();
        String templateName = templateData.templateName + " " + TEMPLATE_NAME_STAMP.format(LocalDateTime.now());

        TemplatePage templatePage = new TemplatePage(page);
        templatePage.openTemplateModule();
        templatePage.clickCreateTemplate();
        System.out.println("Creating template with name: " + templateName);
        templatePage.enterTemplateName(templateName);
        templatePage.enterTemplateDescription(templateData.description);
        templatePage.selectAnyCategory();
        System.out.println("Configuring questions: " + templateData.questions.size());
        templatePage.configureInlineQuestions(templateData.questions);
        for (QuestionData question : templateData.questions) {
            templatePage.verifyQuestionAdded(question.question);
        }
        
        System.out.println(page.locator("body").innerText());
        page.screenshot(
            new Page.ScreenshotOptions()
        .setPath(Paths.get(
            "before-save-template.png")));
        templatePage.saveTemplate();
        System.out.println("Template saved.");
        Locator validationError = page.locator(
        ".ant-form-item-explain-error:visible");

        if (validationError.count() > 0) {

                throw new IllegalStateException("Template validation failed: "+ validationError.first().innerText());
        }
    }

    @DataProvider(name = "templateData")
    public Object[][] templateData() {
        List<TemplateData> templates = TemplateDataLoader.loadTemplateData();
        return templates.stream()
                .map(template -> new Object[]{template})
                .toArray(Object[][]::new);
    }
}
