package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.CalendarTaskModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class CalendarTaskDAO {

    public boolean insertTask(CalendarTaskModel task) {
        String sql = "INSERT INTO calendar_tasks (title, deadline, description, task_list, assigned_to, status, priority) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            
            if (task.getDeadline() != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(task.getDeadline()));
            } else {
                pstmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            
            pstmt.setString(3, task.getDescription());
            pstmt.setString(4, task.getTaskList());
            
            if (task.getAssignedTo() != null) {
                pstmt.setInt(5, task.getAssignedTo());
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(6, task.getStatus());
            pstmt.setString(7, task.getPriority());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public java.util.List<CalendarTaskModel> getTasksByTutor(int tutorId) {
        java.util.List<CalendarTaskModel> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM calendar_tasks WHERE assigned_to = ? OR created_by = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tutorId);
            pstmt.setInt(2, tutorId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CalendarTaskModel task = new CalendarTaskModel();
                task.setTaskId(rs.getInt("task_id"));
                task.setTitle(rs.getString("title"));
                if (rs.getTimestamp("deadline") != null) {
                    task.setDeadline(rs.getTimestamp("deadline").toLocalDateTime());
                }
                task.setDescription(rs.getString("description"));
                task.setTaskList(rs.getString("task_list"));
                task.setStatus(rs.getString("status"));
                list.add(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}