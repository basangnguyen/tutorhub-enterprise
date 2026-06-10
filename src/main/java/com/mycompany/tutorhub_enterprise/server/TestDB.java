package com.mycompany.tutorhub_enterprise.server;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Testing DB Manager initialization...");
        try {
            Class.forName("com.mycompany.tutorhub_enterprise.server.DatabaseManager");
            System.out.println("DB Manager loaded, tables should be created!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
