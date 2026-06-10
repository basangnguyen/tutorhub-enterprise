package com.mycompany.tutorhub_enterprise.client;



import com.mycompany.tutorhub_enterprise.models.DriveFileModel;

import com.mycompany.tutorhub_enterprise.server.dao.DriveFileDAO;

import javafx.application.Platform;

import javafx.embed.swing.JFXPanel;

import javafx.event.EventHandler;

import javafx.geometry.Insets;

import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;

import javafx.scene.image.ImageView;

import javafx.scene.input.MouseEvent;

import javafx.scene.layout.*;

import javafx.scene.paint.Color;

import javafx.scene.text.Font;

import javafx.scene.text.FontWeight;

import javax.swing.JPanel;

import java.awt.BorderLayout;

import java.util.List;



public class DriveTab extends JPanel {



    private JFXPanel fxPanel;

    private DriveFileDAO fileDAO;

    

    // UI Elements

    private TilePane mainGrid;

    private VBox rightSidebar;

    private BorderPane mainContentPane;

    

    // State
    private int currentUserId;
    private Label lblBreadcrumb;
    
    // UI Elements cho Upload Manager
    private VBox uploadManagerPanel;
    private VBox uploadTaskList;
    
    private javafx.scene.control.ProgressBar pbStorage;
    private java.util.Set<Integer> currentStarredIds = new java.util.HashSet<>();
    private String currentViewMode = "recent"; // "recent", "my_drive", "trash"
    private java.util.Set<Integer> selectedFileIds = new java.util.HashSet<>();
    private java.util.List<DriveFileModel> selectedFiles = new java.util.ArrayList<>();
    private DriveFileModel lastSelectedFile = null;
    
    // Clipboard
    private java.util.List<DriveFileModel> clipboardFiles = new java.util.ArrayList<>();
    private boolean isCutOperation = false;
    private Integer currentFolderId = null;
    private java.util.List<DriveFileModel> breadcrumbs = new java.util.ArrayList<>();
    private HBox breadcrumbContainer;
    private TableView<DriveFileModel> listTable;
    private ScrollPane gridScroll;
    private boolean isGridView = true;
    private javafx.scene.layout.VBox uploadProgressPanel;
    private javafx.scene.control.Label lblUploadStatus;
    private javafx.scene.control.ProgressBar uploadProgressBar;
    private String currentSearchKeyword = "";
    private String currentTypeFilter = null;
    private String currentSortMode = null;
    private Button activeNavButton = null;

    private com.mycompany.tutorhub_enterprise.client.sync.TutorHubSyncDaemon syncDaemon;

    // Performance: Thumbnail cache (SoftReference để GC tự dọn khi thiếu bộ nhớ)
    private final java.util.Map<String, java.lang.ref.SoftReference<Image>> thumbnailCache = new java.util.concurrent.ConcurrentHashMap<>();
    private javafx.scene.control.ProgressIndicator loadingSpinner;
    private boolean isLoadingFiles = false;
    
    // Nút và thanh dung lượng lưu trữ (Phase 11 fix)
    private javafx.scene.control.Label lblStorageText;



    // Design System Colors

    private final String PRIMARY_BLUE = "#2563EB";

    private final String BG_WHITE = "#FFFFFF";

    private final String BG_GRAY_LIGHT = "#F9FAFB";

    private final String BORDER_COLOR = "#E5E7EB";

    private final String TEXT_MAIN = "#111827";

    private final String TEXT_MUTED = "#6B7280";

    private final String PRIMARY_BG = "#EFF6FF";
    private final String PRIMARY_COLOR = "#2563EB";



    public DriveTab(int currentUserId) {
        this.currentUserId = currentUserId;

        setLayout(new BorderLayout());

        fileDAO = new DriveFileDAO();

        fxPanel = new JFXPanel();

        add(fxPanel, BorderLayout.CENTER);

        Platform.runLater(this::initFX);

    }



