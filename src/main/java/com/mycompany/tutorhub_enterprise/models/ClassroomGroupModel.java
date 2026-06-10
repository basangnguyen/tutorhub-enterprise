package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class ClassroomGroupModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int ownerId;
    private String name;
    private String coverImage;
    private String description;
    private String organizationName;
    private String joinCode;
    private String status;
    private Timestamp createdAt;

    public ClassroomGroupModel() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
