package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;
import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveFormatUtils;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.Consumer;

/**
 * Panel chi tiết bên phải cho tài liệu đang chọn.
 */
public class RightPreviewPanel extends VBox {
    private final DriveViewModel viewModel;
    private final Consumer<DriveFileModel> previewFileAction;
    private final Consumer<DriveFileModel> downloadAction;

    private static final String TEXT_MUTED = "#64748B";
    private static final String TEXT_MAIN = "#111827";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5EBF5";

    public RightPreviewPanel(DriveViewModel viewModel,
                             Consumer<DriveFileModel> previewFileAction,
                             Consumer<DriveFileModel> downloadAction) {
        this.viewModel = viewModel;
        this.previewFileAction = previewFileAction;
        this.downloadAction = downloadAction;

        this.setPrefWidth(340);
        this.setMinWidth(320);
        this.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 22, 0, 0, 8);"
        );

        bindViewModel();
        render();
    }

    private void bindViewModel() {
        viewModel.getSelectedFiles().addListener((javafx.collections.ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(this::render);
        });
    }

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

    private void renderEmptyState() {
        this.setPadding(new Insets(24));
        this.setAlignment(Pos.CENTER);
        this.setSpacing(12);

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(66, 66);
        iconWrap.setMaxSize(66, 66);
        iconWrap.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 22;");
        iconWrap.getChildren().add(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FILE, 30, "#94A3B8"));

        Label title = new Label("Chọn tài liệu");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(TEXT_MAIN));
        Label hint = new Label("Thông tin chi tiết, hành động nhanh và metadata sẽ hiển thị tại đây.");
        hint.setFont(Font.font("Segoe UI", 12));
        hint.setTextFill(Color.web(TEXT_MUTED));
        hint.setWrapText(true);
        hint.setMaxWidth(240);
        hint.setAlignment(Pos.CENTER);
        this.getChildren().addAll(iconWrap, title, hint);
    }

    private void renderSingleFile(DriveFileModel file) {
        this.setPadding(new Insets(22));
        this.setAlignment(Pos.TOP_LEFT);
        this.setSpacing(18);

        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);
        VBox titleBox = new VBox(4);
        Label lblEyebrow = new Label("CHI TIẾT");
        lblEyebrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lblEyebrow.setTextFill(Color.web("#94A3B8"));
        Label lblName = new Label(file.getName());
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblName.setTextFill(Color.web(TEXT_MAIN));
        lblName.setWrapText(true);
        lblName.setPrefWidth(230);
        titleBox.getChildren().addAll(lblEyebrow, lblName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("×");
        btnClose.setStyle("-fx-background-color: #F3F6FA; -fx-background-radius: 999; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 2 8;");
        btnClose.setOnAction(e -> viewModel.selectFile(null, false));
        header.getChildren().addAll(titleBox, spacer, btnClose);

        StackPane preview = new StackPane();
        preview.setPrefHeight(150);
        preview.setStyle(
            "-fx-background-color: " + previewBackground(file) + ";" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.65);" +
            "-fx-border-radius: 20;"
        );
        preview.getChildren().add(DriveSvgIconFactory.createSvgIcon(iconForFile(file), 66, iconColor(file)));

        VBox metadata = new VBox(10);
        metadata.setPadding(new Insets(14));
        metadata.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 18; -fx-border-color: #EDF2F7; -fx-border-radius: 18;");
        metadata.getChildren().addAll(
            createMetaRow("Loại", safeType(file).isEmpty() ? "Tài liệu" : safeType(file).toUpperCase()),
            createMetaRow("Kích thước", file.getFileSize() > 0 ? DriveFormatUtils.formatFileSize(file.getFileSize()) : "-"),
            createMetaRow("Vị trí", file.getSourceLocation()),
            createMetaRow("Chủ sở hữu", "TutorHub"),
            createMetaRow("Cập nhật", DriveFormatUtils.formatRelativeTime(file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt()))
        );

        VBox actions = new VBox(10);
        Button btnOpen = primaryButton("Mở tài liệu");
        btnOpen.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                viewModel.openFolder(file);
            } else if (previewFileAction != null) {
                previewFileAction.accept(file);
            }
        });

        Button btnDownload = secondaryButton("Tải xuống");
        btnDownload.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                new Alert(Alert.AlertType.INFORMATION, "Chưa hỗ trợ tải thư mục trực tiếp.").showAndWait();
            } else if (downloadAction != null) {
                downloadAction.accept(file);
            }
        });

        Button btnShare = secondaryButton("Chia sẻ");
        btnShare.setOnAction(e -> {
            new Alert(Alert.AlertType.INFORMATION, "Tính năng chia sẻ sẽ được triển khai ở giai đoạn tiếp theo.").showAndWait();
        });
        actions.getChildren().addAll(btnOpen, btnDownload, btnShare);

        this.getChildren().addAll(header, preview, metadata, new Separator(), actions);
    }

    private void renderBatchState(int count) {
        this.setPadding(new Insets(22));
        this.setAlignment(Pos.TOP_LEFT);
        this.setSpacing(16);

        Label lblTitle = new Label("Đã chọn " + count + " mục");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web(TEXT_MAIN));

        Label lblHint = new Label("Áp dụng hành động hàng loạt cho các tài liệu đang chọn.");
        lblHint.setFont(Font.font("Segoe UI", 12));
        lblHint.setTextFill(Color.web(TEXT_MUTED));
        lblHint.setWrapText(true);

        Button btnStar = secondaryButton("Gắn sao tất cả");
        btnStar.setOnAction(e -> {
            for (DriveFileModel f : viewModel.getSelectedFiles()) {
                if (!viewModel.getCurrentStarredIds().contains(f.getFileId())) {
                    viewModel.getDriveService().toggleStar(f.getFileId(), viewModel.getCurrentUserId());
                }
            }
            viewModel.selectFile(null, false);
            viewModel.loadStarredIds();
        });

        Button btnTrash = dangerButton("Xóa tất cả");
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

        this.getChildren().addAll(lblTitle, lblHint, btnStar, btnTrash);
    }

    private HBox createMetaRow(String label, String val) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        Label l = new Label(label);
        l.setPrefWidth(82);
        l.setTextFill(Color.web(TEXT_MUTED));
        l.setFont(Font.font("Segoe UI", 12));
        Label v = new Label(val == null || val.isBlank() ? "-" : val);
        v.setTextFill(Color.web(TEXT_MAIN));
        v.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        v.setWrapText(true);
        v.setMaxWidth(178);
        row.getChildren().addAll(l, v);
        return row;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(
            "-fx-background-color: linear-gradient(to right, #7C3AED, " + PRIMARY_BLUE + ");" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 11;" +
            "-fx-cursor: hand;"
        );
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + TEXT_MAIN + ";" +
            "-fx-cursor: hand;"
        );
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(
            "-fx-background-color: #FEF2F2;" +
            "-fx-text-fill: #DC2626;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        return button;
    }

    private String safeType(DriveFileModel file) {
        return file.getFileType() == null ? "" : file.getFileType().toLowerCase();
    }

    private String iconForFile(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return DriveSvgIcons.FOLDER;
        if ("pdf".equals(type)) return DriveSvgIcons.PDF;
        if ("video".equals(type) || "mp4".equals(type)) return DriveSvgIcons.FILE_VIDEO;
        if ("doc".equals(type) || "docx".equals(type)) return DriveSvgIcons.WORD;
        if ("excel".equals(type) || "xlsx".equals(type)) return DriveSvgIcons.EXCEL;
        return DriveSvgIcons.FILE;
    }

    private String previewBackground(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return "linear-gradient(to bottom right, #E0F2FE, #EDE9FE)";
        if ("pdf".equals(type)) return "linear-gradient(to bottom right, #FEE2E2, #FFF1F2)";
        if ("video".equals(type) || "mp4".equals(type)) return "linear-gradient(to bottom right, #E0E7FF, #F5F3FF)";
        if ("excel".equals(type) || "xlsx".equals(type)) return "linear-gradient(to bottom right, #DCFCE7, #ECFDF5)";
        return "linear-gradient(to bottom right, #F1F5F9, #F8FAFC)";
    }

    private String iconColor(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return "#2563EB";
        if ("pdf".equals(type)) return "#EF4444";
        if ("video".equals(type) || "mp4".equals(type)) return "#6366F1";
        if ("excel".equals(type) || "xlsx".equals(type)) return "#10B981";
        return "#64748B";
    }
}
