package com.mycompany.tutorhub_enterprise.client;

public class TestBoard {
    public static void main(String[] args) {
        // Initialize JCEF off the EDT to avoid message loop starvation
        JcefManager.getClient();
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            new BlackboardFrame(null, "test_class", "teacher", true).setVisible(true);
            
            new Thread(() -> {
                try {
                    Thread.sleep(30000);
                    System.exit(0);
                } catch (Exception e) {}
            }).start();
        });
    }
}
