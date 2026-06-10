package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class QuestionBankCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int creatorId;
    public String name;
    public Integer parentId;
    public String description;
    public Timestamp createdAt;

    public QuestionBankCategory() {}
}
