package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Màn hình Container tổng cho Drive Tab.
 * Nó kết nối Sidebar, Header, Center Grid và Right Panel lại với nhau.
 * Quản lý kéo thả File và bảng Trạng thái Tải lên (Upload Manager).
 */
public class DriveMainPane extends BorderPane {
    private final DriveViewModel viewModel;

    private SidebarView sidebarView;
    private HeaderToolbarView headerView;
    private MainContentView mainContentView;
    private RightPreviewPanel rightPreviewPanel;

    // Quản lý upload progress tạm thời
    private VBox uploadManagerPanel;
    private VBox uploadTaskList;

    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5E7EB";

    public DriveMainPane(DriveViewModel viewModel) {
        this.viewModel = viewModel;
        setupUI();
    }

    private void setupUI() {
        this.setStyle("-fx-background-color: #F9FAFB;");

        // 1. Sidebar (Left)
        sidebarView = new SidebarView(viewModel);
        this.setLeft(sidebarView);

        // 2. Right Preview Panel
        rightPreviewPanel = new RightPreviewPanel(viewModel, this::previewFile, this::downloadFile);
        rightPreviewPanel.setVisible(false);
        rightPreviewPanel.setManaged(false);
        this.setRight(rightPreviewPanel);

        // Lắng nghe Selection để ẩn/hiện Right Panel tự động
        viewModel.getSelectedFiles().addListener((javafx.collections.ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(() -> {
                if (viewModel.getSelectedFiles().isEmpty()) {
                    rightPreviewPanel.setVisible(false);
                    rightPreviewPanel.setManaged(false);
                } else {
                    rightPreviewPanel.setVisible(true);
                    rightPreviewPanel.setManaged(true);
                }
            });
        });

        // 3. Center Area (Header + Main Content)
        StackPane centerContainer = new StackPane();
        VBox centerContent = new VBox();
        
        headerView = new HeaderToolbarView(viewModel, 
            this::uploadFiles, 
            () -> {
                boolean isVis = rightPreviewPanel.isVisible();
                rightPreviewPanel.setVisible(!isVis);
                rightPreviewPanel.setManaged(!isVis);
            },
            isGrid -> {
                if (mainContentView != null) {
                    mainContentView.setGridView(isGrid);
                }
            }
        );

        mainContentView = new MainContentView(viewModel, this::previewFile, this::downloadFile);
        VBox.setVgrow(mainContentView, Priority.ALWAYS);

        centerContent.getChildren().addAll(headerView, mainContentView);
        
        // Popup Quản lý Tải lên (Nằm đè ở góc phải dưới của Center Area)
        createUploadProgressPanel();
        StackPane.setAlignment(uploadManagerPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(uploadManagerPanel, new Insets(0, 20, 20, 0));

        centerContainer.getChildren().addAll(centerContent, uploadManagerPanel);

        this.setCenter(centerContainer);

        // Kéo thả (Drag & Drop)
        setupDragAndDrop(this, centerContainer);
    }

    private void setupDragAndDrop(BorderPane pane, StackPane parentStack) {
        VBox dragOverlay = new VBox(12);
        dragOverlay.setAlignment(Pos.CENTER);
        dragOverlay.setStyle("-fx-background-color: rgba(37,99,235,0.06); -fx-border-color: #2563EB; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 16; -fx-background-radius: 16;");
        dragOverlay.setVisible(false);
        dragOverlay.setMouseTransparent(true);
        Label dragIcon = new Label("☁");
        dragIcon.setFont(Font.font(42));
        Label dragText = new Label("Thả file vào đây để tải lên");
        dragText.setFont(Font.font("System", FontWeight.BOLD, 16));
        dragText.setTextFill(Color.web(PRIMARY_BLUE));
        Label dragHint = new Label("Hỗ trợ tất cả các định dạng tệp tin");
        dragHint.setTextFill(Color.web("#6B7280"));
        dragOverlay.getChildren().addAll(dragIcon, dragText, dragHint);
        
        if (parentStack != null) parentStack.getChildren().add(dragOverlay);

        pane.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                dragOverlay.setVisible(true);
            }
            e.consume();
        });

        pane.setOnDragExited(e -> {
            dragOverlay.setVisible(false);
        });

        pane.setOnDragDropped(e -> {
            dragOverlay.setVisible(false);
            javafx.scene.input.Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                uploadFiles(files);
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });
    }

    private void createUploadProgressPanel() {
        uploadManagerPanel = new VBox(10);
        uploadManagerPanel.setPadding(new Insets(10));
        uploadManagerPanel.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");
        uploadManagerPanel.setMaxSize(350, 400);
        uploadManagerPanel.setVisible(false);
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Quản lý tải lên");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("✖");
        btnClose.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btnClose.setOnAction(e -> uploadManagerPanel.setVisible(false));
        header.getChildren().addAll(lblTitle, spacer, btnClose);
        
        uploadTaskList = new VBox(5);
        ScrollPane scroll = new ScrollPane(uploadTaskList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: transparent;");
        
        uploadManagerPanel.getChildren().addAll(header, new Separator(), scroll);
    }

    private void previewFile(DriveFileModel file) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            DrivePreviewDialog dialog = new DrivePreviewDialog(null, file, this::downloadFile);
            dialog.setVisible(true);
        });
    }

    private void downloadFile(DriveFileModel file) {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Chọn thư mục lưu");
        File dest = dc.showDialog(this.getScene().getWindow());
        if (dest != null) {
            try {
                File destFile = new File(dest, file.getName());
                if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                    com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                    java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                    if (is != null) {
                        Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        new Alert(Alert.AlertType.INFORMATION, "Tải xuống thành công!").showAndWait();
                    } else {
                        throw new Exception("Cloud download trả về null");
                    }
                } else {
                    File sourceFile = new File(file.getFileUrl());
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    new Alert(Alert.AlertType.INFORMATION, "Tải xuống thành công!").showAndWait();
                }
            } catch (Exception ex) { 
                new Alert(Alert.AlertType.ERROR, "Lỗi khi tải xuống: " + ex.getMessage()).showAndWait(); 
            }
        }
    }

    private void uploadFiles(List<File> files) {
        uploadManagerPanel.setVisible(true);

        for (File file : files) {
            HBox taskRow = new HBox(10);
            taskRow.setAlignment(Pos.CENTER_LEFT);
            taskRow.setPadding(new Insets(5, 0, 5, 0));
            
            VBox infoBox = new VBox(3);
            Label lblName = new Label(file.getName());
            lblName.setFont(Font.font(12));
            lblName.setMaxWidth(200);
            lblName.setTextOverrun(OverrunStyle.ELLIPSIS);
            
            ProgressBar pb = new ProgressBar(0.0);
            pb.setPrefWidth(200);
            pb.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");
            
            Label lblStatus = new Label("Đang phân mảnh & tải lên...");
            lblStatus.setFont(Font.font(10));
            lblStatus.setTextFill(Color.GRAY);
            
            infoBox.getChildren().addAll(lblName, pb, lblStatus);
            Label lblIcon = new Label("⏳");
            taskRow.getChildren().addAll(lblIcon, infoBox);
            
            uploadTaskList.getChildren().add(0, taskRow);

            javafx.beans.property.DoubleProperty progressProp = new javafx.beans.property.SimpleDoubleProperty(0);
            progressProp.addListener((obs, oldVal, newVal) -> {
                pb.setProgress(newVal.doubleValue());
                lblStatus.setText(String.format("Đang tải lên... %.1f%%", newVal.doubleValue() * 100));
            });

            DriveUploadManager uploadManager = DriveUploadManager.getInstance();
            uploadManager.uploadFileAsync(file, progressProp).thenAccept(fileUrl -> {
                if (fileUrl != null) {
                    // DUAL STORAGE: Sao chép dự phòng sang Local (bất đồng bộ)
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            File uploadDir = new File("drive_uploads");
                            if (!uploadDir.exists()) uploadDir.mkdirs();
                            File destFile = new File(uploadDir, System.currentTimeMillis() + "_" + file.getName());
                            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            System.err.println("[DUAL STORAGE] Lỗi sao chép dự phòng local: " + e.getMessage());
                        }
                    });

                    String fileName = file.getName();
                    String ext = "";
                    int dotIdx = fileName.lastIndexOf('.');
                    if (dotIdx > 0) ext = fileName.substring(dotIdx + 1);

                    DriveFileModel newFile = new DriveFileModel();
                    newFile.setName(fileName);
                    newFile.setFileType(ext.isEmpty() ? "document" : ext);
                    newFile.setFileSize(file.length());
                    newFile.setFileUrl(fileUrl);
                    newFile.setOwnerId(viewModel.getCurrentUserId());
                    newFile.setSourceLocation("B2_AND_LOCAL");
                    newFile.setParentId(viewModel.currentFolderIdProperty().get());
                    newFile.setStatus("active");
                    
                    viewModel.getDriveService().insertFile(newFile).thenAccept(success -> {
                        Platform.runLater(() -> {
                            lblStatus.setText("Đã hoàn tất (B2 + Local)");
                            pb.setProgress(1.0);
                            lblIcon.setText("✅");
                            viewModel.loadFiles();
                            
                            // [PHASE 4: REAL-TIME SYNC] Phát sóng cho các máy khác cập nhật
                            try {
                                com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(
                                    new com.mycompany.tutorhub_enterprise.models.Packet("BROADCAST", "SYNC_DRIVE_UPDATE")
                                );
                            } catch (Exception ex) {
                                System.err.println("[SYNC ERROR] Không thể phát sóng sự kiện: " + ex.getMessage());
                            }
                        });
                    });
                } else {
                    Platform.runLater(() -> {
                        lblStatus.setText("Lỗi mạng, vui lòng thử lại!");
                        lblStatus.setTextFill(Color.RED);
                        pb.setStyle("-fx-accent: red;");
                        lblIcon.setText("❌");
                    });
                }
            });
        }
    }
}
