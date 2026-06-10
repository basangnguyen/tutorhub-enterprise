package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class ClassroomMemberModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private int lessonId;
    private int classroomId;
    private int userId;
    private String fullName;
    private String email;
    private String role;
    private String memberStatus;
    private Timestamp joinedAt;

    public ClassroomMemberModel() {}

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public int getClassroomId() { return classroomId; }
    public void setClassroomId(int classroomId) { this.classroomId = classroomId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }
}
