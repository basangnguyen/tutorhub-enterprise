package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TSEV2ChildDebugSummaryPanel extends JPanel {

    private final TSEV2ChildDebugLoadResult result;

    public TSEV2ChildDebugSummaryPanel(TSEV2ChildDebugLoadResult result) {
        this.result = result;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("V2 Secure Exam Debug Mode");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(headerLabel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Status section
        JLabel statusLabel = new JLabel("Load Status: ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(statusLabel, gbc);
        gbc.gridx = 1;
        JLabel statusValue = new JLabel(result.isSuccess() ? "SUCCESS" : "FAIL");
        statusValue.setForeground(result.isSuccess() ? new Color(0, 150, 0) : Color.RED);
        contentPanel.add(statusValue, gbc);

        if (!result.isSuccess()) {
            gbc.gridx = 0;
            gbc.gridy++;
            JLabel errorLabel = new JLabel("Error Code: ");
            errorLabel.setFont(errorLabel.getFont().deriveFont(Font.BOLD));
            contentPanel.add(errorLabel, gbc);
            gbc.gridx = 1;
            JLabel errorValue = new JLabel(result.getErrorCode() != null ? result.getErrorCode() : "UNKNOWN_ERROR");
            errorValue.setForeground(Color.RED);
            contentPanel.add(errorValue, gbc);
            
            if (result.getErrorMessage() != null) {
                gbc.gridx = 0;
                gbc.gridy++;
                JLabel errorMsgLabel = new JLabel("Message: ");
                errorMsgLabel.setFont(errorMsgLabel.getFont().deriveFont(Font.BOLD));
                contentPanel.add(errorMsgLabel, gbc);
                gbc.gridx = 1;
                // Only showing short message, no stack trace or sensitive path
                String safeMsg = result.getErrorMessage();
                if (safeMsg.length() > 100) safeMsg = safeMsg.substring(0, 100) + "...";
                JLabel errorMsgValue = new JLabel(safeMsg);
                contentPanel.add(errorMsgValue, gbc);
            }
        }

        // Flags
        gbc.gridx = 0;
        gbc.gridy++;
        addFlag(contentPanel, gbc, "Meta loaded", result.isMetaLoaded());
        gbc.gridy++;
        addFlag(contentPanel, gbc, "IPC key fetched", result.isKeyFetched());
        gbc.gridy++;
        addFlag(contentPanel, gbc, "Hash verified", result.isHashVerified());
        gbc.gridy++;
        addFlag(contentPanel, gbc, "Payload decrypted", result.isDecrypted());
        gbc.gridy++;
        addFlag(contentPanel, gbc, "Bundle parsed", result.isParsed());
        gbc.gridy++;
        addFlag(contentPanel, gbc, "Security validated", result.isSecurityValidated());

        // Metadata section
        if (result.getExamId() > 0) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Exam ID", String.valueOf(result.getExamId()));
        }
        if (result.getPaperId() > 0) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Paper ID", String.valueOf(result.getPaperId()));
        }
        if (result.getAttemptId() != null && !result.getAttemptId().isEmpty()) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Attempt ID", result.getAttemptId());
        }
        if (result.getQuestionCount() > 0) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Question Count", String.valueOf(result.getQuestionCount()));
        }
        if (result.getTotalScore() > 0) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Total Score", String.valueOf(result.getTotalScore()));
        }
        if (result.getDeadlineAt() != null && !result.getDeadlineAt().isEmpty()) {
            gbc.gridy++;
            try {
                long ts = Long.parseLong(result.getDeadlineAt());
                String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ts));
                addMetadataField(contentPanel, gbc, "Deadline At", dateString);
            } catch(Exception e) {
                addMetadataField(contentPanel, gbc, "Deadline At", result.getDeadlineAt());
            }
        }
        if (result.getPackageHash() != null) {
            gbc.gridy++;
            addMetadataField(contentPanel, gbc, "Package Hash", result.getPackageHash());
        }

        // Wrap in a scroll pane just in case
        JPanel container = new JPanel(new BorderLayout());
        container.add(contentPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JLabel footerLabel = new JLabel("Read-only debug summary. V2 rendering, autosave and submit are disabled in this phase.");
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, 11f));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        footerLabel.setForeground(Color.GRAY);
        add(footerLabel, BorderLayout.SOUTH);
    }

    private void addFlag(JPanel panel, GridBagConstraints gbc, String label, boolean value) {
        gbc.gridx = 0;
        JLabel title = new JLabel(label + ": ");
        panel.add(title, gbc);
        gbc.gridx = 1;
        JLabel valLabel = new JLabel(value ? "YES" : "NO");
        valLabel.setForeground(value ? new Color(0, 150, 0) : Color.DARK_GRAY);
        panel.add(valLabel, gbc);
    }

    private void addMetadataField(JPanel panel, GridBagConstraints gbc, String label, String value) {
        gbc.gridx = 0;
        JLabel title = new JLabel(label + ": ");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        panel.add(title, gbc);
        gbc.gridx = 1;
        JTextArea valArea = new JTextArea(value);
        valArea.setEditable(false);
        valArea.setOpaque(false);
        valArea.setLineWrap(true);
        valArea.setWrapStyleWord(true);
        valArea.setBorder(null);
        panel.add(valArea, gbc);
    }
}
