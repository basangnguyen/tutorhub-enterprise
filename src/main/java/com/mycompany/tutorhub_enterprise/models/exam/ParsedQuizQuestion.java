package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO representing a single quiz question parsed from an HTML file
 * containing a quizData JavaScript array.
 *
 * <p>This class is a pure data holder — it does not execute any JavaScript
 * and stores only sanitised text extracted by {@code HtmlQuizDataParser}.</p>
 */
public class ParsedQuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The question text, e.g. "1. Chip vi điều khiển 8051..." */
    private String question;

    /**
     * Answer options keyed by their original label (a, b, c, d).
     * Insertion order is preserved via {@link LinkedHashMap}.
     */
    private Map<String, String> answers;

    /** The key (a/b/c/d) of the correct answer. */
    private String correctAnswer;

    /** Optional explanation shown after answering. May be null or empty. */
    private String explanation;

    /** 0-based index in the source quizData array. */
    private int sourceIndex;

    /** Validation error message, null if this question is valid. */
    private String validationError;

    public ParsedQuizQuestion() {
        this.answers = new LinkedHashMap<>();
    }

    // --- Getters ---

    public String getQuestion() { return question; }
    public Map<String, String> getAnswers() { return answers; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public int getSourceIndex() { return sourceIndex; }
    public String getValidationError() { return validationError; }

    // --- Setters ---

    public void setQuestion(String question) { this.question = question; }
    public void setAnswers(Map<String, String> answers) { this.answers = answers; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setSourceIndex(int sourceIndex) { this.sourceIndex = sourceIndex; }
    public void setValidationError(String validationError) { this.validationError = validationError; }

    // --- Validation ---

    /** @return true if this question has valid structure for import */
    public boolean isValid() {
        return validationError == null;
    }

    /**
     * Validate this question and set {@code validationError} if invalid.
     * @return this instance for chaining
     */
    public ParsedQuizQuestion validate() {
        if (question == null || question.trim().isEmpty()) {
            validationError = "Missing question text at index " + sourceIndex;
            return this;
        }
        if (answers == null || answers.size() < 2) {
            validationError = "Less than 2 answer options at index " + sourceIndex;
            return this;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            validationError = "Missing correctAnswer at index " + sourceIndex;
            return this;
        }
        if (!answers.containsKey(correctAnswer.trim().toLowerCase())) {
            validationError = "correctAnswer '" + correctAnswer + "' not found in answer keys " + answers.keySet() + " at index " + sourceIndex;
            return this;
        }
        // explanation is optional — no validation needed
        validationError = null;
        return this;
    }

    @Override
    public String toString() {
        return "ParsedQuizQuestion{index=" + sourceIndex
                + ", question='" + (question != null && question.length() > 60 ? question.substring(0, 60) + "..." : question) + "'"
                + ", answers=" + (answers != null ? answers.keySet() : "null")
                + ", correctAnswer='" + correctAnswer + "'"
                + ", valid=" + isValid()
                + "}";
    }
}
