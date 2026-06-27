package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackageOption;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackagePreview;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackageQuestion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class ExamStartV2DebugDialog extends JDialog {

    private ExamStartV2DebugDialog(Window parent, Map<String, Object> debugData) {
        super(parent, "V2 Debug Preview: Exam " + debugData.get("examId"), ModalityType.MODELESS);
        setSize(800, 600);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new GridLayout(4, 1));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setBackground(new Color(255, 245, 230)); // Slightly orange to distinguish from normal preview
        
        JLabel titleLabel = new JLabel("[DEBUG] START REQUEST V2 PACKAGE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.RED);
        
        JLabel flowLabel = new JLabel("Flow: " + debugData.get("flow") + " | Package Version: " + debugData.get("packageVersion"));
        JLabel idsLabel = new JLabel("Exam ID: " + debugData.get("examId") + " | Paper ID: " + debugData.get("paperId") + " | Hash: " + debugData.get("packageHash"));
        JLabel statsLabel = new JLabel("Số câu: " + debugData.get("questionCount") + " | Tổng điểm: " + debugData.get("totalScore") + " | Thời gian: " + debugData.get("durationMinutes") + " phút");
        
        headerPanel.add(titleLabel);
        headerPanel.add(flowLabel);
        headerPanel.add(idsLabel);
        headerPanel.add(statsLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content
        JPanel questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsPanel.setBackground(Color.WHITE);
        
        Gson gson = new Gson();
        ExamPackagePreview parsedPackage = null;
        try {
            String json = gson.toJson(debugData);
            parsedPackage = gson.fromJson(json, ExamPackagePreview.class);
        } catch(Exception e) {}
        
        if (parsedPackage != null && parsedPackage.questions != null) {
            int qIndex = 1;
            for (ExamPackageQuestion q : parsedPackage.questions) {
                JPanel qPanel = new JPanel();
                qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));
                qPanel.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(10, 10, 10, 10),
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true)
                ));
                qPanel.setBackground(Color.WHITE);
                
                JPanel qHeader = new JPanel(new BorderLayout());
                qHeader.setBackground(Color.WHITE);
                JLabel qTitle = new JLabel("Câu " + qIndex + " (QID: " + q.questionId + ") (" + q.score + " điểm) - " + q.type);
                qTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
                qHeader.add(qTitle, BorderLayout.WEST);
                if (q.required) {
                    JLabel reqLabel = new JLabel("* Bắt buộc");
                    reqLabel.setForeground(Color.RED);
                    qHeader.add(reqLabel, BorderLayout.EAST);
                }
                
                JTextArea qContent = new JTextArea(q.content);
                qContent.setLineWrap(true);
                qContent.setWrapStyleWord(true);
                qContent.setEditable(false);
                qContent.setBorder(new EmptyBorder(5, 5, 5, 5));
                qContent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                
                qPanel.add(qHeader);
                qPanel.add(qContent);
                
                if (q.options != null && !q.options.isEmpty()) {
                    JPanel optsPanel = new JPanel();
                    optsPanel.setLayout(new BoxLayout(optsPanel, BoxLayout.Y_AXIS));
                    optsPanel.setBackground(Color.WHITE);
                    optsPanel.setBorder(new EmptyBorder(5, 15, 5, 5));
                    
                    for (ExamPackageOption opt : q.options) {
                        // V2 debug should ensure no correctOption / isCorrect is printed
                        JLabel optLabel = new JLabel(opt.optionLabel + ". " + opt.content);
                        optLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        optsPanel.add(optLabel);
                        optsPanel.add(Box.createVerticalStrut(3));
                    }
                    qPanel.add(optsPanel);
                }
                
                questionsPanel.add(qPanel);
                questionsPanel.add(Box.createVerticalStrut(10));
                qIndex++;
            }
        }
        
        JScrollPane rawScrollPane = new JScrollPane(questionsPanel);
        rawScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rawScrollPane.setBorder(null);

        // V2 Renderer Tab
        JPanel rendererPanel = new JPanel(new BorderLayout());
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        try {
            com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle bundle = com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.buildHandoffBundleFromMap(debugData);
            boolean isDebugMode = true; 
            if (debugData.get("debugMode") instanceof Boolean) {
                isDebugMode = (Boolean) debugData.get("debugMode");
            }
            com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.validateHandoffBundle(bundle, isDebugMode);
            String html = com.mycompany.tutorhub_enterprise.client.exam.ui.V2ExamPackageRenderer.renderHtml(bundle);
            htmlPane.setText(html);
        } catch (Exception e) {
            htmlPane.setText("<html><body><h3 style='color:red;'>Renderer Error:</h3><p>" + e.getMessage() + "</p></body></html>");
        }
        
        JScrollPane renderScrollPane = new JScrollPane(htmlPane);
        renderScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        renderScrollPane.setBorder(null);
        rendererPanel.add(renderScrollPane, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Render Preview", rendererPanel);
        tabbedPane.addTab("Raw Components", rawScrollPane);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(Color.WHITE);
        
        JButton encBtn = new JButton("Create encrypted handoff dry-run");
        encBtn.addActionListener(e -> {
            try {
                com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle bundle = com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.buildHandoffBundleFromMap(debugData);
                boolean isDebugMode = true; 
                if (debugData.get("debugMode") instanceof Boolean) {
                    isDebugMode = (Boolean) debugData.get("debugMode");
                }
                
                // For test dry run, generate a random key
                String testKey = com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils.generateAESKey();
                String encPath = com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, isDebugMode, null, null, null, 0);
                JOptionPane.showMessageDialog(this, "Created encrypted handoff at:\n" + encPath, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi tạo file encrypted: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        footerPanel.add(encBtn);

        JButton closeBtn = new JButton("Đóng");
        closeBtn.addActionListener(e -> dispose());
        footerPanel.add(closeBtn);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    public static void showDebugPreview(Component parent, Map<String, Object> debugData) {
        try {
            Window window = SwingUtilities.getWindowAncestor(parent);
            
            // Build and write handoff artifact
            String artifactPath = null;
            try {
                com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle bundle = com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.buildHandoffBundleFromMap(debugData);
                boolean isDebugMode = true; // since this is debug dialog
                if (debugData.get("debugMode") instanceof Boolean) {
                    isDebugMode = (Boolean) debugData.get("debugMode");
                }
                com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.validateHandoffBundle(bundle, isDebugMode);
                artifactPath = com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService.writeDebugHandoffArtifact(bundle);
            } catch (Exception e) {
                System.err.println("[V2_HANDOFF_ERROR] " + e.getMessage());
                e.printStackTrace();
            }

            ExamStartV2DebugDialog dialog = new ExamStartV2DebugDialog(window, debugData);
            
            // Append artifact info to dialog footer
            if (artifactPath != null) {
                JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                infoPanel.setBackground(Color.WHITE);
                JLabel pathLabel = new JLabel("Handoff Artifact: " + artifactPath);
                pathLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                pathLabel.setForeground(new Color(0, 100, 0));
                infoPanel.add(pathLabel);
                
                JPanel mainPanel = (JPanel) dialog.getContentPane().getComponent(0);
                JPanel footerPanel = (JPanel) mainPanel.getComponent(2); // South panel is index 2
                footerPanel.add(infoPanel, 0); // insert before close button
            }

            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Lỗi khi render dữ liệu V2 debug.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
