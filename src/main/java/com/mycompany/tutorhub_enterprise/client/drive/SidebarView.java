package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;
import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveFormatUtils;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Sidebar điều hướng cho tab Tài liệu.
 */
public class SidebarView extends VBox {
    private final DriveViewModel viewModel;
    private ProgressBar pbStorage;
    private Label lblStorageText;
    private TreeView<DriveFileModel> folderTreeView;
    private TreeItem<DriveFileModel> rootItem;

    private static final String TEXT_MUTED = "#64748B";
    private static final String TEXT_MAIN = "#111827";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BRAND_PURPLE = "#7C3AED";
    private static final String BORDER_COLOR = "#E5EBF5";

    public SidebarView(DriveViewModel viewModel) {
        this.viewModel = viewModel;
        setupUI();
        bindViewModel();
    }

    private void setupUI() {
        this.setPrefWidth(270);
        this.setMinWidth(250);
        this.setPadding(new Insets(20, 16, 18, 16));
        this.setSpacing(18);
        this.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 22, 0, 0, 8);"
        );

        VBox brandBlock = new VBox(4);
        brandBlock.setPadding(new Insets(0, 6, 4, 6));
        Label title = new Label("TutorHub Drive");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(TEXT_MAIN));
        Label subtitle = new Label("Tài liệu học tập và chia sẻ");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(TEXT_MUTED));
        brandBlock.getChildren().addAll(title, subtitle);

        VBox driveGroup = new VBox(6);
        Label lblDriveTitle = sectionLabel("Không gian");

        Button btnRecent = createNavItem("Gần đây", "images/icon/recent_color.svg", "recent");
        Button btnMyDrive = createNavItem("Drive của tôi", "images/icon/my_drive_color.svg", "my_drive");
        Button btnOrgDrive = createNavItem("Drive tổ chức", "images/icon/org_drive_color.svg", "org_drive");
        Button btnShared = createNavItem("Được chia sẻ", "images/icon/shared_color.svg", "shared");
        Button btnStarred = createNavItem("Đã gắn sao", "images/icon/starred_color.svg", "starred");
        Button btnTrash = createNavItem("Thùng rác", "images/icon/trash_color.svg", "trash");
        driveGroup.getChildren().addAll(lblDriveTitle, btnRecent, btnMyDrive, btnOrgDrive, btnShared, btnStarred, btnTrash);

        VBox folderGroup = new VBox(8);
        HBox folderHeader = new HBox(8);
        folderHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblFolderTitle = sectionLabel("Thư mục");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnAddFolder = new Button("+");
        btnAddFolder.setStyle(
            "-fx-background-color: #F3F6FF;" +
            "-fx-background-radius: 999;" +
            "-fx-text-fill: " + PRIMARY_BLUE + ";" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 1 8;"
        );
        folderHeader.getChildren().addAll(lblFolderTitle, spacer, btnAddFolder);

        folderTreeView = new TreeView<>();
        folderTreeView.setShowRoot(false);
        folderTreeView.setPrefHeight(360);
        folderTreeView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        rootItem = new TreeItem<>(new DriveFileModel());
        rootItem.getValue().setName("Root");
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);

        folderTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(DriveFileModel item, boolean empty) {
                super.updateItem(item, empty);
                graphicProperty().unbind();
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                    setTextFill(Color.web(TEXT_MAIN));
                    setStyle("-fx-background-radius: 10; -fx-padding: 7 8;");
                    javafx.scene.image.ImageView folderIcon = new javafx.scene.image.ImageView();
                    if (getTreeItem() != null) {
                        javafx.beans.binding.ObjectBinding<javafx.scene.image.Image> imageBinding =
                            Bindings.createObjectBinding(() -> {
                                boolean expanded = getTreeItem().isExpanded();
                                String iconPath = expanded ? "images/icon/yellow-open-folder-11567.svg" : "images/icon/folder-1484.svg";
                                return com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory.loadSvgImage(iconPath, 26);
                            }, getTreeItem().expandedProperty());
                        folderIcon.imageProperty().bind(imageBinding);
                    } else {
                        javafx.scene.image.Image closedImg = com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory.loadSvgImage("images/icon/folder-1484.svg", 26);
                        if (closedImg != null) folderIcon.setImage(closedImg);
                    }
                    folderIcon.setFitWidth(26);
                    folderIcon.setFitHeight(26);
                    setGraphic(folderIcon);
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

        VBox storageGroup = new VBox(9);
        storageGroup.setPadding(new Insets(14));
        storageGroup.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 18; -fx-border-color: #EDF2F7; -fx-border-radius: 18;");
        Label lblStorageTitle = new Label("Dung lượng");
        lblStorageTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblStorageTitle.setTextFill(Color.web(TEXT_MAIN));

        pbStorage = new ProgressBar(0);
        pbStorage.setMaxWidth(Double.MAX_VALUE);
        pbStorage.setStyle("-fx-accent: " + BRAND_PURPLE + ";");

        lblStorageText = new Label("Đang tính toán...");
        lblStorageText.setFont(Font.font("Segoe UI", 11));
        lblStorageText.setTextFill(Color.web(TEXT_MUTED));
        storageGroup.getChildren().addAll(lblStorageTitle, pbStorage, lblStorageText);

        this.getChildren().addAll(brandBlock, driveGroup, folderGroup, storageGroup);
        buildFolderTree(rootItem, null);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        label.setTextFill(Color.web("#94A3B8"));
        label.setPadding(new Insets(4, 8, 3, 8));
        return label;
    }

    private Button createNavItem(String text, String iconUrl, String mode) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(mode);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));

        javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView();
        try {
            if (iconUrl.endsWith(".svg")) {
                javafx.scene.image.Image img = com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory.loadSvgImage(iconUrl, 18);
                if (img != null) icon.setImage(img);
            } else {
                String path = iconUrl.startsWith("/") ? iconUrl : "/" + iconUrl;
                java.net.URL url = getClass().getResource(path);
                if (url != null) icon.setImage(new javafx.scene.image.Image(url.toExternalForm()));
            }
        } catch (Exception e) {}
        icon.setFitWidth(18);
        icon.setFitHeight(18);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(12);

        applyNavStyle(btn, icon, mode.equals(viewModel.currentViewModeProperty().get()), false);

        btn.setOnMouseEntered(e -> {
            if (!mode.equals(viewModel.currentViewModeProperty().get())) {
                applyNavStyle(btn, icon, false, true);
            }
        });
        btn.setOnMouseExited(e -> {
            if (!mode.equals(viewModel.currentViewModeProperty().get())) {
                applyNavStyle(btn, icon, false, false);
            }
        });

        btn.setOnAction(e -> {
            if ("org_drive".equals(mode)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Drive tổ chức đang được phát triển.");
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

        viewModel.currentViewModeProperty().addListener((obs, oldMode, newMode) -> {
            applyNavStyle(btn, icon, mode.equals(newMode), false);
        });

        return btn;
    }

    private void applyNavStyle(Button btn, javafx.scene.image.ImageView icon, boolean active, boolean hover) {
        if (active) {
            btn.setStyle(
                "-fx-padding: 11 12;" +
                "-fx-background-radius: 14;" +
                "-fx-background-color: linear-gradient(to right, rgba(124,58,237,0.14), rgba(37,99,235,0.10));" +
                "-fx-text-fill: " + PRIMARY_BLUE + ";" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
            );
            icon.setOpacity(1.0);
        } else {
            btn.setStyle(
                "-fx-padding: 11 12;" +
                "-fx-background-radius: 14;" +
                "-fx-background-color: " + (hover ? "#F6F8FC" : "transparent") + ";" +
                "-fx-text-fill: " + TEXT_MAIN + ";" +
                "-fx-cursor: hand;"
            );
            icon.setOpacity(hover ? 0.95 : 0.72);
        }
    }

    private void bindViewModel() {
        viewModel.usedStorageBytesProperty().addListener((obs, oldVal, newVal) -> {
            long usedBytes = newVal.longValue();
            long maxBytes = 15L * 1024 * 1024 * 1024;
            double ratio = (double) usedBytes / maxBytes;
            pbStorage.setProgress(ratio);
            lblStorageText.setText("Đã dùng " + DriveFormatUtils.formatFileSize(usedBytes) + " / 15 GB");
            pbStorage.setStyle("-fx-accent: " + (ratio > 0.9 ? "#EF4444" : BRAND_PURPLE) + ";");
        });
    }

    private void buildFolderTree(TreeItem<DriveFileModel> parentItem, Integer parentId) {
        viewModel.getDriveService().getFiles(viewModel.getCurrentUserId(), parentId).thenAccept(files -> {
            Platform.runLater(() -> {
                for (DriveFileModel f : files) {
                    if ("folder".equalsIgnoreCase(f.getFileType())) {
                        TreeItem<DriveFileModel> child = new TreeItem<>(f);
                        parentItem.getChildren().add(child);
                        if (f.getChildCount() > 0) {
                            child.getChildren().add(new TreeItem<>(new DriveFileModel()));
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
