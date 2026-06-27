package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExamPackagePreview implements Serializable {
    private static final long serialVersionUID = 1L;

    public String packageVersion = "tutorhub_exam_package_preview_v1";
    public int examId;
    public Integer paperId;
    public String examTitle;
    public String paperTitle;
    public int durationMinutes;
    public int questionCount;
    public double totalScore;
    
    public List<ExamPackageQuestion> questions = new ArrayList<>();
    
    public ExamPackagePreview() {}
}
