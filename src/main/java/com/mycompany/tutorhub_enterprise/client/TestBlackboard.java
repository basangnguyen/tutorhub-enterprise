package com.mycompany.tutorhub_enterprise.client;

import javax.swing.SwingUtilities;

public class TestBlackboard {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BlackboardFrame frame = new BlackboardFrame(null, "test_class", "teacher", true);
            frame.setVisible(true);
        });
    }
}
