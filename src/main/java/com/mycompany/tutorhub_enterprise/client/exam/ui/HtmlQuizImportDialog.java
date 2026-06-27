package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.HtmlQuizDataParser;
import com.mycompany.tutorhub_enterprise.client.exam.services.HtmlQuizDataParser.ParseResult;
import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class HtmlQuizImportDialog extends JDialog {

    public static HtmlQuizImportDialog currentActiveDialog;

    private NetworkManager networkManager;
    private QuestionBankTab parentTab;
    private ParseResult currentParseResult;

    private JLabel lblFileStatus;
    private JTextField txtBankName;
    private JTextField txtPaperTitle;
    private JTextField txtDefaultPoints;
    private JCheckBox chkCreatePaper;
    private JButton btnImport;

    public HtmlQuizImportDialog(Frame owner, NetworkManager networkManager, QuestionBankTab parentTab) {
        super(owner, "Import đề từ HTML", true);
        this.networkManager = networkManager;
        this.parentTab = parentTab;

        setSize(500, 450);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(6, 2, 10, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 1. File Selection
        mainPanel.add(new JLabel("File HTML:"));
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        JButton btnSelectFile = new JButton("Chọn file...");
        lblFileStatus = new JLabel("Chưa chọn file");
        lblFileStatus.setForeground(Color.GRAY);
        filePanel.add(btnSelectFile, BorderLayout.WEST);
        filePanel.add(lblFileStatus, BorderLayout.CENTER);
        mainPanel.add(filePanel);

        // 2. Bank Name
        mainPanel.add(new JLabel("Tên ngân hàng câu hỏi (*):"));
        txtBankName = new JTextField();
        mainPanel.add(txtBankName);

        // 3. Create Paper Checkbox
        mainPanel.add(new JLabel("Tạo đề thi từ file này:"));
        chkCreatePaper = new JCheckBox("Có");
        chkCreatePaper.setSelected(true);
        mainPanel.add(chkCreatePaper);

        // 4. Paper Title
        mainPanel.add(new JLabel("Tên đề thi (*):"));
        txtPaperTitle = new JTextField();
        mainPanel.add(txtPaperTitle);

        // 5. Default Points
        mainPanel.add(new JLabel("Điểm mỗi câu mặc định (*):"));
        txtDefaultPoints = new JTextField("1.0");
        mainPanel.add(txtDefaultPoints);

        add(mainPanel, BorderLayout.CENTER);

        // Preview panel for errors
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(new EmptyBorder(0, 15, 10, 15));
        JLabel lblPreview = new JLabel("Hướng dẫn: Chọn file HTML định dạng VSL để import.");
        previewPanel.add(lblPreview, BorderLayout.CENTER);
        add(previewPanel, BorderLayout.NORTH);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnImport = new JButton("Import");
        btnImport.setEnabled(false);
        JButton btnCancel = new JButton("Hủy");

        btnPanel.add(btnCancel);
        btnPanel.add(btnImport);
        add(btnPanel, BorderLayout.SOUTH);

        // Listeners
        chkCreatePaper.addActionListener(e -> txtPaperTitle.setEnabled(chkCreatePaper.isSelected()));

        btnSelectFile.addActionListener(e -> selectFile(lblPreview));

        btnCancel.addActionListener(e -> dispose());

        btnImport.addActionListener(e -> performImport());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                currentActiveDialog = HtmlQuizImportDialog.this;
            }
            @Override
            public void windowClosed(WindowEvent e) {
                if (currentActiveDialog == HtmlQuizImportDialog.this) {
                    currentActiveDialog = null;
                }
            }
        });
    }

    private void selectFile(JLabel lblPreview) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("HTML Files", "html", "htm"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Check size < 10MB
            if (selectedFile.length() > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "File không được vượt quá 10MB.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Path path = selectedFile.toPath();
            currentParseResult = HtmlQuizDataParser.parse(path);

            if (currentParseResult.isSuccess()) {
                int validCount = currentParseResult.getValidCount();
                int invalidCount = currentParseResult.getInvalidCount();
                
                if (validCount == 0) {
                    lblFileStatus.setText("Lỗi: Không tìm thấy câu hỏi hợp lệ");
                    lblFileStatus.setForeground(Color.RED);
                    lblPreview.setText("<html><font color='red'>Không có câu hỏi nào hợp lệ được tìm thấy.</font></html>");
                    btnImport.setEnabled(false);
                } else {
                    lblFileStatus.setText(selectedFile.getName() + " (" + validCount + " câu)");
                    lblFileStatus.setForeground(new Color(0, 150, 0)); // Dark green
                    
                    StringBuilder previewHtml = new StringBuilder("<html><b>Kết quả parse:</b><br/>");
                    previewHtml.append("Số câu hợp lệ: <font color='green'>").append(validCount).append("</font><br/>");
                    if (invalidCount > 0) {
                        previewHtml.append("Số câu bị lỗi: <font color='red'>").append(invalidCount).append("</font> (Sẽ bị bỏ qua)<br/>");
                    }
                    if (validCount > 0) {
                        previewHtml.append("<i>Preview Q1: ").append(currentParseResult.getQuestions().get(0).getQuestion()).append("</i>");
                    }
                    previewHtml.append("</html>");
                    lblPreview.setText(previewHtml.toString());
                    
                    btnImport.setEnabled(true);
                }
            } else {
                lblFileStatus.setText("Lỗi parse file");
                lblFileStatus.setForeground(Color.RED);
                lblPreview.setText("<html><font color='red'>Lỗi: " + currentParseResult.getErrorMessage() + "</font></html>");
                btnImport.setEnabled(false);
            }
        }
    }

    private void performImport() {
        if (currentParseResult == null || currentParseResult.getValidCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String bankName = txtBankName.getText().trim();
        if (bankName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên ngân hàng câu hỏi không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            txtBankName.requestFocus();
            return;
        }

        boolean createPaper = chkCreatePaper.isSelected();
        String paperTitle = txtPaperTitle.getText().trim();
        if (createPaper && paperTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đề thi không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            txtPaperTitle.requestFocus();
            return;
        }

        float defaultPoints = 1.0f;
        try {
            defaultPoints = Float.parseFloat(txtDefaultPoints.getText().trim());
            if (defaultPoints <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Điểm mỗi câu phải là một số lớn hơn 0.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            txtDefaultPoints.requestFocus();
            return;
        }

        btnImport.setEnabled(false);
        btnImport.setText("Đang xử lý...");

        // Chuẩn bị payload gửi lên server
        Map<String, Object> payload = new HashMap<>();
        payload.put("bankName", bankName);
        payload.put("paperTitle", paperTitle);
        payload.put("defaultPoints", defaultPoints);
        payload.put("createPaper", createPaper);
        payload.put("questions", currentParseResult.getQuestions());

        try {
            Packet req = new Packet("IMPORT_HTML_QUIZ", new Gson().toJson(payload));
            networkManager.sendPacket(req);
        } catch (Exception ex) {
            ex.printStackTrace();
            btnImport.setEnabled(true);
            btnImport.setText("Import");
            JOptionPane.showMessageDialog(this, "Lỗi kết nối khi gửi dữ liệu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onImportSuccess(Packet packet) {
        btnImport.setEnabled(true);
        btnImport.setText("Import");
        
        String msg = "Import đề HTML thành công.";
        if (packet.data instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) packet.data;
            Object qCount = data.get("questionCount");
            if (qCount != null) {
                msg += "\nSố câu đã lưu: " + qCount;
            }
        }
        
        JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        if (parentTab != null) {
            parentTab.reloadBanks();
            // TODO: call reload exam papers if exam papers panel is active, 
            // but for now the user can just switch tabs.
        }
        dispose();
    }

    public void onImportFailed(String errorMessage) {
        btnImport.setEnabled(true);
        btnImport.setText("Import");
        JOptionPane.showMessageDialog(this, "Lỗi từ server: " + errorMessage, "Import thất bại", JOptionPane.ERROR_MESSAGE);
    }
}
