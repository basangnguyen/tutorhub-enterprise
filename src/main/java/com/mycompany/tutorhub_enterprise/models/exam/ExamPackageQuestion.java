package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExamPackageQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    public int questionId;
    public String type;
    public String content;
    public double score;
    public int orderIndex;
    public boolean required;
    
    public List<ExamPackageOption> options = new ArrayList<>();
    
    public ExamPackageQuestion() {}
}
