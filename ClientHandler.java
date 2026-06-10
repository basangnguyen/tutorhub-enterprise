package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.UserInfo;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult;
import com.mycompany.tutorhub_enterprise.server.AuthService.LoginSession;
import com.mycompany.tutorhub_enterprise.server.dao.BoardDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ChatDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ClassDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO;
import com.mycompany.tutorhub_enterprise.server.dao.UserDAO;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;

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
      System.out.println("[NGẮT KẾT NỐI] " + this.clientId + " đã rời khỏi hệ thống.");
      if (this.userId != -1) {
         DatabaseManager.updateLastSeen(this.userId);
         onlineClients.remove(this.clientId);
         this.broadcastToAll(new Packet("USER_OFFLINE", String.valueOf(this.userId)));
      }

      this.closeConnections();
   }

   public static boolean isUserOnline(String email) {
      return onlineClients.containsKey(email);
   }

   public void processClientRequest(Packet packet) {
      try {
         System.out.println("[NHẬN LỆNH TỪ " + this.clientId + "] " + packet.action);
         String e = packet.action;
         switch (e) {
            case "AUTH_LOGIN":
               AuthRequest requestxxxxxxxx = this.requireAuthRequest(packet);
               if (requestxxxxxxxx != null) {
                  this.sendAuthLoginResult(
                     requestxxxxxxxx.getRequestId(), AuthService.authenticateWithPassword(requestxxxxxxxx.getEmail(), requestxxxxxxxx.getPassword())
                  );
               }
               break;
            case "AUTH_REQUEST_REGISTRATION_OTP":
               AuthRequest requestxxxxxxx = this.requireAuthRequest(packet);
               if (requestxxxxxxx != null) {
                  this.sendAuthResult(requestxxxxxxx.getRequestId(), AuthService.requestRegistrationOtp(requestxxxxxxx.getEmail()));
               }
               break;
            case "AUTH_VERIFY_AND_REGISTER":
               AuthRequest requestxxxxxx = this.requireAuthRequest(packet);
               if (requestxxxxxx != null) {
                  this.sendAuthResult(
                     requestxxxxxx.getRequestId(),
                     AuthService.verifyAndRegister(requestxxxxxx.getEmail(), requestxxxxxx.getOtp(), requestxxxxxx.getPassword(), requestxxxxxx.getFullName())
                  );
               }
               break;
            case "AUTH_REQUEST_PASSWORD_RESET_OTP":
               AuthRequest requestxxxxx = this.requireAuthRequest(packet);
               if (requestxxxxx != null) {
                  this.sendAuthResult(requestxxxxx.getRequestId(), AuthService.requestPasswordResetOtp(requestxxxxx.getEmail()));
               }
               break;
            case "AUTH_VERIFY_AND_RESET_PASSWORD":
               AuthRequest requestxxxx = this.requireAuthRequest(packet);
               if (requestxxxx != null) {
                  this.sendAuthResult(
                     requestxxxx.getRequestId(), AuthService.verifyAndResetPassword(requestxxxx.getEmail(), requestxxxx.getOtp(), requestxxxx.getPassword())
                  );
               }
               break;
            case "AUTH_REQUEST_SMS_LOGIN_OTP":
               AuthRequest requestxxx = this.requireAuthRequest(packet);
               if (requestxxx != null) {
                  this.sendAuthResult(requestxxx.getRequestId(), AuthService.requestSmsLoginOtp(requestxxx.getPhone()));
               }
               break;
            case "AUTH_VERIFY_SMS_LOGIN":
               AuthRequest requestxx = this.requireAuthRequest(packet);
               if (requestxx != null) {
                  this.sendAuthLoginResult(requestxx.getRequestId(), AuthService.verifySmsLogin(requestxx.getPhone(), requestxx.getOtp()));
               }
               break;
            case "AUTH_REQUEST_PHONE_VERIFICATION_OTP":
               AuthRequest requestx = this.requireAuthRequest(packet);
               if (requestx != null) {
                  this.sendAuthResult(requestx.getRequestId(), AuthService.requestPhoneVerificationOtp(this.userId, requestx.getPhone()));
               }
               break;
            case "AUTH_VERIFY_PHONE_OTP":
               AuthRequest request = this.requireAuthRequest(packet);
               if (request != null) {
                  this.sendAuthResult(request.getRequestId(), AuthService.verifyPhoneOtp(this.userId, request.getPhone(), request.getOtp()));
               }
               break;
            case "GET_ALL_CLASSES":
               for (String classDatax : ClassDAO.getAvailableClasses()) {
                  this.sendPacket(new Packet("BROADCAST_CLASS", classDatax));
                  Thread.sleep(50L);
               }
               break;
            case "SYNC_SESSION":
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
               } catch (Exception var42) {
                  System.err.println("Lỗi đồng bộ session: " + var42.getMessage());
               }
               break;
            case "GET_CONVO_LIST":
               int uid = Integer.parseInt(packet.payload);
               List<ConversationInfo> listConvo = ChatDAO.getConversationList(uid);
               this.sendPacket(new Packet("GET_CONVO_LIST", listConvo));
               break;
            case "GET_REELS":
               List<String> reels = DatabaseManager.getReels(this.userId);
               this.sendPacket(new Packet("GET_REELS_RESPONSE", reels));
               break;
            case "LIKE_REEL":
               try {
                  int reelId = Integer.parseInt(packet.payload);
                  DatabaseManager.likeReel(reelId, this.userId);
               } catch (Exception var41) {
               }
               break;
            case "GET_REEL_COMMENTS":
               try {
                  int reelId = Integer.parseInt(packet.payload);
                  List<String> comments = DatabaseManager.getReelComments(reelId);
                  this.sendPacket(new Packet("GET_REEL_COMMENTS_RESPONSE", comments));
               } catch (Exception var40) {
               }
               break;
            case "ADD_REEL_COMMENT":
               try {
                  String[] partsxx = packet.payload.split(";;");
                  if (partsxx.length >= 2) {
                     int reelId = Integer.parseInt(partsxx[0]);
                     String content = partsxx[1];
                     if (DatabaseManager.insertReelComment(reelId, this.userId, content)) {
                        List<String> comments = DatabaseManager.getReelComments(reelId);
                        this.sendPacket(new Packet("GET_REEL_COMMENTS_RESPONSE", comments));
                     }
                  }
               } catch (Exception var39) {
               }
               break;
            case "UPLOAD_REEL":
               String[] partsx = packet.payload.split(";;", -1);
               if (partsx.length >= 3) {
                  String videoUrl = partsx[0];
                  String caption = partsx[1];
                  String hashtags = partsx[2];
                  String location = partsx.length >= 4 ? partsx[3] : "";
                  String productLink = partsx.length >= 5 ? partsx[4] : "";
                  boolean ok = DatabaseManager.insertReel(this.userId, videoUrl, caption, hashtags, location, productLink);
                  this.sendPacket(new Packet(ok, ok ? "Đăng thước phim thành công!" : "Lỗi khi đăng."));
               }
               break;
            case "UPLOAD_LOCKET":
               String[] parts = packet.payload.split(";;", -1);
               if (parts.length >= 2) {
                  String mediaUrl = parts[0];
                  String title = parts[1];
                  String mediaType = parts.length >= 3 ? parts[2] : "video";
                  boolean ok = DatabaseManager.insertLocket(this.userId, mediaUrl, title, mediaType);
                  this.sendPacket(new Packet(ok, ok ? "Đăng Locket thành công!" : "Lỗi khi đăng."));
                  if (ok) {
                     this.sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
                  }
               }
               break;
            case "GET_LOCKET_VIDEOS":
               this.sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
               break;
            case "DELETE_LOCKET":
               try {
                  int locketId = Integer.parseInt(packet.payload);
                  DatabaseManager.deleteLocket(locketId, this.userId);
                  this.sendPacket(new Packet("GET_LOCKET_VIDEOS_RESPONSE", DatabaseManager.getLocketVideos()));
               } catch (Exception var38) {
                  var38.printStackTrace();
               }
               break;
            case "GET_FULL_PROFILE":
               int uidProf = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
               String profileDataStr = DatabaseManager.getFullProfile(uidProf);
               String[] pParts = profileDataStr.split(";;", -1);
               if (pParts.length < 13) {
                  String[] expanded = new String[13];
                  System.arraycopy(pParts, 0, expanded, 0, pParts.length);

                  for (int i = pParts.length; i < 13; i++) {
                     expanded[i] = "null";
                  }

                  pParts = expanded;
               }

               File fFront = new File("server_uploads/ekyc/front_" + uidProf + ".jpg");
               if (fFront.exists()) {
                  try {
                     pParts[11] = Base64.getEncoder().encodeToString(Files.readAllBytes(fFront.toPath()));
                  } catch (Exception var37) {
                     pParts[11] = "null";
                  }
               } else {
                  pParts[11] = "null";
               }

               File fBack = new File("server_uploads/ekyc/back_" + uidProf + ".jpg");
               if (fBack.exists()) {
                  try {
                     pParts[12] = Base64.getEncoder().encodeToString(Files.readAllBytes(fBack.toPath()));
                  } catch (Exception var36) {
                     pParts[12] = "null";
                  }
               } else {
                  pParts[12] = "null";
               }

               this.sendPacket(new Packet("FULL_PROFILE_RESULT", String.join(";;", pParts)));
               break;
            case "GET_DEGREES":
               int uidDeg = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
               this.sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(uidDeg)));
               break;
            case "GET_CERTIFICATES":
               int uidCert = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
               this.sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(uidCert)));
               break;
            case "GET_EXPERIENCES":
               int uidExp = packet.payload.isEmpty() ? this.userId : Integer.parseInt(packet.payload);
               this.sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(uidExp)));
               break;
            case "UPDATE_PROFILE":
               String[] pData = packet.payload.split(";;", -1);
               if (pData.length >= 8) {
                  DatabaseManager.updateProfile(this.userId, pData[0], pData[1], pData[2], pData[3], pData[4], pData[5], pData[6], pData[7]);
                  this.broadcastToAll(new Packet("ALL_TUTORS_RESULT", DatabaseManager.getAllTutors()));
               }
               break;
            case "UPDATE_CV":
               String[] cvData = packet.payload.split("\\|");
               if (cvData.length == 2) {
                  try {
                     File dirxx = new File("server_uploads/cv");
                     if (!dirxx.exists()) {
                        dirxx.mkdirs();
                     }

                     File f = new File(dirxx, "cv_" + this.userId + "_" + cvData[0]);
                     Files.write(f.toPath(), Base64.getDecoder().decode(cvData[1]));
                     DatabaseManager.updateCV(this.userId, f.getPath());
                     this.sendPacket(new Packet("FULL_PROFILE_RESULT", DatabaseManager.getFullProfile(this.userId)));
                  } catch (Exception var35) {
                  }
               }
               break;
            case "UPDATE_EKYC":
               String[] ekycData = packet.payload.split("\\|\\|\\|");
               if (ekycData.length == 2) {
                  try {
                     File dirxx = new File("server_uploads/ekyc");
                     if (!dirxx.exists()) {
                        dirxx.mkdirs();
                     }

                     File fileFront = new File(dirxx, "front_" + this.userId + ".jpg");
                     File fileBack = new File(dirxx, "back_" + this.userId + ".jpg");
                     Files.write(fileFront.toPath(), Base64.getDecoder().decode(ekycData[0]));
                     Files.write(fileBack.toPath(), Base64.getDecoder().decode(ekycData[1]));
                     DatabaseManager.updateEkyc(this.userId, fileFront.getPath(), fileBack.getPath());
                     this.processClientRequest(new Packet("GET_FULL_PROFILE", String.valueOf(this.userId)));
                  } catch (Exception var34) {
                  }
               }
               break;
            case "DOWNLOAD_FILE":
               String reqFileName = packet.payload;

               try {
                  File targetFile = new File(reqFileName);
                  if (!targetFile.exists() && !targetFile.isAbsolute()) {
                     targetFile = new File("server_uploads/documents", reqFileName);
                     if (!targetFile.exists()) {
                        targetFile = new File("server_uploads/cv", reqFileName);
                     }
                  }

                  if (targetFile.exists()) {
                     byte[] fileBytes = Files.readAllBytes(targetFile.toPath());
                     String base64FileData = Base64.getEncoder().encodeToString(fileBytes);
                     this.sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", base64FileData));
                  } else {
                     System.err.println("[SERVER LỖI] Không tìm thấy file: " + targetFile.getPath());
                     this.sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", "ERROR"));
                  }
               } catch (Exception var33) {
                  var33.printStackTrace();
                  this.sendPacket(new Packet("DOWNLOAD_FILE_RESPONSE", "ERROR"));
               }
               break;
            case "ADD_EXPERIENCE":
               String[] expData = packet.payload.split("\\|");
               if (expData.length >= 3) {
                  DatabaseManager.insertExperience(this.userId, expData[0], expData[1], expData[2]);
                  this.sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(this.userId)));
               }
               break;
            case "ADD_TUTOR_BY_ADMIN":
               String[] newTutorData = packet.payload.split("\\|");
               if (newTutorData.length >= 6) {
                  String emailx = newTutorData[0];
                  String rawPass = newTutorData[1];
                  String fullName = newTutorData[2];
                  if (DatabaseManager.isEmailExists(emailx)) {
                     this.sendPacket(new Packet(false, "Email này đã tồn tại trong hệ thống!"));
                  } else {
                     boolean isSuccess = DatabaseManager.registerUser(emailx, rawPass, fullName, "TUTOR");
                     if (isSuccess) {
                        this.sendPacket(new Packet(true, "Tạo tài khoản gia sư thành công!"));
                     } else {
                        this.sendPacket(new Packet(false, "Lỗi Server: Không thể ghi vào cơ sở dữ liệu."));
                     }
                  }
               }
               break;
            case "ADD_DEGREE":
               String[] degData = packet.payload.split("\\|", 6);
               if (degData.length == 6) {
                  try {
                     File dirx = new File("server_uploads/documents");
                     if (!dirx.exists()) {
                        dirx.mkdirs();
                     }

                     String safeFileName = this.userId + "_" + System.currentTimeMillis() + "_" + degData[4].replaceAll("[^a-zA-Z0-9.-]", "_");
                     File docFile = new File(dirx, safeFileName);
                     byte[] fileBytes = Base64.getDecoder().decode(degData[5]);
                     Files.write(docFile.toPath(), fileBytes);
                     boolean ok = DatabaseManager.insertDegree(this.userId, degData[0], degData[1], degData[2], degData[3], docFile.getPath());
                     if (ok) {
                        this.sendPacket(new Packet("RESPONSE", "Đã thêm Bằng cấp thành công! Chờ Admin duyệt."));
                        this.sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(this.userId)));
                     }
                  } catch (Exception var32) {
                  }
               }
               break;
            case "ADD_CERTIFICATE":
               String[] certData = packet.payload.split("\\|", 6);
               if (certData.length == 6) {
                  try {
                     File dir = new File("server_uploads/documents");
                     if (!dir.exists()) {
                        dir.mkdirs();
                     }

                     String safeFileName = "cert_" + this.userId + "_" + System.currentTimeMillis() + "_" + certData[4].replaceAll("[^a-zA-Z0-9.-]", "_");
                     File docFile = new File(dir, safeFileName);
                     byte[] fileBytes = Base64.getDecoder().decode(certData[5]);
                     Files.write(docFile.toPath(), fileBytes);
                     boolean ok = DatabaseManager.insertCertificate(this.userId, certData[0], certData[1], certData[2], certData[3], docFile.getPath());
                     if (ok) {
                        this.sendPacket(new Packet("RESPONSE", "Đã thêm Chứng chỉ thành công! Chờ Admin duyệt."));
                        this.sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(this.userId)));
                     }
                  } catch (Exception var31) {
                  }
               }
               break;
            case "SEARCH_USER":
               String keyword = packet.payload;
               List<UserInfo> searchResults = UserDAO.searchUsers(keyword, this.userId);
               this.sendPacket(new Packet("SEARCH_USER_RESULT", searchResults));
               break;
            case "SEND_FRIEND_REQUEST":
               int targetId = Integer.parseInt(packet.payload);
               boolean isReqSent = UserDAO.sendFriendRequest(this.userId, targetId);
               if (isReqSent) {
                  this.sendPacket(new Packet(true, "Đã gửi lời mời kết bạn!", "FRIEND_REQUEST_SENT"));
               } else {
                  this.sendPacket(new Packet(false, "Lỗi: Không thể gửi lời mời."));
               }
               break;
            case "ACCEPT_FRIEND":
               int requesterId = Integer.parseInt(packet.payload);
               boolean isAccepted = UserDAO.acceptFriendRequest(requesterId, this.userId);
               if (isAccepted) {
                  this.sendPacket(new Packet(true, "Đã trở thành bạn bè!", "FRIEND_ACCEPTED"));
                  List<ConversationInfo> listConvoNew = ChatDAO.getConversationList(this.userId);
                  this.sendPacket(new Packet("GET_CONVO_LIST", listConvoNew));
               }
               break;
            case "GET_MESSAGES":
               String[] partsMsg = packet.payload.split("\\|");
               int currentUid = Integer.parseInt(partsMsg[0]);
               int convoId = Integer.parseInt(partsMsg[1]);
               List<Message> msgs = ChatDAO.getMessages(convoId, currentUid);
               this.sendPacket(new Packet("GET_MESSAGES", msgs));
               break;
            case "TYPING":
            case "CALL_INIT":
            case "CALL_ACCEPT":
            case "CALL_REJECT":
            case "CALL_CANCEL":
               String[] parts = packet.payload.split("\\|", 2);
               String convoIdStr = parts[0];

               try {
                  List<Integer> memberIds = ChatDAO.getConversationMembers(Integer.parseInt(convoIdStr));
                  if (!this.isCurrentUserConversationMember(memberIds)) {
                     System.err.println("[CHAT SECURITY] User " + this.userId + " tried to send " + packet.action + " to conversation " + convoIdStr);
                  } else {
                     String senderName = "Gia sư " + this.clientId;
                     String forwardPayload = convoIdStr + "|" + senderName;
                     if (parts.length > 1) {
                        forwardPayload = forwardPayload + "|" + parts[1];
                     }

                     this.sendToConversationMembers(memberIds, new Packet(packet.action, forwardPayload), false);
                  }
               } catch (Exception var30) {
                  var30.printStackTrace();
               }
               break;
            case "SEND_CHAT":
               String[] chatData = packet.payload == null ? new String[0] : packet.payload.split("\\|", 4);
               if (chatData.length >= 3) {
                  String conversationId = chatData[0];
                  String messageType = chatData[1];
                  String clientMessageId = "";
                  String content;
                  if (chatData.length >= 4 && this.isLikelyClientMessageId(chatData[2])) {
                     clientMessageId = chatData[2];
                     content = chatData[3];
                  } else if (chatData.length >= 4) {
                     content = chatData[2] + "|" + chatData[3];
                  } else {
                     content = chatData[2];
                  }

                  int conversationIdInt = this.parseIntOrDefault(conversationId, -1);
                  List<Integer> memberIds = ChatDAO.getConversationMembers(conversationIdInt);
                  if (!this.isCurrentUserConversationMember(memberIds)) {
                     System.err.println("[CHAT SECURITY] User " + this.userId + " tried to send chat to conversation " + conversationId);
                     this.sendActionMessage("RESPONSE", false, "Ban khong co quyen gui tin nhan trong hoi thoai nay.");
                  } else {
                     String senderName = "Gia sư " + this.clientId;
                     String forwardPayload = conversationId + "|" + senderName + "|" + content;

                     try {
                        Connection conn = DatabaseManager.getConnection();
                        if (conn != null) {
                           int serverMessageId = 0;
                           boolean inserted = false;
                           if (!clientMessageId.isEmpty()) {
                              String existingSql = "SELECT id FROM messages WHERE conversation_id = ? AND sender_id = ? AND client_message_id = ?";

                              try (PreparedStatement existingPst = conn.prepareStatement(existingSql)) {
                                 existingPst.setInt(1, conversationIdInt);
                                 existingPst.setInt(2, this.userId);
                                 existingPst.setString(3, clientMessageId);

                                 try (ResultSet existingRs = existingPst.executeQuery()) {
                                    if (existingRs.next()) {
                                       serverMessageId = existingRs.getInt("id");
                                    }
                                 }
                              }
                           }

                           if (serverMessageId == 0) {
                              String sql = clientMessageId.isEmpty()
                                 ? "INSERT INTO messages (conversation_id, sender_id, message_type, content, is_read) VALUES (?, ?, ?, ?, false)"
                                 : "INSERT INTO messages (conversation_id, sender_id, message_type, content, is_read, client_message_id) VALUES (?, ?, ?, ?, false, ?)";
                              PreparedStatement pst = conn.prepareStatement(sql, 1);
                              pst.setInt(1, conversationIdInt);
                              pst.setInt(2, this.userId);
                              pst.setString(3, messageType);
                              pst.setString(4, content);
                              if (!clientMessageId.isEmpty()) {
                                 pst.setString(5, clientMessageId);
                              }

                              pst.executeUpdate();

                              try (ResultSet keys = pst.getGeneratedKeys()) {
                                 if (keys.next()) {
                                    serverMessageId = keys.getInt(1);
                                 }
                              }

                              pst.close();
                              inserted = true;
                           }

                           if (inserted) {
                              this.sendToConversationMembers(memberIds, new Packet("RECEIVE_CHAT", forwardPayload), false);
                           }

                           String ackPayload = clientMessageId.isEmpty() ? conversationId : conversationId + "|" + clientMessageId + "|" + serverMessageId;
                           this.sendPacket(new Packet("SEND_CHAT_ACK", ackPayload));
                        }
                     } catch (Exception var52) {
                        System.err.println("[SERVER DB LỖI] Không thể lưu tin nhắn: " + var52.getMessage());
                     }
                  }
               }
               break;
            case "MARK_AS_READ":
               String convoIdStr = packet.payload;

               try {
                  int conversationIdInt = this.parseIntOrDefault(convoIdStr, -1);
                  List<Integer> memberIds = ChatDAO.getConversationMembers(conversationIdInt);
                  if (!this.isCurrentUserConversationMember(memberIds)) {
                     System.err.println("[CHAT SECURITY] User " + this.userId + " tried to mark conversation " + convoIdStr + " as read");
                  } else {
                     Connection connRead = DatabaseManager.getConnection();
                     if (connRead != null) {
                        String sqlRead = "UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id != ? AND COALESCE(is_read, false) = false";
                        PreparedStatement pstRead = connRead.prepareStatement(sqlRead);
                        pstRead.setInt(1, conversationIdInt);
                        pstRead.setInt(2, this.userId);
                        int rowsAffected = pstRead.executeUpdate();
                        pstRead.close();
                        if (rowsAffected > 0) {
                           this.sendToConversationMembers(memberIds, new Packet("READ_ACK", convoIdStr), false);
                        }
                     }
                  }
               } catch (Exception var29) {
                  System.err.println("[SERVER LỖI] Lỗi cập nhật trạng thái đã xem: " + var29.getMessage());
               }
               break;
            case "SAVE_BOARD":
               String[] boardData = packet.payload.split("\\|", 3);
               if (boardData.length == 3) {
                  String title = boardData[0];
                  String className = boardData[1];
                  String base64Data = boardData[2];
                  boolean isSaved = BoardDAO.saveBoard(this.userId, title, className, base64Data);
                  if (isSaved) {
                     this.sendPacket(new Packet("SAVE_BOARD_SUCCESS", ""));
                     String boardsDataStr = BoardDAO.getUserBoards(this.userId);
                     this.sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataStr));
                  } else {
                     this.sendPacket(new Packet("RESPONSE", "Lỗi: Không thể lưu bảng vẽ lên Database!"));
                  }
               }
               break;
            case "UPDATE_TUTOR_STATUS":
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
                        List<String> updatedTutors = DatabaseManager.getAllTutors();
                        this.broadcastToAll(new Packet("ALL_TUTORS_RESULT", updatedTutors));
                     }
                  } catch (Exception var28) {
                     System.err.println("Lỗi cập nhật trạng thái gia sư: " + var28.getMessage());
                  }
               }
               break;
            case "UPDATE_BOARD":
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

                        this.sendPacket(new Packet("SAVE_BOARD_SUCCESS", ""));
                        String boardsDataStr = BoardDAO.getUserBoards(this.userId);
                        this.sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataStr));
                     }
                  } catch (Exception var27) {
                     System.err.println("[SERVER LỖI] Cập nhật bảng vẽ: " + var27.getMessage());
                  }
               }
               break;
            case "GET_USER_BOARDS":
               String boardsDataList = BoardDAO.getUserBoards(this.userId);
               this.sendPacket(new Packet("USER_BOARDS_RESULT", boardsDataList));
               break;
            case "DELETE_BOARD":
               String boardId = packet.payload;
               Connection connDel = DatabaseManager.getConnection();
               if (connDel != null) {
                  PreparedStatement pstDel = connDel.prepareStatement("DELETE FROM blackboards WHERE board_id = ?");
                  pstDel.setString(1, boardId);
                  pstDel.executeUpdate();
                  pstDel.close();
               }
               break;
            case "DELETE_DEGREE":
               try {
                  Connection connDelx = DatabaseManager.getConnection();
                  if (connDelx != null) {
                     boolean deleted = false;
                     String[] possibleColumns = new String[]{"university", "degree_name"};

                     for (String col : possibleColumns) {
                        try {
                           PreparedStatement pst = connDelx.prepareStatement("DELETE FROM tutor_degrees WHERE user_id = ? AND " + col + " = ?");
                           pst.setInt(1, this.userId);
                           pst.setString(2, packet.payload);
                           int rows = pst.executeUpdate();
                           pst.close();
                           if (rows > 0) {
                              deleted = true;
                              break;
                           }
                        } catch (Exception var47) {
                        }
                     }

                     if (!deleted) {
                        System.err.println("Không thể xóa Bằng cấp, hãy kiểm tra lại tên cột trong DB!");
                     }

                     this.sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(this.userId)));
                  }
               } catch (Exception var48) {
                  System.err.println("Lỗi xóa Bằng cấp: " + var48.getMessage());
               }
               break;
            case "DELETE_CERTIFICATE":
               try {
                  Connection connDelx = DatabaseManager.getConnection();
                  if (connDelx != null) {
                     boolean deleted = false;
                     String[] possibleColumns = new String[]{"cert_name", "name", "certificate_name", "title"};

                     for (String col : possibleColumns) {
                        try {
                           PreparedStatement pst = connDelx.prepareStatement("DELETE FROM tutor_certificates WHERE user_id = ? AND " + col + " = ?");
                           pst.setInt(1, this.userId);
                           pst.setString(2, packet.payload);
                           int rows = pst.executeUpdate();
                           pst.close();
                           if (rows > 0) {
                              deleted = true;
                              break;
                           }
                        } catch (Exception var45) {
                        }
                     }

                     if (!deleted) {
                        System.err.println("Không thể xóa Chứng chỉ, hãy kiểm tra lại tên cột trong DB!");
                     }

                     this.sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(this.userId)));
                  }
               } catch (Exception var46) {
                  System.err.println("Lỗi xóa Chứng chỉ: " + var46.getMessage());
               }
               break;
            case "DELETE_EXPERIENCE":
               try {
                  String[] expParts = packet.payload.split("\\|");
                  if (expParts.length >= 2) {
                     Connection connDelx = DatabaseManager.getConnection();
                     if (connDelx != null) {
                        boolean deleted = false;
                        String[] timeCols = new String[]{"duration", "time_period", "period", "time"};
                        String[] titleCols = new String[]{"location", "title", "position", "company"};

                        for (String tCol : timeCols) {
                           for (String pCol : titleCols) {
                              try {
                                 PreparedStatement pst = connDelx.prepareStatement(
                                    "DELETE FROM tutor_experiences WHERE user_id = ? AND " + tCol + " = ? AND " + pCol + " = ?"
                                 );
                                 pst.setInt(1, this.userId);
                                 pst.setString(2, expParts[0].trim());
                                 pst.setString(3, expParts[1].trim());
                                 int rows = pst.executeUpdate();
                                 pst.close();
                                 if (rows > 0) {
                                    deleted = true;
                                    break;
                                 }
                              } catch (Exception var43) {
                              }
                           }

                           if (deleted) {
                              break;
                           }
                        }

                        if (!deleted) {
                           System.err.println("Không thể xóa Kinh nghiệm, hãy kiểm tra lại tên cột trong DB!");
                        }

                        this.sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(this.userId)));
                     }
                  }
               } catch (Exception var44) {
                  System.err.println("Lỗi xóa Kinh nghiệm: " + var44.getMessage());
               }
               break;
            case "RENAME_BOARD":
               String[] renameParts = packet.payload.split("\\|");
               if (renameParts.length == 2) {
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
            case "REQUEST_OTP_RESET":
               String emailReset = packet.payload;
               if (!DatabaseManager.isEmailExists(emailReset)) {
                  this.sendPacket(new Packet(false, "Email này chưa được đăng ký trong hệ thống!"));
               } else {
                  String otpReset = String.format("%06d", new Random().nextInt(999999));
                  otpStorage.put(emailReset, otpReset);
                  new Thread(() -> {
                     boolean isSent = EmailService.sendOTP(emailReset, otpReset);
                     if (isSent) {
                        this.sendPacket(new Packet(true, "Mã OTP khôi phục đã được gửi đến email!"));
                     } else {
                        this.sendPacket(new Packet(false, "Lỗi Server: Không thể gửi Email lúc này."));
                        otpStorage.remove(emailReset);
                     }
                  }).start();
               }
               break;
            case "VERIFY_AND_RESET":
               String[] resetData = packet.payload.split(",");
               if (resetData.length == 3) {
                  String reqEmail = resetData[0];
                  String reqOtp = resetData[1];
                  String newPass = resetData[2];
                  String expectedOtp = otpStorage.get(reqEmail);
                  if (expectedOtp == null || !expectedOtp.equals(reqOtp)) {
                     this.sendPacket(new Packet(false, "Mã OTP không chính xác hoặc đã hết hạn!"));
                  } else if (DatabaseManager.resetPassword(reqEmail, newPass)) {
                     otpStorage.remove(reqEmail);
                     this.sendPacket(new Packet(true, "Tuyệt vời! Mật khẩu của bạn đã được cập nhật."));
                  } else {
                     this.sendPacket(new Packet(false, "Lỗi Server: Không thể cập nhật mật khẩu!"));
                  }
               }
               break;
            case "UPDATE_AVATAR":
               String base64Image = packet.payload;
               byte[] imageBytes = Base64.getDecoder().decode(base64Image);
               File uploadDir = new File("server_uploads/avatars");
               if (!uploadDir.exists()) {
                  uploadDir.mkdirs();
               }

               String safeEmail = this.clientId.replaceAll("[^a-zA-Z0-9.-]", "_");
               String fileName = "avatar_" + safeEmail + ".jpg";
               File avatarFile = new File(uploadDir, fileName);
               Files.write(avatarFile.toPath(), imageBytes);
               Connection connAva = DatabaseManager.getConnection();
               if (connAva != null) {
                  String sql = "UPDATE users SET avatar_url = ? WHERE email = ?";
                  PreparedStatement pst = connAva.prepareStatement(sql);
                  pst.setString(1, "server_uploads/avatars/" + fileName);
                  pst.setString(2, this.clientId);
                  pst.executeUpdate();
                  pst.close();
               }

               this.sendPacket(new Packet("UPDATE_AVATAR_SUCCESS", base64Image));
               break;
            case "GET_PROFILE":
               Connection connPro = DatabaseManager.getConnection();
               if (connPro != null) {
                  String sql = "SELECT avatar_url FROM users WHERE email = ?";
                  PreparedStatement pst = connPro.prepareStatement(sql);
                  pst.setString(1, this.clientId);
                  ResultSet rs = pst.executeQuery();
                  if (rs.next()) {
                     String avatarUrl = rs.getString("avatar_url");
                     if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        File aFile = new File(avatarUrl);
                        if (aFile.exists()) {
                           byte[] fileBytes = Files.readAllBytes(aFile.toPath());
                           String b64Image = Base64.getEncoder().encodeToString(fileBytes);
                           this.sendPacket(new Packet("LOAD_AVATAR", b64Image));
                        }
                     }
                  }

                  rs.close();
                  pst.close();
               }
               break;
            case "GET_USER_AVATAR":
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
                           File aFile = new File(avatarUrl);
                           if (aFile.exists()) {
                              byte[] fileBytes = Files.readAllBytes(aFile.toPath());
                              String b64Image = Base64.getEncoder().encodeToString(fileBytes);
                              this.sendPacket(new Packet("LOAD_TUTOR_AVATAR", b64Image));
                           }
                        }
                     }

                     rs.close();
                     pst.close();
                  }
               } catch (Exception var26) {
                  System.err.println("Lỗi lấy avatar gia sư: " + var26.getMessage());
               }
               break;
            case "ACCEPT_CLASS":
               String classCode = packet.payload;
               boolean success = ClassDispatcher.getInstance().processAcceptClass(classCode, 1);
               if (success) {
                  this.sendPacket(new Packet("ACCEPT_SUCCESS", classCode));
                  this.broadcastToAll(new Packet("CLASS_TAKEN", classCode));
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

                  this.sendPacket(new Packet("REFRESH_TASKS", ""));
               } else {
                  this.sendPacket(new Packet("ACCEPT_FAIL", classCode));
               }
               break;
            case "GET_TASKS":
               Connection connTask = DatabaseManager.getConnection();
               if (connTask != null) {
                  String sql = "SELECT * FROM tutor_tasks WHERE tutor_name = ? ORDER BY created_at DESC";
                  PreparedStatement pst = connTask.prepareStatement(sql);
                  pst.setString(1, this.clientId);
                  ResultSet rs = pst.executeQuery();
                  StringBuilder sb = new StringBuilder();

                  while (rs.next()) {
                     sb.append(rs.getInt("id"))
                        .append("|")
                        .append(rs.getString("category"))
                        .append("|")
                        .append(rs.getString("title"))
                        .append("|")
                        .append(rs.getString("schedule_time"))
                        .append("|")
                        .append(rs.getString("location"))
                        .append("|")
                        .append(rs.getBoolean("is_completed"))
                        .append(";;");
                  }

                  rs.close();
                  pst.close();
                  this.sendPacket(new Packet("SYNC_TASKS", sb.toString()));
               }
               break;
            case "CREATE_CLASSROOM_AND_ENTER":
               String[] dataxx = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
               String className = dataxx.length > 0 ? dataxx[0].trim() : "";
               String organizationNamex = dataxx.length > 1 && !dataxx[1].trim().isEmpty() ? dataxx[1].trim() : "My Account";
               if (this.userId <= 0) {
                  this.sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Ban can dang nhap truoc khi tao lop hoc.");
               } else if (className.isEmpty()) {
                  this.sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Ten lop hoc khong duoc de trong.");
               } else {
                  ClassroomGroupModel classroom = new ClassroomGroupModel();
                  classroom.setOwnerId(this.userId);
                  classroom.setName(className);
                  classroom.setDescription("");
                  classroom.setCoverImage("");
                  classroom.setOrganizationName(organizationNamex);
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
                  ClassroomDAO daox = new ClassroomDAO();
                  if (daox.createLiveClassroom(classroom, lesson)) {
                     String resultPayload = classroom.getId() + "|" + lesson.getId() + "|" + lesson.getBoardId() + "|" + classroom.getName();
                     Packet successx = new Packet("CREATE_CLASSROOM_AND_ENTER_SUCCESS", resultPayload);
                     successx.success = true;
                     successx.message = "Tao lop hoc thanh cong.";
                     this.sendPacket(successx);
                     this.sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", daox.getClassroomsByUser(this.userId)));
                     this.sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", daox.getLessonsByUser(this.userId)));
                     this.broadcastToOthers(new Packet("BROADCAST_LIVE_CLASS", lesson.getBoardId() + "|" + classroom.getName()));
                  } else {
                     this.sendActionMessage("CREATE_CLASSROOM_AND_ENTER_FAIL", false, "Khong the tao lop hoc live.");
                  }
               }
               break;
            case "BROADCAST_LIVE_CLASS":
               this.broadcastToOthers(packet);
               break;
            case "CREATE_PUBLIC_LESSON":
               String[] datax = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
               String lessonName = datax.length > 0 ? datax[0].trim() : "";
               String organizationName = datax.length > 1 && !datax[1].trim().isEmpty() ? datax[1].trim() : "My Account";
               long startMillis = datax.length > 2 ? this.parseLongOrDefault(datax[2], System.currentTimeMillis()) : System.currentTimeMillis();
               int durationMinutes = datax.length > 3 ? this.parseIntOrDefault(datax[3], 40) : 40;
               String stageLayout = datax.length > 4 && !datax[4].trim().isEmpty() ? datax[4].trim() : "1V6";
               boolean lobbyEnabled = datax.length > 5 ? Boolean.parseBoolean(datax[5]) : true;
               boolean allowStudentDraw = datax.length > 6 && Boolean.parseBoolean(datax[6]);
               boolean recordingEnabled = datax.length > 7 && Boolean.parseBoolean(datax[7]);
               String coTeachers = datax.length > 8 ? datax[8].trim() : "";
               if (this.userId <= 0) {
                  this.sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Ban can dang nhap truoc khi tao public lesson.");
               } else if (lessonName.isEmpty()) {
                  this.sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Ten public lesson khong duoc de trong.");
               } else {
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
                  lesson.setStartTime(new Timestamp(startMillis));
                  lesson.setDurationMinutes(durationMinutes);
                  lesson.setSeatCount(this.seatCountForStageLayout(stageLayout));
                  lesson.setStatus("SCHEDULED");
                  lesson.setLessonType("PUBLIC");
                  lesson.setStageLayout(stageLayout);
                  lesson.setLobbyEnabled(lobbyEnabled);
                  lesson.setAllowStudentDraw(allowStudentDraw);
                  lesson.setRecordingEnabled(recordingEnabled);
                  lesson.setCreatedBy(this.userId);
                  ClassroomDAO daox = new ClassroomDAO();
                  if (daox.createPublicLesson(classroom, lesson)) {
                     String resultPayload = classroom.getId()
                        + "|"
                        + lesson.getId()
                        + "|"
                        + lesson.getBoardId()
                        + "|"
                        + lesson.getTitle()
                        + "|"
                        + startMillis
                        + "|"
                        + classroom.getJoinCode();
                     Packet successx = new Packet("CREATE_PUBLIC_LESSON_SUCCESS", resultPayload);
                     successx.success = true;
                     successx.message = "Da post public lesson.";
                     this.sendPacket(successx);
                     this.sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", daox.getClassroomsByUser(this.userId)));
                     this.sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", daox.getLessonsByUser(this.userId)));
                  } else {
                     this.sendActionMessage("CREATE_PUBLIC_LESSON_FAIL", false, "Khong the tao public lesson.");
                  }
               }
               break;
            case "CREATE_CLASSROOM":
               String[] data = packet.payload.split("\\|", -1);
               if (data.length >= 2) {
                  ClassroomGroupModel model = new ClassroomGroupModel();
                  model.setOwnerId(this.userId);
                  model.setName(data[0]);
                  model.setDescription(data[1]);
                  model.setCoverImage(data.length > 2 ? data[2] : "");
                  ClassroomDAO daox = new ClassroomDAO();
                  boolean ok = daox.createClassroom(model);
                  if (ok) {
                     this.sendPacket(new Packet(true, "Tạo lớp học thành công!", "CREATE_CLASSROOM_SUCCESS"));
                     this.sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", daox.getClassroomsByUser(this.userId)));
                  } else {
                     this.sendPacket(new Packet(false, "Lỗi khi tạo lớp học!", "CREATE_CLASSROOM_FAIL"));
                  }
               }
               break;
            case "GET_CLASSROOMS": {
               ClassroomDAO dao = new ClassroomDAO();
               List<ClassroomGroupModel> list = dao.getClassroomsByUser(this.userId);
               this.sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", list));
               break;
            }
            case "GET_CLASSROOM_LESSONS": {
               ClassroomDAO dao = new ClassroomDAO();
               List<ClassroomLessonModel> list = dao.getLessonsByUser(this.userId);
               this.sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", list));
               break;
            }
            case "JOIN_PUBLIC_LESSON":
               String joinCode = packet.payload == null ? "" : packet.payload.trim();
               if (this.userId <= 0) {
                  this.sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Ban can dang nhap truoc khi tham gia public lesson.");
               } else if (joinCode.isEmpty()) {
                  this.sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Ma public lesson khong duoc de trong.");
               } else {
                  ClassroomDAO daox = new ClassroomDAO();
                  ClassroomLessonModel lesson = daox.joinPublicLessonByCode(this.userId, joinCode);
                  if (lesson != null) {
                     boolean waiting = "WAITING".equalsIgnoreCase(lesson.getMemberStatus());
                     Packet result = new Packet(waiting ? "JOIN_PUBLIC_LESSON_WAITING" : "JOIN_PUBLIC_LESSON_SUCCESS", lesson);
                     result.success = true;
                     result.message = waiting ? "Da gui yeu cau vao lobby. Vui long cho giao vien duyet." : "Tham gia public lesson thanh cong.";
                     this.sendPacket(result);
                     this.sendPacket(new Packet("GET_CLASSROOM_LESSONS_RESPONSE", daox.getLessonsByUser(this.userId)));
                     if (waiting) {
                        Packet waitingList = new Packet(
                           "PUBLIC_LESSON_WAITING_ROOM_UPDATED", daox.getWaitingMembersForLesson(lesson.getId(), lesson.getCreatedBy())
                        );
                        waitingList.message = "Co hoc vien dang cho vao lobby.";
                        this.sendToUser(lesson.getCreatedBy(), waitingList);
                     }
                  } else {
                     this.sendActionMessage("JOIN_PUBLIC_LESSON_FAIL", false, "Khong tim thay public lesson tu ma nay.");
                  }
               }
               break;
            case "GET_PUBLIC_LESSON_WAITING_ROOM": {
               int lessonId = this.parseIntOrDefault(packet.payload, 0);
               ClassroomDAO dao = new ClassroomDAO();
               List<ClassroomMemberModel> waitingMembers = dao.getWaitingMembersForLesson(lessonId, this.userId);
               this.sendPacket(new Packet("GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE", waitingMembers));
               break;
            }
            case "APPROVE_PUBLIC_LESSON_STUDENT": {
               String[] approveData = packet.payload == null ? new String[0] : packet.payload.split("\\|", -1);
               int lessonId = approveData.length > 0 ? this.parseIntOrDefault(approveData[0], 0) : 0;
               int studentId = approveData.length > 1 ? this.parseIntOrDefault(approveData[1], 0) : 0;
               ClassroomDAO dao = new ClassroomDAO();
               ClassroomLessonModel approvedLesson = dao.approveLessonStudent(lessonId, studentId, this.userId);
               if (approvedLesson != null) {
                  this.sendPacket(new Packet("GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE", dao.getWaitingMembersForLesson(lessonId, this.userId)));
                  Packet approved = new Packet("PUBLIC_LESSON_APPROVED", approvedLesson);
                  approved.success = true;
                  approved.message = "Giao vien da duyet ban vao public lesson.";
                  this.sendToUser(studentId, approved);
                  this.sendLessonsToUser(studentId, dao);
               } else {
                  this.sendActionMessage("APPROVE_PUBLIC_LESSON_STUDENT_FAIL", false, "Khong the duyet hoc vien nay.");
               }
               break;
            }
            case "CREATE_CLASS":
               String[] classData = packet.payload.split("\\|", -1);
               if (classData.length >= 6) {
                  String type = classData[0];
                  String subj = classData[1];
                  String tuition = classData[2];
                  String loc = classData[3];
                  String title = classData[4];
                  String desc = classData[5];
                  String newId = "CLASS_" + System.currentTimeMillis();
                  boolean isInserted = ClassDAO.insertClass(newId, subj, tuition, loc, title, desc);
                  if (isInserted) {
                     String time = "Thứ 2,4,6";
                     double salNum = 0.0;

                     try {
                        salNum = Double.parseDouble(tuition.replaceAll("[^0-9]", ""));
                     } catch (Exception var25) {
                     }

                     String formattedSalary = String.format("%,.0fđ/buổi", salNum).replace(",", ".");
                     String broadcastPayload = newId + "|" + subj + "|" + formattedSalary + "|" + loc + "|" + time + "|" + title + "|MỚI|#10B981";
                     this.broadcastToAll(new Packet("BROADCAST_CLASS", broadcastPayload));
                  }
               }
               break;
            case "LOGIN":
               String[] loginData = packet.payload.split("\\|");
               if (loginData.length == 2) {
                  int loginUid = DatabaseManager.authenticateByEmail(loginData[0], loginData[1]);
                  if (loginUid > 0) {
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
                                 File aFile = new File(avatarUrl);
                                 if (aFile.exists()) {
                                    byte[] fileBytes = Files.readAllBytes(aFile.toPath());
                                    avatarBase64 = Base64.getEncoder().encodeToString(fileBytes);
                                 }
                              }
                           }

                           rsRole.close();
                           pstRole.close();
                        }
                     } catch (Exception var24) {
                        System.err.println("[SERVER LỖI] Không thể lấy Role và Avatar: " + var24.getMessage());
                     }

                     onlineClients.remove(this.clientId);
                     this.clientId = loginData[0];
                     this.userId = loginUid;
                     onlineClients.put(this.clientId, this);
                     this.sendPacket(new Packet(true, "Đăng nhập thành công!", "DASHBOARD_GO|" + loginUid + "|" + userRole + "|" + avatarBase64));

                     for (ClientHandler client : onlineClients.values()) {
                        if (!client.clientId.equals(this.clientId)) {
                           client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
                        }
                     }
                  } else {
                     this.sendPacket(new Packet(false, "Sai Email hoặc mật khẩu!", ""));
                  }
               }
               break;
            case "GET_ALL_TUTORS":
               List<String> tutors = DatabaseManager.getAllTutors();
               this.sendPacket(new Packet("ALL_TUTORS_RESULT", tutors));
               break;
            case "REQUEST_OTP":
               String email = packet.payload;
               if (DatabaseManager.isEmailExists(email)) {
                  this.sendPacket(new Packet(false, "Email này đã được đăng ký trong hệ thống!"));
               } else {
                  String otpCode = String.format("%06d", new Random().nextInt(999999));
                  otpStorage.put(email, otpCode);
                  new Thread(() -> {
                     boolean isSent = EmailService.sendOTP(email, otpCode);
                     if (isSent) {
                        this.sendPacket(new Packet(true, "Mã OTP đã được gửi đến hộp thư của bạn."));
                     } else {
                        this.sendPacket(new Packet(false, "Không thể gửi Email. Vui lòng kiểm tra lại cấu hình SMTP hoặc Email."));
                        otpStorage.remove(email);
                     }
                  }).start();
               }
               break;
            case "VERIFY_AND_REGISTER":
               String[] regData = packet.payload.split(",");
               if (regData.length == 4) {
                  String regEmail = regData[0];
                  String otpInput = regData[1];
                  String rawPass = regData[2];
                  String name = regData[3];
                  String expectedOtp = otpStorage.get(regEmail);
                  if (expectedOtp != null && expectedOtp.equals(otpInput)) {
                     boolean regOk = DatabaseManager.registerUser(regEmail, rawPass, name, "TUTOR");
                     if (regOk) {
                        otpStorage.remove(regEmail);
                        this.sendPacket(new Packet(true, "Đăng ký thành công! Hãy quay lại đăng nhập."));
                     } else {
                        this.sendPacket(new Packet(false, "Lỗi Server: Không thể tạo tài khoản lúc này!"));
                     }
                  } else {
                     this.sendPacket(new Packet(false, "Mã OTP không chính xác hoặc đã hết hạn!"));
                  }
               }
         }
      } catch (Exception var53) {
         System.err.println("[CẢNH BÁO] Lỗi xử lý lệnh " + packet.action + ": " + var53.getMessage());
         var53.printStackTrace();
      }
   }

   public void sendPacket(Packet packet) {
      try {
         if (this.socket != null && this.socket.isOpen()) {
            byte[] data = SerializationUtils.serialize(packet);
            this.socket.send(data);
         }
      } catch (Exception var3) {
         System.err.println("Lỗi gửi gói tin: " + var3.getMessage());
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

   private boolean isCurrentUserConversationMember(List<Integer> memberIds) {
      return this.userId > 0 && memberIds != null && memberIds.contains(this.userId);
   }

   private boolean isLikelyClientMessageId(String value) {
      return value != null && value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
   }

   private void sendToConversationMembers(List<Integer> memberIds, Packet packet, boolean includeSelf) {
      if (memberIds != null && !memberIds.isEmpty()) {
         for (ClientHandler client : onlineClients.values()) {
            if (memberIds.contains(client.userId) && (includeSelf || client.userId != this.userId)) {
               client.sendPacket(packet);
            }
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
      this.sendPacket(response);
   }

   private AuthRequest requireAuthRequest(Packet packet) {
      Object var3 = packet.data;
      if (var3 instanceof AuthRequest) {
         return (AuthRequest)var3;
      } else {
         this.sendAuthResponse(AuthResponse.fail("", "Invalid auth request."));
         return null;
      }
   }

   private void sendAuthResult(String requestId, AuthResult result) {
      this.sendAuthResponse(result.isSuccess() ? AuthResponse.ok(requestId, result.getMessage()) : AuthResponse.fail(requestId, result.getMessage()));
   }

   private void sendAuthLoginResult(String requestId, LoginSession login) {
      if (!login.isSuccess()) {
         this.sendAuthResponse(AuthResponse.fail(requestId, login.getMessage()));
      } else {
         onlineClients.remove(this.clientId);
         this.clientId = login.getIdentity();
         this.userId = login.getUserId();
         onlineClients.put(this.clientId, this);
         this.broadcastUserOnline();
         this.sendAuthResponse(AuthResponse.login(requestId, login.getMessage(), this.buildDashboardPayload(login)));
      }
   }

   private String buildDashboardPayload(LoginSession login) {
      return "DASHBOARD_GO|" + login.getUserId() + "|" + login.getRole() + "|" + login.getAvatarBase64();
   }

   private void broadcastUserOnline() {
      for (ClientHandler client : onlineClients.values()) {
         if (!client.clientId.equals(this.clientId)) {
            client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
         }
      }
   }

   private void sendAuthResponse(AuthResponse response) {
      this.sendPacket(new Packet("AUTH_RESPONSE", response));
   }

   private int parseIntOrDefault(String value, int fallback) {
      try {
         return Integer.parseInt(value.trim());
      } catch (Exception var4) {
         return fallback;
      }
   }

   private long parseLongOrDefault(String value, long fallback) {
      try {
         return Long.parseLong(value.trim());
      } catch (Exception var5) {
         return fallback;
      }
   }

   private int seatCountForStageLayout(String stageLayout) {
      if (stageLayout == null) {
         return 6;
      } else {
         String normalized = stageLayout.trim().toUpperCase();
         int marker = normalized.indexOf(86);
         return marker >= 0 && marker < normalized.length() - 1 ? this.parseIntOrDefault(normalized.substring(marker + 1), 6) : 6;
      }
   }

   private void closeConnections() {
      try {
         if (this.socket != null && this.socket.isOpen()) {
            this.socket.close();
         }
      } catch (Exception var2) {
      }
   }
}
