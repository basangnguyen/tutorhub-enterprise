package com.mycompany.tutorhub_enterprise.models;

import java.time.LocalDateTime;

public class CalendarEventModel {
    private int eventId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isAllDay;
    private String repeatType;
    private String calendarType;
    private String location;
    private String onlineMeetingLink;
    private String description;
    private String attachments; // Chuỗi JSON
    private String guests;      // Chuỗi JSON
    private Integer tutorId;
    private Integer studentId;
    private Integer parentId;
    private Integer classId;
    private String status;
    private String visibility;
    private int reminderTime;
    private String color;
    private Integer createdBy;

    // Constructors
    public CalendarEventModel() {}

    // Getters and Setters (Bạn dùng Alt+Insert trong NetBeans để Generate toàn bộ Getter/Setter nhé, mình viết rút gọn để tránh dài dòng)
    
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
    public String getCalendarType() { return calendarType; }
    public void setCalendarType(String calendarType) { this.calendarType = calendarType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOnlineMeetingLink() { return onlineMeetingLink; }
    public void setOnlineMeetingLink(String onlineMeetingLink) { this.onlineMeetingLink = onlineMeetingLink; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public String getGuests() { return guests; }
    public void setGuests(String guests) { this.guests = guests; }
    public Integer getTutorId() { return tutorId; }
    public void setTutorId(Integer tutorId) { this.tutorId = tutorId; }
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public int getReminderTime() { return reminderTime; }
    public void setReminderTime(int reminderTime) { this.reminderTime = reminderTime; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}