package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Enterprise Data Transfer Object (DTO) cho hệ thống TutorHub Drive
 * Hỗ trợ cấu trúc cây thư mục (parentId) và lưu trữ Multi-Cloud.
 */
public class DriveFileModel implements Serializable {
    
    // Đảm bảo tính toàn vẹn khi truyền object qua mạng (Socket)
    private static final long serialVersionUID = 1L;

    private int fileId;
    private Integer parentId; // Dùng Integer để có thể lưu giá trị null (thư mục gốc)
    private String name;
    private String fileType;  // VD: "folder", "pdf", "mp4"
    private long fileSize;
    private String fileUrl;
    private int ownerId;
    private String sourceLocation; // Phân loại hạ tầng: "MINIO", "CLOUDINARY"
    private String status;         // VD: "ACTIVE", "TRASHED"
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int childCount; // Số mục con (dùng cho folder, tránh N+1 query)

    // Default Constructor bắt buộc phải có cho các Framework Serialize
    public DriveFileModel() {}

    // Constructor đầy đủ tham số (Không bao gồm Timestamp vì DB tự sinh)
    public DriveFileModel(int fileId, Integer parentId, String name, String fileType, 
                          long fileSize, String fileUrl, int ownerId, String sourceLocation, String status) {
        this.fileId = fileId;
        this.parentId = parentId;
        this.name = name;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.ownerId = ownerId;
        this.sourceLocation = sourceLocation;
        this.status = status;
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================

    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }

    /**
     * Alias method: Giúp tương thích ngược với các file DAO cũ đang gọi setId().
     * Sửa lỗi UnsupportedOperationException của NetBeans.
     */
    public void setId(int id) { 
        this.fileId = id; 
    }
    // Tương tự, thêm getId() để đồng bộ
    public int getId() {
        return this.fileId;
    }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }
}