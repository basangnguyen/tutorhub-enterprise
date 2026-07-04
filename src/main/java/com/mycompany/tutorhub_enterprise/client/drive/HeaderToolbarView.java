package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.DriveSvgIcons;
import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Header và toolbar cho tab Tài liệu.
 */
public class HeaderToolbarView extends VBox {
    private final DriveViewModel viewModel;
    private final Consumer<java.util.List<java.io.File>> uploadAction;
    private final Runnable toggleRightSidebarAction;
    private final Consumer<Boolean> toggleViewModeAction;

    private HBox breadcrumbContainer;
    private Button btnGrid;
    private Button btnList;

    private static final String TEXT_MUTED = "#64748B";
    private static final String TEXT_MAIN = "#111827";
    private static final String PRIMARY_BLUE = "#2563EB";
    private static final String BRAND_PURPLE = "#7C3AED";
    private static final String BORDER_COLOR = "#E5EBF5";

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
        this.setPadding(new Insets(24, 28, 18, 28));
        this.setSpacing(18);
        this.setStyle("-fx-background-color: transparent;");

        HBox topLine = new HBox(16);
        topLine.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(5);
        breadcrumbContainer = new HBox(8);
        breadcrumbContainer.setAlignment(Pos.CENTER_LEFT);
        renderBreadcrumbs();
        Label subtitle = new Label("Quản lý tài liệu học tập, bài giảng và tài nguyên chia sẻ trong TutorHub.");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web(TEXT_MUTED));
        titleBlock.getChildren().addAll(breadcrumbContainer, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        MenuButton btnCreate = createPrimaryMenuButton("+ Tạo mới");
        MenuItem itemCreateFolder = new MenuItem("Tạo thư mục");
        itemCreateFolder.setOnAction(e -> handleCreateFolder());
        btnCreate.getItems().addAll(
            itemCreateFolder,
            new MenuItem("Tạo tài liệu"),
            new MenuItem("Tạo slide"),
            new MenuItem("Tạo bài giảng")
        );

        MenuButton btnUpload = new MenuButton("Tải lên");
        String styleNormal = 
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #FCA5A5;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-border-width: 1.5;" +
            "-fx-text-fill: #DC2626;" +
            "-fx-mark-color: #DC2626;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;";
        String styleHover = 
            "-fx-background-color: #FEF2F2;" +
            "-fx-border-color: #F87171;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-border-width: 1.5;" +
            "-fx-text-fill: #DC2626;" +
            "-fx-mark-color: #DC2626;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;";
        String stylePressed = 
            "-fx-background-color: #FEE2E2;" +
            "-fx-border-color: #EF4444;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-border-width: 1.5;" +
            "-fx-text-fill: #DC2626;" +
            "-fx-mark-color: #DC2626;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;";

        btnUpload.setStyle(styleNormal);
        btnUpload.setOnMouseEntered(e -> btnUpload.setStyle(styleHover));
        btnUpload.setOnMouseExited(e -> btnUpload.setStyle(styleNormal));

        javafx.scene.image.Image upImg = com.mycompany.tutorhub_enterprise.utils.DriveSvgIconFactory.loadSvgImage("images/icon/arrow-cloud-upload-svgrepo-com.svg", 22);
        if (upImg != null) {
            javafx.scene.image.ImageView uploadIcon = new javafx.scene.image.ImageView(upImg);
            uploadIcon.setFitWidth(22);
            uploadIcon.setFitHeight(22);
            btnUpload.setGraphic(uploadIcon);
        }
        MenuItem itemUploadFile = new MenuItem("Tải file lên");
        itemUploadFile.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn file để tải lên");
            java.util.List<java.io.File> selectedFiles = fileChooser.showOpenMultipleDialog(this.getScene().getWindow());
            if (selectedFiles != null && !selectedFiles.isEmpty() && uploadAction != null) {
                uploadAction.accept(selectedFiles);
            }
        });

        MenuItem itemUploadFolder = new MenuItem("Tải thư mục lên");
        itemUploadFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Chọn thư mục tải lên");
            java.io.File selectedDir = dirChooser.showDialog(this.getScene().getWindow());
            if (selectedDir != null) {
                java.util.List<java.io.File> filesList = new ArrayList<>();
                java.io.File[] files = selectedDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.isFile()) {
                            filesList.add(f);
                        }
                    }
                }
                if (!filesList.isEmpty() && uploadAction != null) {
                    uploadAction.accept(filesList);
                } else {
                    new Alert(Alert.AlertType.INFORMATION, "Thư mục trống hoặc không có file hợp lệ.").showAndWait();
                }
            }
        });
        btnUpload.getItems().addAll(itemUploadFile, itemUploadFolder, new MenuItem("Nhập từ Google Drive"));

        topLine.getChildren().addAll(titleBlock, topSpacer, btnUpload, btnCreate);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 14, 0, 14));
        searchBox.setPrefWidth(360);
        searchBox.setMinHeight(42);
        searchBox.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 14;"
        );
        searchBox.getChildren().add(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.SEARCH, 18, "#94A3B8"));
        TextField searchInput = new TextField();
        searchInput.setPromptText("Tìm kiếm trong tài liệu...");
        searchInput.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0; -fx-font-size: 13;");
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        searchBox.getChildren().add(searchInput);

        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> {
                String keyword = newVal == null ? "" : newVal.trim();
                if (!keyword.isEmpty()) {
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

        ComboBox<String> cbType = createFilterDropdown("Loại: Tất cả", "PDF", "Word", "Video", "Excel", "Thư mục");
        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            String filter = null;
            if ("PDF".equals(newVal)) {
                filter = "pdf";
            } else if ("Word".equals(newVal)) {
                filter = "docx";
            } else if ("Video".equals(newVal)) {
                filter = "mp4";
            } else if ("Excel".equals(newVal)) {
                filter = "xlsx";
            } else if ("Thư mục".equals(newVal)) {
                filter = "folder";
            }

            viewModel.getDriveService().getFilesFiltered(viewModel.getCurrentUserId(), viewModel.currentFolderIdProperty().get(), filter, "newest").thenAccept(res -> {
                Platform.runLater(() -> viewModel.getFiles().setAll(res));
            });
        });

        ComboBox<String> cbSort = createFilterDropdown("Sắp xếp: Mới nhất", "Cũ nhất", "Tên A-Z", "Dung lượng");

        HBox viewToggle = new HBox();
        viewToggle.setAlignment(Pos.CENTER);
        btnGrid = new Button();
        btnGrid.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.GRID, 18, PRIMARY_BLUE));
        btnList = new Button();
        btnList.setGraphic(DriveSvgIconFactory.createSvgIcon(DriveSvgIcons.LIST, 18, TEXT_MUTED));
        applyToggleStyle(true);

        btnGrid.setOnAction(e -> {
            applyToggleStyle(true);
            if (toggleViewModeAction != null) {
                toggleViewModeAction.accept(true);
            }
        });
        btnList.setOnAction(e -> {
            applyToggleStyle(false);
            if (toggleViewModeAction != null) {
                toggleViewModeAction.accept(false);
            }
        });
        viewToggle.getChildren().addAll(btnGrid, btnList);

        Button btnInfo = new Button("Chi tiết");
        btnInfo.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 9 14;" +
            "-fx-text-fill: " + TEXT_MAIN + ";" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        btnInfo.setOnAction(e -> {
            if (toggleRightSidebarAction != null) {
                toggleRightSidebarAction.run();
            }
        });

        toolbar.getChildren().addAll(searchBox, spacer, cbType, cbSort, viewToggle, btnInfo);
        this.getChildren().addAll(topLine, toolbar, new Separator());
    }

    private MenuButton createPrimaryMenuButton(String text) {
        MenuButton btn = new MenuButton(text);
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + BRAND_PURPLE + ", " + PRIMARY_BLUE + ");" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 9 16;" +
            "-fx-background-radius: 14;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    private MenuButton createSecondaryMenuButton(String text) {
        MenuButton btn = new MenuButton(text);
        btn.setTextFill(Color.web(TEXT_MAIN));
        btn.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-text-fill: " + TEXT_MAIN + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 9 16;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    private void applyToggleStyle(boolean gridActive) {
        String active = "-fx-background-color: #EEF2FF; -fx-border-color: " + BORDER_COLOR + "; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-cursor: hand; -fx-padding: 9 10;";
        String inactive = "-fx-background-color: #FFFFFF; -fx-border-color: " + BORDER_COLOR + "; -fx-text-fill: " + TEXT_MUTED + "; -fx-cursor: hand; -fx-padding: 9 10;";
        btnGrid.setStyle((gridActive ? active : inactive) + "-fx-border-radius: 12 0 0 12; -fx-background-radius: 12 0 0 12;");
        btnList.setStyle((gridActive ? inactive : active) + "-fx-border-width: 1 1 1 0; -fx-border-radius: 0 12 12 0; -fx-background-radius: 0 12 12 0;");
        ((SVGPath) btnGrid.getGraphic()).setFill(Color.web(gridActive ? PRIMARY_BLUE : TEXT_MUTED));
        ((SVGPath) btnList.getGraphic()).setFill(Color.web(gridActive ? TEXT_MUTED : PRIMARY_BLUE));
    }

    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog("Thư mục mới");
        dialog.setTitle("Tạo thư mục mới");
        dialog.setHeaderText("Nhập tên cho thư mục mới");
        dialog.setContentText("Tên thư mục:");
        dialog.showAndWait().ifPresent(folderName -> {
            String cleanName = folderName.trim();
            if (cleanName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Tên thư mục không được để trống.").showAndWait();
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
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Không thể tạo thư mục vào lúc này.").showAndWait());
                }
            });
        });
    }

    private ComboBox<String> createFilterDropdown(String defaultItem, String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add(defaultItem);
        cb.getItems().addAll(items);
        cb.getSelectionModel().selectFirst();
        cb.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 3 8;" +
            "-fx-font-size: 13;" +
            "-fx-cursor: hand;"
        );
        return cb;
    }

    private void bindViewModel() {
        viewModel.getBreadcrumbs().addListener((ListChangeListener<DriveFileModel>) c -> {
            Platform.runLater(this::renderBreadcrumbs);
        });
    }

    private void renderBreadcrumbs() {
        breadcrumbContainer.getChildren().clear();

        Label lblRoot = new Label("Tài liệu");
        lblRoot.setFont(Font.font("Segoe UI", viewModel.getBreadcrumbs().isEmpty() ? FontWeight.BOLD : FontWeight.SEMI_BOLD, 28));
        lblRoot.setTextFill(Color.web(viewModel.getBreadcrumbs().isEmpty() ? TEXT_MAIN : TEXT_MUTED));
        lblRoot.setCursor(javafx.scene.Cursor.HAND);
        lblRoot.setOnMouseClicked(e -> viewModel.navigateHome());
        breadcrumbContainer.getChildren().add(lblRoot);

        for (int i = 0; i < viewModel.getBreadcrumbs().size(); i++) {
            DriveFileModel folder = viewModel.getBreadcrumbs().get(i);
            Label separator = new Label("›");
            separator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
            separator.setTextFill(Color.web("#CBD5E1"));

            boolean isLast = i == viewModel.getBreadcrumbs().size() - 1;
            Label lblFolder = new Label(folder.getName());
            lblFolder.setFont(Font.font("Segoe UI", isLast ? FontWeight.BOLD : FontWeight.SEMI_BOLD, isLast ? 24 : 20));
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
