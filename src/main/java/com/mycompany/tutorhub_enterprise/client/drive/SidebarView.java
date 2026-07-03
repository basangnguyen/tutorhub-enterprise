package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import com.mycompany.tutorhub_enterprise.utils.DriveFormatUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.SVGPath;
import javafx.application.Platform;

import java.util.List;

/**
 * Thành phần giao diện thanh bên trái (Left Sidebar) của hệ thống Drive.
 * Xử lý điều hướng (Navigation), hiển thị cây thư mục (Tree) và dung lượng lưu trữ.
 */
public class SidebarView extends VBox {
    private final DriveViewModel viewModel;
    private ProgressBar pbStorage;
    private Label lblStorageText;
    private TreeView<DriveFileModel> folderTreeView;
    private TreeItem<DriveFileModel> rootItem;

    // Các hằng số màu sắc giao diện
    private static final String TEXT_MUTED = "#6B7280";
    private static final String TEXT_MAIN = "#1F2937";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BG_GRAY_LIGHT = "#F3F4F6";

    public SidebarView(DriveViewModel viewModel) {
        this.viewModel = viewModel;
        setupUI();
        bindViewModel();
    }

    private void setupUI() {
        this.setPrefWidth(250);
        this.setPadding(new Insets(24, 16, 24, 16));
        this.setStyle("-fx-background-color: #F9FAFB; -fx-border-width: 0;");
        this.setSpacing(20);

        // 1. Nhóm Điều hướng Drive (Navigation)
        VBox driveGroup = new VBox(4);
        Label lblDriveTitle = new Label("Drive của tôi");
        lblDriveTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblDriveTitle.setTextFill(Color.web(TEXT_MUTED));
        lblDriveTitle.setPadding(new Insets(0, 0, 5, 12));

        Button btnRecent = createNavItem("Gần đây", DriveSvgIcons.RECENT, "#3b82f6", "recent");
        Button btnMyDrive = createNavItem("Drive của tôi", DriveSvgIcons.MY_DRIVE, "#10b981", "my_drive");
        Button btnOrgDrive = createNavItem("Drive tổ chức", DriveSvgIcons.ORG_DRIVE, "#8b5cf6", "org_drive");
        Button btnShared = createNavItem("Được chia sẻ với tôi", DriveSvgIcons.SHARED, "#06b6d4", "shared");
        Button btnStarred = createNavItem("Có gắn dấu sao", DriveSvgIcons.STARRED, "#f59e0b", "starred");
        Button btnTrash = createNavItem("Thùng rác", DriveSvgIcons.TRASH, "#ef4444", "trash");

        driveGroup.getChildren().addAll(lblDriveTitle, btnRecent, btnMyDrive, btnOrgDrive, btnShared, btnStarred, btnTrash);

        // 2. Nhóm Cây Thư mục
        VBox folderGroup = new VBox(4);
        HBox folderHeader = new HBox();
        folderHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblFolderTitle = new Label("THƯ MỤC");
        lblFolderTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblFolderTitle.setTextFill(Color.web(TEXT_MUTED));
        Region spacer = new Region(); 
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnAddFolder = new Button("+");
        btnAddFolder.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 0;");
        folderHeader.getChildren().addAll(lblFolderTitle, spacer, btnAddFolder);
        folderHeader.setPadding(new Insets(0, 10, 5, 12));

        folderTreeView = new TreeView<>();
        folderTreeView.setShowRoot(false);
        folderTreeView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        folderTreeView.setPrefHeight(400);

        rootItem = new TreeItem<>(new DriveFileModel());
        rootItem.getValue().setName("Root");
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);

