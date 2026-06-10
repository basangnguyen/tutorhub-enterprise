package com.mycompany.tutorhub_enterprise.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CalendarTaskModel {
    private int taskId;
    private String title;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private LocalDateTime deadline;
    private String description;
    private String taskList;
    private Integer assignedTo;
    private String status;
    private String priority;
    private int reminderTime;
    private Integer createdBy;

    public CalendarTaskModel() {}

    // Getters & Setters
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalTime getDueTime() { return dueTime; }
    public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTaskList() { return taskList; }
    public void setTaskList(String taskList) { this.taskList = taskList; }
    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getReminderTime() { return reminderTime; }
    public void setReminderTime(int reminderTime) { this.reminderTime = reminderTime; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}