package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.server.dao.ClassDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO;
import org.java_websocket.WebSocket;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler {
    private WebSocket socket;
    private String clientId;
    private int userId = -1;

    public static ConcurrentHashMap<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> otpStorage = new ConcurrentHashMap<>();

    public ClientHandler(WebSocket socket) {
        this.socket = socket;
        this.clientId = "User_" + socket.getRemoteSocketAddress().getPort(); 
        onlineClients.put(this.clientId, this);
    }

    public void onDisconnect() {
        System.out.println("[NGẮT KẾT NỐI] " + clientId + " đã rời khỏi hệ thống.");
        if (this.userId != -1) {
            DatabaseManager.updateLastSeen(this.userId);
            onlineClients.remove(this.clientId);
            broadcastToAll(new Packet("USER_OFFLINE", String.valueOf(this.userId)));
        }
        closeConnections();
    }
    
    public static boolean isUserOnline(String email) {
        return onlineClients.containsKey(email);
    }

    public void processClientRequest(Packet packet) {
        try {
            System.out.println("[NHẬN LỆNH TỪ " + clientId + "] " + packet.action);

            switch (packet.action) {
                case "GET_ALL_CLASSES": {
                    List<String> classes = ClassDAO.getAvailableClasses();
                    for (String classData : classes) {
                        sendPacket(new Packet("BROADCAST_CLASS", classData));
                        Thread.sleep(50); 
                    }
                    break;
                }
                
                case "SYNC_SESSION": {
                    this.userId = Integer.parseInt(packet.payload);
                    try {
                        Connection connSync = DatabaseManager.getConnection();
                        if (connSync != null) {
                            PreparedStatement pstSync = connSync.prepareStatement("SELECT email FROM users WHERE id = ?");
                            pstSync.setInt(1, this.userId);
                            ResultSet rsSync = pstSync.executeQuery();
                            if (rsSync.next()) {
                                this.clientId = rsSync.getString("email");
                            }
                            rsSync.close(); 
                            pstSync.close();
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi đồng bộ session: " + ex.getMessage());
                    }
                    break;
                }
                
                case "GET_CONVO_LIST": {
                    int uid = Integer.parseInt(packet.payload);
                    List<com.mycompany.tutorhub_enterprise.models.ConversationInfo> listConvo = com.mycompany.tutorhub_enterprise.server.dao.ChatDAO.getConversationList(uid);
                    sendPacket(new Packet("GET_CONVO_LIST", listConvo));
                    break;
                }

                case "GET_REELS": {
                    List<String> reels = DatabaseManager.getReels(this.userId);
                    sendPacket(new Packet("GET_REELS_RESPONSE", reels));
                    break;
                }
                case "LIKE_REEL": {
                    try {
                        int reelId = Integer.parseInt(packet.payload);
                        DatabaseManager.likeReel(reelId, this.userId);
                    } catch(Exception e) {}
                    break;
                }
                
                case "GET_REEL_COMMENTS": {
                    try {
                        int reelId = Integer.parseInt(packet.payload);
                        List<String> comments = DatabaseManager.getReelComments(reelId);
                        sendPacket(new Packet("GET_REEL_COMMENTS_RESPONSE", comments));
                    } catch(Exception e) {}
                    break;
                }
                
                case "ADD_REEL_COMMENT": {
                    try {
                        String[] parts = packet.payload.split(";;"); // reelId;;content
                        if (parts.length >= 2) {
                            int reelId = Integer.parseInt(parts[0]);
                            String content = parts[1];
                            if (DatabaseManager.insertReelComment(reelId, this.userId, content)) {
                                List<String> comments = DatabaseManager.getReelComments(reelId);
                                sendPacket(new Packet("GET_REEL_COMMENTS_RESPONSE", comments));
                            }
                        }
                    } catch(Exception e) {}
                    break;
                }
                
                case "UPLOAD_REEL": {
                    String[] parts = packet.payload.split(";;", -1); 
                    if(parts.length >= 3) {
                        String videoUrl = parts[0];
                        String caption = parts[1];
                        String hashtags = parts[2];
                        String location = parts.length >= 4 ? parts[3] : "";
                        String productLink = parts.length >= 5 ? parts[4] : "";
                        boolean ok = DatabaseManager.insertReel(this.userId, videoUrl, caption, hashtags, location, productLink);
                        sendPacket(new Packet(ok, ok ? "Đăng thước phim thành công!" : "Lỗi khi đăng."));
                    }
                    break;
                }

                case "UPLOAD_LOCKET": {
                    // payload: videoUrl;;title;;mediaType (image/video)
                    String[] parts = packet.payload.split(";;", -1);
                    if (parts.length >= 2) {
                        String mediaUrl = parts[0];
                        String title = parts[1];
                        String mediaType = parts.length >= 3 ? parts[2] : "video";
                        boolean ok = DatabaseManager.insertLocket(this.userId, mediaUrl, title, mediaType);
                        sendPacket(new Packet(ok, ok ? "Đăng Locket thành công!" : "Lỗi khi đăng."));
                        if (ok) {
                            // Reload for current user
                            sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
                        }
                    }
                    break;
                }
                
                case "GET_LOCKET_VIDEOS": {
                    sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
                    break;
                }
                
                case "DELETE_LOCKET": {
                    try {
                        int locketId = Integer.parseInt(packet.payload);
                        DatabaseManager.deleteLocket(locketId, this.userId);
                        sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
                    } catch (Exception e) { e.printStackTrace(); }
                    break;
                }

                case "GET_FULL_PROFILE": {
                    int uidProf = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
                    String profileDataStr = DatabaseManager.getFullProfile(uidProf);
                    
                    String[] pParts = profileDataStr.split(";;", -1);
                    if (pParts.length < 13) {
                        String[] expanded = new String[13];
                        System.arraycopy(pParts, 0, expanded, 0, pParts.length);
                        for(int i = pParts.length; i < 13; i++) expanded[i] = "null";
                        pParts = expanded;
                    }
                    
                    java.io.File fFront = new java.io.File("server_uploads/ekyc/front_" + uidProf + ".jpg");
                    if (fFront.exists()) {
                        try { pParts[11] = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(fFront.toPath())); } 
                        catch (Exception e) { pParts[11] = "null"; }
                    } else { pParts[11] = "null"; }

                    java.io.File fBack = new java.io.File("server_uploads/ekyc/back_" + uidProf + ".jpg");
                    if (fBack.exists()) {
                        try { pParts[12] = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(fBack.toPath())); } 
                        catch (Exception e) { pParts[12] = "null"; }
                    } else { pParts[12] = "null"; }
                    
                    sendPacket(new Packet("FULL_PROFILE_RESULT", String.join(";;", pParts)));
                    break;
                }
                
                case "GET_DEGREES": {
                    int uidDeg = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
                    sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(uidDeg)));
                    break;
                }

                case "GET_CERTIFICATES": {
                    int uidCert = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
                    sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(uidCert)));
                    break;
                }
                    
                case "GET_EXPERIENCES": {
                    int uidExp = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
                    sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(uidExp)));
                    break;
                }

                case "UPDATE_PROFILE": {
                    String[] pData = packet.payload.split(";;", -1); 
                    if (pData.length >= 8) {
                        DatabaseManager.updateProfile(this.userId, pData[0], pData[1], pData[2], pData[3], pData[4], pData[5], pData[6], pData[7]);
                        broadcastToAll(new Packet("ALL_TUTORS_RESULT", DatabaseManager.getAllTutors()));
                    }
                    break;   
                }

                case "UPDATE_CV": {
                    String[] cvData = packet.payload.split("\\|");
                    if (cvData.length == 2) {
                        try {
                            java.io.File dir = new java.io.File("server_uploads/cv"); if (!dir.exists()) dir.mkdirs();
                            java.io.File f = new java.io.File(dir, "cv_" + this.userId + "_" + cvData[0]);
                            java.nio.file.Files.write(f.toPath(), java.util.Base64.getDecoder().decode(cvData[1]));
                            DatabaseManager.updateCV(this.userId, f.getPath());
                            sendPacket(new Packet("FULL_PROFILE_RESULT", DatabaseManager.getFullProfile(this.userId)));
                        } catch (Exception e) {}
                    }
                    break;
                }

                case "UPDATE_EKYC": {
                    String[] ekycData = packet.payload.split("\\|\\|\\|");
                    if (ekycData.length == 2) {
                        try {
                            java.io.File dir = new java.io.File("server_uploads/ekyc"); if (!dir.exists()) dir.mkdirs();
                            java.io.File fileFront = new java.io.File(dir, "front_" + this.userId + ".jpg");
                            java.io.File fileBack = new java.io.File(dir, "back_" + this.userId + ".jpg");
                            java.nio.file.Files.write(fileFront.toPath(), java.util.Base64.getDecoder().decode(ekycData[0]));
                            java.nio.file.Files.write(fileBack.toPath(), java.util.Base64.getDecoder().decode(ekycData[1]));
                            DatabaseManager.updateEkyc(this.userId, fileFront.getPath(), fileBack.getPath());
                            
                            processClientRequest(new Packet("GET_FULL_PROFILE", String.valueOf(this.userId)));
                        } catch (Exception e) {}
                    }
                    break;
                }

                case "DOWNLOAD_FILE": {
                    String reqFileName = packet.payload;
                    try {
                        java.io.File targetFile = new java.io.File(reqFileName);
                        if (!targetFile.exists() && !targetFile.isAbsolute()) {
                            targetFile = new java.io.File("server_uploads/documents", reqFileName);
                            if (!targetFile.exists()) {
                                targetFile = new java.io.File("server_uploads/cv", reqFileName);
                            }
                        }

                        if (targetFile.exists()) {
                            byte[] fileBytes = java.nio.file.Files.readAllBytes(targetFile.toPath());
                            String base64FileData = java.util.Base64.getEncoder().encodeToString(fileBytes);
                            sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", base64FileData));
                        } else {
                            System.err.println("[SERVER LỖI] Không tìm thấy file: " + targetFile.getPath());
                            sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", "ERROR"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", "ERROR"));
                    }
                    break;
                }
                    
                case "ADD_EXPERIENCE": {
                    String[] expData = packet.payload.split("\\|");
                    if (expData.length >= 3) {
                        DatabaseManager.insertExperience(this.userId, expData[0], expData[1], expData[2]);
                        sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(this.userId)));
                    }
                    break;
                }

                case "ADD_TUTOR_BY_ADMIN": {
                    String[] newTutorData = packet.payload.split("\\|");
                    if (newTutorData.length >= 6) {
                        String email = newTutorData[0];
                        String rawPass = newTutorData[1];
                        String fullName = newTutorData[2];
                        
                        if (DatabaseManager.isEmailExists(email)) {
                            sendPacket(new Packet(false, "Email này đã tồn tại trong hệ thống!"));
                        } else {
                            boolean isSuccess = DatabaseManager.registerUser(email, rawPass, fullName, "TUTOR");
                            if (isSuccess) sendPacket(new Packet(true, "Tạo tài khoản gia sư thành công!"));
                            else sendPacket(new Packet(false, "Lỗi Server: Không thể ghi vào cơ sở dữ liệu."));
                        }
                    }
                    break;
                }

                case "ADD_DEGREE": {
                    String[] degData = packet.payload.split("\\|", 6);
                    if (degData.length == 6) {
                        try {
                            java.io.File dir = new java.io.File("server_uploads/documents");
                            if (!dir.exists()) dir.mkdirs();
                            String safeFileName = this.userId + "_" + System.currentTimeMillis() + "_" + degData[4].replaceAll("[^a-zA-Z0-9.-]", "_");
                            java.io.File docFile = new java.io.File(dir, safeFileName);
                            byte[] fileBytes = java.util.Base64.getDecoder().decode(degData[5]);
                            java.nio.file.Files.write(docFile.toPath(), fileBytes);
                            
                            boolean ok = DatabaseManager.insertDegree(this.userId, degData[0], degData[1], degData[2], degData[3], docFile.getPath());
                            if (ok) {
                                sendPacket(new Packet("RESPONSE", "Đã thêm Bằng cấp thành công! Chờ Admin duyệt."));
                                sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(this.userId)));
                            }
                        } catch (Exception e) {}
                    }
                    break;
                }

                case "ADD_CERTIFICATE": {
                    String[] certData = packet.payload.split("\\|", 6);
                    if (certData.length == 6) {
                        try {
                            java.io.File dir = new java.io.File("server_uploads/documents");
                            if (!dir.exists()) dir.mkdirs();
                            String safeFileName = "cert_" + this.userId + "_" + System.currentTimeMillis() + "_" + certData[4].replaceAll("[^a-zA-Z0-9.-]", "_");
                            java.io.File docFile = new java.io.File(dir, safeFileName);
                            byte[] fileBytes = java.util.Base64.getDecoder().decode(certData[5]);
                            java.nio.file.Files.write(docFile.toPath(), fileBytes);
                            
                            boolean ok = DatabaseManager.insertCertificate(this.userId, certData[0], certData[1], certData[2], certData[3], docFile.getPath());
                            if (ok) {
                                sendPacket(new Packet("RESPONSE", "Đã thêm Chứng chỉ thành công! Chờ Admin duyệt."));
                                sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(this.userId)));
                            }
                        } catch (Exception e) {}
                    }
                    break;
                }

                case "SEARCH_USER": {
                    String keyword = packet.payload;
                    List<com.mycompany.tutorhub_enterprise.models.UserInfo> searchResults = 
                        com.mycompany.tutorhub_enterprise.server.dao.UserDAO.searchUsers(keyword, this.userId);
                    sendPacket(new Packet("SEARCH_USER_RESULT", searchResults));
                    break;
                }

                case "SEND_FRIEND_REQUEST": {
                    int targetId = Integer.parseInt(packet.payload);
                    boolean isReqSent = com.mycompany.tutorhub_enterprise.server.dao.UserDAO.sendFriendRequest(this.userId, targetId);
                    if (isReqSent) sendPacket(new Packet(true, "Đã gửi lời mời kết bạn!", "FRIEND_REQUEST_SENT"));
                    else sendPacket(new Packet(false, "Lỗi: Không thể gửi lời mời."));
                    break;
                }

                case "ACCEPT_FRIEND": {
                    int requesterId = Integer.parseInt(packet.payload);
                    boolean isAccepted = com.mycompany.tutorhub_enterprise.server.dao.UserDAO.acceptFriendRequest(requesterId, this.userId);
                    if (isAccepted) {
                        sendPacket(new Packet(true, "Đã trở thành bạn bè!", "FRIEND_ACCEPTED"));
                        List<com.mycompany.tutorhub_enterprise.models.ConversationInfo> listConvoNew = 
                            com.mycompany.tutorhub_enterprise.server.dao.ChatDAO.getConversationList(this.userId);
                        sendPacket(new Packet("GET_CONVO_LIST", listConvoNew));
                    }
                    break;  
                }
                    
                case "GET_MESSAGES": {
                    String[] partsMsg = packet.payload.split("\\|");
                    int currentUid = Integer.parseInt(partsMsg[0]);
                    int convoId = Integer.parseInt(partsMsg[1]);
                    List<com.mycompany.tutorhub_enterprise.models.Message> msgs = com.mycompany.tutorhub_enterprise.server.dao.ChatDAO.getMessages(convoId, currentUid);
                    sendPacket(new Packet("GET_MESSAGES", msgs));
                    break;
                }

               case "SEND_CHAT": {
                    String[] chatData = packet.payload.split("\\|", 3);
                    if (chatData.length >= 3) {
                        String conversationId = chatData[0];
                        String messageType = chatData[1];
                        String content = chatData[2];

                        String senderName = "Gia sư " + this.clientId; 
                        String forwardPayload = conversationId + "|" + senderName + "|" + content;
                        
                        for (ClientHandler client : onlineClients.values()) {
                            if (!client.clientId.equals(this.clientId)) {
                                client.sendPacket(new Packet("RECEIVE_CHAT", forwardPayload));
                            }
                        }

                        try {
                            Connection conn = DatabaseManager.getConnection();
                            if (conn != null) {
                                String sql = "INSERT INTO messages (conversation_id, sender_id, message_type, content, is_read) VALUES (?, ?, ?, ?, false)";
                                PreparedStatement pst = conn.prepareStatement(sql);
                                pst.setInt(1, Integer.parseInt(conversationId));
                                pst.setInt(2, this.userId); 
                                pst.setString(3, messageType);
                                pst.setString(4, content);
                                pst.executeUpdate();
                                pst.close();
                            }
                        } catch (Exception dbErr) {
                            System.err.println("[SERVER DB LỖI] Không thể lưu tin nhắn: " + dbErr.getMessage());
                        }
                    }
                    break;
               }

                case "MARK_AS_READ": {
                    String convoIdStr = packet.payload;
                    try {
                        Connection connRead = DatabaseManager.getConnection();
                        if (connRead != null) {
                            String sqlRead = "UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id != ?";
                            PreparedStatement pstRead = connRead.prepareStatement(sqlRead);
                            pstRead.setInt(1, Integer.parseInt(convoIdStr));
                            pstRead.setInt(2, this.userId);
                            
                            int rowsAffected = pstRead.executeUpdate();
                            pstRead.close();
                            
                            if (rowsAffected > 0) {
                                broadcastToAll(new Packet("READ_ACK", convoIdStr));
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[SERVER LỖI] Lỗi cập nhật trạng thái đã xem: " + ex.getMessage());
                    }
                    break;
                }
                
                case "SAVE_BOARD": {
                    String[] boardData = packet.payload.split("\\|", 3);
                    if (boardData.length == 3) {
                        String title = boardData[0];
                        String className = boardData[1];
                        String base64Data = boardData[2];

                        boolean isSaved = com.mycompany.tutorhub_enterprise.server.dao.BoardDAO.saveBoard(this.userId, title, className, base64Data);
                        if (isSaved) {
                            sendPacket(new Packet("SAVE_BOARD_SUCCESS", ""));
                            String boardsDataStr = com.mycompany.tutorhub_enterprise.server.dao.BoardDAO.getUserBoards(this.userId);
                            sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataStr));
                        } else {
                            sendPacket(new Packet("RESPONSE", "Lỗi: Không thể lưu bảng vẽ lên Database!"));
                        }
                    }
                    break;
                }
                
                case "UPDATE_TUTOR_STATUS": {
                    String[] statusData = packet.payload.split("\\|");
                    if (statusData.length == 2) {
                        try {
                            Connection connStat = DatabaseManager.getConnection();
                            if (connStat != null) {
                                PreparedStatement pstStat = connStat.prepareStatement("UPDATE users SET status = ? WHERE id = ?");
                                pstStat.setString(1, statusData[1]);
                                pstStat.setInt(2, Integer.parseInt(statusData[0]));
                                pstStat.executeUpdate();
                                pstStat.close();
                                
                                java.util.List<String> updatedTutors = DatabaseManager.getAllTutors();
                                broadcastToAll(new Packet("ALL_TUTORS_RESULT", updatedTutors));
                            }
                        } catch (Exception ex) {
                            System.err.println("Lỗi cập nhật trạng thái gia sư: " + ex.getMessage());
                        }
                    }
                    break;
                }
                    
                case "UPDATE_BOARD": {
                    String[] updateData = packet.payload.split("\\|", 4);
                    if (updateData.length == 4) {
                        String uBoardId = updateData[0];
                        String uTitle = updateData[1];
                        String uClass = updateData[2];
                        String uBase64 = updateData[3];

                        try {
                            Connection connUpd = DatabaseManager.getConnection();
                            if (connUpd != null) {
                                PreparedStatement pstUpd = connUpd.prepareStatement(
                                    "UPDATE blackboards SET title = ?, class_name = ?, thumbnail_base64 = ?, last_modified = CURRENT_TIMESTAMP WHERE board_id = ?"
                                );
                                pstUpd.setString(1, uTitle);
                                pstUpd.setString(2, uClass);
                                pstUpd.setString(3, uBase64);
                                pstUpd.setString(4, uBoardId);
                                int updatedRows = pstUpd.executeUpdate();
                                pstUpd.close();

                                if (updatedRows == 0) {
                                    PreparedStatement pstInsert = connUpd.prepareStatement(
                                        "INSERT INTO blackboards (board_id, tutor_id, title, class_name, thumbnail_base64) VALUES (?, ?, ?, ?, ?)"
                                    );
                                    pstInsert.setString(1, uBoardId);
                                    pstInsert.setInt(2, this.userId);
                                    pstInsert.setString(3, uTitle);
                                    pstInsert.setString(4, uClass);
                                    pstInsert.setString(5, uBase64);
                                    pstInsert.executeUpdate();
                                    pstInsert.close();
                                }

                                sendPacket(new Packet("SAVE_BOARD_SUCCESS", ""));
                                String boardsDataStr = com.mycompany.tutorhub_enterprise.server.dao.BoardDAO.getUserBoards(this.userId);
                                sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataStr));
                            }
                        } catch (Exception e) {
                            System.err.println("[SERVER LỖI] Cập nhật bảng vẽ: " + e.getMessage());
                        }
                    }
                    break;
                }

                case "GET_USER_BOARDS": {
                    String boardsDataList = com.mycompany.tutorhub_enterprise.server.dao.BoardDAO.getUserBoards(this.userId);
                    sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataList));
                    break;
                }
                    
                case "DELETE_BOARD": {
                    String boardId = packet.payload;
                    Connection connDel = DatabaseManager.getConnection();
                    if (connDel != null) {
                        PreparedStatement pstDel = connDel.prepareStatement("DELETE FROM blackboards WHERE board_id = ?");
                        pstDel.setString(1, boardId);
                        pstDel.executeUpdate();
                        pstDel.close();
                    }
                    break;
                }
                
                case "DELETE_DEGREE": {
                    try {
                        Connection connDel = DatabaseManager.getConnection();
                        if (connDel != null) {
                            boolean deleted = false;
                            String[] possibleColumns = {"university", "degree_name"};
                            for (String col : possibleColumns) {
                                try {
                                    PreparedStatement pst = connDel.prepareStatement("DELETE FROM tutor_degrees WHERE user_id = ? AND " + col + " = ?");
                                    pst.setInt(1, this.userId);
                                    pst.setString(2, packet.payload);
                                    int rows = pst.executeUpdate();
                                    pst.close();
                                    if (rows > 0) { deleted = true; break; }
                                } catch(Exception ex) {} 
                            }
                            if(!deleted) System.err.println("Không thể xóa Bằng cấp, hãy kiểm tra lại tên cột trong DB!");
                            sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(this.userId)));
                        }
                    } catch (Exception e) { System.err.println("Lỗi xóa Bằng cấp: " + e.getMessage()); }
                    break;
                }

                case "DELETE_CERTIFICATE": {
                    try {
                        Connection connDel = DatabaseManager.getConnection();
                        if (connDel != null) {
                            boolean deleted = false;
                            String[] possibleColumns = {"cert_name", "name", "certificate_name", "title"};
                            for (String col : possibleColumns) {
                                try {
                                    PreparedStatement pst = connDel.prepareStatement("DELETE FROM tutor_certificates WHERE user_id = ? AND " + col + " = ?");
                                    pst.setInt(1, this.userId);
                                    pst.setString(2, packet.payload);
                                    int rows = pst.executeUpdate();
                                    pst.close();
                                    if (rows > 0) { deleted = true; break; }
                                } catch(Exception ex) {} 
                            }
                            if(!deleted) System.err.println("Không thể xóa Chứng chỉ, hãy kiểm tra lại tên cột trong DB!");
                            sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(this.userId)));
                        }
                    } catch (Exception e) { System.err.println("Lỗi xóa Chứng chỉ: " + e.getMessage()); }
                    break;
                }

                case "DELETE_EXPERIENCE": {
                    try {
                        String[] expParts = packet.payload.split("\\|");
                        if (expParts.length >= 2) {
                            Connection connDel = DatabaseManager.getConnection();
                            if (connDel != null) {
                                boolean deleted = false;
                                String[] timeCols = {"duration", "time_period", "period", "time"};
                                String[] titleCols = {"location", "title", "position", "company"};
                                
                                for (String tCol : timeCols) {
                                    for (String pCol : titleCols) {
                                        try {
                                            PreparedStatement pst = connDel.prepareStatement("DELETE FROM tutor_experiences WHERE user_id = ? AND " + tCol + " = ? AND " + pCol + " = ?");
                                            pst.setInt(1, this.userId); 
                                            pst.setString(2, expParts[0].trim()); 
                                            pst.setString(3, expParts[1].trim());
                                            int rows = pst.executeUpdate();
                                            pst.close();
                                            if (rows > 0) { deleted = true; break; }
                                        } catch(Exception ex) {}
                                    }
                                    if (deleted) break;
                                }
                                if(!deleted) System.err.println("Không thể xóa Kinh nghiệm, hãy kiểm tra lại tên cột trong DB!");
                                sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(this.userId)));
                            }
                        }
                    } catch (Exception e) { System.err.println("Lỗi xóa Kinh nghiệm: " + e.getMessage()); }
                    break;
                }
                    
                case "RENAME_BOARD": {
                    String[] renameParts = packet.payload.split("\\|");
                    if(renameParts.length == 2) {
                        Connection connRen = DatabaseManager.getConnection();
                        if (connRen != null) {
                            PreparedStatement pstRen = connRen.prepareStatement("UPDATE blackboards SET title = ? WHERE board_id = ?");
                            pstRen.setString(1, renameParts[1]);
                            pstRen.setString(2, renameParts[0]);
                            pstRen.executeUpdate();
                            pstRen.close();
                        }
                    }
                    break;
                }

                case "REQUEST_OTP_RESET": {
                    String emailReset = packet.payload;
                    if (!DatabaseManager.isEmailExists(emailReset)) {
                        sendPacket(new Packet(false, "Email này chưa được đăng ký trong hệ thống!"));
                        break;
                    }
                    String otpReset = String.format("%06d", new java.util.Random().nextInt(999999));
                    otpStorage.put(emailReset, otpReset);
                    new Thread(() -> {
                        boolean isSent = EmailService.sendOTP(emailReset, otpReset);
                        if (isSent) sendPacket(new Packet(true, "Mã OTP khôi phục đã được gửi đến email!"));
                        else {
                            sendPacket(new Packet(false, "Lỗi Server: Không thể gửi Email lúc này."));
                            otpStorage.remove(emailReset);
                        }
                    }).start();
                    break;
                }

                case "VERIFY_AND_RESET": {
                    String[] resetData = packet.payload.split(",");
                    if (resetData.length == 3) {
                        String reqEmail = resetData[0];
                        String reqOtp = resetData[1];
                        String newPass = resetData[2];

                        String expectedOtp = otpStorage.get(reqEmail);
                        if (expectedOtp != null && expectedOtp.equals(reqOtp)) {
                            if (DatabaseManager.resetPassword(reqEmail, newPass)) {
                                otpStorage.remove(reqEmail); 
                                sendPacket(new Packet(true, "Tuyệt vời! Mật khẩu của bạn đã được cập nhật."));
                            } else {
                                sendPacket(new Packet(false, "Lỗi Server: Không thể cập nhật mật khẩu!"));
                            }
                        } else {
                            sendPacket(new Packet(false, "Mã OTP không chính xác hoặc đã hết hạn!"));
                        }
                    }
                    break;  
                }

               case "UPDATE_AVATAR": {
                    String base64Image = packet.payload;
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
                    java.io.File uploadDir = new java.io.File("server_uploads/avatars");
                    if (!uploadDir.exists()) uploadDir.mkdirs();
                    
                    String safeEmail = this.clientId.replaceAll("[^a-zA-Z0-9.-]", "_");
                    String fileName = "avatar_" + safeEmail + ".jpg";
                    java.io.File avatarFile = new java.io.File(uploadDir, fileName);
                    java.nio.file.Files.write(avatarFile.toPath(), imageBytes);
                    
                    Connection connAva = DatabaseManager.getConnection();
                    if (connAva != null) {
                        String sql = "UPDATE users SET avatar_url = ? WHERE email = ?";
                        PreparedStatement pst = connAva.prepareStatement(sql);
                        pst.setString(1, "server_uploads/avatars/" + fileName);
                        pst.setString(2, this.clientId); 
                        pst.executeUpdate();
                        pst.close();
                    }
                    sendPacket(new Packet("UPDATE_AVATAR_SUCCESS", base64Image));
                    break;
               }

                case "GET_PROFILE": {
                    Connection connPro = DatabaseManager.getConnection();
                    if (connPro != null) {
                        String sql = "SELECT avatar_url FROM users WHERE email = ?";
                        PreparedStatement pst = connPro.prepareStatement(sql);
                        pst.setString(1, this.clientId);
                        ResultSet rs = pst.executeQuery();
                        if (rs.next()) {
                            String avatarUrl = rs.getString("avatar_url");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                java.io.File aFile = new java.io.File(avatarUrl);
                                if (aFile.exists()) {
                                    byte[] fileBytes = java.nio.file.Files.readAllBytes(aFile.toPath());
                                    String b64Image = java.util.Base64.getEncoder().encodeToString(fileBytes);
                                    sendPacket(new Packet("LOAD_AVATAR", b64Image));
                                }
                            }
                        }
                        rs.close();
                        pst.close();
                    }
                    break;
                }

                case "GET_USER_AVATAR": {
                    try {
                        int targetUid = Integer.parseInt(packet.payload);
                        Connection connPro = DatabaseManager.getConnection();
                        if (connPro != null) {
                            String sql = "SELECT avatar_url FROM users WHERE id = ?";
                            PreparedStatement pst = connPro.prepareStatement(sql);
                            pst.setInt(1, targetUid);
                            ResultSet rs = pst.executeQuery();
                            if (rs.next()) {
                                String avatarUrl = rs.getString("avatar_url");
                                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                    java.io.File aFile = new java.io.File(avatarUrl);
                                    if (aFile.exists()) {
                                        byte[] fileBytes = java.nio.file.Files.readAllBytes(aFile.toPath());
                                        String b64Image = java.util.Base64.getEncoder().encodeToString(fileBytes);
                                        sendPacket(new Packet("LOAD_TUTOR_AVATAR", b64Image));
                                    }
                                }
                            }
                            rs.close();
                            pst.close();
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi lấy avatar gia sư: " + ex.getMessage());
                    }
                    break;
                }
                    
                case "ACCEPT_CLASS": {
                    String classCode = packet.payload;
                    boolean success = ClassDispatcher.getInstance().processAcceptClass(classCode, 1); 
                    if (success) {
                        sendPacket(new Packet("ACCEPT_SUCCESS", classCode));
                        broadcastToAll(new Packet("CLASS_TAKEN", classCode));
                        Connection connAcc = DatabaseManager.getConnection();
                        if (connAcc != null) {
                            String sqlTask = "INSERT INTO tutor_tasks (tutor_name, category, title, schedule_time, location) VALUES (?, 'TEACH', ?, ?, ?)";
                            PreparedStatement pstTask = connAcc.prepareStatement(sqlTask);
                            pstTask.setString(1, this.clientId); 
                            pstTask.setString(2, "Dạy lớp " + classCode);
                            pstTask.setString(3, "Thời gian: Theo thỏa thuận");
                            pstTask.setString(4, "Địa điểm: Đang cập nhật");
                            pstTask.executeUpdate();
                            pstTask.close();
                        }
                        sendPacket(new Packet("REFRESH_TASKS", ""));
                    } else {
                        sendPacket(new Packet("ACCEPT_FAIL", classCode));
                    }
                    break;
                }

                case "GET_TASKS": {
                    Connection connTask = DatabaseManager.getConnection();
                    if (connTask != null) {
                        String sql = "SELECT * FROM tutor_tasks WHERE tutor_name = ? ORDER BY created_at DESC";
                        PreparedStatement pst = connTask.prepareStatement(sql);
                        pst.setString(1, this.clientId);
                        ResultSet rs = pst.executeQuery();
                        StringBuilder sb = new StringBuilder();
                        while(rs.next()) {
                            sb.append(rs.getInt("id")).append("|")
                              .append(rs.getString("category")).append("|")
                              .append(rs.getString("title")).append("|")
                              .append(rs.getString("schedule_time")).append("|")
                              .append(rs.getString("location")).append("|")
                              .append(rs.getBoolean("is_completed")).append(";;");
                        }
                        rs.close(); pst.close();
                        sendPacket(new Packet("SYNC_TASKS", sb.toString()));
                    }
                    break;
                }
                
                case "CREATE_CLASSROOM_AND_ENTER": {
                    // payload: className|organizationName
                    String[] data = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
                    String className = data.length > 0 ? data[0].trim() : "";
                    String organizationName = data.length > 1 && !data[1].trim().isEmpty() ? data[1].trim() : "My Account";

                    if (this.userId <= 0) {
                        sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Ban can dang nhap truoc khi tao lop hoc.");
                        break;
                    }
                    if (className.isEmpty()) {
                        sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Ten lop hoc khong duoc de trong.");
                        break;
                    }

                    ClassroomGroupModel classroom = new ClassroomGroupModel();
                    classroom.setOwnerId(this.userId);
                    classroom.setName(className);
                    classroom.setDescription("");
                    classroom.setCoverImage("");
                    classroom.setOrganizationName(organizationName);

                    ClassroomLessonModel lesson = new ClassroomLessonModel();
                    lesson.setTitle(className);
                    lesson.setDurationMinutes(40);
                    lesson.setSeatCount(6);
                    lesson.setStatus("LIVE");
                    lesson.setLessonType("CLASSROOM");
                    lesson.setStageLayout("1V6");
                    lesson.setLobbyEnabled(true);
                    lesson.setAllowStudentDraw(false);
                    lesson.setRecordingEnabled(false);
                    lesson.setCreatedBy(this.userId);

                    ClassroomDAO dao = new ClassroomDAO();
                    if (dao.createLiveClassroom(classroom, lesson)) {
                        String resultPayload = classroom.getId() + "|" + lesson.getId() + "|" + lesson.getBoardId() + "|" + classroom.getName();
                        Packet success = new Packet("CREATE_CLASSROOM_AND_ENTER_SUCCESS", resultPayload);
                        success.success = true;
                        success.message = "Tao lop hoc thanh cong.";
                        sendPacket(success);
                        sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", dao.getClassroomsByUser(this.userId)));
                        sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", dao.getLessonsByUser(this.userId)));
                        broadcastToOthers(new Packet("BROADCAST_LIVE_CLASS", lesson.getBoardId() + "|" + classroom.getName()));
                    } else {
                        sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Khong the tao lop hoc live.");
                    }
                    break;
                }

                case "BROADCAST_LIVE_CLASS": {
                    broadcastToOthers(packet);
                    break;
                }

                case "CREATE_PUBLIC_LESSON": {
                    // payload: lessonName|organizationName|startMillis|durationMinutes|stageLayout|lobby|allowDraw|recording|coTeachers
                    String[] data = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
                    String lessonName = data.length > 0 ? data[0].trim() : "";
                    String organizationName = data.length > 1 && !data[1].trim().isEmpty() ? data[1].trim() : "My Account";
                    long startMillis = data.length > 2 ? parseLongOrDefault(data[2], System.currentTimeMillis()) : System.currentTimeMillis();
                    int durationMinutes = data.length > 3 ? parseIntOrDefault(data[3], 40) : 40;
                    String stageLayout = data.length > 4 && !data[4].trim().isEmpty() ? data[4].trim() : "1V6";
                    boolean lobbyEnabled = data.length > 5 ? Boolean.parseBoolean(data[5]) : true;
                    boolean allowStudentDraw = data.length > 6 && Boolean.parseBoolean(data[6]);
                    boolean recordingEnabled = data.length > 7 && Boolean.parseBoolean(data[7]);
                    String coTeachers = data.length > 8 ? data[8].trim() : "";

                    if (this.userId <= 0) {
                        sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Ban can dang nhap truoc khi tao public lesson.");
                        break;
                    }
                    if (lessonName.isEmpty()) {
                        sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Ten public lesson khong duoc de trong.");
                        break;
                    }
                    if (durationMinutes <= 0) {
                        durationMinutes = 40;
                    }

                    ClassroomGroupModel classroom = new ClassroomGroupModel();
                    classroom.setOwnerId(this.userId);
                    classroom.setName(lessonName);
                    classroom.setDescription(coTeachers.isEmpty() ? "Public Lesson" : "Public Lesson - Co-teachers: " + coTeachers);
                    classroom.setCoverImage("");
                    classroom.setOrganizationName(organizationName);

                    ClassroomLessonModel lesson = new ClassroomLessonModel();
                    lesson.setTitle(lessonName);
                    lesson.setStartTime(new java.sql.Timestamp(startMillis));
                    lesson.setDurationMinutes(durationMinutes);
                    lesson.setSeatCount(seatCountForStageLayout(stageLayout));
                    lesson.setStatus("SCHEDULED");
                    lesson.setLessonType("PUBLIC");
                    lesson.setStageLayout(stageLayout);
                    lesson.setLobbyEnabled(lobbyEnabled);
                    lesson.setAllowStudentDraw(allowStudentDraw);
                    lesson.setRecordingEnabled(recordingEnabled);
                    lesson.setCreatedBy(this.userId);

                    ClassroomDAO dao = new ClassroomDAO();
                    if (dao.createPublicLesson(classroom, lesson)) {
                        String resultPayload = classroom.getId() + "|" +
                                lesson.getId() + "|" +
                                lesson.getBoardId() + "|" +
                                lesson.getTitle() + "|" +
                                startMillis + "|" +
                                classroom.getJoinCode();
                        Packet success = new Packet("CREATE_PUBLIC_LESSON_SUCCESS", resultPayload);
                        success.success = true;
                        success.message = "Da post public lesson.";
                        sendPacket(success);
                        sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", dao.getClassroomsByUser(this.userId)));
                        sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", dao.getLessonsByUser(this.userId)));
                    } else {
                        sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Khong the tao public lesson.");
                    }
                    break;
                }

                case "CREATE_CLASSROOM": {
                    // payload: name|description|coverImage
                    String[] data = packet.payload.split("\\|", -1);
                    if (data.length >= 2) {
                        com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel model = new com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel();
                        model.setOwnerId(this.userId);
                        model.setName(data[0]);
                        model.setDescription(data[1]);
                        model.setCoverImage(data.length > 2 ? data[2] : "");
                        
                        com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO dao = new com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO();
                        boolean ok = dao.createClassroom(model);
                        if (ok) {
                            sendPacket(new Packet(true, "Tạo lớp học thành công!", "CREATE_CLASSROOM_SUCCESS"));
                            sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", dao.getClassroomsByUser(this.userId)));
                        } else {
                            sendPacket(new Packet(false, "Lỗi khi tạo lớp học!", "CREATE_CLASSROOM_FAIL"));
                        }
                    }
                    break;
                }
                
                case "GET_CLASSROOMS": {
                    com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO dao = new com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO();
                    java.util.List<com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel> list = dao.getClassroomsByUser(this.userId);
                    sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", list));
                    break;
                }

                case "GET_CLASSROOM_LESSONS": {
                    ClassroomDAO dao = new ClassroomDAO();
                    java.util.List<ClassroomLessonModel> list = dao.getLessonsByUser(this.userId);
                    sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", list));
                    break;
                }

                case "JOIN_PUBLIC_LESSON": {
                    String joinCode = packet.payload == null ? "" : packet.payload.trim();
                    if (this.userId <= 0) {
                        sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Ban can dang nhap truoc khi tham gia public lesson.");
                        break;
                    }
                    if (joinCode.isEmpty()) {
                        sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Ma public lesson khong duoc de trong.");
                        break;
                    }

                    ClassroomDAO dao = new ClassroomDAO();
                    ClassroomLessonModel lesson = dao.joinPublicLessonByCode(this.userId, joinCode);
                    if (lesson != null) {
                        boolean waiting = "WAITING".equalsIgnoreCase(lesson.getMemberStatus());
                        Packet result = new Packet(waiting ? "JOIN_PUBLIC_LESSON_WAITING" : "JOIN_PUBLIC_LESSON_SUCCESS", lesson);
                        result.success = true;
                        result.message = waiting
                                ? "Da gui yeu cau vao lobby. Vui long cho giao vien duyet."
                                : "Tham gia public lesson thanh cong.";
                        sendPacket(result);
                        sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", dao.getLessonsByUser(this.userId)));
                        if (waiting) {
                            Packet waitingList = new Packet(
                                    "PUBLIC_LESSON_WAITING_ROOM_UPDATED",
                                    dao.getWaitingMembersForLesson(lesson.getId(), lesson.getCreatedBy())
                            );
                            waitingList.message = "Co hoc vien dang cho vao lobby.";
                            sendToUser(lesson.getCreatedBy(), waitingList);
                        }
                    } else {
                        sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Khong tim thay public lesson tu ma nay.");
                    }
                    break;
                }

                case "GET_PUBLIC_LESSON_WAITING_ROOM": {
                    int lessonId = parseIntOrDefault(packet.payload, 0);
                    ClassroomDAO dao = new ClassroomDAO();
                    List<ClassroomMemberModel> waitingMembers = dao.getWaitingMembersForLesson(lessonId, this.userId);
                    sendPacket(new Packet("GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE", waitingMembers));
                    break;
                }

                case "APPROVE_PUBLIC_LESSON_STUDENT": {
                    String[] approveData = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
                    int lessonId = approveData.length > 0 ? parseIntOrDefault(approveData[0], 0) : 0;
                    int studentId = approveData.length > 1 ? parseIntOrDefault(approveData[1], 0) : 0;

                    ClassroomDAO dao = new ClassroomDAO();
                    ClassroomLessonModel approvedLesson = dao.approveLessonStudent(lessonId, studentId, this.userId);
                    if (approvedLesson != null) {
                        sendPacket(new Packet("GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE", dao.getWaitingMembersForLesson(lessonId, this.userId)));
                        Packet approved = new Packet("PUBLIC_LESSON_APPROVED", approvedLesson);
                        approved.success = true;
                        approved.message = "Giao vien da duyet ban vao public lesson.";
                        sendToUser(studentId, approved);
                        sendLessonsToUser(studentId, dao);
                    } else {
                        sendActionMessage("APPROVE_PUBLIC_LESSON_STUDENT_FAIL", false, "Khong the duyet hoc vien nay.");
                    }
                    break;
                }
                    
                case "CREATE_CLASS": {
                    String[] classData = packet.payload.split("\\|", -1);
                    if (classData.length >= 6) {
                        String type = classData[0]; String subj = classData[1];
                        String tuition = classData[2]; String loc = classData[3];
                        String title = classData[4]; String desc = classData[5];
                        String newId = "CLASS_" + System.currentTimeMillis(); 
                        boolean isInserted = ClassDAO.insertClass(newId, subj, tuition, loc, title, desc);
                        if (isInserted) {
                            String time = "Thứ 2,4,6"; 
                            double salNum = 0;
                            try { salNum = Double.parseDouble(tuition.replaceAll("[^0-9]", "")); } catch(Exception e){}
                            String formattedSalary = String.format("%,.0fđ/buổi", salNum).replace(",", ".");
                            String broadcastPayload = newId + "|" + subj + "|" + formattedSalary + "|" + loc + "|" + time + "|" + title + "|MỚI|#10B981";
                            broadcastToAll(new Packet("BROADCAST_CLASS", broadcastPayload));
                        }
                    }
                    break;
                }
                
                case "LOGIN": {
                    String[] loginData = packet.payload.split("\\|");
                    if (loginData.length == 2) {
                        int loginUid = DatabaseManager.authenticateByEmail(loginData[0], loginData[1]);
                        if (loginUid != -1) {
                            
                            String userRole = "TUTOR"; 
                            String avatarBase64 = "NO_AVATAR"; 
                            try {
                                Connection connRole = DatabaseManager.getConnection();
                                if (connRole != null) {
                                    PreparedStatement pstRole = connRole.prepareStatement("SELECT role, avatar_url FROM users WHERE id = ?");
                                    pstRole.setInt(1, loginUid);
                                    ResultSet rsRole = pstRole.executeQuery();
                                    
                                    if (rsRole.next()) {
                                        String fetchedRole = rsRole.getString("role");
                                        if (fetchedRole != null && !fetchedRole.trim().isEmpty()) {
                                            userRole = fetchedRole.trim().toUpperCase(); 
                                        }
                                        
                                        String avatarUrl = rsRole.getString("avatar_url");
                                        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                                            java.io.File aFile = new java.io.File(avatarUrl);
                                            if (aFile.exists()) {
                                                byte[] fileBytes = java.nio.file.Files.readAllBytes(aFile.toPath());
                                                avatarBase64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
                                            }
                                        }
                                    }
                                    rsRole.close();
                                    pstRole.close();
                                }
                            } catch (Exception ex) {
                                System.err.println("[SERVER LỖI] Không thể lấy Role và Avatar: " + ex.getMessage());
                            }

                            onlineClients.remove(this.clientId);
                            this.clientId = loginData[0]; 
                            this.userId = loginUid; 
                            onlineClients.put(this.clientId, this);
                            sendPacket(new Packet(true, "Đăng nhập thành công!", "DASHBOARD_GO|" + loginUid + "|" + userRole + "|" + avatarBase64));

                            for (ClientHandler client : onlineClients.values()) {
                                if (!client.clientId.equals(this.clientId)) {
                                    client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
                                }
                            }
                        } else {
                            sendPacket(new Packet(false, "Sai Email hoặc mật khẩu!", ""));
                        }
                    }
                    break;
                }
                
             case "GET_ALL_TUTORS": {
                    java.util.List<String> tutors = DatabaseManager.getAllTutors();
                    sendPacket(new Packet("ALL_TUTORS_RESULT", tutors));
                    break;
             }

                case "REQUEST_OTP": {
                    String email = packet.payload;
                    if (DatabaseManager.isEmailExists(email)) {
                        sendPacket(new Packet(false, "Email này đã được đăng ký trong hệ thống!"));
                        break;
                    }
                    String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
                    otpStorage.put(email, otpCode);
                    new Thread(() -> {
                        boolean isSent = EmailService.sendOTP(email, otpCode);
                        if (isSent) sendPacket(new Packet(true, "Mã OTP đã được gửi đến hộp thư của bạn."));
                        else {
                            sendPacket(new Packet(false, "Không thể gửi Email. Vui lòng kiểm tra lại cấu hình SMTP hoặc Email."));
                            otpStorage.remove(email);
                        }
                    }).start();
                    break;
                }

                case "VERIFY_AND_REGISTER": {
                    String[] regData = packet.payload.split(",");
                    if (regData.length == 4) {
                        String regEmail = regData[0]; String otpInput = regData[1];
                        String rawPass = regData[2]; String name = regData[3];
                        String expectedOtp = otpStorage.get(regEmail);
                        if (expectedOtp != null && expectedOtp.equals(otpInput)) {
                            boolean regOk = DatabaseManager.registerUser(regEmail, rawPass, name, "TUTOR");
                            if (regOk) {
                                otpStorage.remove(regEmail); 
                                sendPacket(new Packet(true, "Đăng ký thành công! Hãy quay lại đăng nhập."));
                            } else {
                                sendPacket(new Packet(false, "Lỗi Server: Không thể tạo tài khoản lúc này!"));
                            }
                        } else {
                            sendPacket(new Packet(false, "Mã OTP không chính xác hoặc đã hết hạn!"));
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] Lỗi xử lý lệnh " + packet.action + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPacket(Packet packet) {
        try {
            if (socket != null && socket.isOpen()) {
                byte[] data = SerializationUtils.serialize(packet);
                socket.send(data);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi gói tin: " + e.getMessage());
        }
    }

    private void broadcastToAll(Packet packet) {
        for (ClientHandler client : onlineClients.values()) {
            client.sendPacket(packet);
        }
    }

    private void broadcastToOthers(Packet packet) {
        for (ClientHandler client : onlineClients.values()) {
            if (client != this) {
                client.sendPacket(packet);
            }
        }
    }

    private void sendToUser(int targetUserId, Packet packet) {
        for (ClientHandler client : onlineClients.values()) {
            if (client.userId == targetUserId) {
                client.sendPacket(packet);
            }
        }
    }

    private void sendLessonsToUser(int targetUserId, ClassroomDAO dao) {
        for (ClientHandler client : onlineClients.values()) {
            if (client.userId == targetUserId) {
                client.sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", dao.getLessonsByUser(targetUserId)));
            }
        }
    }

    private void sendActionMessage(String action, boolean success, String message) {
        Packet response = new Packet(action, "");
        response.success = success;
        response.message = message;
        sendPacket(response);
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private int seatCountForStageLayout(String stageLayout) {
        if (stageLayout == null) {
            return 6;
        }
        String normalized = stageLayout.trim().toUpperCase();
        int marker = normalized.indexOf('V');
        if (marker >= 0 && marker < normalized.length() - 1) {
            return parseIntOrDefault(normalized.substring(marker + 1), 6);
        }
        return 6;
    }

    private void closeConnections() {
        try {
            if (socket != null && socket.isOpen()) {
                socket.close();
            }
        } catch (Exception e) {}
    }
}