        folderTreeView.setCellFactory(tv -> new TreeCell<DriveFileModel>() {
            @Override
            protected void updateItem(DriveFileModel item, boolean empty) {
                super.updateItem(item, empty);
                graphicProperty().unbind();
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    if (getTreeItem() != null) {
                        javafx.beans.binding.ObjectBinding<javafx.scene.Node> graphicBinding = 
                            Bindings.createObjectBinding(() -> {
                                boolean isExpanded = getTreeItem().isExpanded();
                                boolean isSelected = isSelected();
                                return DriveSvgIconFactory.createSvgIcon((isExpanded || isSelected) ? DriveSvgIcons.FOLDER_OPEN : DriveSvgIcons.FOLDER, 18, "#9CA3AF");
                            }, getTreeItem().expandedProperty(), selectedProperty());
                        graphicProperty().bind(graphicBinding);
                    } else {
                        setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FOLDER, 18, "#9CA3AF"));
                    }
                }
            }
        });

        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().getFileId() > 0) {
                viewModel.openFolder(newVal.getValue());
            }
        });

        VBox.setVgrow(folderTreeView, Priority.ALWAYS);
        folderGroup.getChildren().addAll(folderHeader, folderTreeView);
        VBox.setVgrow(folderGroup, Priority.ALWAYS);

        // 3. Nhóm Quản lý Storage Quota
        VBox storageGroup = new VBox(6);
        storageGroup.setPadding(new Insets(20, 0, 0, 12));
        Label lblStorageTitle = new Label("Dung lượng lưu trữ");
        lblStorageTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblStorageTitle.setTextFill(Color.web(TEXT_MUTED));
        
        pbStorage = new ProgressBar(0);
        pbStorage.setMaxWidth(Double.MAX_VALUE);
        pbStorage.setStyle("-fx-accent: #7C3AED;");
        
        lblStorageText = new Label("Đang tính toán...");
        lblStorageText.setFont(Font.font("System", 11));
        lblStorageText.setTextFill(Color.web(TEXT_MUTED));
        
        storageGroup.getChildren().addAll(lblStorageTitle, pbStorage, lblStorageText);

        this.getChildren().addAll(driveGroup, folderGroup, storageGroup);
        
        // Bắt đầu khởi tạo dữ liệu cây thư mục đệ quy
        buildFolderTree(rootItem, null);
    }

    private Button createNavItem(String text, String svgPathStr, String defaultColor, String mode) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(mode);
        
        SVGPath icon = DriveSvgIconFactory.createSvgIcon(svgPathStr, 18, defaultColor);
        icon.setOpacity(0.6);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(12);

        String baseStyle = "-fx-padding: 10 12; -fx-background-radius: 8; -fx-cursor: hand; ";
        btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";");

        btn.setOnMouseEntered(e -> {
            if (!mode.equals(viewModel.currentViewModeProperty().get())) {
                btn.setStyle(baseStyle + "-fx-background-color: " + BG_GRAY_LIGHT + "; -fx-text-fill: " + TEXT_MAIN + ";");
                icon.setOpacity(1.0);
            }
        });

        btn.setOnMouseExited(e -> {
            if (!mode.equals(viewModel.currentViewModeProperty().get())) {
                btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";");
                icon.setOpacity(0.6);
            }
        });

        btn.setOnAction(e -> {
            if ("org_drive".equals(mode)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tính năng Drive tổ chức đang được phát triển.");
                alert.showAndWait();
            } else {
                viewModel.currentViewModeProperty().set(mode);
                if ("my_drive".equals(mode)) {
                    viewModel.navigateHome();
                } else {
                    viewModel.currentFolderIdProperty().set(null);
                    viewModel.loadFiles();
                }
            }
        });

        // Liên kết (Bind) trạng thái ViewMode từ ViewModel để tự động active button
        viewModel.currentViewModeProperty().addListener((obs, oldMode, newMode) -> {
            if (mode.equals(newMode)) {
                btn.setStyle(baseStyle + "-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-font-weight: bold;");
                icon.setOpacity(1.0);
            } else {
                btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";");
                icon.setOpacity(0.6);
            }
        });

        if (mode.equals(viewModel.currentViewModeProperty().get())) {
            btn.setStyle(baseStyle + "-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-font-weight: bold;");
            icon.setOpacity(1.0);
        }

        return btn;
    }

    private void bindViewModel() {
        // Cập nhật Storage Progress khi biến trong ViewModel thay đổi
        viewModel.usedStorageBytesProperty().addListener((obs, oldVal, newVal) -> {
            long usedBytes = newVal.longValue();
            long MAX_BYTES = 15L * 1024 * 1024 * 1024; // 15 GB
            double ratio = (double) usedBytes / MAX_BYTES;
            pbStorage.setProgress(ratio);
            String usedStr = DriveFormatUtils.formatFileSize(usedBytes);
            lblStorageText.setText("Đã dùng " + usedStr + " / 15 GB");
            if (ratio > 0.9) pbStorage.setStyle("-fx-accent: #EF4444;");
            else pbStorage.setStyle("-fx-accent: #7C3AED;");
        });
        
        viewModel.currentFolderIdProperty().addListener((obs, oldVal, newVal) -> {
             // Logic để select tree node nếu cần thiết
        });
    }

    /**
     * Tải Lazy-loading cây thư mục (chỉ load nhánh con khi được Expand)
     */
    private void buildFolderTree(TreeItem<DriveFileModel> parentItem, Integer parentId) {
        viewModel.getDriveService().getFiles(viewModel.getCurrentUserId(), parentId).thenAccept(files -> {
            Platform.runLater(() -> {
                for (DriveFileModel f : files) {
                    if ("folder".equalsIgnoreCase(f.getFileType())) {
                        TreeItem<DriveFileModel> child = new TreeItem<>(f);
                        parentItem.getChildren().add(child);
                        if (f.getChildCount() > 0) {
                            child.getChildren().add(new TreeItem<>(new DriveFileModel())); // Fake node để có mũi tên Expand
                        }
                        child.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                            if (isNowExpanded && child.getChildren().size() == 1 && child.getChildren().get(0).getValue().getFileId() == 0) {
                                child.getChildren().clear();
                                buildFolderTree(child, f.getFileId());
                            }
                        });
                    }
                }
            });
        });
    }
}
