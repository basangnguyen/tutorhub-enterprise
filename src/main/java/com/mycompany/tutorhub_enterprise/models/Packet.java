package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;

public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String action;
    public String payload; // Giữ nguyên để không lỗi Login/Signup cũ
    public Object data;    // THÊM MỚI: Để chứa dữ liệu phức tạp (List, Object) từ Database
    public boolean success;
    public String message;

    public Packet() {}

    // Constructor 1: Dùng cho String payload (Cũ)
    public Packet(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    // Constructor 2: Dùng cho Object data (Mới)
    public Packet(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    // Constructor 3: Phản hồi cơ bản
    public Packet(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Constructor 4: Phản hồi có String payload
    public Packet(boolean success, String message, String payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
    }
    
    // Constructor 5: Phản hồi có Object data (Mới)
    public Packet(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}