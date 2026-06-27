package com.mycompany.tutorhub_enterprise.models.exam;

// SECURITY NOTE:
// PracticeOptionViewDTO intentionally contains isCorrect for practice feedback only.
// Do not reuse this DTO for exam/test/secure exam flows before submission.
public class PracticeOptionViewDTO {
    public int optionId;
    public String label;
    public String text;
    public boolean isCorrect;
}
