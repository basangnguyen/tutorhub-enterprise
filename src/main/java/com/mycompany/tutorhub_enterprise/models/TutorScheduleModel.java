package com.mycompany.tutorhub_enterprise.models;

import java.sql.Time;

public class TutorScheduleModel {
    private int id;
    private int userId;
    private String title;
    private int dayOfWeek; // 1: CN, 2: Thứ 2... 7: Thứ 7
    private Time startTime;
    private Time endTime;
    private String location;
    private String category; // 'CLASS' hoặc 'EVENT'
    private String colorCode;

    public TutorScheduleModel(int id, int userId, String title, int dayOfWeek, Time startTime, Time endTime, String location, String category, String colorCode) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.category = category;
        this.colorCode = colorCode;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public int getDayOfWeek() { return dayOfWeek; }
    public Time getStartTime() { return startTime; }
    public Time getEndTime() { return endTime; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getColorCode() { return colorCode; }
}