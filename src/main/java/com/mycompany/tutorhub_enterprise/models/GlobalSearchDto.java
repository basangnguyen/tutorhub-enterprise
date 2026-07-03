package com.mycompany.tutorhub_enterprise.models;

public class GlobalSearchDto {
    public String id;
    public String type; // USER, CLASS, DOC...
    public String title;
    public String subtitle;

    public GlobalSearchDto() {}

    public GlobalSearchDto(String id, String type, String title, String subtitle) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
    }
}
