package com.mycompany.tutorhub_enterprise.client.services;

import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import com.mycompany.tutorhub_enterprise.models.DriveFileVersionModel;
import com.mycompany.tutorhub_enterprise.server.dao.DriveFileDAO;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service Layer điều phối và chạy ngầm (Asynchronous) các truy vấn File Drive.
 * Ngăn chặn block luồng UI chính của JavaFX.
 */
public class DriveService {
    private final DriveFileDAO driveFileDAO;
    private final ExecutorService executor;

    public DriveService() {
        this.driveFileDAO = new DriveFileDAO();
        // Sử dụng một Fixed Thread Pool với 4 luồng để cân bằng hiệu suất
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<Boolean> insertFile(DriveFileModel file) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.insertFile(file), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getFiles(int ownerId, Integer parentId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getFiles(ownerId, parentId), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getSharedFiles(int currentUserId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getSharedFiles(currentUserId), executor);
    }

    public CompletableFuture<Boolean> shareFile(int fileId, int targetUserId, String permission) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.shareFile(fileId, targetUserId, permission), executor);
    }

    public CompletableFuture<Boolean> removeShare(int fileId, int targetUserId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.removeShare(fileId, targetUserId), executor);
    }

    public CompletableFuture<Void> cleanupTrash() {
        return CompletableFuture.runAsync(() -> driveFileDAO.cleanupTrash(), executor);
    }

    public CompletableFuture<DriveFileModel> getFileById(int fileId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getFileById(fileId), executor);
    }

    public CompletableFuture<Boolean> renameFile(int fileId, String newName) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.renameFile(fileId, newName), executor);
    }

    public CompletableFuture<Boolean> moveToTrash(int fileId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.moveToTrash(fileId), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getRecentFiles(int ownerId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getRecentFiles(ownerId), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getTrashedFiles(int ownerId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getTrashedFiles(ownerId), executor);
    }

    public CompletableFuture<Boolean> restoreFromTrash(int fileId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.restoreFromTrash(fileId), executor);
    }

    public CompletableFuture<Boolean> permanentDelete(int fileId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.permanentDelete(fileId), executor);
    }

    public CompletableFuture<List<DriveFileModel>> searchFiles(int ownerId, String keyword) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.searchFiles(ownerId, keyword), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getFilesFiltered(int ownerId, Integer parentId, String typeFilter, String sortMode) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getFilesFiltered(ownerId, parentId, typeFilter, sortMode), executor);
    }

    public CompletableFuture<Boolean> toggleStar(int fileId, int userId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.toggleStar(fileId, userId), executor);
    }

    public CompletableFuture<Boolean> isStarred(int fileId, int userId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.isStarred(fileId, userId), executor);
    }

    public CompletableFuture<List<DriveFileModel>> getStarredFiles(int userId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getStarredFiles(userId), executor);
    }

    public CompletableFuture<Set<Integer>> getStarredFileIds(int userId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getStarredFileIds(userId), executor);
    }

    public CompletableFuture<Boolean> updateFileVersion(int fileId, String newUrl, String newSourceLoc, long newSize) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.updateFileVersion(fileId, newUrl, newSourceLoc, newSize), executor);
    }

    public CompletableFuture<Long> getUsedStorage(int userId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getUsedStorage(userId), executor);
    }

    public CompletableFuture<Boolean> moveFile(int fileId, Integer newParentId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.moveFile(fileId, newParentId), executor);
    }

    public CompletableFuture<List<DriveFileVersionModel>> getFileVersions(int fileId) {
        return CompletableFuture.supplyAsync(() -> driveFileDAO.getFileVersions(fileId), executor);
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
