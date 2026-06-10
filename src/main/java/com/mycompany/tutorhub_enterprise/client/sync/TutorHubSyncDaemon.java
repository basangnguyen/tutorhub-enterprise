package com.mycompany.tutorhub_enterprise.client.sync;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.server.CloudStorageService;
import com.mycompany.tutorhub_enterprise.server.dao.DriveFileDAO;
import javafx.application.Platform;

import java.io.File;
import java.nio.file.*;
import java.util.List;

public class TutorHubSyncDaemon extends Thread {
    private final int userId;
    private final DriveFileDAO fileDAO;
    private final Runnable onSyncComplete;
    private Path syncPath;

    public TutorHubSyncDaemon(int userId, DriveFileDAO fileDAO, Runnable onSyncComplete) {
        this.userId = userId;
        this.fileDAO = fileDAO;
        this.onSyncComplete = onSyncComplete;
        this.setDaemon(true);
        this.setName("TutorHubSyncDaemon");
    }

    @Override
    public void run() {
        try {
            String userHome = System.getProperty("user.home");
            File syncDir = new File(userHome, "TutorHub_Sync_" + userId);
            if (!syncDir.exists()) {
                syncDir.mkdirs();
            }
            syncPath = syncDir.toPath();
            
            WatchService watchService = FileSystems.getDefault().newWatchService();
            syncPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            
            System.out.println("✅ Bắt đầu theo dõi thư mục đồng bộ: " + syncPath.toString());
            
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take(); // Block until event
                boolean changesDetected = false;
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    Path fileName = (Path) event.context();
                    File file = syncPath.resolve(fileName).toFile();
                    
                    // Bỏ qua các file rác của hệ thống (như temp)
                    if (file.getName().startsWith("~") || file.getName().endsWith(".tmp") || file.getName().equals("desktop.ini")) {
                        continue;
                    }
                    
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // Tránh lỗi lock file (đợi file ghi xong)
                        Thread.sleep(500); 
                        if (file.exists() && file.isFile() && file.length() > 0) {
                            handleFileUpload(file);
                            changesDetected = true;
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        handleFileDelete(file.getName());
                        changesDetected = true;
                    }
                }
                
                if (changesDetected && onSyncComplete != null) {
                    Platform.runLater(onSyncComplete);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("⚠️ Sync Daemon bị ngắt.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi Sync Daemon: " + e.getMessage());
        }
    }

    private void handleFileUpload(File file) {
        try {
            // Kiểm tra xem file đã tồn tại trên DB chưa (dựa vào tên)
            List<DriveFileModel> existingFiles = fileDAO.searchFiles(userId, file.getName());
            boolean exists = false;
            int existingId = -1;
            for (DriveFileModel dfm : existingFiles) {
                if (dfm.getName().equals(file.getName()) && dfm.getParentId() == null) {
                    exists = true;
                    existingId = dfm.getFileId();
                    break;
                }
            }

            // Upload
            CloudStorageService cloudService = null;
            boolean useCloud = false;
            try {
                cloudService = CloudStorageService.getInstance();
                useCloud = cloudService.isAvailable();
            } catch (Exception ex) { useCloud = false; }
            
            String newUrl = null;
            String newSource = "LOCAL";
            if (useCloud) {
                newUrl = cloudService.uploadFile(file);
                if (newUrl != null) newSource = "MINIO";
            }
            if (newUrl == null) {
                File uploadDir = new File("drive_uploads");
                if (!uploadDir.exists()) uploadDir.mkdirs();
                File destFile = new File(uploadDir, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                newUrl = destFile.getAbsolutePath();
                newSource = "LOCAL";
            }

            if (exists) {
                // Update
                fileDAO.updateFileVersion(existingId, newUrl, newSource, file.length());
            } else {
                // Insert
                DriveFileModel newFile = new DriveFileModel();
                newFile.setName(file.getName());
                String ext = file.getName().contains(".") ? file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase() : "unknown";
                newFile.setFileType(ext);
                newFile.setFileSize(file.length());
                newFile.setFileUrl(newUrl);
                newFile.setOwnerId(userId);
                newFile.setSourceLocation(newSource);
                newFile.setStatus("active");
                fileDAO.insertFile(newFile);
            }
        } catch (Exception ex) {
            System.err.println("❌ Lỗi upload file từ Sync Daemon: " + ex.getMessage());
        }
    }

    private void handleFileDelete(String fileName) {
        List<DriveFileModel> existingFiles = fileDAO.searchFiles(userId, fileName);
        for (DriveFileModel dfm : existingFiles) {
            if (dfm.getName().equals(fileName) && dfm.getParentId() == null) {
                fileDAO.moveToTrash(dfm.getFileId());
                break;
            }
        }
    }
}
