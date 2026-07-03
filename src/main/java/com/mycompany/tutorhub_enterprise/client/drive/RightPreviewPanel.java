package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveFormatUtils;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.SVGPath;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thành phần giao diện thanh bên phải (Right Sidebar).
 * Hiển thị thông tin chi tiết (Meta) khi chọn 1 file, hoặc hiển thị Batch Actions khi chọn nhiều file.
 */
public class RightPreviewPanel extends VBox {
    private final DriveViewModel viewModel;
    private final Consumer<DriveFileModel> previewFileAction;
    private final Consumer<DriveFileModel> downloadAction;

    // Định nghĩa màu sắc
    private static final String TEXT_MUTED = "#6B7280";
    private static final String TEXT_MAIN = "#1F2937";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5E7EB";
    private static final String BG_WHITE = "#FFFFFF";
    private static final String BG_GRAY_LIGHT = "#F3F4F6";

    public RightPreviewPanel(DriveViewModel viewModel, 
                             Consumer<DriveFileModel> previewFileAction,
                             Consumer<DriveFileModel> downloadAction) {
        this.viewModel = viewModel;
        this.previewFileAction = previewFileAction;
        this.downloadAction = downloadAction;

        this.setPrefWidth(320);
        this.setStyle("-fx-background-color: " + BG_WHITE + "; -fx-border-color: transparent transparent transparent " + BORDER_COLOR + ";");
        
        bindViewModel();
        render(); // Render mặc định lúc khởi tạo
    }

    private void bindViewModel() {
        // Lắng nghe danh sách file được chọn thay đổi
        viewModel.getSelectedFiles().addListener((javafx.collections.ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(this::render);
        });
    }

    /**
     * Logic thay đổi giao diện linh hoạt dựa trên số lượng file được chọn
     */
    public void render() {
        this.getChildren().clear();
        List<DriveFileModel> selected = viewModel.getSelectedFiles();

        if (selected.isEmpty()) {
            renderEmptyState();
        } else if (selected.size() == 1) {
            renderSingleFile(selected.get(0));
        } else {
            renderBatchState(selected.size());
        }
    }

    // Trạng thái 0 File: Trống
    private void renderEmptyState() {
        this.setPadding(new Insets(24));
        this.setAlignment(Pos.CENTER);
        Label lblEmpty = new Label("Chọn một tài liệu để xem chi tiết");
        lblEmpty.setTextFill(Color.web(TEXT_MUTED));
        this.getChildren().add(lblEmpty);
    }

