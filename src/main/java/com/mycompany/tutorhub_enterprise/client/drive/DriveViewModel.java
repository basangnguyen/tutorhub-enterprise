package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.client.services.DriveService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.application.Platform;

import java.util.List;

/**
 * Lớp ViewModel chịu trách nhiệm quản lý toàn bộ State của phân hệ Drive.
 * Ứng dụng mô hình MVVM và Data Binding để đồng bộ dữ liệu với View.
 */
public class DriveViewModel {
    private final int currentUserId;
    private final DriveService driveService;

    // Các biến trạng thái quản lý điều hướng
    private final StringProperty currentViewMode = new SimpleStringProperty("recent"); // "recent", "my_drive", "trash", "starred"
    private final ObjectProperty<Integer> currentFolderId = new SimpleObjectProperty<>(null);
    
    // Dữ liệu File và Breadcrumbs hiển thị
    private final ObservableList<DriveFileModel> files = FXCollections.observableArrayList();
    private final ObservableList<DriveFileModel> breadcrumbs = FXCollections.observableArrayList();
    
    // Trạng thái chọn file và đánh dấu sao
    private final ObservableSet<Integer> currentStarredIds = FXCollections.observableSet();
    private final ObservableSet<Integer> selectedFileIds = FXCollections.observableSet();
    private final ObservableList<DriveFileModel> selectedFiles = FXCollections.observableArrayList();
    private final ObjectProperty<DriveFileModel> lastSelectedFile = new SimpleObjectProperty<>(null);
    
    // Quản lý Clipboard (Cut/Copy/Paste)
    private final ObservableList<DriveFileModel> clipboardFiles = FXCollections.observableArrayList();
    private final BooleanProperty isCutOperation = new SimpleBooleanProperty(false);
    
    // Thông tin lưu trữ và Load
    private final LongProperty usedStorageBytes = new SimpleLongProperty(0);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    
    public DriveViewModel(int currentUserId, DriveService driveService) {
        this.currentUserId = currentUserId;
        this.driveService = driveService;
        
        // [PHASE 4: REAL-TIME SYNC] Đăng ký lắng nghe sự kiện đồng bộ
        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().addGlobalListener(packet -> {
            if ("SYNC_DRIVE_UPDATE".equals(packet.action)) {
                Platform.runLater(this::loadFiles);
            }
        });
    }
    
    // ---- Getters cho Property Binding ----
    public int getCurrentUserId() { return currentUserId; }
    public DriveService getDriveService() { return driveService; }

    public StringProperty currentViewModeProperty() { return currentViewMode; }
    public ObjectProperty<Integer> currentFolderIdProperty() { return currentFolderId; }
    
    public ObservableList<DriveFileModel> getFiles() { return files; }
    public ObservableList<DriveFileModel> getBreadcrumbs() { return breadcrumbs; }
    public ObservableSet<Integer> getCurrentStarredIds() { return currentStarredIds; }
    public ObservableSet<Integer> getSelectedFileIds() { return selectedFileIds; }
    public ObservableList<DriveFileModel> getSelectedFiles() { return selectedFiles; }
    public ObjectProperty<DriveFileModel> lastSelectedFileProperty() { return lastSelectedFile; }
    
    public ObservableList<DriveFileModel> getClipboardFiles() { return clipboardFiles; }
    public BooleanProperty isCutOperationProperty() { return isCutOperation; }
    
    public LongProperty usedStorageBytesProperty() { return usedStorageBytes; }
    public BooleanProperty isLoadingProperty() { return isLoading; }
    
    // ---- Các hàm Action Tương tác (Business Logic) ----
    
    /**
     * Tải danh sách file dựa trên chế độ View hiện tại
     */
    public void loadFiles() {
        isLoading.set(true);
        String mode = currentViewMode.get();
        Integer folderId = currentFolderId.get();
        
        if ("recent".equals(mode)) {
            driveService.getRecentFiles(currentUserId).thenAccept(result -> {
                Platform.runLater(() -> {
                    files.setAll(result);
                    isLoading.set(false);
                });
            });
        } else if ("trash".equals(mode)) {
            driveService.getTrashedFiles(currentUserId).thenAccept(result -> {
                Platform.runLater(() -> {
                    files.setAll(result);
                    isLoading.set(false);
                });
            });
        } else if ("starred".equals(mode)) {
            driveService.getStarredFiles(currentUserId).thenAccept(result -> {
                Platform.runLater(() -> {
                    files.setAll(result);
                    isLoading.set(false);
                });
            });
        } else {
            driveService.getFiles(currentUserId, folderId).thenAccept(result -> {
                Platform.runLater(() -> {
                    files.setAll(result);
                    isLoading.set(false);
                });
            });
        }
    }

    public void loadStorageQuota() {
        driveService.getUsedStorage(currentUserId).thenAccept(bytes -> {
            Platform.runLater(() -> usedStorageBytes.set(bytes));
        });
    }

    public void loadStarredIds() {
        driveService.getStarredFileIds(currentUserId).thenAccept(ids -> {
            Platform.runLater(() -> {
                currentStarredIds.clear();
                currentStarredIds.addAll(ids);
            });
        });
    }
    
    public void toggleStar(DriveFileModel file) {
        int fileId = file.getFileId();
        driveService.toggleStar(fileId, currentUserId).thenAccept(success -> {
            if (success) {
                Platform.runLater(() -> {
                    if (currentStarredIds.contains(fileId)) {
                        currentStarredIds.remove(fileId);
                    } else {
                        currentStarredIds.add(fileId);
                    }
                    // Load lại danh sách nếu đang ở màn hình Starred
                    if ("starred".equals(currentViewMode.get())) {
                        loadFiles();
                    }
                });
            }
        });
    }

    /**
     * Xử lý click chọn file
     */
    public void selectFile(DriveFileModel file, boolean multiSelect) {
        if (!multiSelect) {
            selectedFiles.clear();
            selectedFileIds.clear();
        }
        if (file != null) {
            if (!selectedFileIds.contains(file.getFileId())) {
                selectedFiles.add(file);
                selectedFileIds.add(file.getFileId());
            }
            lastSelectedFile.set(file);
        } else {
            lastSelectedFile.set(null);
        }
    }
    
    /**
     * Chuyển vào thư mục con và xử lý Breadcrumbs
     */
    public void openFolder(DriveFileModel folder) {
        if (!"folder".equals(folder.getFileType())) return;
        currentFolderId.set(folder.getFileId());
        currentViewMode.set("my_drive");
        
        int index = -1;
        for (int i = 0; i < breadcrumbs.size(); i++) {
            if (breadcrumbs.get(i).getFileId() == folder.getFileId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            // Cắt bớt phần tử thừa phía sau nếu click vào breadcrumb cũ
            List<DriveFileModel> toRemove = FXCollections.observableArrayList(breadcrumbs.subList(index + 1, breadcrumbs.size()));
            breadcrumbs.removeAll(toRemove);
        } else {
            breadcrumbs.add(folder);
        }
        
        loadFiles();
    }
    
    /**
     * Quay về thư mục gốc của My Drive
     */
    public void navigateHome() {
        currentFolderId.set(null);
        currentViewMode.set("my_drive");
        breadcrumbs.clear();
        loadFiles();
    }
}
