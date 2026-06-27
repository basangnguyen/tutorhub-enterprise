package com.mycompany.tutorhub_enterprise.models.exam;

import java.util.List;

public class ExamQuestionViewDTO {
    public int questionId;
    public String question;
    public String questionType;
    public List<ExamOptionViewDTO> options;
}
