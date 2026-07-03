package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
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
import javafx.collections.ListChangeListener;
import javafx.application.Platform;
import java.util.function.Consumer;

/**
 * Thành phần giao diện thanh công cụ (Header Toolbar).
 * Chứa Breadcrumbs (Đường dẫn thư mục) và các nút thao tác: Tạo mới, Tải lên, Tìm kiếm, Bật/Tắt chi tiết.
 */
public class HeaderToolbarView extends VBox {
    private final DriveViewModel viewModel;
    private final Consumer<java.util.List<java.io.File>> uploadAction;
    private final Runnable toggleRightSidebarAction;
    private final Consumer<Boolean> toggleViewModeAction;
    
    private HBox breadcrumbContainer;
    
    private static final String TEXT_MUTED = "#6B7280";
    private static final String TEXT_MAIN = "#1F2937";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BORDER_COLOR = "#E5E7EB";

    public HeaderToolbarView(DriveViewModel viewModel, 
                             Consumer<java.util.List<java.io.File>> uploadAction,
                             Runnable toggleRightSidebarAction,
                             Consumer<Boolean> toggleViewModeAction) {
        this.viewModel = viewModel;
        this.uploadAction = uploadAction;
        this.toggleRightSidebarAction = toggleRightSidebarAction;
        this.toggleViewModeAction = toggleViewModeAction;
        
        setupUI();
        bindViewModel();
    }

