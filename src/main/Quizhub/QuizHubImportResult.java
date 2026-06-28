// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/importer/QuizHubImportResult.java
package com.mycompany.tutorhub_enterprise.client.quizhub.importer;

import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubQuestion;

import java.util.ArrayList;
import java.util.List;

/** Kết quả của bước parse() — CHƯA lưu gì cả, chỉ đọc + validate. */
public class QuizHubImportResult {

    private String sourceFileName;

    private String deckTitleDraft;
    private String deckDescriptionDraft;
    private String deckSubjectDraft;
    private String deckColorDraft;
    private boolean shuffleQuestionsDefault = true;
    private boolean showExplanationImmediatelyDefault = true;

    private List<QuizHubQuestion> validQuestions = new ArrayList<>();
    private List<QuizHubRowError> errors = new ArrayList<>();

    public QuizHubImportResult() {
    }

    public int getValidCount() { return validQuestions == null ? 0 : validQuestions.size(); }
    public int getErrorCount() { return errors == null ? 0 : errors.size(); }
    public boolean hasErrors() { return getErrorCount() > 0; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getDeckTitleDraft() { return deckTitleDraft; }
    public void setDeckTitleDraft(String deckTitleDraft) { this.deckTitleDraft = deckTitleDraft; }

    public String getDeckDescriptionDraft() { return deckDescriptionDraft; }
    public void setDeckDescriptionDraft(String deckDescriptionDraft) { this.deckDescriptionDraft = deckDescriptionDraft; }

    public String getDeckSubjectDraft() { return deckSubjectDraft; }
    public void setDeckSubjectDraft(String deckSubjectDraft) { this.deckSubjectDraft = deckSubjectDraft; }

    public String getDeckColorDraft() { return deckColorDraft; }
    public void setDeckColorDraft(String deckColorDraft) { this.deckColorDraft = deckColorDraft; }

    public boolean isShuffleQuestionsDefault() { return shuffleQuestionsDefault; }
    public void setShuffleQuestionsDefault(boolean shuffleQuestionsDefault) { this.shuffleQuestionsDefault = shuffleQuestionsDefault; }

    public boolean isShowExplanationImmediatelyDefault() { return showExplanationImmediatelyDefault; }
    public void setShowExplanationImmediatelyDefault(boolean v) { this.showExplanationImmediatelyDefault = v; }

    public List<QuizHubQuestion> getValidQuestions() { return validQuestions; }
    public void setValidQuestions(List<QuizHubQuestion> validQuestions) { this.validQuestions = validQuestions; }

    public List<QuizHubRowError> getErrors() { return errors; }
    public void setErrors(List<QuizHubRowError> errors) { this.errors = errors; }
}