    // Trạng thái 1 File: Xem Meta
    private void renderSingleFile(DriveFileModel file) {
        this.setPadding(new Insets(24));
        this.setAlignment(Pos.TOP_LEFT);
        this.setSpacing(20);

        // Header (Tên File + Nút Đóng)
        HBox header = new HBox(10);
        header.setAlignment(Pos.TOP_LEFT);
        Label lblName = new Label(file.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblName.setWrapText(true);
        lblName.setPrefWidth(220);
        
        Region s = new Region(); 
        HBox.setHgrow(s, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14; -fx-cursor: hand;");
        btnClose.setOnAction(e -> {
            viewModel.selectFile(null, false);
        });

        header.getChildren().addAll(lblName, s, btnClose);

        // Khung ảnh Preview thu nhỏ
        StackPane preview = new StackPane();
        preview.setPrefHeight(160);
        preview.setStyle("-fx-background-color: " + BG_GRAY_LIGHT + "; -fx-background-radius: 12; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 12;");
        
        javafx.scene.Node previewIcon = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FILE, 72, TEXT_MUTED); 
        preview.getChildren().add(previewIcon);

        // Bảng dữ liệu Metadata
        VBox metadata = new VBox(12);
        metadata.getChildren().addAll(
            createMetaRow("Loại", file.getFileType()),
            createMetaRow("Kích thước", file.getFileSize() > 0 ? DriveFormatUtils.formatFileSize(file.getFileSize()) : "-"),
            createMetaRow("Vị trí", "📁 " + file.getSourceLocation()),
            createOwnerRow("Người sở hữu", "Hệ thống TutorHub"),
            createMetaRow("Cập nhật", DriveFormatUtils.formatRelativeTime(file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt()))
        );

        // Các nút hành động chính (Mở, Tải xuống, Chia sẻ)
        VBox actions = new VBox(10);
        Button btnOpen = new Button("Mở ↗");
        btnOpen.setMaxWidth(Double.MAX_VALUE);
        btnOpen.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
        btnOpen.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                viewModel.openFolder(file);
            } else {
                if (previewFileAction != null) previewFileAction.accept(file);
            }
        });

        Button btnDownload = new Button("↓ Tải xuống");
        btnDownload.setMaxWidth(Double.MAX_VALUE);
        btnDownload.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + "; -fx-cursor: hand;");
        btnDownload.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                new Alert(Alert.AlertType.INFORMATION, "Chưa hỗ trợ tải thư mục trực tiếp.").showAndWait();
            } else {
                if (downloadAction != null) downloadAction.accept(file);
            }
        });

        Button btnShare = new Button("➦ Chia sẻ");
        btnShare.setMaxWidth(Double.MAX_VALUE);
        btnShare.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + "; -fx-cursor: hand;");
        btnShare.setOnAction(e -> {
            new Alert(Alert.AlertType.INFORMATION, "Tính năng Chia sẻ đang được phát triển trong giai đoạn tiếp theo.").showAndWait();
        });

        actions.getChildren().addAll(btnOpen, btnDownload, btnShare);

        this.getChildren().addAll(header, preview, metadata, new Separator(), actions);
    }

    // Trạng thái > 1 File: Bảng Batch Actions
    private void renderBatchState(int count) {
        this.setPadding(new Insets(24));
        this.setAlignment(Pos.TOP_LEFT);
        this.setSpacing(20);

        Label lblTitle = new Label("Đã chọn " + count + " mục");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Button btnTrash = new Button("🗑️ Xóa tất cả");
        btnTrash.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        btnTrash.setMaxWidth(Double.MAX_VALUE);
        btnTrash.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Đưa " + count + " mục vào thùng rác?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    for (DriveFileModel f : viewModel.getSelectedFiles()) {
                        viewModel.getDriveService().moveToTrash(f.getFileId());
                    }
                    viewModel.selectFile(null, false);
                    viewModel.loadFiles();
                }
            });
        });

        Button btnStar = new Button("⭐ Gắn sao tất cả");
        btnStar.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        btnStar.setMaxWidth(Double.MAX_VALUE);
        btnStar.setOnAction(e -> {
            for (DriveFileModel f : viewModel.getSelectedFiles()) {
                if (!viewModel.getCurrentStarredIds().contains(f.getFileId())) {
                    viewModel.getDriveService().toggleStar(f.getFileId(), viewModel.getCurrentUserId());
                }
            }
            viewModel.selectFile(null, false);
            viewModel.loadStarredIds(); // update set
        });
        
        this.getChildren().addAll(lblTitle, btnStar, btnTrash);
    }

    private HBox createMetaRow(String label, String val) {
        HBox row = new HBox(10);
        Label l = new Label(label); l.setPrefWidth(80); l.setTextFill(Color.web(TEXT_MUTED)); l.setFont(Font.font(12));
        Label v = new Label(val); v.setTextFill(Color.web(TEXT_MAIN)); v.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12)); v.setWrapText(true);
        row.getChildren().addAll(l, v);
        return row;
    }

    private HBox createOwnerRow(String label, String name) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label); l.setPrefWidth(80); l.setTextFill(Color.web(TEXT_MUTED)); l.setFont(Font.font(12));
        HBox owner = new HBox(6); owner.setAlignment(Pos.CENTER_LEFT);
        ImageView avt = new ImageView();
        try {
            java.net.URL url = getClass().getResource("/images/icon/user.svg");
            if (url != null) avt.setImage(new Image(url.toExternalForm()));
        } catch (Exception ex) {}
        avt.setFitWidth(20); avt.setFitHeight(20);
        Label n = new Label(name); n.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        owner.getChildren().addAll(avt, n);
        row.getChildren().addAll(l, owner);
        return row;
    }
}
