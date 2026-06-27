package com.mycompany.tutorhub_enterprise.models.exam.readonly;

import java.util.List;
import java.util.ArrayList;

public class TSEV2ReadOnlyQuestionView {
    private int id;
    private String content;
    private int orderIndex;
    private List<TSEV2ReadOnlyOptionView> options = new ArrayList<>();

    public TSEV2ReadOnlyQuestionView() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public List<TSEV2ReadOnlyOptionView> getOptions() { return options; }
    public void setOptions(List<TSEV2ReadOnlyOptionView> options) { this.options = options; }
}
