package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Container tổng cho tab Tài liệu.
 * Kết nối sidebar, toolbar, danh sách tài liệu, preview panel và upload manager.
 */
public class DriveMainPane extends BorderPane {
    private final DriveViewModel viewModel;

    private SidebarView sidebarView;
    private HeaderToolbarView headerView;
    private MainContentView mainContentView;
    private RightPreviewPanel rightPreviewPanel;

    private VBox uploadManagerPanel;
    private VBox uploadTaskList;

    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#DFE6F1";
    private static final String TEXT_MUTED = "#64748B";

    public DriveMainPane(DriveViewModel viewModel) {
        this.viewModel = viewModel;
        setupUI();
    }

    private void setupUI() {
        this.setStyle("-fx-background-color: linear-gradient(to bottom right, #F7F9FE, #EEF3FB);");

        sidebarView = new SidebarView(viewModel);
        BorderPane.setMargin(sidebarView, new Insets(18, 0, 18, 18));
        this.setLeft(sidebarView);

        rightPreviewPanel = new RightPreviewPanel(viewModel, this::previewFile, this::downloadFile);
        rightPreviewPanel.setVisible(false);
        rightPreviewPanel.setManaged(false);
        BorderPane.setMargin(rightPreviewPanel, new Insets(18, 18, 18, 0));
        this.setRight(rightPreviewPanel);

        viewModel.getSelectedFiles().addListener((javafx.collections.ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(() -> {
                boolean hasSelection = !viewModel.getSelectedFiles().isEmpty();
                rightPreviewPanel.setVisible(hasSelection);
                rightPreviewPanel.setManaged(hasSelection);
            });
        });

        StackPane centerContainer = new StackPane();
        VBox centerContent = new VBox();
        centerContent.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: #E5EBF5;" +
            "-fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 22, 0, 0, 8);"
        );

        headerView = new HeaderToolbarView(
            viewModel,
            this::uploadFiles,
            () -> {
                boolean visible = rightPreviewPanel.isVisible();
                rightPreviewPanel.setVisible(!visible);
                rightPreviewPanel.setManaged(!visible);
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

        createUploadProgressPanel();
        StackPane.setAlignment(uploadManagerPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(uploadManagerPanel, new Insets(0, 24, 24, 0));

        centerContainer.getChildren().addAll(centerContent, uploadManagerPanel);
        BorderPane.setMargin(centerContainer, new Insets(18, 14, 18, 14));
        this.setCenter(centerContainer);

        setupDragAndDrop(this, centerContainer);

        // Nạp thông tin dung lượng và trạng thái sao ngay khi khởi tạo
        Platform.runLater(() -> {
            viewModel.loadStorageQuota();
            viewModel.loadStarredIds();
            viewModel.loadFiles();
        });
    }

    private void setupDragAndDrop(BorderPane pane, StackPane parentStack) {
        VBox dragOverlay = new VBox(14);
        dragOverlay.setAlignment(Pos.CENTER);
        dragOverlay.setStyle(
            "-fx-background-color: rgba(248,250,252,0.94);" +
            "-fx-border-color: #7C3AED;" +
            "-fx-border-width: 2;" +
            "-fx-border-style: segments(12, 10);" +
            "-fx-border-radius: 24;" +
            "-fx-background-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.16), 26, 0, 0, 10);"
        );
        dragOverlay.setVisible(false);
        dragOverlay.setMouseTransparent(true);

        Label dragIcon = new Label("UPLOAD");
        dragIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        dragIcon.setTextFill(Color.web(PRIMARY_BLUE));
        dragIcon.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 999; -fx-padding: 7 14;");

        Label dragText = new Label("Thả tài liệu vào đây để tải lên");
        dragText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        dragText.setTextFill(Color.web("#111827"));

        Label dragHint = new Label("TutorHub sẽ tự động lưu vào thư mục hiện tại");
        dragHint.setTextFill(Color.web(TEXT_MUTED));
        dragHint.setFont(Font.font("Segoe UI", 13));
        dragOverlay.getChildren().addAll(dragIcon, dragText, dragHint);

        if (parentStack != null) {
            parentStack.getChildren().add(dragOverlay);
        }

        pane.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                dragOverlay.setVisible(true);
            }
            e.consume();
        });

        pane.setOnDragExited(e -> dragOverlay.setVisible(false));

        pane.setOnDragDropped(e -> {
            dragOverlay.setVisible(false);
            javafx.scene.input.Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                uploadFiles(db.getFiles());
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });
    }

    private void createUploadProgressPanel() {
        uploadManagerPanel = new VBox(12);
        uploadManagerPanel.setPadding(new Insets(14));
        uploadManagerPanel.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 18;" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 28, 0, 0, 12);"
        );
        uploadManagerPanel.setMaxSize(380, 420);
        uploadManagerPanel.setVisible(false);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Tiến trình tải lên");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTitle.setTextFill(Color.web("#111827"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("×");
        btnClose.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 999; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-padding: 2 8;");
        btnClose.setOnAction(e -> uploadManagerPanel.setVisible(false));
        header.getChildren().addAll(lblTitle, spacer, btnClose);

        uploadTaskList = new VBox(8);
        ScrollPane scroll = new ScrollPane(uploadTaskList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

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
                        new Alert(Alert.AlertType.INFORMATION, "Tải xuống thành công.").showAndWait();
                    } else {
                        throw new Exception("Cloud download trả về null");
                    }
                } else {
                    File sourceFile = new File(file.getFileUrl());
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    new Alert(Alert.AlertType.INFORMATION, "Tải xuống thành công.").showAndWait();
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
            taskRow.setPadding(new Insets(8, 4, 8, 4));
            taskRow.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12;");

            VBox infoBox = new VBox(5);
            Label lblName = new Label(file.getName());
            lblName.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            lblName.setMaxWidth(250);
            lblName.setTextOverrun(OverrunStyle.ELLIPSIS);

            ProgressBar pb = new ProgressBar(0.0);
            pb.setPrefWidth(250);
            pb.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");

            Label lblStatus = new Label("Đang chuẩn bị tải lên...");
            lblStatus.setFont(Font.font("Segoe UI", 10));
            lblStatus.setTextFill(Color.web(TEXT_MUTED));

            infoBox.getChildren().addAll(lblName, pb, lblStatus);
            Label lblIcon = new Label("UP");
            lblIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            lblIcon.setTextFill(Color.web(PRIMARY_BLUE));
            lblIcon.setStyle("-fx-background-color: #DBEAFE; -fx-background-radius: 999; -fx-padding: 5 7;");
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
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            File uploadDir = new File("drive_uploads");
                            if (!uploadDir.exists()) {
                                uploadDir.mkdirs();
                            }
                            File destFile = new File(uploadDir, System.currentTimeMillis() + "_" + file.getName());
                            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            System.err.println("[DUAL STORAGE] Lỗi sao chép dự phòng local: " + e.getMessage());
                        }
                    });

                    String fileName = file.getName();
                    String ext = "";
                    int dotIdx = fileName.lastIndexOf('.');
                    if (dotIdx > 0) {
                        ext = fileName.substring(dotIdx + 1);
                    }

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
                            lblStatus.setText("Đã hoàn tất");
                            pb.setProgress(1.0);
                            lblIcon.setText("OK");
                            viewModel.loadFiles();

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
                        lblStatus.setText("Lỗi mạng, vui lòng thử lại.");
                        lblStatus.setTextFill(Color.RED);
                        pb.setStyle("-fx-accent: #EF4444;");
                        lblIcon.setText("ERR");
                    });
                }
            });
        }
    }
}
