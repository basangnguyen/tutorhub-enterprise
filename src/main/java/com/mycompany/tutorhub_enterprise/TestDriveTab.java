package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.DriveTab;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestDriveTab {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Test Drive Tab");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                DriveTab tab = new DriveTab(1);
                frame.add(tab);
                frame.setSize(1200, 800);
                frame.setVisible(true);
                System.out.println("DriveTab created successfully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
