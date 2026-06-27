package com.mycompany.tutorhub_enterprise.models.exam;

import java.util.List;

public class V2ExamHandoffQuestion {
    public int questionId;
    public String type;
    public String content;
    public float score;
    public int orderIndex;
    public boolean required;
    public List<V2ExamHandoffOption> options;
}