    private void initFX() {

        BorderPane root = new BorderPane();

        root.setStyle("-fx-background-color: " + BG_WHITE + ";");



        // --- 1. LEFT SIDEBAR (Sidebar phụ của Drive) ---

        VBox leftSidebar = createLeftSidebar();

        root.setLeft(leftSidebar);



        // --- 2. MAIN CONTENT AREA ---

        mainContentPane = new BorderPane();

        

        // 2.1 Header & Toolbar

        VBox headerArea = createHeaderAndToolbar();

        mainContentPane.setTop(headerArea);

        

        // 2.2 Grid Area

        mainGrid = new TilePane();

        mainGrid.setHgap(20);

        mainGrid.setVgap(24);

        mainGrid.setPrefColumns(4);

        mainGrid.setPadding(new Insets(10, 0, 40, 0));



        gridScroll = new ScrollPane(mainGrid);
        gridScroll.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.getWidth();
            int columns = Math.max(1, (int) (width / 240.0)); // 220 card width + 20 gap
            mainGrid.setPrefColumns(columns);
        });

        gridScroll.setFitToWidth(true);

        gridScroll.setStyle("-fx-background: " + BG_WHITE + "; -fx-background-color: transparent; -fx-border-color: transparent;");

        gridScroll.setPadding(new Insets(0, 30, 0, 30));



        listTable = createListViewTable();

        listTable.setVisible(false);

        listTable.setManaged(false);



        StackPane viewStack = new StackPane(gridScroll, listTable);

        mainContentPane.setCenter(viewStack);



        root.setCenter(mainContentPane);



        // --- 3. RIGHT SIDEBAR (Chi tiết) ---

        rightSidebar = createRightSidebar(null);

        rightSidebar.setVisible(false);

        rightSidebar.setManaged(false); // Ẩn hoàn toàn khỏi layout khi chưa chọn file

        root.setRight(rightSidebar);



        StackPane mainStack = new StackPane(root);
        
        // Setup Drag & Drop
        setupDragAndDrop(mainContentPane, mainStack);
        
        // Setup Upload Progress UI
        uploadProgressPanel = createUploadProgressPanel();
        StackPane.setAlignment(uploadProgressPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(uploadProgressPanel, new Insets(0, 30, 30, 0));
        mainStack.getChildren().add(uploadProgressPanel);



        Scene scene = new Scene(mainStack, 1200, 800);

        // Load External CSS
        String cssUrl = getClass().getResource("/css/DriveTab.css") != null 
            ? getClass().getResource("/css/DriveTab.css").toExternalForm() : null;
        if (cssUrl != null) scene.getStylesheets().add(cssUrl);

        

        fxPanel.setScene(scene);
        scene.setOnKeyPressed(this::handleGlobalKeyPress);

        // Chạy ngầm dọn rác (Giai đoạn 11)
        new Thread(() -> {
            fileDAO.cleanupTrash();
        }).start();

        loadFiles();

    }



    // ==========================================

    // KHU VỰC 1: HEADER & THANH CÔNG CỤ

    // ==========================================

    private VBox createHeaderAndToolbar() {

        VBox container = new VBox(15);

        container.setPadding(new Insets(24, 30, 15, 30));

        container.setStyle("-fx-background-color: " + BG_WHITE + ";");



        breadcrumbContainer = new HBox(8);

        breadcrumbContainer.setAlignment(Pos.CENTER_LEFT);

        renderBreadcrumbs();



        HBox toolbar = new HBox(12);

        toolbar.setAlignment(Pos.CENTER_LEFT);



        // --- SỰ KIỆN NÚT: + TẠO MỚI ---

        MenuButton btnCreate = new MenuButton("+ Tạo mới");

        btnCreate.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");

        

        MenuItem itemCreateFolder = new MenuItem("Tạo thư mục");

        itemCreateFolder.setOnAction(e -> handleCreateFolder()); // Gọi hàm tạo thư mục

        

        MenuItem itemCreateDoc = new MenuItem("Tạo tài liệu");

        MenuItem itemCreateSlide = new MenuItem("Tạo slide");

        MenuItem itemCreateVideo = new MenuItem("Tạo bài giảng");

        

        btnCreate.getItems().addAll(itemCreateFolder, itemCreateDoc, itemCreateSlide, itemCreateVideo);



        // --- SỰ KIỆN NÚT: TẢI LÊN ---

        MenuButton btnUpload = new MenuButton("↑ Tải lên");

        btnUpload.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");

        MenuItem itemUploadFile = new MenuItem("Tải file lên");
        itemUploadFile.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn file để tải lên");
            java.util.List<java.io.File> selectedFiles = fileChooser.showOpenMultipleDialog(fxPanel.getScene().getWindow());
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                uploadFiles(selectedFiles);
            }
        });
        
        MenuItem itemUploadFolder = new MenuItem("Tải thư mục lên");
        itemUploadFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Chọn thư mục tải lên");
            java.io.File selectedDir = dirChooser.showDialog(fxPanel.getScene().getWindow());
            if (selectedDir != null) {
                java.util.List<java.io.File> filesList = new java.util.ArrayList<>();
                java.io.File[] files = selectedDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.isFile()) filesList.add(f);
                    }
                }
                if (!filesList.isEmpty()) {
                    uploadFiles(filesList);
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Thư mục trống hoặc không có file hợp lệ.");
                    alert.showAndWait();
                }
            }
        });
        
        btnUpload.getItems().addAll(itemUploadFile, itemUploadFolder, new MenuItem("Nhập từ Google Drive"));

        javafx.scene.control.TextField searchInput = new javafx.scene.control.TextField();
        searchInput.setPromptText("🔍 Tìm kiếm trong Drive...");
        searchInput.setPrefWidth(220);
        searchInput.setStyle("-fx-background-color: #F3F4F6; -fx-border-color: transparent; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 12;");
        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> {
                currentSearchKeyword = newVal == null ? "" : newVal.trim();
                loadFiles();
            });
            debounce.playFromStart();
        });

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);



        // Bộ lọc
        ComboBox<String> cbType = createFilterDropdown("Loại: Tất cả", "PDF", "Word", "Video", "Excel", "Thư mục");
        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.startsWith("Loại:")) { currentTypeFilter = null; }
            else if ("PDF".equals(newVal)) { currentTypeFilter = "pdf"; }
            else if ("Word".equals(newVal)) { currentTypeFilter = "docx"; }
            else if ("Video".equals(newVal)) { currentTypeFilter = "mp4"; }
            else if ("Excel".equals(newVal)) { currentTypeFilter = "xlsx"; }
            else if ("Thư mục".equals(newVal)) { currentTypeFilter = "folder"; }
            loadFiles();
        });

        ComboBox<String> cbSort = createFilterDropdown("Sắp xếp: Mới nhất", "Cũ nhất", "Tên A-Z", "Dung lượng");
        cbSort.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.startsWith("Sắp xếp:")) { currentSortMode = null; }
            else if ("Cũ nhất".equals(newVal)) { currentSortMode = "oldest"; }
            else if ("Tên A-Z".equals(newVal)) { currentSortMode = "name_asc"; }
            else if ("Dung lượng".equals(newVal)) { currentSortMode = "size_desc"; }
            loadFiles();
        });



        // Nút View Mode

        HBox viewToggle = new HBox();

        Button btnGrid = new Button("\u229E"); 

        btnGrid.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");

        Button btnList = new Button("\u2261"); 

        btnList.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");

        

        btnGrid.setOnAction(e -> {

            btnGrid.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");

            btnList.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");

            isGridView = true;

            if (gridScroll != null) { gridScroll.setVisible(true); gridScroll.setManaged(true); }

            if (listTable != null) { listTable.setVisible(false); listTable.setManaged(false); }

        });



        btnList.setOnAction(e -> {

            btnList.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; -fx-cursor: hand;");

            btnGrid.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6 0 0 6; -fx-background-radius: 6 0 0 6; -fx-cursor: hand;");

            isGridView = false;

            if (gridScroll != null) { gridScroll.setVisible(false); gridScroll.setManaged(false); }

            if (listTable != null) { listTable.setVisible(true); listTable.setManaged(true); }

        });



        viewToggle.getChildren().addAll(btnGrid, btnList);



        // Nút Info bật/tắt Right Panel

        Button btnInfo = new Button("ⓘ");

        btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 16; -fx-cursor: hand;");

        btnInfo.setOnAction(e -> {

            boolean isVisible = rightSidebar.isVisible();

            rightSidebar.setVisible(!isVisible);

            rightSidebar.setManaged(!isVisible);

        });

        // Nút Sync Giai đoạn 12
        Button btnSync = new Button("🔄 Sync: OFF");
        btnSync.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSync.setOnAction(e -> {
            if (syncDaemon == null || !syncDaemon.isAlive()) {
                syncDaemon = new com.mycompany.tutorhub_enterprise.client.sync.TutorHubSyncDaemon(currentUserId, fileDAO, () -> {
                    loadFiles();
                });
                syncDaemon.start();
                btnSync.setText("🔄 Sync: ON");
                btnSync.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-border-color: #22C55E; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
            } else {
                syncDaemon.interrupt();
                syncDaemon = null;
                btnSync.setText("🔄 Sync: OFF");
                btnSync.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
            }
        });

        toolbar.getChildren().addAll(btnCreate, btnUpload, searchInput, spacer, cbType, cbSort, viewToggle, btnSync, btnInfo);

        container.getChildren().addAll(breadcrumbContainer, toolbar, new Separator());

        return container;

    }



    private void renderBreadcrumbs() {
        breadcrumbContainer.getChildren().clear();
        
        Label lblRoot = new Label("Tài liệu");
        lblRoot.setFont(Font.font("System", breadcrumbs.isEmpty() ? FontWeight.BOLD : FontWeight.NORMAL, 24));
        lblRoot.setTextFill(Color.web(breadcrumbs.isEmpty() ? TEXT_MAIN : TEXT_MUTED));
        lblRoot.setCursor(javafx.scene.Cursor.HAND);
        lblRoot.setOnMouseClicked(e -> {
            currentFolderId = null;
            breadcrumbs.clear();
            renderBreadcrumbs();
            loadFiles();
        });
        
        breadcrumbContainer.getChildren().add(lblRoot);
        
        for (int i = 0; i < breadcrumbs.size(); i++) {
            DriveFileModel folder = breadcrumbs.get(i);
            Label separator = new Label(" > ");
            separator.setFont(Font.font("System", FontWeight.BOLD, 18));
            separator.setTextFill(Color.web(TEXT_MUTED));
            
            Label lblFolder = new Label(folder.getName());
            boolean isLast = (i == breadcrumbs.size() - 1);
            lblFolder.setFont(Font.font("System", isLast ? FontWeight.BOLD : FontWeight.NORMAL, isLast ? 24 : 20));
            lblFolder.setTextFill(Color.web(isLast ? TEXT_MAIN : TEXT_MUTED));
            
            if (!isLast) {
                lblFolder.setCursor(javafx.scene.Cursor.HAND);
                final int index = i;
                lblFolder.setOnMouseClicked(e -> {
                    breadcrumbs.subList(index + 1, breadcrumbs.size()).clear();
                    currentFolderId = folder.getFileId();
                    renderBreadcrumbs();
                    loadFiles();
                });
            }
            breadcrumbContainer.getChildren().addAll(separator, lblFolder);
        }
    }



    // ==========================================

    // HÀM XỬ LÝ LOGIC: TẠO THƯ MỤC

    // ==========================================

    private void handleCreateFolder() {

        // Mở hộp thoại nhập tên thư mục của JavaFX

        TextInputDialog dialog = new TextInputDialog("Thư mục không tên");

        dialog.setTitle("Tạo thư mục mới");

        dialog.setHeaderText("Nhập tên cho thư mục mới:");

        dialog.setContentText("Tên thư mục:");



        // Bắt sự kiện khi người dùng bấm OK

        dialog.showAndWait().ifPresent(folderName -> {

            String cleanName = folderName.trim();

            if (cleanName.isEmpty()) {

                Alert alert = new Alert(Alert.AlertType.WARNING, "Tên thư mục không được để trống!");

                alert.showAndWait();

                return;

            }



            // Đóng gói dữ liệu để gửi xuống Database

            DriveFileModel newFolder = new DriveFileModel();

            newFolder.setName(cleanName);

            newFolder.setFileType("folder");

            newFolder.setFileSize(0);

            newFolder.setOwnerId(currentUserId);

            newFolder.setSourceLocation("my_drive");

            newFolder.setParentId(currentFolderId); // Lưu vào thư mục hiện tại

            newFolder.setStatus("active");



            // Gọi DAO để Insert

            boolean success = fileDAO.insertFile(newFolder);



            if (success) {

                // Tải lại Grid để hiển thị thư mục mới

                loadFiles();

            } else {

                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi! Không thể tạo thư mục vào lúc này.");

                alert.showAndWait();

            }

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



    // ==========================================

    // KHU VỰC 2: LEFT SIDEBAR

    // ==========================================

    private VBox createLeftSidebar() {

        VBox sidebar = new VBox(20);

        sidebar.setPrefWidth(250);

        sidebar.setPadding(new Insets(24, 16, 24, 16));

        sidebar.setStyle("-fx-background-color: " + BG_WHITE + "; -fx-border-color: transparent " + BORDER_COLOR + " transparent transparent;");



        // Nhóm Drive của tôi
        VBox driveGroup = new VBox(4);
        Label lblDriveTitle = new Label("Drive của tôi");
        lblDriveTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblDriveTitle.setTextFill(Color.web(TEXT_MUTED));
        lblDriveTitle.setPadding(new Insets(0, 0, 5, 12));

        String navBase = "-fx-padding: 10 12; -fx-background-radius: 8; -fx-cursor: hand; ";
        Button btnRecent = createNavItem("Gần đây", "/images/icon/drive_time.png", true);
        activeNavButton = btnRecent;
        btnRecent.setOnAction(e -> { switchView("recent", btnRecent, navBase); });

        Button btnMyDrive = createNavItem("Drive của tôi", "/images/icon/drive_home.png", false);
        btnMyDrive.setOnAction(e -> { switchView("my_drive", btnMyDrive, navBase); });

        Button btnOrgDrive = createNavItem("Drive tổ chức", "/images/icon/drive_home.png", false);
        btnOrgDrive.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tính năng Drive tổ chức đang được phát triển.");
            alert.showAndWait();
        });

        Button btnShared = createNavItem("Được chia sẻ với tôi", "/images/icon/user.svg", false);
        btnShared.setOnAction(e -> { switchView("shared", btnShared, navBase); });

        Button btnStarred = createNavItem("Có gắn dấu sao", "/images/icon/drive_star.png", false);
        btnStarred.setOnAction(e -> { switchView("starred", btnStarred, navBase); });

        Button btnTrash = createNavItem("Thùng rác", "/images/icon/drive_trash.png", false);
        btnTrash.setOnAction(e -> { switchView("trash", btnTrash, navBase); });

        driveGroup.getChildren().addAll(lblDriveTitle, btnRecent, btnMyDrive, btnOrgDrive, btnShared, btnStarred, btnTrash);



        // Nhóm Thư mục
        VBox folderGroup = new VBox(4);
        HBox folderHeader = new HBox();
        folderHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblFolderTitle = new Label("THƯ MỤC");
        lblFolderTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblFolderTitle.setTextFill(Color.web(TEXT_MUTED));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnAddFolder = new Button("+");
        btnAddFolder.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 0;");
        folderHeader.getChildren().addAll(lblFolderTitle, spacer, btnAddFolder);
        folderHeader.setPadding(new Insets(0, 10, 5, 12));

        TreeView<DriveFileModel> folderTreeView = new TreeView<>();
        folderTreeView.setShowRoot(false);
        folderTreeView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        folderTreeView.setPrefHeight(400);

        TreeItem<DriveFileModel> rootItem = new TreeItem<>(new DriveFileModel());
        rootItem.getValue().setName("Root");
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);

        folderTreeView.setCellFactory(tv -> new javafx.scene.control.TreeCell<DriveFileModel>() {
            @Override
            protected void updateItem(DriveFileModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    ImageView folderIcon = new ImageView();
                    try {
                        java.net.URL url = getClass().getResource(getTreeItem().isExpanded() ? "/images/icon/drive_folder_opened.png" : "/images/icon/drive_folder_closed.png");
                        if (url != null) folderIcon.setImage(new Image(url.toExternalForm()));
                    } catch (Exception ex) {}
                    folderIcon.setFitWidth(18); folderIcon.setFitHeight(18);
                    setGraphic(folderIcon);
                }
            }
        });

        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().getFileId() > 0) {
                currentFolderId = newVal.getValue().getFileId();
                breadcrumbs.clear();
                TreeItem<DriveFileModel> current = newVal;
                while (current != null && current.getParent() != null) {
                    breadcrumbs.add(0, current.getValue());
                    current = current.getParent();
                }
                renderBreadcrumbs();
                loadFiles();
            }
        });

        buildFolderTree(rootItem, null);
        VBox.setVgrow(folderTreeView, Priority.ALWAYS);
        folderGroup.getChildren().addAll(folderHeader, folderTreeView);
        VBox.setVgrow(folderGroup, Priority.ALWAYS);

        // Nhóm Storage Quota
        VBox storageGroup = new VBox(6);
        storageGroup.setPadding(new Insets(20, 0, 0, 12));
        Label lblStorageTitle = new Label("Dung lượng lưu trữ");
        lblStorageTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        lblStorageTitle.setTextFill(Color.web(TEXT_MUTED));
        
        pbStorage = new javafx.scene.control.ProgressBar(0);
        pbStorage.setMaxWidth(Double.MAX_VALUE);
        pbStorage.setStyle("-fx-accent: #7C3AED;");
        
        lblStorageText = new Label("Đang tính toán...");
        lblStorageText.setFont(Font.font("System", 11));
        lblStorageText.setTextFill(Color.web(TEXT_MUTED));
        
        storageGroup.getChildren().addAll(lblStorageTitle, pbStorage, lblStorageText);

        sidebar.getChildren().addAll(driveGroup, folderGroup, storageGroup);

        return sidebar;

    }

    private void updateStorageQuota() {
        javafx.concurrent.Task<Long> quotaTask = new javafx.concurrent.Task<>() {
            @Override
            protected Long call() {
                return fileDAO.getUsedStorage(currentUserId);
            }
        };
        quotaTask.setOnSucceeded(e -> {
            long usedBytes = quotaTask.getValue();
            long MAX_BYTES = 15L * 1024 * 1024 * 1024; // 15 GB
            double ratio = (double) usedBytes / MAX_BYTES;
            pbStorage.setProgress(ratio);
            String usedStr = formatFileSize(usedBytes);
            lblStorageText.setText("Đã dùng " + usedStr + " / 15 GB");
            if (ratio > 0.9) pbStorage.setStyle("-fx-accent: #EF4444;");
            else pbStorage.setStyle("-fx-accent: #7C3AED;");
        });
        new Thread(quotaTask).start();
    }



    private Button createNavItem(String text, String iconUrl, boolean active) {

        Button btn = new Button(text);

        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setAlignment(Pos.CENTER_LEFT);

        

        ImageView icon = new ImageView();

        try {

            java.net.URL url = getClass().getResource(iconUrl);

            if (url != null) icon.setImage(new Image(url.toExternalForm()));

            else if (iconUrl.startsWith("http")) icon.setImage(new Image(iconUrl));

        } catch (Exception ex) {}

        icon.setFitWidth(18); icon.setFitHeight(18);

        btn.setGraphic(icon);

        btn.setGraphicTextGap(12);



        String baseStyle = "-fx-padding: 10 12; -fx-background-radius: 8; -fx-cursor: hand; ";

        if (active) {

            btn.setStyle(baseStyle + "-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-font-weight: bold;");

        } else {

            btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";");

            btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-background-color: " + BG_GRAY_LIGHT + "; -fx-text-fill: " + TEXT_MAIN + ";"));

            btn.setOnMouseExited(e -> btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";"));

        }

        return btn;

    }



    /**
     * Chuyển đổi giữa các chế độ xem sidebar (Gần đây / Drive của tôi / Thùng rác)
     */
    private void switchView(String viewMode, Button clickedBtn, String navBase) {
        currentViewMode = viewMode;
        currentFolderId = null;
        breadcrumbs.clear();
        currentSearchKeyword = "";
        renderBreadcrumbs();

        // Update active state
        if (activeNavButton != null) {
            activeNavButton.setStyle(navBase + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MAIN + ";");
        }
        clickedBtn.setStyle(navBase + "-fx-background-color: #EFF6FF; -fx-text-fill: " + PRIMARY_BLUE + "; -fx-font-weight: bold;");
        activeNavButton = clickedBtn;

        loadFiles();
    }

    /**
     * Format timestamp thành thời gian tương đối dễ đọc ("3 phút trước", "Hôm qua", ...)
     */
    private String formatRelativeTime(java.sql.Timestamp ts) {
        if (ts == null) return "Mới đây";
        long diff = System.currentTimeMillis() - ts.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        if (days == 1) return "Hôm qua";
        if (days < 7) return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(ts);
    }

    /**
     * Helper: Thêm icon vào thumbnail card
     */
    private void addIconToThumb(StackPane thumb, String iconUrl) {
        ImageView centerIcon = new ImageView();
        try {
            java.net.URL url = getClass().getResource(iconUrl);
            if (url != null) centerIcon.setImage(new Image(url.toExternalForm()));
        } catch (Exception ex) {}
        centerIcon.setFitWidth(64); centerIcon.setFitHeight(64);
        thumb.getChildren().add(centerIcon);
    }

    /**
     * Helper: Format kích thước file thành chuỗi dễ đọc
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Performance: Lấy thumbnail từ cache hoặc load mới và cache lại.
     * SoftReference cho phép GC tự dọn khi JVM thiếu bộ nhớ.
     */
    private Image getCachedThumbnail(String filePath, double width, double height) {
        java.lang.ref.SoftReference<Image> ref = thumbnailCache.get(filePath);
        if (ref != null) {
            Image cached = ref.get();
            if (cached != null) return cached;
        }
        try {
            java.io.File imgFile = new java.io.File(filePath);
            if (imgFile.exists()) {
                Image img = new Image(imgFile.toURI().toString(), width, height, false, true, true); // background loading
                thumbnailCache.put(filePath, new java.lang.ref.SoftReference<>(img));
                return img;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void loadCloudThumbnailAsync(DriveFileModel file, double width, double height, ImageView targetView) {
        String cacheKey = "cloud_" + file.getFileId();
        java.lang.ref.SoftReference<Image> ref = thumbnailCache.get(cacheKey);
        if (ref != null) {
            Image cached = ref.get();
            if (cached != null) {
                targetView.setImage(cached);
                return;
            }
        }
        
        javafx.concurrent.Task<Image> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Image call() throws Exception {
                com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                if (is != null) {
                    Image img = new Image(is, width, height, false, true);
                    is.close();
                    return img;
                }
                return null;
            }
        };
        loadTask.setOnSucceeded(e -> {
            Image img = loadTask.getValue();
            if (img != null) {
                thumbnailCache.put(cacheKey, new java.lang.ref.SoftReference<>(img));
                targetView.setImage(img);
            }
        });
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }


    // ==========================================

    // KHU VỰC 3: RIGHT PANEL (CHI TIẾT FILE)

    // ==========================================

    private VBox createRightSidebar(DriveFileModel file) {

        VBox sidebar = new VBox(20);

        sidebar.setPrefWidth(320);

        sidebar.setPadding(new Insets(24));

        sidebar.setStyle("-fx-background-color: " + BG_WHITE + "; -fx-border-color: transparent transparent transparent " + BORDER_COLOR + ";");



        if (file == null) {

            Label lblEmpty = new Label("Chọn một tài liệu để xem chi tiết");

            lblEmpty.setTextFill(Color.web(TEXT_MUTED));

            sidebar.setAlignment(Pos.CENTER);

            sidebar.getChildren().add(lblEmpty);

            return sidebar;

        }



        // Header

        HBox header = new HBox(10);

        header.setAlignment(Pos.TOP_LEFT);

        Label lblName = new Label(file.getName());

        lblName.setFont(Font.font("System", FontWeight.BOLD, 16));

        lblName.setWrapText(true);

        lblName.setPrefWidth(220);

        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);

        Button btnClose = new Button("✕");

        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14; -fx-cursor: hand;");

        btnClose.setOnAction(e -> {

            rightSidebar.setVisible(false);

            rightSidebar.setManaged(false);

            selectedFileIds.clear();
            selectedFiles.clear();
            updateSidebarState();

            loadFiles(); // Render lại để xóa viền xanh

        });

        header.getChildren().addAll(lblName, s, btnClose);



        // Preview Box

        StackPane preview = new StackPane();

        preview.setPrefHeight(160);

        preview.setStyle("-fx-background-color: " + BG_GRAY_LIGHT + "; -fx-background-radius: 12; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 12;");

        ImageView previewIcon = new ImageView();

        try {

            java.net.URL url = getClass().getResource(getIconUrl(file.getFileType()));

            if (url != null) previewIcon.setImage(new Image(url.toExternalForm()));

        } catch (Exception ex) {}

        previewIcon.setFitWidth(72); previewIcon.setFitHeight(72);

        preview.getChildren().add(previewIcon);



        // Metadata

        VBox metadata = new VBox(12);

        metadata.getChildren().addAll(

            createMetaRow("Loại", getTypeName(file.getFileType())),

            createMetaRow("Kích thước", file.getFileSize() > 0 ? (file.getFileSize() / 1024) + " KB" : "-"),

            createMetaRow("Vị trí", "📁 " + file.getSourceLocation()),

            createOwnerRow("Người sở hữu", "Nguyễn Minh Anh"),

            createMetaRow("Cập nhật", formatRelativeTime(file.getUpdatedAt()))
        );



        // Buttons
        VBox actions = new VBox(10);
        Button btnOpen = new Button("Mở ↗");
        btnOpen.setMaxWidth(Double.MAX_VALUE);
        btnOpen.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
        btnOpen.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                breadcrumbs.add(file);
                currentFolderId = file.getFileId();
                currentViewMode = "my_drive";
                renderBreadcrumbs();
                loadFiles();
            } else {
                previewFile(file);
            }
        });

        Button btnDownload = new Button("↓ Tải xuống");
        btnDownload.setMaxWidth(Double.MAX_VALUE);
        btnDownload.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + "; -fx-cursor: hand;");
        btnDownload.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                new Alert(Alert.AlertType.INFORMATION, "Chưa hỗ trợ tải thư mục.").showAndWait();
                return;
            }
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Chọn thư mục lưu");
            java.io.File dest = dc.showDialog(fxPanel.getScene().getWindow());
            if (dest != null) {
                try {
                    java.io.File destFile = new java.io.File(dest, file.getName());
                    if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                        com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                        java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                        if (is != null) {
                            java.nio.file.Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            is.close();
                        } else {
                            throw new Exception("Cloud download trả về null");
                        }
                    } else {
                        java.io.File sourceFile = new java.io.File(file.getFileUrl());
                        java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    new Alert(Alert.AlertType.INFORMATION, "Tải xuống thành công!").showAndWait();
                } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Lỗi khi tải xuống: " + ex.getMessage()).showAndWait(); }
            }
        });

        Button btnShare = new Button("➦ Chia sẻ");
        btnShare.setMaxWidth(Double.MAX_VALUE);
        btnShare.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + "; -fx-cursor: hand;");
        btnShare.setOnAction(e -> {
            new Alert(Alert.AlertType.INFORMATION, "Tính năng Chia sẻ đang được phát triển trong giai đoạn tiếp theo.").showAndWait();
        });

        actions.getChildren().addAll(btnOpen, btnDownload, btnShare);



        sidebar.getChildren().addAll(header, preview, metadata, new Separator(), actions);

        return sidebar;

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



    // ==========================================

    // KHU VỰC 4: GRID CARD LẤY DỮ LIỆU THẬT

    // ==========================================

    private void loadFiles() {
        if (isLoadingFiles) return; // Prevent double-loading
        isLoadingFiles = true;
        mainGrid.getChildren().clear();
        if (listTable != null) listTable.getItems().clear();

        // Show loading spinner
        if (loadingSpinner == null) {
            loadingSpinner = new javafx.scene.control.ProgressIndicator();
            loadingSpinner.setMaxSize(48, 48);
            loadingSpinner.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");
        }
        VBox loadingBox = new VBox(12, loadingSpinner, new Label("Đang tải..."));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(80));
        ((Label) loadingBox.getChildren().get(1)).setTextFill(Color.web(TEXT_MUTED));
        mainGrid.getChildren().add(loadingBox);

        // Capture current state for background thread
        final String viewMode = currentViewMode;
        final Integer folderId = currentFolderId;
        final String searchKeyword = currentSearchKeyword;
        final String typeFilter = currentTypeFilter;
        final String sortMode = currentSortMode;

        javafx.concurrent.Task<List<DriveFileModel>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<DriveFileModel> call() {
                java.util.Set<Integer> newStarredIds = fileDAO.getStarredFileIds(currentUserId);
                Platform.runLater(() -> {
                    DriveTab.this.currentStarredIds = newStarredIds;
                    updateStorageQuota();
                });

                if (!searchKeyword.isEmpty()) {
                    return fileDAO.searchFiles(currentUserId, searchKeyword);
                } else if ("trash".equals(viewMode)) {
                    return fileDAO.getTrashedFiles(currentUserId);
                } else if ("starred".equals(viewMode)) {
                    return fileDAO.getStarredFiles(currentUserId);
                } else if ("shared".equals(viewMode)) {
                    return fileDAO.getSharedFiles(currentUserId);
                } else if ("recent".equals(viewMode) && folderId == null) {
                    return fileDAO.getRecentFiles(currentUserId);
                } else if (typeFilter != null || sortMode != null) {
                    return fileDAO.getFilesFiltered(currentUserId, folderId, typeFilter, sortMode);
                } else {
                    return fileDAO.getFiles(currentUserId, folderId);
                }
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<DriveFileModel> files = loadTask.getValue();
            mainGrid.getChildren().clear();

            if (files.isEmpty()) {
                VBox emptyState = new VBox(12);
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setPadding(new Insets(60));
                Label emptyIcon = new Label();
                if ("trash".equals(viewMode)) emptyIcon.setText("\uD83D\uDDD1");
                else if ("starred".equals(viewMode)) emptyIcon.setText("⭐");
                else emptyIcon.setText("\uD83D\uDCC2");
                emptyIcon.setFont(Font.font(48));

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
                emptyState.getChildren().addAll(emptyIcon, emptyText, emptyHint);
                mainGrid.getChildren().add(emptyState);
            } else {
                for (int i = 0; i < files.size(); i++) {
                    DriveFileModel file = files.get(i);
                    VBox card = createFileGridCard(file);

                    // Staggered fade-in animation (mỗi card delay thêm 40ms)
                    card.setOpacity(0);
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(250), card);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.setDelay(javafx.util.Duration.millis(i * 40));
                    fadeIn.play();

                    mainGrid.getChildren().add(card);
                    if (listTable != null) listTable.getItems().add(file);
                }
            }
            isLoadingFiles = false;
        });

        loadTask.setOnFailed(event -> {
            mainGrid.getChildren().clear();
            loadTask.getException().printStackTrace();
            isLoadingFiles = false;
        });

        new Thread(loadTask, "DriveLoadFiles").start();
    }



    private VBox createFileGridCard(DriveFileModel file) {

        VBox card = new VBox();

        card.setPrefSize(220, 240);

        

        boolean isSelected = selectedFileIds.contains(file.getFileId());

        String baseStyle = "-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; ";

        if (isSelected) {

            card.setStyle(baseStyle + "-fx-border-color: " + PRIMARY_BLUE + "; -fx-border-width: 2;");

        } else {

            card.setStyle(baseStyle + "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1;");

        }



        // Thumbnail (Dynamic Background Color)

        StackPane thumb = new StackPane();

        thumb.setPrefHeight(140);

        String bgThumb = "#F3F4F6"; // Xám nhạt mặc định

        String type = file.getFileType().toLowerCase();

        if (type.equals("pdf") || file.getName().endsWith(".pdf")) bgThumb = "#FEE2E2"; // Đỏ nhạt

        else if (type.equals("video") || file.getName().endsWith(".mp4")) bgThumb = "#E0E7FF"; // Xanh nhạt

        else if (type.equals("slide") || file.getName().endsWith(".ppt")) bgThumb = "#FFEDD5"; // Cam nhạt

        else if (type.equals("excel") || file.getName().endsWith(".xlsx")) bgThumb = "#DCFCE7"; // Xanh lá

        

        thumb.setStyle("-fx-background-color: " + bgThumb + "; -fx-background-radius: 11 11 0 0; -fx-border-color: transparent transparent " + BORDER_COLOR + " transparent;");

        String iconUrl = getIconUrl(type);

        // Thumbnail thực cho ảnh (có cache), icon cho các loại khác
        boolean isImage = type.equals("jpg") || type.equals("png") || type.equals("jpeg") || file.getName().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp)$");
        if (isImage && file.getFileUrl() != null) {
            if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                ImageView thumbView = new ImageView();
                addIconToThumb(thumb, iconUrl); // Placeholder
                thumbView.setFitWidth(220);
                thumbView.setFitHeight(140);
                thumbView.setPreserveRatio(false);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 140);
                clip.setArcWidth(22); clip.setArcHeight(22);
                thumbView.setClip(clip);
                thumb.getChildren().add(thumbView);
                loadCloudThumbnailAsync(file, 220, 140, thumbView);
            } else {
                try {
                    java.io.File imgFile = new java.io.File(file.getFileUrl());
                    if (imgFile.exists()) {
                        Image thumbImg = getCachedThumbnail(file.getFileUrl(), 220, 140);
                        if (thumbImg != null) {
                            ImageView thumbView = new ImageView(thumbImg);
                            thumbView.setFitWidth(220);
                            thumbView.setFitHeight(140);
                            thumbView.setPreserveRatio(false);
                            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 140);
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

        // Star Toggle Button
        Button btnStar = new Button();
        boolean isStarred = currentStarredIds.contains(file.getFileId());
        btnStar.setText(isStarred ? "★" : "☆");
        btnStar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); -fx-background-radius: 50; -fx-text-fill: " + (isStarred ? "#F59E0B" : TEXT_MUTED) + "; -fx-font-size: 16; -fx-padding: 2 6; -fx-cursor: hand;");
        btnStar.setOnAction(e -> {
            boolean success = fileDAO.toggleStar(file.getFileId(), currentUserId);
            if (success) {
                boolean nowStarred = !currentStarredIds.contains(file.getFileId());
                if (nowStarred) currentStarredIds.add(file.getFileId());
                else currentStarredIds.remove(file.getFileId());
                
                btnStar.setText(nowStarred ? "★" : "☆");
                btnStar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); -fx-background-radius: 50; -fx-text-fill: " + (nowStarred ? "#F59E0B" : TEXT_MUTED) + "; -fx-font-size: 16; -fx-padding: 2 6; -fx-cursor: hand;");
                if ("starred".equals(currentViewMode) && !nowStarred) {
                    loadFiles(); // reload if removing from starred view
                }
            }
        });
        StackPane.setAlignment(btnStar, Pos.TOP_RIGHT);
        StackPane.setMargin(btnStar, new Insets(8));
        thumb.getChildren().add(btnStar);


        // Nửa dưới (Thông tin)

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

        ImageView typeSmall = new ImageView();

        try {

            java.net.URL url = getClass().getResource(iconUrl);

            if (url != null) typeSmall.setImage(new Image(url.toExternalForm()));

        } catch (Exception ex) {}

        typeSmall.setFitWidth(16); typeSmall.setFitHeight(16);

        String timeStr = formatRelativeTime(file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt());
        String subText;
        if ("folder".equalsIgnoreCase(file.getFileType())) {
            subText = file.getChildCount() + " mục • " + timeStr;
        } else {
            String sizeStr = file.getFileSize() > 0 ? formatFileSize(file.getFileSize()) : "";
            subText = (sizeStr.isEmpty() ? "" : sizeStr + " • ") + timeStr;
        }
        Label lblSub = new Label(subText);
        lblSub.setFont(Font.font(11)); lblSub.setTextFill(Color.web(TEXT_MUTED));

        subRow.getChildren().addAll(typeSmall, lblSub);



        info.getChildren().addAll(titleRow, subRow);

        card.getChildren().addAll(thumb, info);



        // Hover & Click Events
        card.setUserData(file.getFileId());
        card.getStyleClass().add(isSelected ? "file-card-selected" : "file-card");

        card.setOnMouseEntered(e -> {
            if (!selectedFileIds.contains(file.getFileId())) {
                card.setStyle(baseStyle + "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 18, 0, 0, 6); -fx-scale-x: 1.015; -fx-scale-y: 1.015;");
            }
        });
        card.setOnMouseExited(e -> {
            if (!selectedFileIds.contains(file.getFileId())) {
                card.setStyle(baseStyle + "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1;");
            }
        });
        
        ContextMenu contextMenu = createContextMenu(file);
        card.setOnContextMenuRequested(e -> {
            if (!selectedFileIds.contains(file.getFileId())) {
                selectedFileIds.clear(); selectedFiles.clear();
                selectedFileIds.add(file.getFileId()); selectedFiles.add(file);
                updateCardStyles(); updateSidebarState();
            }
            contextMenu.show(card, e.getScreenX(), e.getScreenY());
        });

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (file.getFileType().equalsIgnoreCase("folder")) {
                    breadcrumbs.add(file);
                    currentFolderId = file.getFileId();
                    renderBreadcrumbs();
                    selectedFileIds.clear(); selectedFiles.clear();
                    updateSidebarState();
                    loadFiles();
                } else {
                    previewFile(file);
                }
                return;
            }
            
            if (e.isControlDown() || e.isMetaDown()) {
                if (selectedFileIds.contains(file.getFileId())) {
                    selectedFileIds.remove(file.getFileId());
                    selectedFiles.removeIf(f -> f.getFileId() == file.getFileId());
                } else {
                    selectedFileIds.add(file.getFileId());
                    selectedFiles.add(file);
                    lastSelectedFile = file;
                }
            } else if (e.isShiftDown() && lastSelectedFile != null) {
                selectedFileIds.add(file.getFileId());
                selectedFiles.add(file);
            } else {
                selectedFileIds.clear(); selectedFiles.clear();
                selectedFileIds.add(file.getFileId()); selectedFiles.add(file);
                lastSelectedFile = file;
            }
            
            updateCardStyles();
            updateSidebarState();
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
                if (idObj != null && selectedFileIds.contains((Integer)idObj)) {
                    c.setStyle("-fx-background-color: " + PRIMARY_BG + "; " + baseStyle + "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2;");
                } else {
                    c.setStyle("-fx-background-color: " + BG_WHITE + "; " + baseStyle + "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1;");
                }
            }
        }
    }

    private void updateSidebarState() {
        if (fxPanel.getScene() == null || fxPanel.getScene().getRoot() == null) return;
        BorderPane root = (BorderPane) fxPanel.getScene().getRoot();
        if (selectedFileIds.isEmpty()) {
            if (rightSidebar != null) {
                rightSidebar.setVisible(false);
                rightSidebar.setManaged(false);
            }
        } else if (selectedFileIds.size() == 1) {
            rightSidebar = createRightSidebar(selectedFiles.get(0));
            rightSidebar.setVisible(true);
            rightSidebar.setManaged(true);
            root.setRight(rightSidebar);
        } else {
            rightSidebar = createBatchSidebar();
            rightSidebar.setVisible(true);
            rightSidebar.setManaged(true);
            root.setRight(rightSidebar);
        }
    }

    private VBox createBatchSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPrefWidth(300);
        sidebar.setPadding(new Insets(24));
        sidebar.setStyle("-fx-background-color: " + BG_WHITE + "; -fx-border-color: transparent transparent transparent " + BORDER_COLOR + ";");
        
        Label lblTitle = new Label("Đã chọn " + selectedFileIds.size() + " mục");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Button btnTrash = new Button("🗑️ Xóa tất cả");
        btnTrash.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        btnTrash.setMaxWidth(Double.MAX_VALUE);
        btnTrash.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Đưa " + selectedFileIds.size() + " mục vào thùng rác?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            alert.showAndWait().ifPresent(res -> {
                if (res == javafx.scene.control.ButtonType.YES) {
                    for (int id : selectedFileIds) fileDAO.moveToTrash(id);
                    selectedFileIds.clear(); selectedFiles.clear();
                    updateSidebarState();
                    loadFiles();
                }
            });
        });

        Button btnStar = new Button("⭐ Gắn sao tất cả");
        btnStar.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        btnStar.setMaxWidth(Double.MAX_VALUE);
        btnStar.setOnAction(e -> {
            for (int id : selectedFileIds) {
                if (!currentStarredIds.contains(id)) {
                    fileDAO.toggleStar(id, currentUserId);
                }
            }
            selectedFileIds.clear(); selectedFiles.clear();
            updateSidebarState();
            loadFiles();
        });
        
        sidebar.getChildren().addAll(lblTitle, btnStar, btnTrash);
        return sidebar;
    }

    private void handleGlobalKeyPress(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
            if (!selectedFileIds.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Đưa " + selectedFileIds.size() + " mục vào thùng rác?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                alert.showAndWait().ifPresent(res -> {
                    if (res == javafx.scene.control.ButtonType.YES) {
                        for (int id : selectedFileIds) fileDAO.moveToTrash(id);
                        selectedFileIds.clear(); selectedFiles.clear();
                        updateSidebarState(); loadFiles();
                    }
                });
            }
        } else if (e.getCode() == javafx.scene.input.KeyCode.F2) {
            if (selectedFileIds.size() == 1) {
                DriveFileModel file = selectedFiles.get(0);
                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(file.getName());
                dialog.setTitle("Đổi tên");
                dialog.setHeaderText("Nhập tên mới:");
                dialog.showAndWait().ifPresent(newName -> {
                    if (!newName.trim().isEmpty() && fileDAO.renameFile(file.getFileId(), newName.trim())) {
                        loadFiles();
                    }
                });
            }
        } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
            if (selectedFileIds.size() == 1) {
                DriveFileModel file = selectedFiles.get(0);
                if (file.getFileType().equalsIgnoreCase("folder")) {
                    breadcrumbs.add(file); currentFolderId = file.getFileId();
                    renderBreadcrumbs(); selectedFileIds.clear(); selectedFiles.clear();
                    updateSidebarState(); loadFiles();
                } else {
                    previewFile(file);
                }
            }
        } else if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
            if (!breadcrumbs.isEmpty()) {
                breadcrumbs.remove(breadcrumbs.size() - 1);
                currentFolderId = breadcrumbs.isEmpty() ? null : breadcrumbs.get(breadcrumbs.size() - 1).getFileId();
                renderBreadcrumbs(); selectedFileIds.clear(); selectedFiles.clear();
                updateSidebarState(); loadFiles();
            }
        } else if (e.isControlDown() || e.isMetaDown()) {
            if (e.getCode() == javafx.scene.input.KeyCode.C) {
                if (!selectedFiles.isEmpty()) {
                    clipboardFiles.clear(); clipboardFiles.addAll(selectedFiles);
                    isCutOperation = false;
                    new Alert(Alert.AlertType.INFORMATION, "Đã sao chép " + clipboardFiles.size() + " mục.").show();
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.X) {
                if (!selectedFiles.isEmpty()) {
                    clipboardFiles.clear(); clipboardFiles.addAll(selectedFiles);
                    isCutOperation = true;
                    new Alert(Alert.AlertType.INFORMATION, "Đã cắt " + clipboardFiles.size() + " mục.").show();
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.V) {
                if (!clipboardFiles.isEmpty()) {
                    if (isCutOperation) {
                        for (DriveFileModel f : clipboardFiles) {
                            fileDAO.moveFile(f.getFileId(), currentFolderId);
                        }
                        clipboardFiles.clear();
                        isCutOperation = false;
                        new Alert(Alert.AlertType.INFORMATION, "Đã di chuyển file thành công.").show();
                        loadFiles();
                    } else {
                        // Sao chép thực sự (Chưa hiện thực hoàn chỉnh, tạo thông báo)
                        new Alert(Alert.AlertType.WARNING, "Tính năng nhân bản file (Copy) lên Cloud đang được hoàn thiện. Vui lòng dùng Cut/Paste để di chuyển.").show();
                    }
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.A) {
                // Chọn tất cả
                selectedFileIds.clear(); selectedFiles.clear();
                for (javafx.scene.Node n : mainGrid.getChildren()) {
                    if (n instanceof VBox && n.getUserData() != null) {
                        int id = (Integer) n.getUserData();
                        selectedFileIds.add(id);
                    }
                }
                // Dựa vào DriveFileDAO getFiles để thêm model
                loadFiles(); // Note: Selecting all requires knowing models. Tạm bỏ qua Ctrl A đầy đủ model.
            }
        }
    }

    // --- Utils ---

    // --- Hỗ trợ Tải lên (Upload) ---
    private VBox createUploadProgressPanel() {
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
        uploadProgressPanel = uploadManagerPanel; // Gán lại biến cũ để ko lỗi logic add layout
        return uploadManagerPanel;
    }

    private void setupDragAndDrop(BorderPane pane, StackPane parentStack) {
        // Create drag overlay
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
        dragHint.setTextFill(Color.web(TEXT_MUTED));
        dragOverlay.getChildren().addAll(dragIcon, dragText, dragHint);
        
        // Add overlay to the StackPane parent
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
                List<java.io.File> files = db.getFiles();
                uploadFiles(files);
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });
    }

    private void uploadFiles(List<java.io.File> files) {
        uploadManagerPanel.setVisible(true);

        com.mycompany.tutorhub_enterprise.server.CloudStorageService cloudService = null;
        boolean useCloud = false;
        try {
            cloudService = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
            useCloud = cloudService.isAvailable();
        } catch (Exception ex) {
            useCloud = false;
        }
        final boolean cloudAvailable = useCloud;
        final com.mycompany.tutorhub_enterprise.server.CloudStorageService finalCloudService = cloudService;

        for (java.io.File file : files) {
            HBox taskRow = new HBox(10);
            taskRow.setAlignment(Pos.CENTER_LEFT);
            taskRow.setPadding(new Insets(5, 0, 5, 0));
            
            VBox infoBox = new VBox(3);
            Label lblName = new Label(file.getName());
            lblName.setFont(Font.font(12));
            lblName.setMaxWidth(200);
            lblName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            
            ProgressBar pb = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
            pb.setPrefWidth(200);
            pb.setStyle("-fx-accent: " + PRIMARY_BLUE + ";");
            
            Label lblStatus = new Label("Đang tải lên...");
            lblStatus.setFont(Font.font(10));
            lblStatus.setTextFill(Color.GRAY);
            
            infoBox.getChildren().addAll(lblName, pb, lblStatus);
            Label lblIcon = new Label("⏳");
            taskRow.getChildren().addAll(lblIcon, infoBox);
            
            uploadTaskList.getChildren().add(0, taskRow);
            
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    String fileName = file.getName();
                    String ext = "";
                    int dotIdx = fileName.lastIndexOf('.');
                    if (dotIdx > 0) ext = fileName.substring(dotIdx + 1);

                    String fileUrl = null;
                    String sourceLocation = "LOCAL";
                    
                    if (cloudAvailable) {
                        fileUrl = finalCloudService.uploadFile(file);
                        if (fileUrl != null) {
                            sourceLocation = "MINIO";
                        }
                    }
                    
                    if (fileUrl == null) {
                        java.io.File uploadDir = new java.io.File("drive_uploads");
                        if (!uploadDir.exists()) uploadDir.mkdirs();
                        java.io.File destFile = new java.io.File(uploadDir, System.currentTimeMillis() + "_" + fileName);
                        java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        fileUrl = destFile.getAbsolutePath();
                        sourceLocation = "LOCAL";
                    }

                    DriveFileModel newFile = new DriveFileModel();
                    newFile.setName(fileName);
                    newFile.setFileType(ext.isEmpty() ? "document" : ext);
                    newFile.setFileSize(file.length());
                    newFile.setFileUrl(fileUrl);
                    newFile.setOwnerId(currentUserId);
                    newFile.setSourceLocation(sourceLocation);
                    newFile.setParentId(currentFolderId);
                    newFile.setStatus("active");
                    
                    fileDAO.insertFile(newFile);
                    return null;
                }
            };
            new Thread(task).start();
        }
        loadFiles();
    }

    private ContextMenu createContextMenu(DriveFileModel file) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");
        
        MenuItem viewItem = new MenuItem("Mở/Xem trước");
        viewItem.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                breadcrumbs.add(file);
                currentFolderId = file.getFileId();
                renderBreadcrumbs();
                loadFiles();
            } else {
                previewFile(file);
            }
        });
        
        MenuItem renameItem = new MenuItem("Đổi tên");
        renameItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(file.getName());
            dialog.setTitle("Đổi tên");
            dialog.setHeaderText("Nhập tên mới:");
            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    fileDAO.renameFile(file.getFileId(), newName.trim());
                    loadFiles();
                }
            });
        });
        
        MenuItem shareItem = new MenuItem("Chia sẻ");
        shareItem.setOnAction(e -> {
            showShareDialog(file);
        });
        
        MenuItem starItem = new MenuItem(currentStarredIds.contains(file.getFileId()) ? "Bỏ gắn sao" : "Gắn sao");
        starItem.setOnAction(e -> {
            fileDAO.toggleStar(file.getFileId(), currentUserId);
            loadFiles();
        });
        
        MenuItem trashItem = new MenuItem("Xóa vào thùng rác");
        trashItem.setStyle("-fx-text-fill: red;");
        trashItem.setOnAction(e -> {
            fileDAO.moveToTrash(file.getFileId());
            loadFiles();
        });

        MenuItem updateVersionItem = new MenuItem("Tải lên phiên bản mới");
        updateVersionItem.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) return;
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Chọn tệp phiên bản mới");
            java.io.File newFile = fc.showOpenDialog(fxPanel.getScene().getWindow());
            if (newFile != null) {
                javafx.concurrent.Task<Void> updateTask = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        com.mycompany.tutorhub_enterprise.server.CloudStorageService cloudService = null;
                        boolean useCloud = false;
                        try {
                            cloudService = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                            useCloud = cloudService.isAvailable();
                        } catch (Exception ex) { useCloud = false; }
                        
                        String newFileUrl = null;
                        String newSourceLocation = "LOCAL";
                        
                        if (useCloud) {
                            newFileUrl = cloudService.uploadFile(newFile);
                            if (newFileUrl != null) newSourceLocation = "MINIO";
                        }
                        
                        if (newFileUrl == null) {
                            java.io.File uploadDir = new java.io.File("drive_uploads");
                            if (!uploadDir.exists()) uploadDir.mkdirs();
                            java.io.File destFile = new java.io.File(uploadDir, System.currentTimeMillis() + "_" + newFile.getName());
                            java.nio.file.Files.copy(newFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            newFileUrl = destFile.getAbsolutePath();
                            newSourceLocation = "LOCAL";
                        }
                        
                        fileDAO.updateFileVersion(file.getFileId(), newFileUrl, newSourceLocation, newFile.length());
                        return null;
                    }
                };

                updateTask.setOnSucceeded(ev -> {
                    lblUploadStatus.setText("Cập nhật phiên bản thành công!");
                    uploadProgressBar.setProgress(1.0);
                    javafx.animation.PauseTransition hideDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(2000));
                    hideDelay.setOnFinished(ev2 -> uploadProgressPanel.setVisible(false));
                    hideDelay.play();
                    loadFiles();
                });

                updateTask.setOnFailed(ev -> {
                    lblUploadStatus.setText("Lỗi cập nhật phiên bản!");
                    uploadProgressBar.setStyle("-fx-accent: red;");
                });

                new Thread(updateTask).start();
            }
        });

        MenuItem historyItem = new MenuItem("Lịch sử phiên bản");
        historyItem.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) return;
            javafx.scene.control.Dialog<Void> historyDialog = new javafx.scene.control.Dialog<>();
            historyDialog.setTitle("Lịch sử phiên bản");
            historyDialog.setHeaderText("Lịch sử phiên bản: " + file.getName());
            
            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(20));
            dialogContent.setPrefWidth(400);
            
            List<com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel> versions = fileDAO.getFileVersions(file.getFileId());
            if (versions.isEmpty()) {
                dialogContent.getChildren().add(new Label("Chưa có phiên bản cũ nào."));
            } else {
                for (com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel v : versions) {
                    HBox vRow = new HBox(10);
                    vRow.setAlignment(Pos.CENTER_LEFT);
                    VBox vInfo = new VBox(2);
                    Label lblVNum = new Label("Phiên bản " + v.getVersionNumber());
                    lblVNum.setFont(Font.font("System", FontWeight.BOLD, 12));
                    Label lblVTime = new Label(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
                    lblVTime.setFont(Font.font(10));
                    lblVTime.setTextFill(Color.web(TEXT_MUTED));
                    vInfo.getChildren().addAll(lblVNum, lblVTime);
                    
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    
                    Button btnDl = new Button("Tải về");
                    btnDl.setOnAction(ev -> {
                        javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
                        dirChooser.setTitle("Chọn thư mục lưu");
                        java.io.File destDir = dirChooser.showDialog(fxPanel.getScene().getWindow());
                        if (destDir != null) {
                            try {
                                java.io.File destFile = new java.io.File(destDir, "v" + v.getVersionNumber() + "_" + file.getName());
                                if ("MINIO".equalsIgnoreCase(v.getSourceLocation())) {
                                    com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                                    java.io.InputStream is = cs.downloadFile(v.getFileUrl());
                                    if (is != null) {
                                        java.nio.file.Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                        is.close();
                                    }
                                } else {
                                    java.io.File sourceFile = new java.io.File(v.getFileUrl());
                                    java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                }
                                new Alert(Alert.AlertType.INFORMATION, "Tải về phiên bản cũ thành công!").showAndWait();
                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR, "Lỗi tải phiên bản cũ: " + ex.getMessage()).showAndWait();
                            }
                        }
                    });
                    
                    vRow.getChildren().addAll(vInfo, spacer, btnDl);
                    dialogContent.getChildren().add(vRow);
                    dialogContent.getChildren().add(new javafx.scene.control.Separator());
                }
            }
            
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(dialogContent);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(300);
            
            historyDialog.getDialogPane().setContent(scroll);
            historyDialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            historyDialog.showAndWait();
        });
        
        MenuItem downloadItem = new MenuItem("Tải xuống");
        downloadItem.setOnAction(e -> {
            if ("folder".equalsIgnoreCase(file.getFileType())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tính năng tải thư mục đang được phát triển.");
                alert.showAndWait();
                return;
            }
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Chọn thư mục lưu");
            java.io.File destDir = dirChooser.showDialog(fxPanel.getScene().getWindow());
            if (destDir != null) {
                try {
                    java.io.File destFile = new java.io.File(destDir, file.getName());
                    if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                        // Tải từ Cloud
                        com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = 
                            com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                        java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                        if (is != null) {
                            java.nio.file.Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            is.close();
                        } else {
                            throw new Exception("Cloud download trả về null");
                        }
                    } else {
                        // Tải từ Local
                        java.io.File sourceFile = new java.io.File(file.getFileUrl());
                        java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    new Alert(Alert.AlertType.INFORMATION, "✅ Tải xuống thành công!").showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Lỗi khi tải xuống: " + ex.getMessage()).showAndWait();
                }
            }
        });
        
        if ("trash".equals(currentViewMode)) {
            // Trong thùng rác: hiện Khôi phục + Xóa vĩnh viễn
            MenuItem restoreItem = new MenuItem("↩ Khôi phục");
            restoreItem.setOnAction(e -> {
                fileDAO.restoreFromTrash(file.getFileId());
                loadFiles();
            });
            MenuItem permDeleteItem = new MenuItem("✖ Xóa vĩnh viễn");
            permDeleteItem.setStyle("-fx-text-fill: red;");
            permDeleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa vĩnh viễn " + file.getName() + "? Hành động này không thể hoàn tác!", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                alert.showAndWait().ifPresent(res -> {
                    if (res == javafx.scene.control.ButtonType.YES) {
                        // Xóa file vật lý (Cloud hoặc Local)
                        if (file.getFileUrl() != null) {
                            if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                                try {
                                    com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance().deleteFile(file.getFileUrl());
                                } catch (Exception ex) { ex.printStackTrace(); }
                            } else {
                                try { new java.io.File(file.getFileUrl()).delete(); } catch (Exception ex) {}
                            }
                        }
                        fileDAO.permanentDelete(file.getFileId());
                        loadFiles();
                    }
                });
            });
            menu.getItems().addAll(viewItem, restoreItem, new SeparatorMenuItem(), permDeleteItem);
        } else {
            MenuItem deleteItem = new MenuItem("Xóa vào thùng rác");
            deleteItem.setStyle("-fx-text-fill: red;");
            deleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa " + file.getName() + " vào thùng rác?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                alert.showAndWait().ifPresent(res -> {
                    if (res == javafx.scene.control.ButtonType.YES) {
                        fileDAO.moveToTrash(file.getFileId());
                        loadFiles();
                    }
                });
            });
            menu.getItems().addAll(viewItem, renameItem, shareItem, downloadItem, updateVersionItem, historyItem, new SeparatorMenuItem(), deleteItem);
        }
        return menu;
    }

    /**
     * Lazy-load folder tree: chỉ load children khi user expand node.
     * Sử dụng placeholder "dummy" item để TreeView hiện mũi tên mở rộng.
     */
    private void buildFolderTree(TreeItem<DriveFileModel> parentItem, Integer parentId) {
        try {
            List<DriveFileModel> children = fileDAO.getFiles(1, parentId);
            for (DriveFileModel child : children) {
                if ("folder".equalsIgnoreCase(child.getFileType())) {
                    TreeItem<DriveFileModel> childItem = new TreeItem<>(child);
                    // Thêm dummy placeholder để hiện mũi tên expand
                    DriveFileModel dummyPlaceholder = new DriveFileModel();
                    dummyPlaceholder.setName("Đang tải...");
                    childItem.getChildren().add(new TreeItem<>(dummyPlaceholder));
                    
                    // Lazy-load: chỉ load children thực sự khi expand
                    childItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                        if (isExpanded && childItem.getChildren().size() == 1 
                            && "Đang tải...".equals(childItem.getChildren().get(0).getValue().getName())) {
                            childItem.getChildren().clear();
                            // Load trên background thread
                            javafx.concurrent.Task<List<DriveFileModel>> lazyTask = new javafx.concurrent.Task<>() {
                                @Override
                                protected List<DriveFileModel> call() {
                                    return fileDAO.getFiles(1, child.getFileId());
                                }
                            };
                            lazyTask.setOnSucceeded(ev -> {
                                List<DriveFileModel> subChildren = lazyTask.getValue();
                                for (DriveFileModel sub : subChildren) {
                                    if ("folder".equalsIgnoreCase(sub.getFileType())) {
                                        TreeItem<DriveFileModel> subItem = new TreeItem<>(sub);
                                        DriveFileModel subDummy = new DriveFileModel();
                                        subDummy.setName("Đang tải...");
                                        subItem.getChildren().add(new TreeItem<>(subDummy));
                                        childItem.getChildren().add(subItem);
                                    }
                                }
                            });
                            new Thread(lazyTask, "LazyTreeLoad").start();
                        }
                    });
                    
                    parentItem.getChildren().add(childItem);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void previewFile(DriveFileModel file) {
        if ("MINIO".equalsIgnoreCase(file.getSourceLocation())) {
            // Hiển thị UI Đang chuẩn bị tải từ Cloud...
            javafx.stage.Stage loadingStage = new javafx.stage.Stage();
            loadingStage.setTitle("Đang chuẩn bị...");
            VBox vbox = new VBox(10);
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(20));
            javafx.scene.control.ProgressIndicator spin = new javafx.scene.control.ProgressIndicator();
            vbox.getChildren().addAll(spin, new Label("Đang tải dữ liệu từ Cloud..."));
            loadingStage.setScene(new Scene(vbox, 300, 150));
            loadingStage.show();
            
            javafx.concurrent.Task<java.io.File> downloadTask = new javafx.concurrent.Task<>() {
                @Override
                protected java.io.File call() throws Exception {
                    com.mycompany.tutorhub_enterprise.server.CloudStorageService cs = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance();
                    java.io.InputStream is = cs.downloadFile(file.getFileUrl());
                    if (is != null) {
                        java.io.File tempFile = java.io.File.createTempFile("tutorhub_", "." + file.getFileType());
                        tempFile.deleteOnExit();
                        java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        return tempFile;
                    }
                    return null;
                }
            };
            downloadTask.setOnSucceeded(e -> {
                loadingStage.close();
                java.io.File tempFile = downloadTask.getValue();
                if (tempFile != null) {
                    showPreviewWindow(file, tempFile);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Không thể tải file từ Cloud").show();
                }
            });
            downloadTask.setOnFailed(e -> {
                loadingStage.close();
                new Alert(Alert.AlertType.ERROR, "Lỗi tải file").show();
            });
            Thread t = new Thread(downloadTask);
            t.setDaemon(true);
            t.start();
        } else {
            showPreviewWindow(file, new java.io.File(file.getFileUrl()));
        }
    }

    private void showPreviewWindow(DriveFileModel file, java.io.File localFile) {
        javafx.stage.Stage previewStage = new javafx.stage.Stage();
        previewStage.setTitle("Xem trước: " + file.getName());
        
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: black;");
        String type = file.getFileType().toLowerCase();
        
        if (type.equals("mp4") || type.equals("video")) {
            try {
                javafx.scene.media.Media media = new javafx.scene.media.Media(localFile.toURI().toString());
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(player);
                mediaView.setFitWidth(800);
                mediaView.setFitHeight(600);
                mediaView.setPreserveRatio(true);
                layout.setCenter(mediaView);
                player.play();
                previewStage.setOnCloseRequest(e -> player.stop());
            } catch (Exception ex) {
                Label lbl = new Label("Không thể phát video này.");
                lbl.setTextFill(Color.WHITE);
                layout.setCenter(lbl);
            }
        } else if (type.equals("jpg") || type.equals("png") || type.equals("jpeg")) {
            try {
                ImageView imgView = new ImageView(new Image(localFile.toURI().toString()));
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(800);
                imgView.setFitHeight(600);
                layout.setCenter(imgView);
            } catch (Exception ex) {
                Label lbl = new Label("Không thể hiển thị ảnh này.");
                lbl.setTextFill(Color.WHITE);
                layout.setCenter(lbl);
            }
        } else {
            try {
                java.awt.Desktop.getDesktop().open(localFile);
                return; // Không mở cửa sổ preview của JavaFX nữa
            } catch (Exception ex) {
                Label lbl = new Label("Không thể mở tệp này bằng ứng dụng mặc định.");
                lbl.setTextFill(Color.WHITE);
                layout.setCenter(lbl);
            }
        }
        
        Scene scene = new Scene(layout, 800, 600);
        previewStage.setScene(scene);
        previewStage.show();
    }

    private TableView<DriveFileModel> createListViewTable() {
        TableView<DriveFileModel> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-font-size: 14px;");
        
        TableColumn<DriveFileModel, String> nameCol = new TableColumn<>("Tên");
        nameCol.setPrefWidth(400);
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setCellFactory(col -> new TableCell<DriveFileModel, String>() {
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
                        ImageView icon = new ImageView();
                        try {
                            java.net.URL url = getClass().getResource(getIconUrl(file.getFileType()));
                            if (url != null) icon.setImage(new Image(url.toExternalForm()));
                        } catch (Exception e) {}
                        icon.setFitWidth(24);
                        icon.setFitHeight(24);
                        Label lbl = new Label(item);
                        lbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                        lbl.setTextFill(Color.web(TEXT_MAIN));
                        box.getChildren().addAll(icon, lbl);
                        setGraphic(box);
                        setText(null);
                    }
                }
            }
        });

        TableColumn<DriveFileModel, String> ownerCol = new TableColumn<>("Chủ sở hữu");
        ownerCol.setPrefWidth(200);
        ownerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty("Nguyễn Minh Anh"));
        
        TableColumn<DriveFileModel, String> dateCol = new TableColumn<>("Sửa đổi lần cuối");
        dateCol.setPrefWidth(200);
        dateCol.setCellValueFactory(data -> {
            DriveFileModel file = data.getValue();
            String date = formatRelativeTime(file.getUpdatedAt());
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        TableColumn<DriveFileModel, String> sizeCol = new TableColumn<>("Kích thước");
        sizeCol.setPrefWidth(150);
        sizeCol.setCellValueFactory(data -> {
            DriveFileModel file = data.getValue();
            String size = file.getFileSize() > 0 ? (file.getFileSize() / 1024) + " KB" : "-";
            return new javafx.beans.property.SimpleStringProperty(size);
        });

        table.getColumns().addAll(nameCol, ownerCol, dateCol, sizeCol);

        table.setRowFactory(tv -> {
            TableRow<DriveFileModel> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    DriveFileModel file = row.getItem();
                    if (e.getClickCount() == 2) {
                        if (file.getFileType().equalsIgnoreCase("folder")) {
                            breadcrumbs.add(file);
                            currentFolderId = file.getFileId();
                            renderBreadcrumbs();
                            selectedFileIds.clear(); selectedFiles.clear();
                            updateSidebarState();
                            loadFiles();
                        } else {
                            previewFile(file);
                        }
                    } else if (e.getClickCount() == 1) {
                        if (e.isControlDown() || e.isMetaDown()) {
                            if (selectedFileIds.contains(file.getFileId())) {
                                selectedFileIds.remove(file.getFileId());
                                selectedFiles.removeIf(f -> f.getFileId() == file.getFileId());
                            } else {
                                selectedFileIds.add(file.getFileId());
                                selectedFiles.add(file);
                                lastSelectedFile = file;
                            }
                        } else if (e.isShiftDown() && lastSelectedFile != null) {
                            selectedFileIds.add(file.getFileId());
                            selectedFiles.add(file);
                        } else {
                            selectedFileIds.clear(); selectedFiles.clear();
                            selectedFileIds.add(file.getFileId()); selectedFiles.add(file);
                            lastSelectedFile = file;
                        }
                        updateSidebarState();
                        table.refresh();
                    }
                }
            });
            
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    ContextMenu menu = createContextMenu(row.getItem());
                    menu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            
            return row;
        });

        return table;
    }

    private void showShareDialog(DriveFileModel file) {
        javafx.scene.control.Dialog<Void> shareDialog = new javafx.scene.control.Dialog<>();
        shareDialog.setTitle("Chia sẻ tệp");
        shareDialog.setHeaderText("Chia sẻ tệp: " + file.getName());

        VBox dialogContent = new VBox(10);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setPrefWidth(400);

        Label lblSearch = new Label("Tìm người dùng bằng Tên hoặc Email:");
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Nhập từ khóa...");
        
        ListView<com.mycompany.tutorhub_enterprise.models.UserInfo> listView = new ListView<>();
        listView.setPrefHeight(150);
        listView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.mycompany.tutorhub_enterprise.models.UserInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.fullName + " (" + item.email + ")");
                }
            }
        });

        txtSearch.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.trim().length() > 2) {
                List<com.mycompany.tutorhub_enterprise.models.UserInfo> users = com.mycompany.tutorhub_enterprise.server.dao.UserDAO.searchUsers(newText.trim(), currentUserId);
                listView.getItems().setAll(users);
            } else {
                listView.getItems().clear();
            }
        });

        HBox permissionBox = new HBox(10);
        permissionBox.setAlignment(Pos.CENTER_LEFT);
        Label lblPerm = new Label("Quyền:");
        ComboBox<String> cbPermission = new ComboBox<>();
        cbPermission.getItems().addAll("VIEW", "EDIT");
        cbPermission.setValue("VIEW");
        permissionBox.getChildren().addAll(lblPerm, cbPermission);

        Button btnShare = new Button("Xác nhận chia sẻ");
        btnShare.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnShare.setOnAction(e -> {
            com.mycompany.tutorhub_enterprise.models.UserInfo selectedUser = listView.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng chọn người dùng từ danh sách!").show();
                return;
            }
            boolean success = fileDAO.shareFile(file.getFileId(), selectedUser.userId, cbPermission.getValue());
            if (success) {
                new Alert(Alert.AlertType.INFORMATION, "Đã chia sẻ thành công cho " + selectedUser.fullName).show();
                shareDialog.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi! Không thể chia sẻ.").show();
            }
        });

        dialogContent.getChildren().addAll(lblSearch, txtSearch, listView, permissionBox, btnShare);
        shareDialog.getDialogPane().setContent(dialogContent);
        shareDialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        shareDialog.showAndWait();
    }

    private void showVersionHistoryDialog(DriveFileModel file) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Lịch sử phiên bản");
        dialog.setHeaderText("Lịch sử các phiên bản: " + file.getName());

        VBox dialogContent = new VBox(10);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setPrefWidth(450);
        
        List<com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel> versions = fileDAO.getFileVersions(file.getFileId());
        
        if (versions.isEmpty()) {
            dialogContent.getChildren().add(new Label("Chưa có phiên bản cũ nào được lưu trữ."));
        } else {
            ListView<VBox> listView = new ListView<>();
            listView.setPrefHeight(250);
            
            for (com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel v : versions) {
                VBox row = new VBox(5);
                row.setPadding(new Insets(5));
                
                Label lblVer = new Label("Phiên bản V" + v.getVersionNumber());
                lblVer.setFont(Font.font("System", FontWeight.BOLD, 14));
                
                long timeMillis = v.getCreatedAt().getTime();
                java.util.Date date = new java.util.Date(timeMillis);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                Label lblTime = new Label("Lưu lúc: " + sdf.format(date) + " (" + v.getSourceLocation() + ")");
                lblTime.setTextFill(Color.GRAY);
                lblTime.setFont(Font.font(11));
                
                Button btnDownload = new Button("📥 Tải xuống / Xem");
                btnDownload.setOnAction(e -> {
                    try {
                        if ("MINIO".equalsIgnoreCase(v.getSourceLocation())) {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(v.getFileUrl()));
                        } else {
                            java.io.File f = new java.io.File(v.getFileUrl());
                            if (f.exists()) java.awt.Desktop.getDesktop().open(f);
                            else new Alert(Alert.AlertType.ERROR, "File không còn tồn tại trên máy").show();
                        }
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Lỗi mở file").show();
                    }
                });
                
                row.getChildren().addAll(lblVer, lblTime, btnDownload);
                listView.getItems().add(row);
            }
            dialogContent.getChildren().add(listView);
        }
        
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private String getIconUrl(String type) {
        switch (type.toLowerCase()) {
            case "pdf": return "/images/icon/file_pdf.png";
            case "document": case "doc": case "docx": return "/images/icon/file_word.png";
            case "video": return "/images/icon/file_video.png";
            case "excel": case "xlsx": return "/images/icon/file_excel.png";
            case "slide": case "ppt": case "pptx": return "/images/icon/file_powerpoint.png";
            case "folder": return "/images/icon/file_folder.png";
            default: return "/images/icon/file_document.png";
        }
    }

    private String getTypeName(String type) {
        switch (type.toLowerCase()) {
            case "pdf": return "PDF Document";
            case "video": return "Video File";
            case "slide": return "PowerPoint Presentation";
            case "excel": return "Excel Spreadsheet";
            case "folder": return "Thư mục";
            default: return "Tài liệu hệ thống";
        }
    }
}