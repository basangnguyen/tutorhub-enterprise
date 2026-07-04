package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;
import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveFormatUtils;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * Vùng nội dung chính hiển thị tài liệu theo dạng lưới hoặc danh sách.
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

    private static final String TEXT_MUTED = "#64748B";
    private static final String TEXT_MAIN = "#111827";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5EBF5";
    private static final String PRIMARY_BG = "#EEF2FF";
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
        this.setStyle("-fx-background-color: transparent;");

        mainGrid = new FlowPane();
        mainGrid.setHgap(18);
        mainGrid.setVgap(18);
        mainGrid.setPadding(new Insets(22, 26, 26, 26));

        gridScroll = new ScrollPane(mainGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        listTable = new TableView<>();
        listTable.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent;");
        listTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        listTable.getStyleClass().add("drive-table");

        TableColumn<DriveFileModel, String> colName = new TableColumn<>("Tên tài liệu");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(col -> new TableCell<DriveFileModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    DriveFileModel file = getTableRow().getItem();
                    if (file != null) {
                        HBox box = new HBox(12);
                        box.setAlignment(Pos.CENTER_LEFT);
                        javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView();
                        String iconPath = getIconUrl(file.getFileType());
                        int optimalSize = getOptimalIconSize(iconPath, 24);
                        javafx.scene.image.Image img = loadIconImage(iconPath, 24);
                        if (img != null) icon.setImage(img);
                        icon.setFitWidth(optimalSize);
                        icon.setFitHeight(optimalSize);
                        Label lbl = new Label(item);
                        lbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                        lbl.setTextFill(Color.web(TEXT_MAIN));
                        box.getChildren().addAll(icon, lbl);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
        });

        TableColumn<DriveFileModel, String> colType = new TableColumn<>("Loại");
        colType.setCellValueFactory(new PropertyValueFactory<>("fileType"));

        TableColumn<DriveFileModel, Long> colSize = new TableColumn<>("Kích thước");
        colSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        colSize.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item > 0 ? DriveFormatUtils.formatFileSize(item) : "-"));
            }
        });

        listTable.getColumns().addAll(colName, colType, colSize);
        listTable.setVisible(false);
        listTable.setManaged(false);

        listTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                viewModel.selectFile(newVal, false);
            }
        });

        listTable.setRowFactory(tv -> {
            TableRow<DriveFileModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    DriveFileModel rowData = row.getItem();
                    if ("folder".equalsIgnoreCase(rowData.getFileType())) {
                        viewModel.openFolder(rowData);
                    } else if (previewFileAction != null) {
                        previewFileAction.accept(rowData);
                    }
                }
            });
            return row;
        });

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(44, 44);
        loadingSpinner.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");
        Label loadingLabel = new Label("Đang tải tài liệu...");
        loadingLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        loadingLabel.setTextFill(Color.web(TEXT_MUTED));
        loadingBox = new VBox(12, loadingSpinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(80));
        loadingBox.setVisible(false);

        this.getChildren().addAll(gridScroll, listTable, loadingBox);
    }

    public void setGridView(boolean isGrid) {
        this.isGridView = isGrid;
        gridScroll.setVisible(isGrid);
        gridScroll.setManaged(isGrid);
        listTable.setVisible(!isGrid);
        listTable.setManaged(!isGrid);
    }

    private void bindViewModel() {
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

        viewModel.getFiles().addListener((ListChangeListener<DriveFileModel>) c -> Platform.runLater(this::renderFiles));
        viewModel.getSelectedFileIds().addListener((javafx.collections.SetChangeListener<Integer>) c -> Platform.runLater(this::updateCardStyles));
    }

    private void renderFiles() {
        mainGrid.getChildren().clear();
        listTable.getItems().clear();

        if (viewModel.getFiles().isEmpty()) {
            mainGrid.getChildren().add(createEmptyState());
            return;
        }

        for (int i = 0; i < viewModel.getFiles().size(); i++) {
            DriveFileModel file = viewModel.getFiles().get(i);
            VBox card = createFileGridCard(file);

            card.setOpacity(0);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), card);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setDelay(javafx.util.Duration.millis(i * 25L));
            fadeIn.play();

            mainGrid.getChildren().add(card);
            listTable.getItems().add(file);
        }
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48, 64, 48, 64));
        emptyState.setPrefWidth(520);
        emptyState.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: #E5EBF5;" +
            "-fx-border-radius: 22;"
        );

        String viewMode = viewModel.currentViewModeProperty().get();
        javafx.scene.Node emptyIconNode;
        String title;
        String hint;
        if ("trash".equals(viewMode)) {
            emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.TRASH, 46, "#94A3B8");
            title = "Thùng rác trống";
            hint = "Các file đã xóa sẽ xuất hiện tại đây.";
        } else if ("starred".equals(viewMode)) {
            emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.STARRED, 46, "#F59E0B");
            title = "Chưa có mục gắn sao";
            hint = "Gắn sao các tài liệu quan trọng để truy cập nhanh.";
        } else {
            emptyIconNode = DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FOLDER_OPEN, 52, TEXT_MUTED);
            title = "Thư mục đang trống";
            hint = "Kéo thả file vào đây hoặc bấm Tải lên để bắt đầu.";
        }

        Label emptyText = new Label(title);
        emptyText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        emptyText.setTextFill(Color.web(TEXT_MAIN));
        Label emptyHint = new Label(hint);
        emptyHint.setFont(Font.font("Segoe UI", 13));
        emptyHint.setTextFill(Color.web(TEXT_MUTED));
        emptyState.getChildren().addAll(emptyIconNode, emptyText, emptyHint);
        return emptyState;
    }

    private VBox createFileGridCard(DriveFileModel file) {
        VBox card = new VBox(10);
        card.setPrefSize(206, 218);
        card.setMinSize(206, 218);
        card.setMaxSize(206, 218);
        card.setUserData(file);
        applyCardStyle(card, file, false);

        StackPane thumb = new StackPane();
        thumb.setPrefHeight(112);
        thumb.setMinHeight(112);
        thumb.setMaxHeight(112);
        VBox.setMargin(thumb, new Insets(8, 8, 0, 8));
        thumb.setStyle(
            "-fx-background-color: " + thumbnailBackground(file) + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(255,255,255,0.70);" +
            "-fx-border-radius: 16;"
        );

        String iconUrl = getIconUrl(file.getFileType());
        boolean isImage = safeType(file).equals("jpg") || safeType(file).equals("png") || safeType(file).equals("jpeg") || file.getName().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp)$");
        if (isImage && file.getFileUrl() != null) {
            if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                javafx.scene.image.ImageView thumbView = new javafx.scene.image.ImageView();
                addIconToThumb(thumb, iconUrl);
                thumbView.setFitWidth(206);
                thumbView.setFitHeight(112);
                thumbView.setPreserveRatio(false);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(206, 112);
                clip.setArcWidth(22); clip.setArcHeight(22);
                thumbView.setClip(clip);
                thumb.getChildren().add(thumbView);
                loadCloudThumbnailAsync(file, 206, 112, thumbView);
            } else {
                try {
                    java.io.File imgFile = new java.io.File(file.getFileUrl());
                    if (imgFile.exists()) {
                        javafx.scene.image.Image thumbImg = getCachedThumbnail(file.getFileUrl(), 206, 112);
                        if (thumbImg != null) {
                            javafx.scene.image.ImageView thumbView = new javafx.scene.image.ImageView(thumbImg);
                            thumbView.setFitWidth(206);
                            thumbView.setFitHeight(112);
                            thumbView.setPreserveRatio(false);
                            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(206, 112);
                            clip.setArcWidth(22); clip.setArcHeight(22);
                            thumbView.setClip(clip);
                            thumb.getChildren().add(thumbView);
                        } else {
                            addIconToThumb(thumb, iconUrl);
                        }
                    } else {
                        addIconToThumb(thumb, iconUrl);
                    }
                } catch (Exception ex) {
                    addIconToThumb(thumb, iconUrl);
                }
            }
        } else {
            addIconToThumb(thumb, iconUrl);
        }

        Label typeBadge = new Label(typeBadge(file));
        typeBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        typeBadge.setTextFill(Color.web(badgeColor(file)));
        typeBadge.setStyle("-fx-background-color: rgba(255,255,255,0.86); -fx-background-radius: 999; -fx-padding: 4 9;");
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(10));
        thumb.getChildren().add(typeBadge);

        Button btnStar = new Button(viewModel.getCurrentStarredIds().contains(file.getFileId()) ? "★" : "☆");
        btnStar.setStyle(
            "-fx-background-color: rgba(255,255,255,0.90);" +
            "-fx-background-radius: 999;" +
            "-fx-text-fill: " + (viewModel.getCurrentStarredIds().contains(file.getFileId()) ? "#F59E0B" : "#94A3B8") + ";" +
            "-fx-font-size: 14;" +
            "-fx-padding: 2 7;" +
            "-fx-cursor: hand;"
        );
        btnStar.setOnAction(e -> viewModel.toggleStar(file));
        StackPane.setAlignment(btnStar, Pos.TOP_RIGHT);
        StackPane.setMargin(btnStar, new Insets(9));
        thumb.getChildren().add(btnStar);

        VBox info = new VBox(7);
        info.setPadding(new Insets(0, 12, 12, 12));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);
        Label lblName = new Label(file.getName());
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(TEXT_MAIN));
        lblName.setWrapText(true);
        lblName.setPrefHeight(38);
        lblName.setMaxWidth(146);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button btnMenu = new Button("⋯");
        btnMenu.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 2;");
        titleRow.getChildren().addAll(lblName, titleSpacer, btnMenu);

        HBox subRow = new HBox(6);
        subRow.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.image.ImageView typeSmall = new javafx.scene.image.ImageView();
        int optimalSizeSmall = getOptimalIconSize(iconUrl, 14);
        javafx.scene.image.Image smallImg = loadIconImage(iconUrl, 14);
        if (smallImg != null) typeSmall.setImage(smallImg);
        typeSmall.setFitWidth(optimalSizeSmall);
        typeSmall.setFitHeight(optimalSizeSmall);

        Label lblSub = new Label(subText(file));
        lblSub.setFont(Font.font("Segoe UI", 11));
        lblSub.setTextFill(Color.web(TEXT_MUTED));
        lblSub.setTextOverrun(OverrunStyle.ELLIPSIS);
        lblSub.setMaxWidth(160);
        subRow.getChildren().addAll(typeSmall, lblSub);
        info.getChildren().addAll(titleRow, subRow);

        card.getChildren().addAll(thumb, info);

        card.setOnMouseEntered(e -> {
            if (!viewModel.getSelectedFileIds().contains(file.getFileId())) {
                applyCardStyle(card, file, true);
                card.setTranslateY(-2);
            }
        });
        card.setOnMouseExited(e -> {
            applyCardStyle(card, file, false);
            card.setTranslateY(0);
        });

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if ("folder".equalsIgnoreCase(file.getFileType())) {
                    viewModel.openFolder(file);
                } else if (previewFileAction != null) {
                    previewFileAction.accept(file);
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

    private void applyCardStyle(VBox card, DriveFileModel file, boolean hover) {
        boolean selected = viewModel.getSelectedFileIds().contains(file.getFileId());
        String border = selected ? PRIMARY_BLUE : (hover ? "#CBD5E1" : BORDER_COLOR);
        String background = selected ? PRIMARY_BG : BG_WHITE;
        String shadow = selected
            ? "dropshadow(gaussian, rgba(37,99,235,0.18), 18, 0, 0, 6)"
            : (hover ? "dropshadow(gaussian, rgba(15,23,42,0.12), 22, 0, 0, 8)" : "dropshadow(gaussian, rgba(15,23,42,0.05), 14, 0, 0, 4)");
        card.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: " + (selected ? "1.6" : "1") + ";" +
            "-fx-effect: " + shadow + ";" +
            "-fx-cursor: hand;"
        );
    }

    private String thumbnailBackground(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return "linear-gradient(to bottom right, #E0F2FE, #EDE9FE)";
        if ("pdf".equals(type)) return "linear-gradient(to bottom right, #FEE2E2, #FFF1F2)";
        if ("video".equals(type) || "mp4".equals(type)) return "linear-gradient(to bottom right, #E0E7FF, #F5F3FF)";
        if ("ppt".equals(type) || "pptx".equals(type) || "slide".equals(type)) return "linear-gradient(to bottom right, #FFEDD5, #FEF3C7)";
        if ("xlsx".equals(type) || "excel".equals(type)) return "linear-gradient(to bottom right, #DCFCE7, #ECFDF5)";
        return "linear-gradient(to bottom right, #F1F5F9, #F8FAFC)";
    }

    private String iconForFile(DriveFileModel file) {
        String type = safeType(file);
        String name = file.getName() == null ? "" : file.getName().toLowerCase();
        if ("folder".equals(type)) return DriveSvgIcons.FOLDER;
        if ("pdf".equals(type) || name.endsWith(".pdf")) return DriveSvgIcons.PDF;
        if ("video".equals(type) || "mp4".equals(type) || name.endsWith(".mp4")) return DriveSvgIcons.FILE_VIDEO;
        if ("doc".equals(type) || "docx".equals(type) || name.endsWith(".docx")) return DriveSvgIcons.WORD;
        if ("excel".equals(type) || "xlsx".equals(type) || name.endsWith(".xlsx")) return DriveSvgIcons.EXCEL;
        return DriveSvgIcons.FILE;
    }

    private String iconColor(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return "#2563EB";
        if ("pdf".equals(type)) return "#EF4444";
        if ("video".equals(type) || "mp4".equals(type)) return "#6366F1";
        if ("xlsx".equals(type) || "excel".equals(type)) return "#10B981";
        if ("ppt".equals(type) || "pptx".equals(type) || "slide".equals(type)) return "#F97316";
        return "#64748B";
    }

    private String badgeColor(DriveFileModel file) {
        return "folder".equals(safeType(file)) ? "#2563EB" : "#475569";
    }

    private String typeBadge(DriveFileModel file) {
        String type = safeType(file);
        if ("folder".equals(type)) return "THƯ MỤC";
        if (type.isEmpty()) return "FILE";
        return type.toUpperCase();
    }

    private String subText(DriveFileModel file) {
        String timeStr = DriveFormatUtils.formatRelativeTime(file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt());
        if ("folder".equalsIgnoreCase(file.getFileType())) {
            return file.getChildCount() + " mục · " + timeStr;
        }
        String sizeStr = file.getFileSize() > 0 ? DriveFormatUtils.formatFileSize(file.getFileSize()) : "";
        return (sizeStr.isEmpty() ? "" : sizeStr + " · ") + timeStr;
    }

    private String safeType(DriveFileModel file) {
        return file.getFileType() == null ? "" : file.getFileType().toLowerCase();
    }

    private void updateCardStyles() {
        for (javafx.scene.Node n : mainGrid.getChildren()) {
            if (n instanceof VBox) {
                VBox card = (VBox) n;
                Object fileObj = card.getUserData();
                if (fileObj instanceof DriveFileModel) {
                    applyCardStyle(card, (DriveFileModel) fileObj, false);
                }
            }
        }
    }

    private ContextMenu createContextMenu(DriveFileModel file) {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("folder".equalsIgnoreCase(file.getFileType()) ? "Mở thư mục" : "Xem trước");
        openItem.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                viewModel.openFolder(file);
            } else if (previewFileAction != null) {
                previewFileAction.accept(file);
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
        trashItem.setStyle("-fx-text-fill: #EF4444;");
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
            if (contextMenuDownloadAction != null) {
                contextMenuDownloadAction.accept(file);
            }
        });

        if ("trash".equals(viewModel.currentViewModeProperty().get())) {
            MenuItem restoreItem = new MenuItem("Khôi phục");
            restoreItem.setOnAction(e -> {
                viewModel.getDriveService().restoreFromTrash(file.getFileId()).thenAccept(s -> {
                    Platform.runLater(viewModel::loadFiles);
                    broadcastSync();
                });
            });
            MenuItem permDeleteItem = new MenuItem("Xóa vĩnh viễn");
            permDeleteItem.setStyle("-fx-text-fill: #EF4444;");
            permDeleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa vĩnh viễn " + file.getName() + "? Hành động này không thể hoàn tác.", ButtonType.YES, ButtonType.NO);
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

    private final java.util.Map<String, java.lang.ref.SoftReference<javafx.scene.image.Image>> thumbnailCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String getIconUrl(String type) {
        switch (type.toLowerCase()) {
            case "pdf": return "images/icon/pdf-2616.svg";
            case "document": case "doc": case "docx": return "images/icon/microsoft-word-icon.svg";
            case "video": case "mp4": return "images/icon/MP4.svg";
            case "excel": case "xlsx": return "images/icon/excel2-svgrepo-com.svg";
            case "slide": case "ppt": case "pptx": return "images/icon/microsoft-powerpoint-icon.svg";
            case "folder": return "images/icon/folder-1484.svg";
            default: return "images/icon/file_document.png";
        }
    }

    private int getOptimalIconSize(String iconPath, int baseSize) {
        if (iconPath == null) return baseSize;
        String pathLower = iconPath.toLowerCase();
        if (pathLower.contains("folder")) {
            return (int) (baseSize * 1.35); // Scale up folder by 35% to compensate for SVG padding
        }
        if (pathLower.contains("microsoft-word") || pathLower.contains("word")) {
            return (int) (baseSize * 0.82); // Scale down Word to fit
        }
        if (pathLower.contains("pdf")) {
            return (int) (baseSize * 0.85); // Scale down PDF to fit
        }
        if (pathLower.contains("excel") || pathLower.contains("xlsx")) {
            return (int) (baseSize * 0.88); // Scale down Excel to fit
        }
        if (pathLower.contains("powerpoint") || pathLower.contains("microsoft-powerpoint-icon")) {
            return (int) (baseSize * 0.88); // Scale down PPT to fit
        }
        if (pathLower.contains("mp4")) {
            return (int) (baseSize * 0.9); // Scale down MP4 to fit
        }
        return baseSize;
    }

    private javafx.scene.image.Image loadIconImage(String iconPath, int size) {
        int optimalSize = getOptimalIconSize(iconPath, size);
        if (iconPath.endsWith(".svg")) {
            return com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory.loadSvgImage(iconPath, optimalSize);
        } else {
            try {
                java.net.URL url = getClass().getResource("/" + iconPath);
                if (url != null) return new javafx.scene.image.Image(url.toExternalForm(), optimalSize, optimalSize, true, true);
            } catch (Exception e) {}
            return null;
        }
    }

    private void addIconToThumb(StackPane thumb, String iconUrl) {
        javafx.scene.image.ImageView centerIcon = new javafx.scene.image.ImageView();
        int optimalSize = getOptimalIconSize(iconUrl, 64);
        javafx.scene.image.Image img = loadIconImage(iconUrl, 64);
        if (img != null) centerIcon.setImage(img);
        centerIcon.setFitWidth(optimalSize); centerIcon.setFitHeight(optimalSize);
        thumb.getChildren().add(centerIcon);
    }

    private javafx.scene.image.Image getCachedThumbnail(String filePath, double width, double height) {
        java.lang.ref.SoftReference<javafx.scene.image.Image> ref = thumbnailCache.get(filePath);
        if (ref != null) {
            javafx.scene.image.Image cached = ref.get();
            if (cached != null) return cached;
        }
        try {
            java.io.File imgFile = new java.io.File(filePath);
            if (imgFile.exists()) {
                javafx.scene.image.Image img = new javafx.scene.image.Image(imgFile.toURI().toString(), width, height, false, true, true);
                thumbnailCache.put(filePath, new java.lang.ref.SoftReference<>(img));
                return img;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void loadCloudThumbnailAsync(DriveFileModel file, double width, double height, javafx.scene.image.ImageView targetView) {
        String cacheKey = "cloud_" + file.getFileId();
        java.lang.ref.SoftReference<javafx.scene.image.Image> ref = thumbnailCache.get(cacheKey);
        if (ref != null) {
            javafx.scene.image.Image cached = ref.get();
            if (cached != null) {
                targetView.setImage(cached);
                return;
            }
        }
        
        javafx.concurrent.Task<javafx.scene.image.Image> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected javafx.scene.image.Image call() throws Exception {
                com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                if (is != null) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(is, width, height, false, true);
                    is.close();
                    return img;
                }
                return null;
            }
        };
        loadTask.setOnSucceeded(e -> {
            javafx.scene.image.Image img = loadTask.getValue();
            if (img != null) {
                thumbnailCache.put(cacheKey, new java.lang.ref.SoftReference<>(img));
                targetView.setImage(img);
            }
        });
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }
}
