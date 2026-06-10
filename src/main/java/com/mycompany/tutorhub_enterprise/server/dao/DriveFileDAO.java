package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Data Access Object (DAO) quản lý hệ thống tệp tin và cây thư mục ảo.
 * Hỗ trợ phân tách dữ liệu người dùng, tối ưu cấu trúc phân cấp và Multi-Cloud.
 */
public class DriveFileDAO {

    /**
     * 1. Khởi tạo tệp tin hoặc thư mục mới trong hệ thống
     * @param file Đối tượng chứa siêu dữ liệu tệp tin cần lưu trữ
     * @return true nếu thao tác ghi dữ liệu xuống Neon DB thành công
     */
    public boolean insertFile(DriveFileModel file) {
        String sql = "INSERT INTO drive_files (parent_id, name, file_type, file_size, file_url, owner_id, source_location, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Xử lý đệ quy cây thư mục: Nếu không có thư mục cha -> Đặt về NULL (Thư mục gốc)
            if (file.getParentId() == null || file.getParentId() == 0) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, file.getParentId());
            }
            
            ps.setString(2, file.getName());
            ps.setString(3, file.getFileType());
            ps.setLong(4, file.getFileSize());
            ps.setString(5, file.getFileUrl());
            ps.setInt(6, file.getOwnerId());
            ps.setString(7, file.getSourceLocation()); // Lưu trữ: "MINIO" hoặc "CLOUDINARY"
            ps.setString(8, file.getStatus() != null ? file.getStatus().toUpperCase() : "ACTIVE");
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                // ĐỒNG BỘ NGƯỢC: Lấy ID tự sinh từ Postgres gán lại vào Model cho các tác vụ UI kế tiếp
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        file.setFileId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi nghiêm trọng tại insertFile: " + e.getMessage());
            return false;
        }
    }

    /**
     * 2. Lấy danh sách tệp tin & thư mục con bên trong một thư mục cụ thể
     * Bảo mật tuyệt đối bằng việc lọc chặt chẽ theo OwnerId.
     * * @param ownerId ID của người dùng đang đăng nhập
     * @param parentId ID của thư mục hiện tại (Truyền null hoặc 0 nếu ở thư mục gốc)
     * @return Danh sách các thư mục lên trước, tệp tin theo sau, sắp xếp theo thời gian mới nhất
     */
    public List<DriveFileModel> getFiles(int ownerId, Integer parentId) {
        List<DriveFileModel> list = new ArrayList<>();
        
        // Performance: child_count subquery thay vì N+1 queries
        StringBuilder sql = new StringBuilder(
            "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id AND f2.status = 'ACTIVE') AS child_count " +
            "FROM drive_files f WHERE f.owner_id = ? AND f.status = 'ACTIVE'");
        
        if (parentId == null || parentId == 0) {
            sql.append(" AND f.parent_id IS NULL");
        } else {
            sql.append(" AND f.parent_id = ?");
        }
        
        sql.append(" ORDER BY (f.file_type = 'folder') DESC, f.created_at DESC");
        
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            
            ps.setInt(1, ownerId);
            if (parentId != null && parentId != 0) {
                ps.setInt(2, parentId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractModel(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi truy vấn danh sách getFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * Tải danh sách tệp được chia sẻ với người dùng (Giai đoạn 10)
     */
    public List<DriveFileModel> getSharedFiles(int currentUserId) {
        List<DriveFileModel> list = new ArrayList<>();
        String sql = "SELECT df.* FROM drive_files df " +
                     "JOIN drive_file_shares dfs ON df.file_id = dfs.file_id " +
                     "WHERE dfs.shared_with_user_id = ? AND df.status = 'active'";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractModel(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getSharedFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * Thêm quyền chia sẻ file (Giai đoạn 10)
     */
    public boolean shareFile(int fileId, int targetUserId, String permission) {
        String sql = "INSERT INTO drive_file_shares (file_id, shared_with_user_id, permission) " +
                     "VALUES (?, ?, ?) ON CONFLICT (file_id, shared_with_user_id) " +
                     "DO UPDATE SET permission = EXCLUDED.permission, created_at = CURRENT_TIMESTAMP";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setInt(2, targetUserId);
            ps.setString(3, permission);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi shareFile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thu hồi quyền chia sẻ (Giai đoạn 10)
     */
    public boolean removeShare(int fileId, int targetUserId) {
        String sql = "DELETE FROM drive_file_shares WHERE file_id = ? AND shared_with_user_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setInt(2, targetUserId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi removeShare: " + e.getMessage());
            return false;
        }
    }

    /**
     * Giai đoạn 11: Thùng rác thông minh
     */
    public void cleanupTrash() {
        String sql = "DELETE FROM drive_files WHERE status = 'trashed' AND updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days'";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int deletedCount = ps.executeUpdate();
            if (deletedCount > 0) {
                System.out.println("♻️ Đã dọn dẹp " + deletedCount + " file quá hạn trong Thùng rác.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cleanupTrash: " + e.getMessage());
        }
    }

    /**
     * 3. Tìm kiếm thông tin chi tiết của một tệp tin thông qua ID độc nhất
     */
    public DriveFileModel getFileById(int fileId) {
        String sql = "SELECT * FROM drive_files WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractModel(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getFileById: " + e.getMessage());
        }
        return null;
    }

    /**
     * 4. Đổi tên cấu trúc hiển thị của Tệp tin hoặc Thư mục
     */
    public boolean renameFile(int fileId, String newName) {
        String sql = "UPDATE drive_files SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, newName);
            ps.setInt(2, fileId);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi thực thi renameFile: " + e.getMessage());
            return false;
        }
    }

    /**
     * 5. Di chuyển phần tử vào thùng rác (Cơ chế Xóa mềm - Soft Delete)
     * Đảm bảo an toàn dữ liệu, cho phép người dùng khôi phục lại khi cần.
     */
    public boolean moveToTrash(int fileId) {
        String sql = "UPDATE drive_files SET status = 'TRASHED', updated_at = CURRENT_TIMESTAMP WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, fileId);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi chuyển trạng thái tại moveToTrash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lớp Trợ năng (Helper): Ánh xạ dòng dữ liệu từ ResultSet của Database sang Đối tượng Java Object.
     */
    private DriveFileModel extractModel(ResultSet rs) throws SQLException {
        DriveFileModel model = new DriveFileModel();
        model.setFileId(rs.getInt("file_id"));
        
        // Kiểm tra tính toàn vẹn của dữ liệu NULL trên PostgreSQL trường parent_id
        int parentId = rs.getInt("parent_id");
        model.setParentId(rs.wasNull() ? null : parentId);
        
        model.setName(rs.getString("name"));
        model.setFileType(rs.getString("file_type"));
        model.setFileSize(rs.getLong("file_size"));
        model.setFileUrl(rs.getString("file_url"));
        model.setOwnerId(rs.getInt("owner_id"));
        model.setSourceLocation(rs.getString("source_location"));
        model.setStatus(rs.getString("status"));
        model.setCreatedAt(rs.getTimestamp("created_at"));
        model.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        // Performance: Đọc child_count nếu có trong ResultSet (tránh crash nếu query không có)
        try { model.setChildCount(rs.getInt("child_count")); } catch (SQLException ignored) {}
        
        return model;
    }

    /**
     * 6. Lấy danh sách file gần đây (theo thời gian cập nhật), không phân biệt thư mục
     */
    public List<DriveFileModel> getRecentFiles(int ownerId) {
        List<DriveFileModel> list = new ArrayList<>();
        String sql = "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id AND f2.status = 'ACTIVE') AS child_count " +
                     "FROM drive_files f WHERE f.owner_id = ? AND f.status = 'ACTIVE' ORDER BY f.updated_at DESC NULLS LAST, f.created_at DESC LIMIT 30";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getRecentFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * 7. Lấy danh sách file đã xóa (thùng rác)
     */
    public List<DriveFileModel> getTrashedFiles(int ownerId) {
        List<DriveFileModel> list = new ArrayList<>();
        String sql = "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id) AS child_count " +
                     "FROM drive_files f WHERE f.owner_id = ? AND f.status = 'TRASHED' ORDER BY f.updated_at DESC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getTrashedFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * 8. Khôi phục file từ thùng rác về trạng thái ACTIVE
     */
    public boolean restoreFromTrash(int fileId) {
        String sql = "UPDATE drive_files SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi restoreFromTrash: " + e.getMessage());
            return false;
        }
    }

    /**
     * 9. Xóa vĩnh viễn file khỏi database
     */
    public boolean permanentDelete(int fileId) {
        String sql = "DELETE FROM drive_files WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi permanentDelete: " + e.getMessage());
            return false;
        }
    }

    /**
     * 10. Tìm kiếm file theo tên trên toàn bộ Drive (không giới hạn thư mục)
     */
    public List<DriveFileModel> searchFiles(int ownerId, String keyword) {
        List<DriveFileModel> list = new ArrayList<>();
        String sql = "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id AND f2.status = 'ACTIVE') AS child_count " +
                     "FROM drive_files f WHERE f.owner_id = ? AND f.status = 'ACTIVE' AND LOWER(f.name) LIKE ? ORDER BY (f.file_type = 'folder') DESC, f.updated_at DESC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ps.setString(2, "%" + keyword.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi searchFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * 11. Lấy danh sách file có hỗ trợ lọc theo loại và sắp xếp
     */
    public List<DriveFileModel> getFilesFiltered(int ownerId, Integer parentId, String typeFilter, String sortMode) {
        List<DriveFileModel> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id AND f2.status = 'ACTIVE') AS child_count " +
            "FROM drive_files f WHERE f.owner_id = ? AND f.status = 'ACTIVE'");

        if (parentId == null || parentId == 0) {
            sql.append(" AND parent_id IS NULL");
        } else {
            sql.append(" AND parent_id = ?");
        }

        // Filter theo loại file
        if (typeFilter != null && !typeFilter.isEmpty()) {
            sql.append(" AND LOWER(file_type) = ?");
        }

        // Sắp xếp
        sql.append(" ORDER BY (file_type = 'folder') DESC, ");
        if ("oldest".equals(sortMode)) {
            sql.append("created_at ASC");
        } else if ("name_asc".equals(sortMode)) {
            sql.append("LOWER(name) ASC");
        } else if ("size_desc".equals(sortMode)) {
            sql.append("file_size DESC");
        } else {
            sql.append("created_at DESC"); // Mặc định: mới nhất
        }

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, ownerId);
            if (parentId != null && parentId != 0) {
                ps.setInt(idx++, parentId);
            }
            if (typeFilter != null && !typeFilter.isEmpty()) {
                ps.setString(idx++, typeFilter.toLowerCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getFilesFiltered: " + e.getMessage());
        }
        return list;
    }

    /**
     * 12. Gắn/bỏ gắn dấu sao cho file
     */
    public boolean toggleStar(int fileId, int userId) {
        boolean currentlyStarred = isStarred(fileId, userId);
        String sql;
        if (currentlyStarred) {
            sql = "DELETE FROM drive_file_stars WHERE file_id = ? AND user_id = ?";
        } else {
            sql = "INSERT INTO drive_file_stars (file_id, user_id) VALUES (?, ?)";
        }
        
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi toggleStar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra xem file đã được gắn sao chưa
     */
    public boolean isStarred(int fileId, int userId) {
        String sql = "SELECT 1 FROM drive_file_stars WHERE file_id = ? AND user_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi isStarred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy danh sách các file đã gắn sao của một user
     */
    public List<DriveFileModel> getStarredFiles(int userId) {
        List<DriveFileModel> list = new ArrayList<>();
        String sql = "SELECT f.*, (SELECT COUNT(*) FROM drive_files f2 WHERE f2.parent_id = f.file_id AND f2.status = 'ACTIVE') AS child_count " +
                     "FROM drive_files f JOIN drive_file_stars s ON f.file_id = s.file_id " +
                     "WHERE s.user_id = ? AND f.status = 'ACTIVE' " +
                     "ORDER BY (f.file_type = 'folder') DESC, f.updated_at DESC";
        
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getStarredFiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * Performance: Lấy tập hợp file_id đã gắn sao (1 query duy nhất thay vì N queries)
     */
    public java.util.Set<Integer> getStarredFileIds(int userId) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String sql = "SELECT file_id FROM drive_file_stars WHERE user_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("file_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getStarredFileIds: " + e.getMessage());
        }
        return ids;
    }

    /**
     * 13. Cập nhật file với phiên bản mới (Lưu lịch sử)
     */
    public boolean updateFileVersion(int fileId, String newUrl, String newSourceLoc, long newSize) {
        // Lấy thông tin file hiện tại để backup
        DriveFileModel oldFile = getFileById(fileId);
        if (oldFile == null) return false;

        String insertVerSql = "INSERT INTO drive_file_versions (file_id, file_url, source_location, version_number) " +
                              "VALUES (?, ?, ?, (SELECT COALESCE(MAX(version_number), 0) + 1 FROM drive_file_versions WHERE file_id = ?))";
        
        String updateFileSql = "UPDATE drive_files SET file_url = ?, source_location = ?, file_size = ?, updated_at = CURRENT_TIMESTAMP WHERE file_id = ?";

        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement psVer = con.prepareStatement(insertVerSql);
                 PreparedStatement psUpd = con.prepareStatement(updateFileSql)) {
                
                // Backup version cũ
                psVer.setInt(1, fileId);
                psVer.setString(2, oldFile.getFileUrl());
                psVer.setString(3, oldFile.getSourceLocation());
                psVer.setInt(4, fileId);
                psVer.executeUpdate();

                // Cập nhật version mới
                psUpd.setString(1, newUrl);
                psUpd.setString(2, newSourceLoc);
                psUpd.setLong(3, newSize);
                psUpd.setInt(4, fileId);
                psUpd.executeUpdate();
                
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                System.err.println("❌ Lỗi updateFileVersion: " + ex.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 14. Lấy tổng dung lượng đã sử dụng của user
     */
    public long getUsedStorage(int userId) {
        String sql = "SELECT SUM(file_size) FROM drive_files WHERE owner_id = ? AND status = 'ACTIVE'";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getUsedStorage: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 15. Di chuyển file/thư mục (Cut / Paste)
     */
    public boolean moveFile(int fileId, Integer newParentId) {
        String sql = "UPDATE drive_files SET parent_id = ?, updated_at = CURRENT_TIMESTAMP WHERE file_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (newParentId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, newParentId);
            }
            ps.setInt(2, fileId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi moveFile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy danh sách lịch sử phiên bản của một file
     */
    public List<com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel> getFileVersions(int fileId) {
        List<com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel> list = new ArrayList<>();
        String sql = "SELECT * FROM drive_file_versions WHERE file_id = ? ORDER BY version_number DESC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel v = new com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel();
                    v.setVersionId(rs.getInt("version_id"));
                    v.setFileId(rs.getInt("file_id"));
                    v.setFileUrl(rs.getString("file_url"));
                    v.setSourceLocation(rs.getString("source_location"));
                    v.setVersionNumber(rs.getInt("version_number"));
                    v.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(v);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getFileVersions: " + e.getMessage());
        }
        return list;
    }
}