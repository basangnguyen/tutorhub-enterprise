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
import javafx.collections.ListChangeListener;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.function.Consumer;

/**
 * Thành phần giao diện vùng chính hiển thị các file/thư mục.
 * Hỗ trợ chuyển đổi giữa chế độ Grid (Lưới) và Table (Danh sách).
 * Xử lý Drag & Drop, Select, Multi-select và Menu Chuột Phải.
 */
public class MainContentView extends StackPane {
    private final DriveViewModel viewModel;
    private final Consumer<DriveFileModel> previewFileAction;
    private final Consumer<DriveFileModel> contextMenuDownloadAction;

    private boolean isGridView = true;

    private FlowPane mainGrid;
    private ScrollPane gridScroll;
    private TableView<DriveFileModel> listTable;
    private ProgressIndicator loadingSpinner;
    private VBox loadingBox;

    private static final String TEXT_MUTED = "#6B7280";
    private static final String TEXT_MAIN = "#1F2937";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5E7EB";
    private static final String PRIMARY_BG = "#EFF6FF";
    private static final String BG_WHITE = "#FFFFFF";

    public MainContentView(DriveViewModel viewModel, 
                           Consumer<DriveFileModel> previewFileAction,
                           Consumer<DriveFileModel> contextMenuDownloadAction) {
        this.viewModel = viewModel;
        this.previewFileAction = previewFileAction;
        this.contextMenuDownloadAction = contextMenuDownloadAction;

        setupUI();
        bindViewModel();
    }

