package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.CalendarEventModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class CalendarEventDAO {

    public boolean insertEvent(CalendarEventModel event) {
        String sql = "INSERT INTO calendar_events (title, start_time, end_time, is_all_day, "
                   + "calendar_type, location, description, guests, tutor_id, status, color, created_by, online_meeting_link, reminder_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, event.getTitle());
            pstmt.setTimestamp(2, Timestamp.valueOf(event.getStartTime()));
            pstmt.setTimestamp(3, Timestamp.valueOf(event.getEndTime()));
            pstmt.setBoolean(4, event.isAllDay());
            pstmt.setString(5, event.getCalendarType());
            pstmt.setString(6, event.getLocation());
            pstmt.setString(7, event.getDescription());
            pstmt.setString(8, event.getGuests() != null ? event.getGuests() : "[]"); // JSON mảng rỗng nếu null
            
            if (event.getTutorId() != null) {
                pstmt.setInt(9, event.getTutorId());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(10, event.getStatus());
            pstmt.setString(11, event.getColor());
            
            if (event.getCreatedBy() != null) {
                pstmt.setInt(12, event.getCreatedBy());
            } else {
                pstmt.setNull(12, java.sql.Types.INTEGER);
            }
            pstmt.setString(13, event.getOnlineMeetingLink());
            pstmt.setInt(14, event.getReminderTime());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public java.util.List<CalendarEventModel> getEventsByTutor(int tutorId) {
        java.util.List<CalendarEventModel> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM calendar_events WHERE tutor_id = ? OR created_by = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tutorId);
            pstmt.setInt(2, tutorId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CalendarEventModel event = new CalendarEventModel();
                event.setEventId(rs.getInt("event_id"));
                event.setTitle(rs.getString("title"));
                event.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                event.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                event.setAllDay(rs.getBoolean("is_all_day"));
                event.setLocation(rs.getString("location"));
                event.setDescription(rs.getString("description"));
                event.setColor(rs.getString("color"));
                // Load additional fields if they exist
                try { event.setOnlineMeetingLink(rs.getString("online_meeting_link")); } catch (Exception ignored) {}
                try { event.setReminderTime(rs.getInt("reminder_time")); } catch (Exception ignored) {}
                try { event.setGuests(rs.getString("guests")); } catch (Exception ignored) {}
                list.add(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateEventTime(int eventId, java.time.LocalTime newStart, java.time.LocalTime newEnd, int targetDBDayOfWeek) throws Exception {
        String sql = "UPDATE calendar_events SET start_time = CAST(CAST(start_time AS DATE) AS TIMESTAMP) + CAST(? AS TIME), end_time = CAST(CAST(end_time AS DATE) AS TIMESTAMP) + CAST(? AS TIME) WHERE event_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTime(1, java.sql.Time.valueOf(newStart));
            pstmt.setTime(2, java.sql.Time.valueOf(newEnd));
            pstmt.setInt(3, eventId);
            pstmt.executeUpdate();
        }
    }

    public boolean deleteEvent(int eventId) {
        String sql = "DELETE FROM calendar_events WHERE event_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}