package com.mycompany.tutorhub_enterprise.models;

import java.time.LocalTime;

public class CalendarPollModel {
    private int pollId;
    private int tutorId;
    private String title;
    private String description;
    private String dateList; 
    private LocalTime startTime;
    private LocalTime endTime;
    private String uniqueCode;

    
    private String guests;
    private int reminderTime;
    private String location;

    public String getGuests() { return guests; }
    public void setGuests(String guests) { this.guests = guests; }

    public int getReminderTime() { return reminderTime; }
    public void setReminderTime(int reminderTime) { this.reminderTime = reminderTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getPollId() { return pollId; }
    public void setPollId(int pollId) { this.pollId = pollId; }

    public int getTutorId() { return tutorId; }
    public void setTutorId(int tutorId) { this.tutorId = tutorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateList() { return dateList; }
    public void setDateList(String dateList) { this.dateList = dateList; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getUniqueCode() { return uniqueCode; }
    public void setUniqueCode(String uniqueCode) { this.uniqueCode = uniqueCode; }
}