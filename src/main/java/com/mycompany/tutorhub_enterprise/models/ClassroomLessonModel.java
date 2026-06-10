package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class ClassroomLessonModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int classroomId;
    private String classroomName;
    private String organizationName;
    private String joinCode;
    private String title;
    private Timestamp startTime;
    private int durationMinutes;
    private int seatCount;
    private String status;
    private String boardId;
    private String lessonType;
    private String stageLayout;
    private boolean lobbyEnabled;
    private boolean allowStudentDraw;
    private boolean recordingEnabled;
    private int createdBy;
    private String memberStatus;
    private Timestamp createdAt;
    
    public ClassroomLessonModel() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClassroomId() { return classroomId; }
    public void setClassroomId(int classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }

    public String getStageLayout() { return stageLayout; }
    public void setStageLayout(String stageLayout) { this.stageLayout = stageLayout; }

    public boolean isLobbyEnabled() { return lobbyEnabled; }
    public void setLobbyEnabled(boolean lobbyEnabled) { this.lobbyEnabled = lobbyEnabled; }

    public boolean isAllowStudentDraw() { return allowStudentDraw; }
    public void setAllowStudentDraw(boolean allowStudentDraw) { this.allowStudentDraw = allowStudentDraw; }

    public boolean isRecordingEnabled() { return recordingEnabled; }
    public void setRecordingEnabled(boolean recordingEnabled) { this.recordingEnabled = recordingEnabled; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
