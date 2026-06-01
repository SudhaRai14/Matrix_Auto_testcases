package tests;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.EditTemplateData;
import models.QuestionData;
import models.TemplateData;
import pages.TemplateEditorComponent;
import pages.TemplatePage;
import utils.BaseTest;

public class EditTemplateTest extends BaseTest {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test(dataProvider = "editTemplateData")
    public void shouldEditTemplate(EditTemplateData data) {
        loginWithValidCredentials();

        TemplatePage templatePage = new TemplatePage(page);
        templatePage.openTemplateModule();

        String stamp = STAMP.format(LocalDateTime.now());
        String templateName = prepareTemplate(templatePage, data, stamp);
        String editedTemplateName = resolve(firstNonBlank(data.newTemplateName, data.expectedTemplateName, templateName), stamp);

        TemplateEditorComponent editor = templatePage.openTemplateForEdit(templateName);
        editor.applyScenario(resolveScenario(data, stamp));

        if (data.cancelEdit) {
            editor.cancelChanges();
            templatePage.reopenTemplate(templateName);
            TemplateEditorComponent verifier = new TemplateEditorComponent(page);
            verifier.verifyTemplateFields(templateName, data.seedTemplate == null ? null : data.seedTemplate.description,
                    null, null);
            if (data.questionToAdd != null) {
                verifier.questions().verifyQuestionAbsent(resolve(data.questionToAdd.question, stamp));
            }
            return;
        }

        editor.saveChanges();
        templatePage.reopenTemplate(editedTemplateName);

        TemplateEditorComponent verifier = new TemplateEditorComponent(page);
        verifier.verifyTemplateFields(
                resolve(firstNonBlank(data.expectedTemplateName, data.newTemplateName), stamp),
                resolve(data.expectedDescription, stamp),
                data.expectedTargetScore,
                data.expectedCategory);
        verifier.verifySectionTitles(resolve(data.sectionTitle, stamp), resolve(data.subSectionTitle, stamp));

        if (data.expectedQuestions != null) {
            for (String question : data.expectedQuestions) {
                verifier.questions().verifyQuestionPresent(resolve(question, stamp));
            }
        }
        if (data.absentQuestions != null) {
            for (String question : data.absentQuestions) {
                verifier.questions().verifyQuestionAbsent(resolve(question, stamp));
            }
        }
    }

    private String prepareTemplate(TemplatePage templatePage, EditTemplateData data, String stamp) {
        if (data.seedTemplate == null) {
            throw new IllegalArgumentException("EditTemplateTest requires seedTemplate so it can create the template before editing it.");
        }

        TemplateData seed = resolveTemplate(data.seedTemplate, stamp);
        CreateTemplateTest.createTemplate(templatePage, seed, seed.templateName);
        verifyCreatedSeedTemplate(templatePage, seed);
        return seed.templateName;
    }

    private void verifyCreatedSeedTemplate(TemplatePage templatePage, TemplateData seed) {
        TemplateEditorComponent editor = templatePage.openTemplateForEdit(seed.templateName);
        editor.verifyTemplateFields(seed.templateName, seed.description, null, null);
        for (QuestionData question : seed.questions) {
            editor.questions().verifyQuestionPresent(question.question);
        }
    }

    private EditTemplateData resolveScenario(EditTemplateData data, String stamp) {
        data.templateName = resolve(data.templateName, stamp);
        data.newTemplateName = resolve(data.newTemplateName, stamp);
        data.newDescription = resolve(data.newDescription, stamp);
        data.newCategory = resolve(data.newCategory, stamp);
        data.sectionTitle = resolve(data.sectionTitle, stamp);
        data.subSectionTitle = resolve(data.subSectionTitle, stamp);
        data.expectedTemplateName = resolve(data.expectedTemplateName, stamp);
        data.expectedDescription = resolve(data.expectedDescription, stamp);
        data.expectedCategory = resolve(data.expectedCategory, stamp);

        if (data.questionTextEdit != null) {
            data.questionTextEdit.existingQuestion = resolve(data.questionTextEdit.existingQuestion, stamp);
            data.questionTextEdit.newQuestion = resolve(data.questionTextEdit.newQuestion, stamp);
        }
        if (data.answerTypeEdit != null) {
            data.answerTypeEdit.question = resolve(data.answerTypeEdit.question, stamp);
        }
        if (data.questionToAdd != null) {
            data.questionToAdd.question = resolve(data.questionToAdd.question, stamp);
        }
        if (data.questionToDelete != null) {
            data.questionToDelete = resolve(data.questionToDelete, stamp);
        }
        if (data.mcqOptionsEdit != null) {
            data.mcqOptionsEdit.question = resolve(data.mcqOptionsEdit.question, stamp);
        }
        if (data.checkboxOptionsEdit != null) {
            data.checkboxOptionsEdit.question = resolve(data.checkboxOptionsEdit.question, stamp);
        }
        if (data.sliderEdit != null) {
            data.sliderEdit.question = resolve(data.sliderEdit.question, stamp);
        }
        if (data.mandatoryEdit != null) {
            data.mandatoryEdit.question = resolve(data.mandatoryEdit.question, stamp);
        }
        if (data.scoringEdit != null) {
            data.scoringEdit.question = resolve(data.scoringEdit.question, stamp);
        }
        if (data.reorderEdit != null) {
            data.reorderEdit.sourceQuestion = resolve(data.reorderEdit.sourceQuestion, stamp);
            data.reorderEdit.targetQuestion = resolve(data.reorderEdit.targetQuestion, stamp);
        }
        return data;
    }

    private TemplateData resolveTemplate(TemplateData template, String stamp) {
        template.templateName = resolve(template.templateName, stamp);
        template.description = resolve(template.description, stamp);
        if (template.questions != null) {
            for (QuestionData question : template.questions) {
                question.question = resolve(question.question, stamp);
            }
        }
        return template;
    }

    private String resolve(String value, String stamp) {
        return value == null ? null : value.replace("${stamp}", stamp);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @DataProvider(name = "editTemplateData")
    public Object[][] editTemplateData() {
        List<EditTemplateData> scenarios = loadEditTemplateData();
        return scenarios.stream()
                .map(scenario -> new Object[] { scenario })
                .toArray(Object[][]::new);
    }

    private List<EditTemplateData> loadEditTemplateData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = EditTemplateTest.class.getClassLoader()
                    .getResourceAsStream("testdata/editTemplateData.json");
            if (is == null) {
                is = EditTemplateTest.class.getClassLoader().getResourceAsStream("editTemplateData.json");
            }
            if (is == null) {
                throw new IllegalStateException("Unable to find editTemplateData.json");
            }
            return Arrays.asList(mapper.readValue(is, EditTemplateData[].class));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load edit template data", ex);
        }
    }
}
