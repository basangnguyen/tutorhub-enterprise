package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.server.dao.ClassDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO;
import com.mycompany.tutorhub_enterprise.server.services.ChatMessageService;
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

    private boolean isWebClient = false;
    private static final com.google.gson.Gson gson = new com.google.gson.Gson();

    public static ConcurrentHashMap<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> otpStorage = new ConcurrentHashMap<>();
    private static final String LEGACY_PASSWORD_RESET_REJECT_MESSAGE =
            "This password reset flow is no longer supported. Please update the app.";

    public ClientHandler(WebSocket socket) {
        this.socket = socket;
        this.clientId = "User_" + socket.getRemoteSocketAddress().getPort(); 
        onlineClients.put(this.clientId, this);
    }

    public void onDisconnect() {
        System.out.println("[NGÃ¡ÂºÂ®T KÃ¡ÂºÂ¾T NÃ¡Â»ÂI] " + clientId + " Ã„â€˜ÃƒÂ£ rÃ¡Â»Âi khÃ¡Â»Âi hÃ¡Â»â€¡ thÃ¡Â»â€˜ng.");
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

            if (packet.action != null && packet.action.startsWith("AUTH_")) {
                handleAuthRequest(packet);
                return;
            }

            if (packet.action != null && packet.action.startsWith("LOCKET_")) {
                handleLocketRequest(packet);
                return;
            }

            switch (packet.action) {
                // ==========================================
                // PHASE 1: TSE SECURE EXAM MOCK HANDLERS
                // ==========================================
                case "TSE_GET_CONFIG_LIST": {
                    String reqId = packet.payload;
                    System.out.println("[TSE_DB] Handling TSE_GET_CONFIG_LIST requestId=" + reqId);
                    System.out.println("[TSE_DB] Using exam status ACTIVE as published/available state");
                    
                    java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Exam> exams = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getPublishedExams();
                    java.util.List<java.util.Map<String, Object>> configList = new java.util.ArrayList<>();
                    
                    if (exams.isEmpty()) {
                        System.out.println("[TSE_DB] No published exams found");
                    } else {
                        System.out.println("[TSE_DB] Loaded available exams count=" + exams.size());
                        for (com.mycompany.tutorhub_enterprise.models.exam.Exam e : exams) {
                            java.util.Map<String, Object> cfg = new java.util.HashMap<>();
                            cfg.put("examId", e.id);
                            cfg.put("examTitle", e.title);
                            cfg.put("durationMinutes", e.durationMins);
                            cfg.put("status", e.status);
                            
                            boolean requiresPassword = false;
                            String configName = "TSE_Default_Lockdown";
                            String configVersion = "1.0";
                            
                            if (e.securityConfig != null && !e.securityConfig.trim().isEmpty()) {
                                try {
                                    java.util.Map<String, Object> secMap = gson.fromJson(e.securityConfig, java.util.Map.class);
                                    if (secMap.containsKey("require_password") && secMap.get("require_password") instanceof Boolean) {
                                        requiresPassword = (Boolean) secMap.get("require_password");
                                    }
                                    if (secMap.containsKey("security_level")) {
                                        configName = "TSE_" + secMap.get("security_level") + "_Lockdown";
                                    }
                                } catch (Exception ex) {
                                    System.err.println("[TSE_DB] Lỗi parse securityConfig examId=" + e.id);
                                }
                            }
                            
                            cfg.put("requiresPassword", requiresPassword);
                            cfg.put("configName", configName);
                            cfg.put("configVersion", configVersion);
                            
                            configList.add(cfg);
                        }
                    }
                    
                    System.out.println("[TSE_DB] Sending TSE_GET_CONFIG_LIST_RESPONSE requestId=" + reqId);
                    Packet res = new Packet("TSE_GET_CONFIG_LIST_RESPONSE", configList);
                    res.payload = reqId; // Set requestId vào payload của response
                    sendPacket(res);
                    break;
                }
                
                case "EXAM_START_REQUEST": {
                    System.out.println("[TSE_DB] EXAM_START_REQUEST action=" + packet.action);
                    System.out.println("[TSE_DB] raw packet data=" + packet.data);
                    System.out.println("[TSE_DB] raw packet payload=" + packet.payload);
                    System.out.println("[TSE_DB] raw packet message=" + packet.message);
                    
                    if (this.userId <= 0) {
                        Packet res = new Packet();
                        res.action = "EXAM_START_RESPONSE";
                        res.success = false;
                        res.message = "Vui lòng đăng nhập trước khi bắt đầu bài thi";
                        sendPacket(res);
                        break;
                    }
                    
                    String rawString = null;
                    if (packet.data instanceof String) {
                        rawString = (String) packet.data;
                    } else if (packet.payload != null && !packet.payload.isEmpty()) {
                        rawString = packet.payload;
                    }
                    
                    if (rawString == null || rawString.isEmpty()) {
                        Packet res = new Packet();
                        res.action = "EXAM_START_RESPONSE";
                        res.success = false;
                        res.message = "Payload không hợp lệ: expected examId|password";
                        sendPacket(res);
                        break;
                    }
                    
                    String[] parts = rawString.split("\\|", -1);
                    if (parts.length >= 1) {
                        try {
                            int examId = Integer.parseInt(parts[0]);
                            String password = parts.length > 1 ? parts[1] : "";
                            
                            System.out.println("[TSE_DB] Parsed examId=" + examId + ", passwordLength=" + password.length());
                            
                            com.mycompany.tutorhub_enterprise.models.exam.Exam exam = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getExamById(examId);
                            
                            if (exam == null) {
                                Packet res = new Packet();
                                res.action = "EXAM_START_RESPONSE";
                                res.success = false;
                                res.message = "Kỳ thi không tồn tại";
                                sendPacket(res);
                            } else if (!"ACTIVE".equals(exam.status)) {
                                Packet res = new Packet();
                                res.action = "EXAM_START_RESPONSE";
                                res.success = false;
                                res.message = "Kỳ thi chưa mở";
                                sendPacket(res);
                            } else {
                                // Kiểm tra password
                                boolean requiresPassword = false;
                                String expectedPassword = "";
                                if (exam.securityConfig != null && !exam.securityConfig.trim().isEmpty()) {
                                    try {
                                        java.util.Map<String, Object> secMap = gson.fromJson(exam.securityConfig, java.util.Map.class);
                                        if (secMap.containsKey("require_password") && secMap.get("require_password") instanceof Boolean) {
                                            requiresPassword = (Boolean) secMap.get("require_password");
                                        }
                                        if (secMap.containsKey("password")) {
                                            expectedPassword = String.valueOf(secMap.get("password"));
                                        }
                                    } catch (Exception ex) {
                                        System.err.println("[TSE_DB] Lỗi parse securityConfig examId=" + exam.id);
                                    }
                                }
                                
                                if (requiresPassword && !password.equals(expectedPassword)) {
                                    Packet res = new Packet();
                                    res.action = "EXAM_START_RESPONSE";
                                    res.success = false;
                                    res.message = "Sai mật khẩu kỳ thi";
                                    sendPacket(res);
                                } else {
                                    // Tạo session
                                    com.mycompany.tutorhub_enterprise.models.exam.ExamSession session = new com.mycompany.tutorhub_enterprise.models.exam.ExamSession();
                                    session.examId = exam.id;
                                    session.userId = this.userId;
                                    session.status = "IN_PROGRESS";
                                    // Lưu json rỗng vào client_info/question_order để tránh null constraint nếu có
                                    session.clientInfo = "{}";
                                    session.questionOrder = "[]";
                                    
                                    int sessionId = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.createSession(session);
                                    
                                    if (sessionId > 0) {
                                        java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Question> questions = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getQuestionsByExam(exam.id);
                                        
                                        StringBuilder html = new StringBuilder();
                                        html.append("<html><head><meta charset='UTF-8'><style>");
                                        html.append("body { font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; }");
                                        html.append(".question { margin-bottom: 20px; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }");
                                        html.append(".options { margin-top: 10px; }");
                                        html.append(".option { margin-bottom: 5px; }");
                                        html.append("</style>");
                                        html.append("<script>");
                                        html.append("function collectTSEAnswers() {");
                                        html.append("  var payload = {");
                                        html.append("    sessionId: document.getElementById('meta_sessionId').value,");
                                        html.append("    examId: parseInt(document.getElementById('meta_examId').value),");
                                        html.append("    answers: []");
                                        html.append("  };");
                                        html.append("  var questions = document.getElementsByClassName('question');");
                                        html.append("  for (var i=0; i<questions.length; i++) {");
                                        html.append("    var qId = questions[i].getAttribute('data-qid');");
                                        html.append("    var qType = questions[i].getAttribute('data-qtype');");
                                        html.append("    var ans = '';");
                                        html.append("    if (qType === 'MCQ') {");
                                        html.append("      var radios = questions[i].querySelectorAll('input[type=radio]');");
                                        html.append("      for (var j=0; j<radios.length; j++) {");
                                        html.append("        if (radios[j].checked) {");
                                        html.append("          ans = radios[j].value;");
                                        html.append("        }");
                                        html.append("      }");
                                        html.append("    } else if (qType === 'ESSAY') {");
                                        html.append("      var ta = questions[i].querySelector('textarea');");
                                        html.append("      if (ta) ans = ta.value;");
                                        html.append("    } else {");
                                        html.append("      var inp = questions[i].querySelector('input[type=text]');");
                                        html.append("      if (inp) ans = inp.value;");
                                        html.append("    }");
                                        html.append("    payload.answers.push({ questionId: parseInt(qId), answerType: qType, answer: ans });");
                                        html.append("  }");
                                        html.append("  var jsonStr = JSON.stringify(payload);");
                                        html.append("  if (window.cefQuery) {");
                                        html.append("    window.cefQuery({ request: 'SUBMIT_PAYLOAD:' + jsonStr, onSuccess: function(r){}, onFailure: function(e,m){} });");
                                        html.append("  } else { console.log('TSE_JS_RESULT:' + jsonStr); }");
                                        html.append("}");
                                        html.append("</script>");
                                        html.append("</head><body>");
                                        html.append("<input type='hidden' id='meta_sessionId' value='").append(sessionId).append("'>");
                                        html.append("<input type='hidden' id='meta_examId' value='").append(exam.id).append("'>");
                                        html.append("<h1>").append(exam.title).append("</h1>");
                                        html.append("<p><strong>Session ID:</strong> ").append(sessionId).append("</p>");
                                        html.append("<hr/>");
                                        
                                        if (questions.isEmpty()) {
                                            html.append("<p>Bài thi hiện chưa có câu hỏi.</p>");
                                        } else {
                                            for (int i = 0; i < questions.size(); i++) {
                                                com.mycompany.tutorhub_enterprise.models.exam.Question q = questions.get(i);
                                                html.append("<div class='question' data-qid='").append(q.id).append("' data-qtype='").append(q.questionType).append("'>");
                                                
                                                String qText = "Câu hỏi " + (i+1) + " (Nội dung trống)";
                                                java.util.List<String> optionsList = new java.util.ArrayList<>();
                                                
                                                if (q.content != null && !q.content.trim().isEmpty()) {
                                                    try {
                                                        // Fallback nếu không phải JSON
                                                        if (q.content.trim().startsWith("{")) {
                                                            java.util.Map<String, Object> contentMap = gson.fromJson(q.content, java.util.Map.class);
                                                            if (contentMap.containsKey("text")) {
                                                                qText = "Câu hỏi " + (i+1) + ": " + contentMap.get("text");
                                                            }
                                                            if ("MCQ".equals(q.questionType) && contentMap.containsKey("options")) {
                                                                Object opts = contentMap.get("options");
                                                                if (opts instanceof java.util.List) {
                                                                    optionsList = (java.util.List<String>) opts;
                                                                }
                                                            }
                                                        } else {
                                                            qText = "Câu hỏi " + (i+1) + ": " + q.content;
                                                        }
                                                    } catch (Exception ex) {
                                                        qText = "Câu hỏi " + (i+1) + ": " + q.content;
                                                        System.err.println("[TSE_DB] Error parsing question content ID=" + q.id);
                                                    }
                                                }
                                                
                                                html.append("<h3>").append(qText).append("</h3>");
                                                
                                                if ("MCQ".equals(q.questionType) && !optionsList.isEmpty()) {
                                                    html.append("<div class='options'>");
                                                    for (int j = 0; j < optionsList.size(); j++) {
                                                        String optVal = optionsList.get(j);
                                                        html.append("<div class='option'><input type='radio' name='q_").append(q.id)
                                                            .append("' id='q_").append(q.id).append("_").append(j).append("' value='").append(optVal.replace("'", "&apos;")).append("'>");
                                                        html.append("<label for='q_").append(q.id).append("_").append(j).append("'>")
                                                            .append(optVal).append("</label></div>");
                                                    }
                                                    html.append("</div>");
                                                } else if ("ESSAY".equals(q.questionType)) {
                                                    html.append("<textarea rows='4' style='width:100%;' placeholder='Nhập câu trả lời...' id='q_").append(q.id).append("'></textarea>");
                                                } else {
                                                    html.append("<input type='text' style='width:100%;' placeholder='Nhập câu trả lời...' id='q_").append(q.id).append("'>");
                                                }
                                                
                                                html.append("</div>");
                                            }
                                        }
                                        html.append("</body></html>");
                                        
                                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                                        data.put("sessionId", String.valueOf(sessionId));
                                        data.put("examId", exam.id);
                                        data.put("examTitle", exam.title);
                                        data.put("durationMinutes", exam.durationMins);
                                        data.put("htmlContent", html.toString());
                                        
                                        Packet res = new Packet(true, "Bắt đầu thi thành công", data);
                                        res.action = "EXAM_START_RESPONSE";
                                        sendPacket(res);
                                    } else {
                                        Packet res = new Packet();
                                        res.action = "EXAM_START_RESPONSE";
                                        res.success = false;
                                        res.message = "Không thể tạo phiên thi";
                                        sendPacket(res);
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            Packet res = new Packet();
                            res.action = "EXAM_START_RESPONSE";
                            res.success = false;
                            res.message = "examId không hợp lệ";
                            sendPacket(res);
                        }
                    } else {
                        Packet res = new Packet();
                        res.action = "EXAM_START_RESPONSE";
                        res.success = false;
                        res.message = "Payload không hợp lệ";
                        sendPacket(res);
                    }
                    break;
                }
                
                case "EXAM_SUBMIT": {
                    System.out.println("[TSE_DB] EXAM_SUBMIT action=" + packet.action);
                    
                    if (this.userId <= 0) {
                        Packet res = new Packet();
                        res.action = "EXAM_SUBMIT_ACK";
                        res.success = false;
                        res.message = "Vui lòng đăng nhập trước khi nộp bài";
                        sendPacket(res);
                        break;
                    }
                    
                    String rawString = null;
                    if (packet.data instanceof String) {
                        rawString = (String) packet.data;
                    } else if (packet.payload != null && !packet.payload.isEmpty()) {
                        rawString = packet.payload;
                    }
                    
                    if (rawString == null || rawString.isEmpty()) {
                        Packet res = new Packet();
                        res.action = "EXAM_SUBMIT_ACK";
                        res.success = false;
                        res.message = "Payload không hợp lệ: expected sessionId|submitPayload";
                        sendPacket(res);
                        break;
                    }
                    
                    String[] parts = rawString.split("\\|", 2);
                    if (parts.length >= 2) {
                        try {
                            String sessIdStr = parts[0];
                            if (sessIdStr.startsWith("sess-")) {
                                sessIdStr = sessIdStr.substring(5);
                            }
                            int sessionId = Integer.parseInt(sessIdStr);
                            String submitPayload = parts[1];
                            
                            com.mycompany.tutorhub_enterprise.models.exam.ExamSession session = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getSessionById(sessionId);
                            
                            if (session == null) {
                                Packet res = new Packet();
                                res.action = "EXAM_SUBMIT_ACK";
                                res.success = false;
                                res.message = "Phiên thi không tồn tại";
                                sendPacket(res);
                            } else if (session.userId != this.userId) {
                                Packet res = new Packet();
                                res.action = "EXAM_SUBMIT_ACK";
                                res.success = false;
                                res.message = "Bạn không có quyền nộp bài thi này";
                                sendPacket(res);
                            } else if ("SUBMITTED".equals(session.status)) {
                                Packet res = new Packet();
                                res.action = "EXAM_SUBMIT_ACK";
                                res.success = false;
                                res.message = "Bạn đã nộp bài trước đó";
                                sendPacket(res);
                            } else if (!"IN_PROGRESS".equals(session.status)) {
                                Packet res = new Packet();
                                res.action = "EXAM_SUBMIT_ACK";
                                res.success = false;
                                res.message = "Trạng thái phiên thi không hợp lệ: " + session.status;
                                sendPacket(res);
                            } else {
                                System.out.println("[TSE_DB] Received submitPayload length=" + submitPayload.length());
                                String preview = submitPayload.length() > 100 ? submitPayload.substring(0, 100) + "..." : submitPayload;
                                System.out.println("[TSE_DB] Submit payload preview=" + preview);
                                
                                try {
                                    java.util.Map<String, Object> payloadMap = gson.fromJson(submitPayload, java.util.Map.class);
                                    if (payloadMap == null || !payloadMap.containsKey("answers")) {
                                        throw new Exception("Missing answers");
                                    }
                                    
                                    java.util.List<java.util.Map<String, Object>> answersList = (java.util.List<java.util.Map<String, Object>>) payloadMap.get("answers");
                                    System.out.println("[TSE_DB] Parsed submitPayload answers count=" + answersList.size());
                                    
                                    java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Question> examQuestions = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getQuestionsByExam(session.examId);
                                    java.util.Set<Integer> validQuestionIds = new java.util.HashSet<>();
                                    for (com.mycompany.tutorhub_enterprise.models.exam.Question q : examQuestions) {
                                        validQuestionIds.add(q.id);
                                    }
                                    
                                    int savedCount = 0;
                                    for (java.util.Map<String, Object> ans : answersList) {
                                        if (ans != null && ans.containsKey("questionId")) {
                                            int qId = ((Number) ans.get("questionId")).intValue();
                                            if (validQuestionIds.contains(qId)) {
                                                String ansJson = gson.toJson(ans);
                                                boolean saved = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.saveAnswer(sessionId, qId, ansJson);
                                                if (saved) savedCount++;
                                            }
                                        }
                                    }
                                    System.out.println("[TSE_DB] Saved answers count=" + savedCount);
                                    
                                    boolean updated = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.updateSessionStatus(sessionId, "SUBMITTED");
                                    
                                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                                    data.put("sessionId", String.valueOf(sessionId));
                                    data.put("examId", session.examId);
                                    data.put("submitStatus", "RECEIVED");
                                    data.put("savedAnswerCount", savedCount);
                                    
                                    Packet res = new Packet(updated, updated ? "Nộp bài thành công" : "Lỗi cập nhật DB", data);
                                    res.action = "EXAM_SUBMIT_ACK";
                                    sendPacket(res);
                                    
                                } catch (Exception ex) {
                                    System.err.println("[TSE_DB] Error parsing submit payload: " + ex.getMessage());
                                    Packet res = new Packet();
                                    res.action = "EXAM_SUBMIT_ACK";
                                    res.success = false;
                                    res.message = "Dữ liệu bài làm không hợp lệ";
                                    sendPacket(res);
                                }
                            }
                        } catch (NumberFormatException e) {
                            Packet res = new Packet();
                            res.action = "EXAM_SUBMIT_ACK";
                            res.success = false;
                            res.message = "sessionId không hợp lệ";
                            sendPacket(res);
                        }
                    } else {
                        Packet res = new Packet();
                        res.action = "EXAM_SUBMIT_ACK";
                        res.success = false;
                        res.message = "Payload không hợp lệ";
                        sendPacket(res);
                    }
                    break;
                }
                // ==========================================

                case "CALENDAR_SEND_EVENT_INVITE":
                case "CALENDAR_SEND_POLL_INVITE": {
                    System.out.println("[SCHEDULE_EMAIL] Nhận yêu cầu gửi email: " + packet.action);
                    try {
                        String payload = packet.payload;
                        if (payload != null && !payload.isEmpty()) {
                            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                            String title = json.has("title") ? json.get("title").getAsString() : "";
                            String date = json.has("date") ? json.get("date").getAsString() : "";
                            String time = json.has("time") ? json.get("time").getAsString() : "";
                            String location = json.has("location") ? json.get("location").getAsString() : "";
                            String description = json.has("description") ? json.get("description").getAsString() : "";
                            String meetLink = json.has("meetLink") ? json.get("meetLink").getAsString() : "";
                            
                            boolean isPoll = "CALENDAR_SEND_POLL_INVITE".equals(packet.action);
                            String subject = "TutorHub - Lịch học mới: " + title;
                            String timeStr = date + " " + time;
                            
                            java.util.List<String> emails = new java.util.ArrayList<>();
                            if (json.has("guestEmails")) {
                                com.google.gson.JsonArray arr = json.getAsJsonArray("guestEmails");
                                for (com.google.gson.JsonElement el : arr) {
                                    String email = el.getAsString().trim();
                                    if (!email.isEmpty() && !emails.contains(email)) {
                                        emails.add(email);
                                    }
                                }
                            }
                            System.out.println("[SCHEDULE_EMAIL] invite count=" + emails.size());
                            
                            boolean success = false;
                            for (String email : emails) {
                                String maskedEmail = email.length() > 5 ? email.substring(0, 2) + "***" + email.substring(email.indexOf('@')) : "***";
                                System.out.println("[SCHEDULE_EMAIL] Sending to: " + maskedEmail);
                                if (isPoll) {
                                    success = EmailService.sendPollInvite(email, subject, title, timeStr, location, meetLink, null, description, "");
                                } else {
                                    success = EmailService.sendCalendarInvite(email, subject, title, timeStr, location, meetLink, null, description);
                                }
                            }
                            System.out.println("[SCHEDULE_EMAIL] send success=" + success);
                        }
                    } catch (Exception e) {
                        System.err.println("[SCHEDULE_EMAIL] Error parsing payload: " + e.getClass().getSimpleName());
                    }
                    break;
                }

                case "GET_EXAMS":
                    if (this.userId > 0) {
                        sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleGetExams(this.userId));
                    } else {
                        sendPacket(new Packet(false, "Vui lÃƒÂ²ng Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p!"));
                    }
                    break;
                case "GET_EXAM_QUESTIONS":
                    if (this.userId > 0) {
                        sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleGetExamQuestions(packet.payload));
                    }
                    break;
                case "CREATE_EXAM":
                    if (this.userId > 0) {
                        sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleCreateExam(this.userId, packet.payload));
                    } else {
                        sendPacket(new Packet(false, "Vui lÃƒÂ²ng Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p!"));
                    }
                    break;
                case "ADD_QUESTION":
                    if (this.userId > 0) {
                        sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleAddQuestion(packet.payload));
                    }
                    break;
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
                        System.err.println("LÃ¡Â»â€”i Ã„â€˜Ã¡Â»â€œng bÃ¡Â»â„¢ session: " + ex.getMessage());
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
                        sendPacket(new Packet(ok, ok ? "Ã„ÂÃ„Æ’ng thÃ†Â°Ã¡Â»â€ºc phim thÃƒÂ nh cÃƒÂ´ng!" : "LÃ¡Â»â€”i khi Ã„â€˜Ã„Æ’ng."));
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
                        sendPacket(new Packet(ok, ok ? "Ã„ÂÃ„Æ’ng Locket thÃƒÂ nh cÃƒÂ´ng!" : "LÃ¡Â»â€”i khi Ã„â€˜Ã„Æ’ng."));
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
                            System.err.println("[SERVER LÃ¡Â»â€“I] KhÃƒÂ´ng tÃƒÂ¬m thÃ¡ÂºÂ¥y file: " + targetFile.getPath());
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
                            sendPacket(new Packet(false, "Email nÃƒÂ y Ã„â€˜ÃƒÂ£ tÃ¡Â»â€œn tÃ¡ÂºÂ¡i trong hÃ¡Â»â€¡ thÃ¡Â»â€˜ng!"));
                        } else {
                            boolean isSuccess = DatabaseManager.registerUser(email, rawPass, fullName, "TUTOR");
                            if (isSuccess) sendPacket(new Packet(true, "TÃ¡ÂºÂ¡o tÃƒÂ i khoÃ¡ÂºÂ£n gia sÃ†Â° thÃƒÂ nh cÃƒÂ´ng!"));
                            else sendPacket(new Packet(false, "LÃ¡Â»â€”i Server: KhÃƒÂ´ng thÃ¡Â»Æ’ ghi vÃƒÂ o cÃ†Â¡ sÃ¡Â»Å¸ dÃ¡Â»Â¯ liÃ¡Â»â€¡u."));
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
                                sendPacket(new Packet("RESPONSE", "Ã„ÂÃƒÂ£ thÃƒÂªm BÃ¡ÂºÂ±ng cÃ¡ÂºÂ¥p thÃƒÂ nh cÃƒÂ´ng! ChÃ¡Â»Â Admin duyÃ¡Â»â€¡t."));
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
                                sendPacket(new Packet("RESPONSE", "Ã„ÂÃƒÂ£ thÃƒÂªm ChÃ¡Â»Â©ng chÃ¡Â»â€° thÃƒÂ nh cÃƒÂ´ng! ChÃ¡Â»Â Admin duyÃ¡Â»â€¡t."));
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
                    if (isReqSent) sendPacket(new Packet(true, "Ã„ÂÃƒÂ£ gÃ¡Â»Â­i lÃ¡Â»Âi mÃ¡Â»Âi kÃ¡ÂºÂ¿t bÃ¡ÂºÂ¡n!", "FRIEND_REQUEST_SENT"));
                    else sendPacket(new Packet(false, "LÃ¡Â»â€”i: KhÃƒÂ´ng thÃ¡Â»Æ’ gÃ¡Â»Â­i lÃ¡Â»Âi mÃ¡Â»Âi."));
                    break;
                }

                case "ACCEPT_FRIEND": {
                    int requesterId = Integer.parseInt(packet.payload);
                    boolean isAccepted = com.mycompany.tutorhub_enterprise.server.dao.UserDAO.acceptFriendRequest(requesterId, this.userId);
                    if (isAccepted) {
                        sendPacket(new Packet(true, "Ã„ÂÃƒÂ£ trÃ¡Â»Å¸ thÃƒÂ nh bÃ¡ÂºÂ¡n bÃƒÂ¨!", "FRIEND_ACCEPTED"));
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
                    ChatMessageService.SendChatResult result = ChatMessageService.handleSendChat(this.userId, this.clientId, packet.payload);
                    if (!result.accepted()) {
                        sendPacket(new Packet(false, result.errorMessage()));
                        break;
                    }
                    boolean deliveredToOnlineClient = false;
                    if (result.newMessage()) {
                        String forwardPayload = result.forwardPayload();
                        for (ClientHandler client : onlineClients.values()) {
                            if (!client.clientId.equals(this.clientId)) {
                                client.sendPacket(new Packet("RECEIVE_CHAT", forwardPayload));
                                deliveredToOnlineClient = true;
                            }
                        }

                    }
                    sendPacket(new Packet("SEND_CHAT_ACK", result.ackPayload()));
                    if (deliveredToOnlineClient) {
                        sendPacket(new Packet("MESSAGE_DELIVERED_ACK", result.ackPayload()));
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
                        System.err.println("[SERVER LÃ¡Â»â€“I] LÃ¡Â»â€”i cÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t trÃ¡ÂºÂ¡ng thÃƒÂ¡i Ã„â€˜ÃƒÂ£ xem: " + ex.getMessage());
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
                            sendPacket(new Packet("RESPONSE", "LÃ¡Â»â€”i: KhÃƒÂ´ng thÃ¡Â»Æ’ lÃ†Â°u bÃ¡ÂºÂ£ng vÃ¡ÂºÂ½ lÃƒÂªn Database!"));
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
                            System.err.println("LÃ¡Â»â€”i cÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t trÃ¡ÂºÂ¡ng thÃƒÂ¡i gia sÃ†Â°: " + ex.getMessage());
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
                            System.err.println("[SERVER LÃ¡Â»â€“I] CÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t bÃ¡ÂºÂ£ng vÃ¡ÂºÂ½: " + e.getMessage());
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
                            if(!deleted) System.err.println("KhÃƒÂ´ng thÃ¡Â»Æ’ xÃƒÂ³a BÃ¡ÂºÂ±ng cÃ¡ÂºÂ¥p, hÃƒÂ£y kiÃ¡Â»Æ’m tra lÃ¡ÂºÂ¡i tÃƒÂªn cÃ¡Â»â„¢t trong DB!");
                            sendPacket(new Packet("DEGREES_RESULT", DatabaseManager.getDegrees(this.userId)));
                        }
                    } catch (Exception e) { System.err.println("LÃ¡Â»â€”i xÃƒÂ³a BÃ¡ÂºÂ±ng cÃ¡ÂºÂ¥p: " + e.getMessage()); }
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
                            if(!deleted) System.err.println("KhÃƒÂ´ng thÃ¡Â»Æ’ xÃƒÂ³a ChÃ¡Â»Â©ng chÃ¡Â»â€°, hÃƒÂ£y kiÃ¡Â»Æ’m tra lÃ¡ÂºÂ¡i tÃƒÂªn cÃ¡Â»â„¢t trong DB!");
                            sendPacket(new Packet("CERTIFICATES_RESULT", DatabaseManager.getCertificates(this.userId)));
                        }
                    } catch (Exception e) { System.err.println("LÃ¡Â»â€”i xÃƒÂ³a ChÃ¡Â»Â©ng chÃ¡Â»â€°: " + e.getMessage()); }
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
                                if(!deleted) System.err.println("KhÃƒÂ´ng thÃ¡Â»Æ’ xÃƒÂ³a Kinh nghiÃ¡Â»â€¡m, hÃƒÂ£y kiÃ¡Â»Æ’m tra lÃ¡ÂºÂ¡i tÃƒÂªn cÃ¡Â»â„¢t trong DB!");
                                sendPacket(new Packet("EXPERIENCES_RESULT", DatabaseManager.getExperiences(this.userId)));
                            }
                        }
                    } catch (Exception e) { System.err.println("LÃ¡Â»â€”i xÃƒÂ³a Kinh nghiÃ¡Â»â€¡m: " + e.getMessage()); }
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

                case "REQUEST_OTP_RESET":
                case "VERIFY_AND_RESET": {
                    rejectLegacyPasswordResetAction(packet.action);
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
                        System.err.println("LÃ¡Â»â€”i lÃ¡ÂºÂ¥y avatar gia sÃ†Â°: " + ex.getMessage());
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
                            pstTask.setString(2, "DÃ¡ÂºÂ¡y lÃ¡Â»â€ºp " + classCode);
                            pstTask.setString(3, "ThÃ¡Â»Âi gian: Theo thÃ¡Â»Âa thuÃ¡ÂºÂ­n");
                            pstTask.setString(4, "Ã„ÂÃ¡Â»â€¹a Ã„â€˜iÃ¡Â»Æ’m: Ã„Âang cÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t");
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
                            sendPacket(new Packet(true, "TÃ¡ÂºÂ¡o lÃ¡Â»â€ºp hÃ¡Â»Âc thÃƒÂ nh cÃƒÂ´ng!", "CREATE_CLASSROOM_SUCCESS"));
                            sendPacket(new Packet("GET_CLASSROOMS_RESPONSE", dao.getClassroomsByUser(this.userId)));
                        } else {
                            sendPacket(new Packet(false, "LÃ¡Â»â€”i khi tÃ¡ÂºÂ¡o lÃ¡Â»â€ºp hÃ¡Â»Âc!", "CREATE_CLASSROOM_FAIL"));
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
                            String time = "ThÃ¡Â»Â© 2,4,6"; 
                            double salNum = 0;
                            try { salNum = Double.parseDouble(tuition.replaceAll("[^0-9]", "")); } catch(Exception e){}
                            String formattedSalary = String.format("%,.0fÃ„â€˜/buÃ¡Â»â€¢i", salNum).replace(",", ".");
                            String broadcastPayload = newId + "|" + subj + "|" + formattedSalary + "|" + loc + "|" + time + "|" + title + "|MÃ¡Â»Å¡I|#10B981";
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
                                System.err.println("[SERVER LÃ¡Â»â€“I] KhÃƒÂ´ng thÃ¡Â»Æ’ lÃ¡ÂºÂ¥y Role vÃƒÂ  Avatar: " + ex.getMessage());
                            }

                            onlineClients.remove(this.clientId);
                            this.clientId = loginData[0]; 
                            this.userId = loginUid; 
                            onlineClients.put(this.clientId, this);
                            sendPacket(new Packet(true, "Ã„ÂÃ„Æ’ng nhÃ¡ÂºÂ­p thÃƒÂ nh cÃƒÂ´ng!", "DASHBOARD_GO|" + loginUid + "|" + userRole + "|" + avatarBase64));

                            for (ClientHandler client : onlineClients.values()) {
                                if (!client.clientId.equals(this.clientId)) {
                                    client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
                                }
                            }
                        } else {
                            sendPacket(new Packet(false, "Sai Email hoÃ¡ÂºÂ·c mÃ¡ÂºÂ­t khÃ¡ÂºÂ©u!", ""));
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
                        sendPacket(new Packet(false, "Email nÃƒÂ y Ã„â€˜ÃƒÂ£ Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã„Æ’ng kÃƒÂ½ trong hÃ¡Â»â€¡ thÃ¡Â»â€˜ng!"));
                        break;
                    }
                    String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
                    otpStorage.put(email, otpCode);
                    new Thread(() -> {
                        boolean isSent = EmailService.sendOTP(email, otpCode);
                        if (isSent) sendPacket(new Packet(true, "MÃƒÂ£ OTP Ã„â€˜ÃƒÂ£ Ã„â€˜Ã†Â°Ã¡Â»Â£c gÃ¡Â»Â­i Ã„â€˜Ã¡ÂºÂ¿n hÃ¡Â»â„¢p thÃ†Â° cÃ¡Â»Â§a bÃ¡ÂºÂ¡n."));
                        else {
                            sendPacket(new Packet(false, "KhÃƒÂ´ng thÃ¡Â»Æ’ gÃ¡Â»Â­i Email. Vui lÃƒÂ²ng kiÃ¡Â»Æ’m tra lÃ¡ÂºÂ¡i cÃ¡ÂºÂ¥u hÃƒÂ¬nh SMTP hoÃ¡ÂºÂ·c Email."));
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
                                sendPacket(new Packet(true, "Ã„ÂÃ„Æ’ng kÃƒÂ½ thÃƒÂ nh cÃƒÂ´ng! HÃƒÂ£y quay lÃ¡ÂºÂ¡i Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p."));
                            } else {
                                sendPacket(new Packet(false, "LÃ¡Â»â€”i Server: KhÃƒÂ´ng thÃ¡Â»Æ’ tÃ¡ÂºÂ¡o tÃƒÂ i khoÃ¡ÂºÂ£n lÃƒÂºc nÃƒÂ y!"));
                            }
                        } else {
                            sendPacket(new Packet(false, "MÃƒÂ£ OTP khÃƒÂ´ng chÃƒÂ­nh xÃƒÂ¡c hoÃ¡ÂºÂ·c Ã„â€˜ÃƒÂ£ hÃ¡ÂºÂ¿t hÃ¡ÂºÂ¡n!"));
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[CÃ¡ÂºÂ¢NH BÃƒÂO] LÃ¡Â»â€”i xÃ¡Â»Â­ lÃƒÂ½ lÃ¡Â»â€¡nh " + packet.action + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setWebClient(boolean webClient) {
        this.isWebClient = webClient;
    }

    public void sendPacket(Packet packet) {
        try {
            if (socket != null && socket.isOpen()) {
                if (isWebClient) {
                    socket.send(gson.toJson(packet));
                } else {
                    byte[] data = SerializationUtils.serialize(packet);
                    socket.send(data);
                }
            }
        } catch (Exception e) {
            System.err.println("LÃ¡Â»â€”i gÃ¡Â»Â­i gÃƒÂ³i tin: " + e.getMessage());
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

    private com.mycompany.tutorhub_enterprise.models.auth.AuthRequest toAuthRequest(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof com.mycompany.tutorhub_enterprise.models.auth.AuthRequest) {
            return (com.mycompany.tutorhub_enterprise.models.auth.AuthRequest) payload;
        }
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonElement json = gson.toJsonTree(payload);
            return gson.fromJson(json, com.mycompany.tutorhub_enterprise.models.auth.AuthRequest.class);
        } catch (Exception e) {
            System.err.println("[AUTH] Cannot convert payload to AuthRequest: " + e.getMessage());
            return null;
        }
    }

    private void handleAuthRequest(Packet packet) {
        System.out.println("[AUTH] Received action: " + packet.action);
        System.out.println("[AUTH] Payload class = " + (packet.data != null ? packet.data.getClass().getName() : "null"));
        
        com.mycompany.tutorhub_enterprise.models.auth.AuthRequest request = toAuthRequest(packet.data);
        if (request == null) {
            System.err.println("[AUTH] Lỗi: Không thể convert payload sang AuthRequest.");
            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail("unknown", "Payload gửi lên máy chủ không hợp lệ.");
            sendPacket(new Packet(com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol.RESPONSE, response));
            return;
        }
        System.out.println("[AUTH] Converted payload to AuthRequest successfully");
        
        // Validation cho AUTH_SOCIAL_LOGIN
        if (com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol.AUTH_SOCIAL_LOGIN.equals(packet.action)) {
            if (request.getProvider() == null || request.getProvider().isEmpty() ||
                request.getAuthorizationCode() == null || request.getAuthorizationCode().isEmpty() ||
                request.getCodeVerifier() == null || request.getCodeVerifier().isEmpty() ||
                request.getRedirectUri() == null || request.getRedirectUri().isEmpty() ||
                request.getNonce() == null || request.getNonce().isEmpty()) {
                
                System.err.println("[AUTH] Lỗi: Thiếu tham số cho AUTH_SOCIAL_LOGIN");
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(request.getRequestId(), "Thiếu tham số bắt buộc cho đăng nhập mạng xã hội.");
                sendPacket(new Packet(com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol.RESPONSE, response));
                return;
            }
            System.out.println("[AUTH] Provider = " + request.getProvider());
        }
        String requestId = request.getRequestId();
        
        switch (packet.action) {
            case AuthProtocol.LOGIN: {
                com.mycompany.tutorhub_enterprise.server.AuthService.LoginSession session = com.mycompany.tutorhub_enterprise.server.AuthService.authenticateWithPassword(request.getEmail(), request.getPassword());
                handleAuthLoginSession(session, requestId);
                break;
            }
            case AuthProtocol.AUTH_SOCIAL_LOGIN: {
                System.out.println("[AUTH] Received AUTH_SOCIAL_LOGIN from client for provider: " + request.getProvider());
                com.mycompany.tutorhub_enterprise.server.AuthService.LoginSession session = com.mycompany.tutorhub_enterprise.server.AuthService.authenticateWithSocialProvider(
                        request.getProvider(),
                        request.getAuthorizationCode(),
                        request.getCodeVerifier(),
                        request.getRedirectUri(),
                        request.getNonce()
                );
                handleAuthLoginSession(session, requestId);
                break;
            }
            case AuthProtocol.AUTH_FACEBOOK_START: {
                try {
                    java.util.Map<String, Object> data = com.mycompany.tutorhub_enterprise.server.SocialAuthService.startFacebookLoginSession();
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    String dashboardPayload = gson.toJson(data);
                    
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.login(requestId, "FACEBOOK_START_OK", dashboardPayload);
                    sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                    System.out.println("[FACEBOOK] AUTH_RESPONSE sent for AUTH_FACEBOOK_START success");
                } catch (Exception e) {
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, "Lỗi khi bắt đầu đăng nhập Facebook: " + e.getMessage());
                    sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                    System.out.println("[FACEBOOK] AUTH_RESPONSE sent for AUTH_FACEBOOK_START failure");
                }
                break;
            }
            case AuthProtocol.AUTH_FACEBOOK_POLL: {
                FacebookPendingSession pendingSession = com.mycompany.tutorhub_enterprise.server.SocialAuthService.pollFacebookLogin(request.getSessionId());
                System.out.println("[FACEBOOK_POLL] session found = " + (pendingSession != null));
                if (pendingSession == null) {
                    System.out.println("[FACEBOOK_POLL] returning FAILED (Not found)");
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, "Phiên đăng nhập không tồn tại hoặc đã hết hạn.");
                    sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                } else {
                    System.out.println("[FACEBOOK_POLL] status = " + pendingSession.getStatus());
                    if (pendingSession.getStatus() == FacebookPendingSession.Status.SUCCESS) {
                        System.out.println("[FACEBOOK_POLL] hasAuthResponse = " + (pendingSession.getPayload() != null));
                        System.out.println("[FACEBOOK_POLL] returning SUCCESS");
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse res = pendingSession.getPayload();
                        if (res != null) {
                            String dbPayload = res.getDashboardPayload();
                            // parse uid to register online
                            if (dbPayload != null && dbPayload.contains("|")) {
                                try {
                                    String[] parts = dbPayload.split("\\|");
                                    if (parts.length > 1) {
                                        this.userId = Integer.parseInt(parts[1]);
                                        this.clientId = "User_" + this.userId; // Mock email identity
                                        onlineClients.put(this.clientId, this);
                                        for (ClientHandler client : onlineClients.values()) {
                                            if (!client.clientId.equals(this.clientId)) {
                                                client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse finalRes = 
                                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.loginWithSession(
                                    requestId, res.getMessage(), res.getDashboardPayload(), res.getSessionInfo());
                            sendPacket(new Packet(AuthProtocol.RESPONSE, finalRes));
                        } else {
                            System.out.println("[FACEBOOK_POLL] missing AuthResponse payload despite SUCCESS");
                            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, "Lỗi bất thường: Không có thông tin người dùng.");
                            sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                        }
                    } else if (pendingSession.getStatus() == FacebookPendingSession.Status.FAILED) {
                        System.out.println("[FACEBOOK_POLL] returning FAILED");
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, pendingSession.getErrorMessage());
                        sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                    } else {
                        System.out.println("[FACEBOOK_POLL] returning PENDING");
                        // PENDING
                        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.ok(requestId, "PENDING");
                        sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                    }
                }
                break;
            }
            case AuthProtocol.REQUEST_REGISTRATION_OTP: {
                com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result = com.mycompany.tutorhub_enterprise.server.AuthService.requestRegistrationOtp(request.getEmail());
                handleAuthResult(result, requestId);
                break;
            }
            case AuthProtocol.VERIFY_AND_REGISTER: {
                com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result = com.mycompany.tutorhub_enterprise.server.AuthService.verifyAndRegister(request.getEmail(), request.getOtp(), request.getPassword(), request.getFullName());
                handleAuthResult(result, requestId);
                break;
            }
            case AuthProtocol.REQUEST_PASSWORD_RESET_OTP: {
                com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result = com.mycompany.tutorhub_enterprise.server.AuthService.requestPasswordResetOtp(request.getEmail());
                handleAuthResult(result, requestId);
                break;
            }
            case AuthProtocol.VERIFY_AND_RESET_PASSWORD: {
                com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result = com.mycompany.tutorhub_enterprise.server.AuthService.verifyAndResetPassword(request.getEmail(), request.getOtp(), request.getPassword());
                handleAuthResult(result, requestId);
                break;
            }
            case AuthProtocol.REQUEST_SMS_LOGIN_OTP: {
                com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result = com.mycompany.tutorhub_enterprise.server.AuthService.requestSmsLoginOtp(request.getPhone());
                handleAuthResult(result, requestId);
                break;
            }
            case AuthProtocol.VERIFY_SMS_LOGIN: {
                com.mycompany.tutorhub_enterprise.server.AuthService.LoginSession session = com.mycompany.tutorhub_enterprise.server.AuthService.verifySmsLogin(request.getPhone(), request.getOtp());
                handleAuthLoginSession(session, requestId);
                break;
            }
            case AuthProtocol.AUTH_LOGOUT: {
                System.out.println("[AUTH_LOGOUT] revoke requested");
                try {
                    boolean revoked = com.mycompany.tutorhub_enterprise.server.SessionService.revokeSession(request.getSessionId(), request.getAccessToken());
                    if (revoked) {
                        System.out.println("[AUTH_LOGOUT] session revoked successfully");
                    } else {
                        System.out.println("[AUTH_LOGOUT] session revoked or already inactive");
                    }
                } catch (Exception e) {
                    System.out.println("[AUTH_LOGOUT] session revoked or already inactive");
                }
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.ok(requestId, "Logout successful.");
                sendPacket(new Packet(AuthProtocol.RESPONSE, response));
                break;
            }
            default: {
                System.err.println("Unsupported AUTH action: " + packet.action);
                break;
            }
        }
    }

    private void rejectLegacyPasswordResetAction(String action) {
        System.err.println("[AUTH_SECURITY] Legacy password reset action rejected: " + action);
        sendPacket(new Packet(false, LEGACY_PASSWORD_RESET_REJECT_MESSAGE));
    }

    private void handleAuthLoginSession(com.mycompany.tutorhub_enterprise.server.AuthService.LoginSession session, String requestId) {
        if (session.isSuccess()) {
            System.out.println("[AUTH] AUTH_RESPONSE sent success for requestId: " + requestId);
            onlineClients.remove(this.clientId);
            this.clientId = session.getIdentity();
            this.userId = session.getUserId();
            onlineClients.put(this.clientId, this);
            
            String dashboardPayload = "DASHBOARD_GO|" + this.userId + "|" + session.getRole() + "|" + session.getAvatarBase64() + "|" + session.getIdentity();
            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.loginWithSession(requestId, "Ã„ÂÃ„Æ’ng nhÃ¡ÂºÂ­p thÃƒÂ nh cÃƒÂ´ng!", dashboardPayload, session.getSessionInfo());
            sendPacket(new Packet(AuthProtocol.RESPONSE, response));
            
            for (ClientHandler client : onlineClients.values()) {
                if (!client.clientId.equals(this.clientId)) {
                    client.sendPacket(new Packet("USER_ONLINE", String.valueOf(this.userId)));
                }
            }
        } else {
            System.out.println("[AUTH] AUTH_RESPONSE sent failure for requestId: " + requestId + ". Reason: " + session.getMessage());
            com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response = 
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, session.getMessage());
            sendPacket(new Packet(AuthProtocol.RESPONSE, response));
        }
    }

    private void handleAuthResult(com.mycompany.tutorhub_enterprise.server.AuthService.AuthResult result, String requestId) {
        com.mycompany.tutorhub_enterprise.models.auth.AuthResponse response;
        if (result.isSuccess()) {
            response = com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.ok(requestId, result.getMessage());
        } else {
            response = com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.fail(requestId, result.getMessage());
        }
        sendPacket(new Packet(AuthProtocol.RESPONSE, response));
    }

    private void handleLocketRequest(Packet packet) {
        if (this.userId == -1) {
            sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Unauthorized"));
            return;
        }

        try {
            com.google.gson.JsonObject payload = com.google.gson.JsonParser.parseString(packet.payload).getAsJsonObject();

            switch (packet.action) {
                case AuthProtocol.LOCKET_POST_LIST: {
                    int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 10;
                    long cursor = payload.has("cursor") ? payload.get("cursor").getAsLong() : 0;
                    
                    java.util.List<com.mycompany.tutorhub_enterprise.models.locket.LocketPostViewDTO> posts = 
                        com.mycompany.tutorhub_enterprise.server.services.LocketService.listPosts(this.userId, limit, cursor);
                    
                    sendPacket(new Packet(AuthProtocol.LOCKET_POST_LIST_SUCCESS, new com.google.gson.Gson().toJson(posts)));
                    break;
                }
                case AuthProtocol.LOCKET_POST_CREATE: {
                    String imageUrl = payload.get("imageUrl").getAsString();
                    String thumbnailUrl = payload.has("thumbnailUrl") ? payload.get("thumbnailUrl").getAsString() : imageUrl;
                    String caption = payload.has("caption") ? payload.get("caption").getAsString() : "";
                    
                    com.mycompany.tutorhub_enterprise.models.locket.LocketPostModel post = 
                        com.mycompany.tutorhub_enterprise.server.services.LocketService.createPost(this.userId, imageUrl, thumbnailUrl, caption);
                    
                    if (post != null) {
                        sendPacket(new Packet(AuthProtocol.LOCKET_POST_CREATE_SUCCESS, new com.google.gson.Gson().toJson(post)));
                    } else {
                        sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Failed to create post"));
                    }
                    break;
                }
                case AuthProtocol.LOCKET_POST_DELETE: {
                    long postId = payload.get("postId").getAsLong();
                    boolean success = com.mycompany.tutorhub_enterprise.server.services.LocketService.deletePost(postId, this.userId);
                    if (success) {
                        sendPacket(new Packet(AuthProtocol.LOCKET_POST_DELETE_SUCCESS, String.valueOf(postId)));
                    } else {
                        sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Failed to delete post"));
                    }
                    break;
                }
                case AuthProtocol.LOCKET_POST_REACT: {
                    long postId = payload.get("postId").getAsLong();
                    boolean reacted = com.mycompany.tutorhub_enterprise.server.services.LocketService.toggleReaction(postId, this.userId);
                    
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("postId", postId);
                    resp.addProperty("reacted", reacted);
                    sendPacket(new Packet(AuthProtocol.LOCKET_POST_REACT_SUCCESS, resp.toString()));
                    break;
                }
                case AuthProtocol.LOCKET_COMMENT_LIST: {
                    long postId = payload.get("postId").getAsLong();
                    int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 10;
                    long cursor = payload.has("cursor") ? payload.get("cursor").getAsLong() : 0;
                    
                    java.util.List<com.mycompany.tutorhub_enterprise.models.locket.LocketCommentViewDTO> comments = 
                        com.mycompany.tutorhub_enterprise.server.services.LocketService.listComments(postId, this.userId, limit, cursor);
                    
                    sendPacket(new Packet(AuthProtocol.LOCKET_COMMENT_LIST_SUCCESS, new com.google.gson.Gson().toJson(comments)));
                    break;
                }
                case AuthProtocol.LOCKET_COMMENT_CREATE: {
                    long postId = payload.get("postId").getAsLong();
                    String content = payload.get("content").getAsString();
                    
                    com.mycompany.tutorhub_enterprise.models.locket.LocketCommentModel comment = 
                        com.mycompany.tutorhub_enterprise.server.services.LocketService.createComment(postId, this.userId, content);
                    
                    if (comment != null) {
                        sendPacket(new Packet(AuthProtocol.LOCKET_COMMENT_CREATE_SUCCESS, new com.google.gson.Gson().toJson(comment)));
                    } else {
                        sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Failed to create comment"));
                    }
                    break;
                }
                case AuthProtocol.LOCKET_COMMENT_DELETE: {
                    long commentId = payload.get("commentId").getAsLong();
                    boolean success = com.mycompany.tutorhub_enterprise.server.services.LocketService.deleteComment(commentId, this.userId);
                    if (success) {
                        sendPacket(new Packet(AuthProtocol.LOCKET_COMMENT_DELETE_SUCCESS, String.valueOf(commentId)));
                    } else {
                        sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Failed to delete comment"));
                    }
                    break;
                }
                default:
                    System.err.println("Unsupported LOCKET action: " + packet.action);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[LOCKET ERROR] Failed to handle request: " + e.getMessage());
            e.printStackTrace();
            sendPacket(new Packet(AuthProtocol.LOCKET_ERROR, "Internal server error: " + e.getMessage()));
        }
    }
}
