package models;

import java.util.List;

public class EditTemplateData {
    public String scenarioName;
    public boolean createTemplateBeforeEdit;
    public boolean cancelEdit;

    public TemplateData seedTemplate;

    public String templateName;
    public String newTemplateName;
    public String newDescription;
    public Integer newTargetScore;
    public String newCategory;

    public String sectionTitle;
    public String subSectionTitle;

    public QuestionTextEdit questionTextEdit;
    public AnswerTypeEdit answerTypeEdit;
    public QuestionData questionToAdd;
    public String questionToDelete;
    public OptionsEdit mcqOptionsEdit;
    public OptionsEdit checkboxOptionsEdit;
    public SliderEdit sliderEdit;
    public MandatoryEdit mandatoryEdit;
    public ScoringEdit scoringEdit;
    public ReorderEdit reorderEdit;

    public String expectedTemplateName;
    public String expectedDescription;
    public Integer expectedTargetScore;
    public String expectedCategory;
    public List<String> expectedQuestions;
    public List<String> absentQuestions;

    public static class QuestionTextEdit {
        public String existingQuestion;
        public String newQuestion;
    }

    public static class AnswerTypeEdit {
        public String question;
        public String newQuestionType;
    }

    public static class OptionsEdit {
        public String question;
        public List<String> options;
    }

    public static class SliderEdit {
        public String question;
        public Integer min;
        public Integer max;
    }

    public static class MandatoryEdit {
        public String question;
        public boolean mandatory;
    }

    public static class ScoringEdit {
        public String question;
        public boolean enabled;
        public Integer firstScore;
    }

    public static class ReorderEdit {
        public String sourceQuestion;
        public String targetQuestion;
    }
}