    private void setupUI() {
        // --- 1. Chế độ lưới (Grid) ---
        mainGrid = new FlowPane();
        mainGrid.setHgap(20);
        mainGrid.setVgap(20);
        mainGrid.setPadding(new Insets(20));

        gridScroll = new ScrollPane(mainGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // --- 2. Chế độ danh sách (Table) ---
        listTable = new TableView<>();
        listTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        listTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DriveFileModel, String> colName = new TableColumn<>("Tên");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<DriveFileModel, String> colType = new TableColumn<>("Loại");
        colType.setCellValueFactory(new PropertyValueFactory<>("fileType"));

        TableColumn<DriveFileModel, Long> colSize = new TableColumn<>("Kích thước");
        colSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        colSize.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item > 0 ? DriveFormatUtils.formatFileSize(item) : "-");
                }
            }
        });

        listTable.getColumns().addAll(colName, colType, colSize);
        listTable.setVisible(false);
        listTable.setManaged(false);
        
        // Hỗ trợ chọn file trong TableView
        listTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                viewModel.selectFile(newVal, false); // Chọn đơn lẻ trên Table
            }
        });
        
        listTable.setRowFactory(tv -> {
            TableRow<DriveFileModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    DriveFileModel rowData = row.getItem();
                    if (rowData.getFileType().equalsIgnoreCase("folder")) {
                        viewModel.openFolder(rowData);
                    } else {
                        if (previewFileAction != null) previewFileAction.accept(rowData);
                    }
                }
            });
            return row;
        });

        // --- 3. Vòng tròn tải (Loading Indicator) ---
        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(48, 48);
        loadingSpinner.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");
        loadingBox = new VBox(12, loadingSpinner, new Label("Đang tải dữ liệu..."));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(80));
        ((Label) loadingBox.getChildren().get(1)).setTextFill(Color.web(TEXT_MUTED));
        loadingBox.setVisible(false);

        // Chồng tất cả vào StackPane
        this.getChildren().addAll(gridScroll, listTable, loadingBox);
    }

    /**
     * Bật tắt giao diện theo ViewMode được truyền vào từ HeaderToolbar
     */
    public void setGridView(boolean isGrid) {
        this.isGridView = isGrid;
        if (isGrid) {
            gridScroll.setVisible(true);
            gridScroll.setManaged(true);
            listTable.setVisible(false);
            listTable.setManaged(false);
        } else {
            gridScroll.setVisible(false);
            gridScroll.setManaged(false);
            listTable.setVisible(true);
            listTable.setManaged(true);
        }
    }

    private void bindViewModel() {
        // Lắng nghe trạng thái đang load của ViewModel để hiện vòng quay
        viewModel.isLoadingProperty().addListener((obs, oldVal, isLoading) -> {
            Platform.runLater(() -> {
                if (isLoading) {
                    mainGrid.getChildren().clear();
                    listTable.getItems().clear();
                    loadingBox.setVisible(true);
                } else {
                    loadingBox.setVisible(false);
                }
            });
        });

        // Khi ViewModel cập nhật dữ liệu, vẽ lại màn hình
        viewModel.getFiles().addListener((ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(this::renderFiles);
        });

        // Khi danh sách File đang chọn (Selected) thay đổi, cập nhật màu viền
        viewModel.getSelectedFileIds().addListener((javafx.collections.SetChangeListener<Integer>) c -> {
            Platform.runLater(this::updateCardStyles);
        });
    }

    /**
     * Hàm render toàn bộ File từ ViewModel ra Grid và List Table
     */
    private void renderFiles() {
        mainGrid.getChildren().clear();
        listTable.getItems().clear();
        
        if (viewModel.getFiles().isEmpty()) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(60));
            
            String viewMode = viewModel.currentViewModeProperty().get();
            javafx.scene.Node emptyIconNode;
            if ("trash".equals(viewMode)) {
                emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.TRASH, 56, "#94A3B8");
            } else if ("starred".equals(viewMode)) {
                emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.STARRED, 56, "#FFB300");
            } else {
                emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FOLDER_OPEN, 64, TEXT_MUTED);
            }

            Label emptyText = new Label();
            if ("trash".equals(viewMode)) emptyText.setText("Thùng rác trống");
            else if ("starred".equals(viewMode)) emptyText.setText("Chưa có mục nào được gắn dấu sao");
            else emptyText.setText("Thư mục trống");
            emptyText.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
            emptyText.setTextFill(Color.web(TEXT_MUTED));

            Label emptyHint = new Label();
            if ("trash".equals(viewMode)) emptyHint.setText("Các file đã xóa sẽ xuất hiện tại đây");
            else if ("starred".equals(viewMode)) emptyHint.setText("Thêm các file quan trọng vào đây để truy cập nhanh");
            else emptyHint.setText("Kéo thả file vào đây hoặc bấm Tải lên");
            emptyHint.setTextFill(Color.web(TEXT_MUTED));
            
            emptyState.getChildren().addAll(emptyIconNode, emptyText, emptyHint);
            mainGrid.getChildren().add(emptyState);
        } else {
            for (int i = 0; i < viewModel.getFiles().size(); i++) {
                DriveFileModel file = viewModel.getFiles().get(i);
                VBox card = createFileGridCard(file);

                // Hiệu ứng mờ dần từng Thẻ
                card.setOpacity(0);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), card);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setDelay(javafx.util.Duration.millis(i * 40));
                fadeIn.play();

                mainGrid.getChildren().add(card);
                listTable.getItems().add(file);
            }
        }
    }

    /**
     * Tạo 1 thẻ bài dạng lưới cho một File
     */
    private VBox createFileGridCard(DriveFileModel file) {
        VBox card = new VBox();
        card.setPrefSize(220, 240);
        card.setUserData(file.getFileId());

        boolean isSelected = viewModel.getSelectedFileIds().contains(file.getFileId());
        String baseStyle = "-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4); ";
        if (isSelected) {
            card.setStyle(baseStyle + "-fx-border-color: " + PRIMARY_BLUE + "; -fx-border-width: 2;");
        } else {
            card.setStyle(baseStyle + "-fx-border-width: 0;");
        }

        StackPane thumb = new StackPane();
        thumb.setPrefHeight(140);
        String type = file.getFileType().toLowerCase();
        
        String bgThumb = "#F3F4F6";
        if (type.equals("pdf") || file.getName().endsWith(".pdf")) bgThumb = "#FEE2E2";
        else if (type.equals("video") || file.getName().endsWith(".mp4")) bgThumb = "#E0E7FF";
        else if (type.equals("slide") || file.getName().endsWith(".ppt")) bgThumb = "#FFEDD5";
        else if (type.equals("excel") || file.getName().endsWith(".xlsx")) bgThumb = "#DCFCE7";
        
        thumb.setStyle("-fx-background-color: " + bgThumb + "; -fx-background-radius: 11 11 0 0; -fx-border-color: transparent transparent " + BORDER_COLOR + " transparent;");
        
        String iconKey = DriveSvgIcons.FILE;
        if (type.equals("folder")) iconKey = DriveSvgIcons.FOLDER;
        if (type.equals("pdf")) iconKey = DriveSvgIcons.PDF;
        if (type.equals("video")) iconKey = DriveSvgIcons.FILE_VIDEO;
        
        javafx.scene.Node centerIcon = DriveSvgIconFactory.createSvgIcon(iconKey, 48, TEXT_MUTED);
        thumb.getChildren().add(centerIcon);

        // Nút gắn sao trực tiếp trên Thumbnail
        Button btnStar = new Button();
        boolean isStarred = viewModel.getCurrentStarredIds().contains(file.getFileId());
        btnStar.setText(isStarred ? "★" : "☆");
        btnStar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); -fx-background-radius: 50; -fx-text-fill: " + (isStarred ? "#F59E0B" : TEXT_MUTED) + "; -fx-font-size: 16; -fx-padding: 2 6; -fx-cursor: hand;");
        btnStar.setOnAction(e -> {
            viewModel.toggleStar(file);
        });
        StackPane.setAlignment(btnStar, Pos.TOP_RIGHT);
        StackPane.setMargin(btnStar, new Insets(8));
        thumb.getChildren().add(btnStar);

        // Nửa dưới hiển thị thông tin chữ
        VBox info = new VBox(8);
        info.setPadding(new Insets(12));

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.TOP_LEFT);
        Label lblName = new Label(file.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(TEXT_MAIN));
        lblName.setWrapText(true);
        lblName.setPrefHeight(40);
        lblName.setMaxWidth(160);
        
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Button btnMenu = new Button("⋮");
        btnMenu.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: bold; -fx-cursor: hand;");
        titleRow.getChildren().addAll(lblName, s, btnMenu);

        HBox subRow = new HBox(6);
        subRow.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.Node typeSmall = DriveSvgIconFactory.createSvgIcon(iconKey, 16, TEXT_MUTED);
        
        String timeStr = DriveFormatUtils.formatRelativeTime(file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt());
        String subText;
        if ("folder".equalsIgnoreCase(file.getFileType())) {
            subText = file.getChildCount() + " mục • " + timeStr;
        } else {
            String sizeStr = file.getFileSize() > 0 ? DriveFormatUtils.formatFileSize(file.getFileSize()) : "";
            subText = (sizeStr.isEmpty() ? "" : sizeStr + " • ") + timeStr;
        }
        Label lblSub = new Label(subText);
        lblSub.setFont(Font.font(11)); lblSub.setTextFill(Color.web(TEXT_MUTED));
        subRow.getChildren().addAll(typeSmall, lblSub);
        info.getChildren().addAll(titleRow, subRow);
        
        card.getChildren().addAll(thumb, info);

        // Các sự kiện nhấp chuột (Click)
        card.setOnMouseEntered(e -> {
            if (!viewModel.getSelectedFileIds().contains(file.getFileId())) {
                card.setStyle(baseStyle + "-fx-border-width: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 16, 0, 0, 6); -fx-translate-y: -2;");
            }
        });
        card.setOnMouseExited(e -> {
            if (!viewModel.getSelectedFileIds().contains(file.getFileId())) {
                card.setStyle(baseStyle + "-fx-border-width: 0;");
            }
            card.setTranslateY(0);
        });

        // Xử lý Double Click / Ctrl Click
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (file.getFileType().equalsIgnoreCase("folder")) {
                    viewModel.openFolder(file);
                } else {
                    if (previewFileAction != null) previewFileAction.accept(file);
                }
                return;
            }
            
            boolean multiSelect = e.isControlDown() || e.isMetaDown() || e.isShiftDown();
            viewModel.selectFile(file, multiSelect);
            e.consume();
        });

        ContextMenu contextMenu = createContextMenu(file);
        card.setOnContextMenuRequested(e -> {
            if (!viewModel.getSelectedFileIds().contains(file.getFileId())) {
                viewModel.selectFile(file, false);
            }
            contextMenu.show(card, e.getScreenX(), e.getScreenY());
        });

        btnMenu.setOnMouseClicked(e -> {
            if (!viewModel.getSelectedFileIds().contains(file.getFileId())) {
                viewModel.selectFile(file, false);
            }
            contextMenu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0);
            e.consume();
        });

        return card;
    }

    private void updateCardStyles() {
        String baseStyle = "-fx-background-radius: 12; -fx-padding: 12; ";
        for (javafx.scene.Node n : mainGrid.getChildren()) {
            if (n instanceof VBox) {
                VBox c = (VBox) n;
                Object idObj = c.getUserData();
                if (idObj != null && viewModel.getSelectedFileIds().contains((Integer)idObj)) {
                    c.setStyle("-fx-background-color: " + PRIMARY_BG + "; " + baseStyle + "-fx-border-color: #2563EB; -fx-border-width: 2;");
                } else {
                    c.setStyle("-fx-background-color: " + BG_WHITE + "; " + baseStyle + "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1;");
                }
            }
        }
    }

    /**
     * Tạo Menu chuột phải tự động
     */
    private ContextMenu createContextMenu(DriveFileModel file) {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem(file.getFileType().equalsIgnoreCase("folder") ? "Mở thư mục" : "Xem trước");
        openItem.setOnAction(e -> {
            if (file.getFileType().equalsIgnoreCase("folder")) {
                viewModel.openFolder(file);
            } else {
                if (previewFileAction != null) previewFileAction.accept(file);
            }
        });

        MenuItem renameItem = new MenuItem("Đổi tên");
        renameItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(file.getName());
            dialog.setTitle("Đổi tên");
            dialog.setHeaderText("Nhập tên mới:");
            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    viewModel.getDriveService().renameFile(file.getFileId(), newName.trim()).thenAccept(success -> {
                        if (success) {
                            Platform.runLater(viewModel::loadFiles);
                            broadcastSync();
                        }
                    });
                }
            });
        });

        MenuItem starItem = new MenuItem(viewModel.getCurrentStarredIds().contains(file.getFileId()) ? "Bỏ gắn sao" : "Gắn sao");
        starItem.setOnAction(e -> viewModel.toggleStar(file));
        
        MenuItem trashItem = new MenuItem("Xóa vào thùng rác");
        trashItem.setStyle("-fx-text-fill: red;");
        trashItem.setOnAction(e -> {
            viewModel.getDriveService().moveToTrash(file.getFileId()).thenAccept(success -> {
                if (success) {
                    Platform.runLater(viewModel::loadFiles);
                    broadcastSync();
                }
            });
        });

        MenuItem downloadItem = new MenuItem("Tải xuống");
        downloadItem.setOnAction(e -> {
            if (contextMenuDownloadAction != null) contextMenuDownloadAction.accept(file);
        });

        if ("trash".equals(viewModel.currentViewModeProperty().get())) {
            MenuItem restoreItem = new MenuItem("↩ Khôi phục");
            restoreItem.setOnAction(e -> {
                viewModel.getDriveService().restoreFromTrash(file.getFileId()).thenAccept(s -> {
                    Platform.runLater(viewModel::loadFiles);
                    broadcastSync();
                });
            });
            MenuItem permDeleteItem = new MenuItem("✖ Xóa vĩnh viễn");
            permDeleteItem.setStyle("-fx-text-fill: red;");
            permDeleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa vĩnh viễn " + file.getName() + "? Hành động này không thể hoàn tác!", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        viewModel.getDriveService().permanentDelete(file.getFileId()).thenAccept(s -> {
                            Platform.runLater(viewModel::loadFiles);
                            broadcastSync();
                        });
                    }
                });
            });
            menu.getItems().addAll(restoreItem, permDeleteItem);
        } else {
            menu.getItems().addAll(openItem, new SeparatorMenuItem(), downloadItem, new SeparatorMenuItem(), renameItem, starItem, new SeparatorMenuItem(), trashItem);
        }
        return menu;
    }

    private void broadcastSync() {
        try {
            com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(
                new com.mycompany.tutorhub_enterprise.models.Packet("BROADCAST", "SYNC_DRIVE_UPDATE")
            );
        } catch (Exception ex) {
            System.err.println("[SYNC ERROR] Lỗi phát sóng: " + ex.getMessage());
        }
    }
}
