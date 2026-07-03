package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.client.drive.DriveMainPane;
import com.mycompany.tutorhub_enterprise.client.drive.DriveViewModel;
import com.mycompany.tutorhub_enterprise.client.services.DriveService;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Cửa sổ Drive (Quản lý tài liệu).
 * Đã được tái cấu trúc (Refactor) lại sử dụng kiến trúc MVVM.
 * Lớp này hiện tại chỉ đóng vai trò là Bridge (cầu nối) giữa hệ thống Swing cũ và giao diện JavaFX mới.
 * Toàn bộ logic hiển thị được chuyển giao cho DriveMainPane.
 */
public class DriveTab extends JPanel {

    private JFXPanel fxPanel;
    private DriveViewModel viewModel;

    public DriveTab(int currentUserId) {
        setLayout(new BorderLayout());

        // Khởi tạo Panel cầu nối JavaFX
        fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);

        // Khởi tạo ViewModel trung tâm quản lý State và Data fetching
        viewModel = new DriveViewModel(currentUserId, new DriveService());

        // Chạy Thread dọn rác ngầm (Cleanup Trash) khi khởi động Tab
        viewModel.getDriveService().cleanupTrash();

        // Chạy JavaFX Thread để khởi tạo giao diện
        Platform.runLater(this::initFX);
    }

    private void initFX() {
        // Lắp ráp Layout gốc từ các thành phần nhỏ
        DriveMainPane mainPane = new DriveMainPane(viewModel);
        Scene scene = new Scene(mainPane);
        
        // Nhúng file CSS (nếu có)
        String cssUrl = getClass().getResource("/css/DriveTab.css") != null 
            ? getClass().getResource("/css/DriveTab.css").toExternalForm() : null;
        if (cssUrl != null) scene.getStylesheets().add(cssUrl);

        fxPanel.setScene(scene);
        
        // Bắt đầu tải dữ liệu hiển thị (Thư mục root mặc định)
        viewModel.loadFiles();
    }
}