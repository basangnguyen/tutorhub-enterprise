package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class AntiCheatEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public long id;
    public int sessionId;
    public String eventType;
    public String severity;
    
    public String details; // JSON string
    public String evidenceUrl;
    public float trustScore;
    public Timestamp timestamp;

    public AntiCheatEvent() {}
}