    private void setupUI() {
        this.setPadding(new Insets(24, 30, 15, 30));
        this.setStyle("-fx-background-color: #FFFFFF;");
        this.setSpacing(15);

        breadcrumbContainer = new HBox(8);
        breadcrumbContainer.setAlignment(Pos.CENTER_LEFT);
        renderBreadcrumbs();

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // --- Nút Tạo Mới ---
        MenuButton btnCreate = new MenuButton("Tạo mới");
        btnCreate.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.PLUS, 16, "#FFFFFF"));
        btnCreate.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");

        MenuItem itemCreateFolder = new MenuItem("Tạo thư mục");
        itemCreateFolder.setOnAction(e -> handleCreateFolder());
        MenuItem itemCreateDoc = new MenuItem("Tạo tài liệu");
        MenuItem itemCreateSlide = new MenuItem("Tạo slide");
        MenuItem itemCreateVideo = new MenuItem("Tạo bài giảng");
        btnCreate.getItems().addAll(itemCreateFolder, itemCreateDoc, itemCreateSlide, itemCreateVideo);

        // --- Nút Tải Lên ---
        MenuButton btnUpload = new MenuButton("Tải lên");
        // Giả sử có một icon DOWNLOAD hoặc UPLOAD thay thế, dùng tạm NEW_FOLDER nếu icon không sẵn có
        btnUpload.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.FOLDER_OPEN, 20, TEXT_MAIN));
        btnUpload.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");

        MenuItem itemUploadFile = new MenuItem("Tải file lên");
        itemUploadFile.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn file để tải lên");
            java.util.List<java.io.File> selectedFiles = fileChooser.showOpenMultipleDialog(this.getScene().getWindow());
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                if (uploadAction != null) uploadAction.accept(selectedFiles);
            }
        });
        
        MenuItem itemUploadFolder = new MenuItem("Tải thư mục lên");
        itemUploadFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Chọn thư mục tải lên");
            java.io.File selectedDir = dirChooser.showDialog(this.getScene().getWindow());
            if (selectedDir != null) {
                java.util.List<java.io.File> filesList = new java.util.ArrayList<>();
                java.io.File[] files = selectedDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.isFile()) filesList.add(f);
                    }
                }
                if (!filesList.isEmpty()) {
                    if (uploadAction != null) uploadAction.accept(filesList);
                } else {
                    new Alert(Alert.AlertType.INFORMATION, "Thư mục trống hoặc không có file hợp lệ.").showAndWait();
                }
            }
        });
        btnUpload.getItems().addAll(itemUploadFile, itemUploadFolder, new MenuItem("Nhập từ Google Drive"));

        // --- Ô Nhập Tìm Kiếm ---
        TextField searchInput = new TextField();
        searchInput.setPromptText("🔍 Tìm kiếm trong Drive...");
        searchInput.setPrefWidth(220);
        searchInput.setStyle("-fx-background-color: #F3F4F6; -fx-border-color: transparent; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 12;");
        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> {
                String keyword = newVal == null ? "" : newVal.trim();
                if(!keyword.isEmpty()) {
                     viewModel.getDriveService().searchFiles(viewModel.getCurrentUserId(), keyword).thenAccept(res -> {
                         Platform.runLater(() -> viewModel.getFiles().setAll(res));
                     });
                } else {
                     viewModel.loadFiles();
                }
            });
            debounce.playFromStart();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Combobox Lọc Loại và Sắp Xếp ---
        ComboBox<String> cbType = createFilterDropdown("Loại: Tất cả", "PDF", "Word", "Video", "Excel", "Thư mục");
        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            String filter = null;
            if ("PDF".equals(newVal)) filter = "pdf";
            else if ("Word".equals(newVal)) filter = "docx";
            else if ("Video".equals(newVal)) filter = "mp4";
            else if ("Excel".equals(newVal)) filter = "xlsx";
            else if ("Thư mục".equals(newVal)) filter = "folder";
            
            viewModel.getDriveService().getFilesFiltered(viewModel.getCurrentUserId(), viewModel.currentFolderIdProperty().get(), filter, "newest").thenAccept(res -> {
                Platform.runLater(() -> viewModel.getFiles().setAll(res));
            });
        });

        ComboBox<String> cbSort = createFilterDropdown("Sắp xếp: Mới nhất", "Cũ nhất", "Tên A-Z", "Dung lượng");

        // --- Chuyển đổi Giao Diện Lưới (Grid) / Danh Sách (List) ---
        HBox viewToggle = new HBox();
        Button btnGrid = new Button(); 
        btnGrid.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.GRID, 18, PRIMARY_BLUE));
        btnGrid.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");

        Button btnList = new Button(); 
        btnList.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.LIST, 18, TEXT_MUTED));
        btnList.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");

        btnGrid.setOnAction(e -> {
            btnGrid.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");
            ((SVGPath)btnGrid.getGraphic()).setFill(Color.web(PRIMARY_BLUE));
            btnList.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");
            ((SVGPath)btnList.getGraphic()).setFill(Color.web(TEXT_MUTED));
            if(toggleViewModeAction != null) toggleViewModeAction.accept(true);
        });

        btnList.setOnAction(e -> {
            btnList.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");
            ((SVGPath)btnList.getGraphic()).setFill(Color.web(PRIMARY_BLUE));
            btnGrid.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");
            ((SVGPath)btnGrid.getGraphic()).setFill(Color.web(TEXT_MUTED));
            if(toggleViewModeAction != null) toggleViewModeAction.accept(false);
        });

        viewToggle.getChildren().addAll(btnGrid, btnList);

        // --- Nút Thông Tin (Info) ---
        Button btnInfo = new Button("ⓘ");
        btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 16; -fx-cursor: hand;");
        btnInfo.setOnAction(e -> {
            if (toggleRightSidebarAction != null) toggleRightSidebarAction.run();
        });

        toolbar.getChildren().addAll(btnCreate, btnUpload, searchInput, spacer, cbType, cbSort, viewToggle, btnInfo);

        this.getChildren().addAll(breadcrumbContainer, toolbar, new Separator());
    }

    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog("Thư mục không tên");
        dialog.setTitle("Tạo thư mục mới");
        dialog.setHeaderText("Nhập tên cho thư mục mới:");
        dialog.setContentText("Tên thư mục:");
        dialog.showAndWait().ifPresent(folderName -> {
            String cleanName = folderName.trim();
            if (cleanName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Tên thư mục không được để trống!").showAndWait();
                return;
            }
            DriveFileModel newFolder = new DriveFileModel();
            newFolder.setName(cleanName);
            newFolder.setFileType("folder");
            newFolder.setFileSize(0);
            newFolder.setOwnerId(viewModel.getCurrentUserId());
            newFolder.setSourceLocation("my_drive");
            newFolder.setParentId(viewModel.currentFolderIdProperty().get());
            newFolder.setStatus("active");
            
            viewModel.getDriveService().insertFile(newFolder).thenAccept(success -> {
                if (success) {
                    Platform.runLater(viewModel::loadFiles);
                } else {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Lỗi! Không thể tạo thư mục vào lúc này.").showAndWait());
                }
            });
        });
    }

    private ComboBox<String> createFilterDropdown(String defaultItem, String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add(defaultItem);
        cb.getItems().addAll(items);
        cb.getSelectionModel().selectFirst();
        cb.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-text-fill: " + TEXT_MAIN + ";");
        return cb;
    }

    private void bindViewModel() {
        // Lắng nghe sự thay đổi của Breadcrumbs từ ViewModel
        viewModel.getBreadcrumbs().addListener((ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(this::renderBreadcrumbs);
        });
    }

    private void renderBreadcrumbs() {
        breadcrumbContainer.getChildren().clear();
        
        Label lblRoot = new Label("Tài liệu");
        lblRoot.setFont(Font.font("System", viewModel.getBreadcrumbs().isEmpty() ? FontWeight.BOLD : FontWeight.NORMAL, 24));
        lblRoot.setTextFill(Color.web(viewModel.getBreadcrumbs().isEmpty() ? TEXT_MAIN : TEXT_MUTED));
        lblRoot.setCursor(javafx.scene.Cursor.HAND);
        lblRoot.setOnMouseClicked(e -> {
            viewModel.navigateHome();
        });
        
        breadcrumbContainer.getChildren().add(lblRoot);
        
        for (int i = 0; i < viewModel.getBreadcrumbs().size(); i++) {
            DriveFileModel folder = viewModel.getBreadcrumbs().get(i);
            Label separator = new Label(" > ");
            separator.setFont(Font.font("System", FontWeight.BOLD, 18));
            separator.setTextFill(Color.web(TEXT_MUTED));
            
            boolean isLast = (i == viewModel.getBreadcrumbs().size() - 1);
            Label lblFolder = new Label(folder.getName());
            lblFolder.setFont(Font.font("System", isLast ? FontWeight.BOLD : FontWeight.NORMAL, isLast ? 24 : 20));
            lblFolder.setTextFill(Color.web(isLast ? TEXT_MAIN : TEXT_MUTED));
            
            if (!isLast) {
                lblFolder.setCursor(javafx.scene.Cursor.HAND);
                final int index = i;
                lblFolder.setOnMouseClicked(e -> {
                    viewModel.getBreadcrumbs().subList(index + 1, viewModel.getBreadcrumbs().size()).clear();
                    viewModel.currentFolderIdProperty().set(folder.getFileId());
                    viewModel.loadFiles();
                });
            }
            breadcrumbContainer.getChildren().addAll(separator, lblFolder);
        }
    }
}
