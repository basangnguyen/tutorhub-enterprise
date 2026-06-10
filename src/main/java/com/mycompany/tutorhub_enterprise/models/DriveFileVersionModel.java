package com.mycompany.tutorhub_enterprise.models;

import java.sql.Timestamp;

public class DriveFileVersionModel {
    private int versionId;
    private int fileId;
    private String fileUrl;
    private String sourceLocation;
    private int versionNumber;
    private Timestamp createdAt;

    public int getVersionId() { return versionId; }
    public void setVersionId(int versionId) { this.versionId = versionId; }

    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    
    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
