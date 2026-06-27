package com.mycompany.tutorhub_enterprise.models.exam;

import java.util.List;

// SECURITY NOTE:
// PracticeQuestionViewDTO intentionally contains correctAnswer and options with isCorrect for practice feedback only.
// Do not reuse this DTO for exam/test/secure exam flows before submission.
public class PracticeQuestionViewDTO {
    public int questionId;
    public String question;
    public String questionType;
    public String explanation;
    public String correctAnswer;
    public List<PracticeOptionViewDTO> options;
}
