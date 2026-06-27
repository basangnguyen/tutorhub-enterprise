package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackageOption;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackagePreview;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPackageQuestion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ExamPreviewDialog extends JDialog {

    private ExamPreviewDialog(Window parent, ExamPackagePreview preview) {
        super(parent, "Xem Trước Đề Thi: " + (preview.examTitle != null ? preview.examTitle : preview.paperTitle), ModalityType.MODELESS);
        setSize(800, 600);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new GridLayout(3, 1));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setBackground(new Color(240, 240, 245));
        
        JLabel titleLabel = new JLabel("Đề thi: " + (preview.paperTitle != null ? preview.paperTitle : "Không tên"));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel examLabel = new JLabel("Kỳ thi: " + (preview.examTitle != null ? preview.examTitle : "N/A") + " | Thời gian: " + preview.durationMinutes + " phút");
        JLabel statsLabel = new JLabel("Tổng số câu: " + preview.questionCount + " | Tổng điểm: " + preview.totalScore);
        
        headerPanel.add(titleLabel);
        headerPanel.add(examLabel);
        headerPanel.add(statsLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content
        JPanel questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsPanel.setBackground(Color.WHITE);
        
        if (preview.questions != null) {
            int qIndex = 1;
            for (ExamPackageQuestion q : preview.questions) {
                JPanel qPanel = new JPanel();
                qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));
                qPanel.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(10, 10, 10, 10),
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true)
                ));
                qPanel.setBackground(Color.WHITE);
                
                JPanel qHeader = new JPanel(new BorderLayout());
                qHeader.setBackground(Color.WHITE);
                JLabel qTitle = new JLabel("Câu " + qIndex + " (" + q.score + " điểm) - " + q.type);
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
        
        JScrollPane scrollPane = new JScrollPane(questionsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(Color.WHITE);
        JButton closeBtn = new JButton("Đóng");
        closeBtn.addActionListener(e -> dispose());
        footerPanel.add(closeBtn);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    public static void showPreview(Component parent, Object previewData) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(previewData);
            ExamPackagePreview preview = gson.fromJson(json, ExamPackagePreview.class);
            
            Window window = SwingUtilities.getWindowAncestor(parent);
            ExamPreviewDialog dialog = new ExamPreviewDialog(window, preview);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Lỗi khi phân tích dữ liệu preview.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
