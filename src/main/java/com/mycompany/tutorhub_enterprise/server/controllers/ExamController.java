package com.mycompany.tutorhub_enterprise.server.controllers;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.server.ClientHandler;
import com.mycompany.tutorhub_enterprise.server.services.ExamService;

public final class ExamController {
    private ExamController() {}

    private static boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }

    private static boolean isPracticeTeacherOrAdmin(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase(java.util.Locale.ROOT);
        return "ADMIN".equals(normalized)
                || "TUTOR".equals(normalized)
                || "TEACHER".equals(normalized)
                || "INSTRUCTOR".equals(normalized)
                || "GIA_SU".equals(normalized)
                || "GIASU".equals(normalized);
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Integer.parseInt(((String) value).trim());
        }
        return null;
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private static Integer selectedOptionIdFromPayload(java.util.Map<String, Object> payload) {
        Integer selectedOptionId = asInteger(payload.get("selectedOptionId"));
        if (selectedOptionId != null) {
            return selectedOptionId;
        }
        Object optionIds = payload.get("selectedOptionIds");
        if (optionIds instanceof java.util.List<?>) {
            java.util.List<?> list = (java.util.List<?>) optionIds;
            if (!list.isEmpty()) {
                return asInteger(list.get(0));
            }
        }
        return null;
    }



    public static boolean handlePacket(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (packet.action == null) return false;
        
        switch (packet.action) {
            case "GET_EXAMS":
                handleGetExams(client, packet, userId, gson);
                return true;
            case "GET_EXAM_QUESTIONS":
                handleGetExamQuestions(client, packet, userId, gson);
                return true;
            case "CREATE_EXAM":
                handleCreateExam(client, packet, userId, gson);
                return true;
            case "EXAM_PAPER_CREATE":
                handleExamPaperCreate(client, packet, userId, gson);
                return true;
            case "EXAM_PAPER_LIST":
                handleExamPaperList(client, packet, userId, gson);
                return true;
            case "EXAM_PAPER_ADD_QUESTION":
                handleExamPaperAddQuestion(client, packet, userId, gson);
                return true;
            case "EXAM_PAPER_LIST_QUESTIONS":
                handleExamPaperListQuestions(client, packet, userId, gson);
                return true;
            case "EXAM_PAPER_REMOVE_QUESTION":
                handleExamPaperRemoveQuestion(client, packet, userId, gson);
                return true;
            case "EXAM_ASSIGN_PAPER":
                handleExamAssignPaper(client, packet, userId, gson);
                return true;
            case "EXAM_UNASSIGN_PAPER":
                handleExamUnassignPaper(client, packet, userId, gson);
                return true;
            case "EXAM_GET_ASSIGNED_PAPER":
                handleExamGetAssignedPaper(client, packet, userId, gson);
                return true;
            case "EXAM_PACKAGE_PREVIEW_BY_EXAM":
                handleExamPackagePreviewByExam(client, packet, userId, gson);
                return true;
            case "EXAM_PACKAGE_PREVIEW_BY_PAPER":
                handleExamPackagePreviewByPaper(client, packet, userId, gson);
                return true;
            case "EXAM_START_REQUEST":
                handleExamStartRequest(client, packet, userId, gson);
                return true;
            case "EXAM_START_REQUEST_V2":
                handleExamStartRequestV2(client, packet, userId, gson);
                return true;
            case "EXAM_SUBMIT":
                handleExamSubmit(client, packet, userId, gson);
                return true;
            case "IMPORT_HTML_QUIZ":
                handleImportHtmlQuiz(client, packet, userId, gson);
                return true;

            default:
                return false;
        }
    }

    private static void handleGetExams(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId > 0) {
            client.sendPacket(ExamService.handleGetExams(userId));
        } else {
            client.sendPacket(new Packet(false, "Vui lòng đăng nhập!"));
        }
    }

    private static void handleGetExamQuestions(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId > 0) {
            client.sendPacket(ExamService.handleGetExamQuestions(packet.payload));
        }
    }

    private static void handleCreateExam(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId > 0) {
            client.sendPacket(ExamService.handleCreateExam(userId, packet.payload));
        } else {
            client.sendPacket(new Packet(false, "Vui lòng đăng nhập!"));
        }
    }

    private static void handleExamPaperCreate(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        java.util.Map<String, Object> epCreateReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (epCreateReq == null) { client.sendPacket(new Packet(false, "Payload không hợp lệ")); return; }
        String epTitle = (String) epCreateReq.get("title");
        String epDesc = epCreateReq.containsKey("description") ? (String) epCreateReq.get("description") : "";
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPaperService.handleCreateExamPaper(userId, epTitle, epDesc));
    }

    private static void handleExamPaperList(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        com.mycompany.tutorhub_enterprise.models.Packet epListResult = 
            com.mycompany.tutorhub_enterprise.server.services.ExamPaperService.handleListExamPapers(userId);
        epListResult.action = "EXAM_PAPER_LIST_SUCCESS";
        client.sendPacket(epListResult);
    }

    private static void handleExamPaperAddQuestion(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        java.util.Map<String, Object> epAQReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (epAQReq == null) { client.sendPacket(new Packet(false, "Payload không hợp lệ")); return; }
        int epAQPaperId = ((Number) epAQReq.get("paperId")).intValue();
        int epAQQuestionId = ((Number) epAQReq.get("questionId")).intValue();
        float epAQScore = epAQReq.containsKey("score") ? ((Number) epAQReq.get("score")).floatValue() : 1.0f;
        int epAQOrder = epAQReq.containsKey("orderIndex") ? ((Number) epAQReq.get("orderIndex")).intValue() : 0;
        boolean epAQRequired = epAQReq.containsKey("required") && Boolean.TRUE.equals(epAQReq.get("required"));
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPaperService.handleAddQuestionToPaper(userId, epAQPaperId, epAQQuestionId, epAQScore, epAQOrder, epAQRequired));
    }

    private static void handleExamPaperListQuestions(ClientHandler client, Packet packet, int userId, Gson gson) {
        try {
            int epLQPaperId = Integer.parseInt(packet.payload.trim());
            client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPaperService.handleListQuestionsInPaper(epLQPaperId));
        } catch (NumberFormatException e) {
            client.sendPacket(new Packet(false, "paperId không hợp lệ"));
        }
    }

    private static void handleExamPaperRemoveQuestion(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        java.util.Map<String, Object> epRQReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (epRQReq == null) { client.sendPacket(new Packet(false, "Payload không hợp lệ")); return; }
        int epRQPaperId = ((Number) epRQReq.get("paperId")).intValue();
        int epRQQuestionId = ((Number) epRQReq.get("questionId")).intValue();
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPaperService.handleRemoveQuestionFromPaper(userId, epRQPaperId, epRQQuestionId));
    }

    private static void handleExamAssignPaper(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        java.util.Map<String, Object> eapReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (eapReq == null) { client.sendPacket(new Packet(false, "Payload không hợp lệ")); return; }
        int eapExamId = ((Number) eapReq.get("examId")).intValue();
        int eapPaperId = ((Number) eapReq.get("paperId")).intValue();
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamAssignmentService.handleAssignPaperToExam(userId, eapExamId, eapPaperId));
    }

    private static void handleExamUnassignPaper(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        try {
            int eunExamId = Integer.parseInt(packet.payload.trim());
            client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamAssignmentService.handleUnassignPaperFromExam(userId, eunExamId));
        } catch (NumberFormatException e) {
            client.sendPacket(new Packet(false, "examId không hợp lệ"));
        }
    }

    private static void handleExamGetAssignedPaper(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) { client.sendPacket(new Packet(false, "Vui lòng đăng nhập")); return; }
        try {
            int egapExamId = Integer.parseInt(packet.payload.trim());
            client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamAssignmentService.handleGetAssignedPaper(userId, egapExamId));
        } catch (NumberFormatException e) {
            client.sendPacket(new Packet(false, "examId không hợp lệ"));
        }
    }

    private static void handleExamPackagePreviewByExam(ClientHandler client, Packet packet, int userId, Gson gson) {
        java.util.Map<String, Object> eppExamReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (eppExamReq == null || !eppExamReq.containsKey("examId")) {
            Packet failRes = new Packet(false, "Payload không hợp lệ"); failRes.action = "EXAM_PACKAGE_PREVIEW_FAILED"; client.sendPacket(failRes); return;
        }
        int eppExamId = ((Number) eppExamReq.get("examId")).intValue();
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPackagePreviewService.handlePreviewByExamId(eppExamId));
    }

    private static void handleExamPackagePreviewByPaper(ClientHandler client, Packet packet, int userId, Gson gson) {
        java.util.Map<String, Object> eppPaperReq = gson.fromJson(packet.payload, java.util.Map.class);
        if (eppPaperReq == null || !eppPaperReq.containsKey("paperId")) {
            Packet failRes = new Packet(false, "Payload không hợp lệ"); failRes.action = "EXAM_PACKAGE_PREVIEW_FAILED"; client.sendPacket(failRes); return;
        }
        int eppPaperId = ((Number) eppPaperReq.get("paperId")).intValue();
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamPackagePreviewService.handlePreviewByPaperId(eppPaperId));
    }

    private static void handleExamStartRequest(ClientHandler client, Packet packet, int userId, Gson gson) {
        System.out.println("[EXAM] EXAM_START_REQUEST userId=" + userId);
        
        if (userId <= 0) {
            Packet res = new Packet();
            res.action = "EXAM_START_RESPONSE";
            res.success = false;
            res.message = "Vui lòng đăng nhập trước khi bắt đầu bài thi";
            client.sendPacket(res);
            return;
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
            client.sendPacket(res);
            return;
        }
        
        String[] parts = rawString.split("\\|", -1);
        if (parts.length >= 1) {
            try {
                int examId = Integer.parseInt(parts[0]);
                String password = parts.length > 1 ? parts[1] : "";
                
                System.out.println("[EXAM] EXAM_START_REQUEST examId=" + examId);
                
                com.mycompany.tutorhub_enterprise.models.exam.Exam exam = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getExamById(examId);
                
                if (exam == null) {
                    Packet res = new Packet();
                    res.action = "EXAM_START_RESPONSE";
                    res.success = false;
                    res.message = "Kỳ thi không tồn tại";
                    client.sendPacket(res);
                } else if (!"ACTIVE".equals(exam.status)) {
                    Packet res = new Packet();
                    res.action = "EXAM_START_RESPONSE";
                    res.success = false;
                    res.message = "Kỳ thi chưa mở";
                    client.sendPacket(res);
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
                            System.err.println("[EXAM] Lỗi parse securityConfig examId=" + exam.id);
                        }
                    }
                    
                    if (requiresPassword && !password.equals(expectedPassword)) {
                        Packet res = new Packet();
                        res.action = "EXAM_START_RESPONSE";
                        res.success = false;
                        res.message = "Sai mật khẩu kỳ thi";
                        client.sendPacket(res);
                    } else {
                        // Tạo session
                        com.mycompany.tutorhub_enterprise.models.exam.ExamSession examSession = new com.mycompany.tutorhub_enterprise.models.exam.ExamSession();
                        examSession.examId = exam.id;
                        examSession.userId = userId;
                        examSession.status = "IN_PROGRESS";
                        examSession.clientInfo = "{}";
                        examSession.questionOrder = "[]";
                        
                        int sessionId = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.createSession(examSession);
                        
                        if (sessionId > 0) {
                            // HF-5: Ưu tiên load câu hỏi từ paper (V2 source), fallback V1
                            java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Question> questions = new java.util.ArrayList<>();
                            boolean fromPaper = false;
                            Integer assignedPaperId = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getAssignedPaperId(exam.id);
                            if (assignedPaperId != null) {
                                java.util.List<com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion> paperQs = 
                                    com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO.listQuestionsByPaper(assignedPaperId);
                                if (paperQs != null && !paperQs.isEmpty()) {
                                    for (com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion epq : paperQs) {
                                        com.mycompany.tutorhub_enterprise.models.exam.Question q = 
                                            com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO.getQuestionById(epq.questionId);
                                        if (q != null) questions.add(q);
                                    }
                                    fromPaper = true;
                                }
                            }
                            if (questions.isEmpty()) {
                                // Fallback V1: questions linked directly to exam
                                questions = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getQuestionsByExam(exam.id);
                            }
                            System.out.println("[EXAM] EXAM_START_REQUEST examId=" + examId + " sessionId=" + sessionId + " questionCount=" + questions.size() + " fromPaper=" + fromPaper);
                            
                            String finalHtml = com.mycompany.tutorhub_enterprise.server.services.ExamHtmlTemplateRenderer.renderExam(sessionId, exam.id, exam.title, questions);
                            if (finalHtml == null) {
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
                                for (int qi = 0; qi < questions.size(); qi++) {
                                    com.mycompany.tutorhub_enterprise.models.exam.Question q = questions.get(qi);
                                    html.append("<div class='question' data-qid='").append(q.id).append("' data-qtype='").append(q.questionType).append("'>");
                                    
                                    // Extract question text
                                    String qText = "Câu hỏi " + (qi+1) + ": (Nội dung trống)";
                                    if (q.content != null && !q.content.trim().isEmpty()) {
                                        try {
                                            if (q.content.trim().startsWith("{")) {
                                                java.util.Map<String, Object> contentMap = gson.fromJson(q.content, java.util.Map.class);
                                                if (contentMap.containsKey("text")) {
                                                    qText = "Câu hỏi " + (qi+1) + ": " + contentMap.get("text");
                                                } else {
                                                    qText = "Câu hỏi " + (qi+1) + ": " + q.content;
                                                }
                                            } else {
                                                qText = "Câu hỏi " + (qi+1) + ": " + q.content;
                                            }
                                        } catch (Exception ex) {
                                            qText = "Câu hỏi " + (qi+1) + ": " + q.content;
                                        }
                                    }
                                    html.append("<h3>").append(qText).append("</h3>");
                                    
                                    // HF-2: Render options từ question_options table (chuẩn), không lấy từ embedded JSON
                                    if ("MCQ".equals(q.questionType) || "SINGLE_CHOICE".equals(q.questionType) || "TRUE_FALSE".equals(q.questionType)) {
                                        java.util.List<com.mycompany.tutorhub_enterprise.models.exam.QuestionOption> dbOptions = 
                                            com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO.getOptionsByQuestionId(q.id);
                                        if (dbOptions != null && !dbOptions.isEmpty()) {
                                            html.append("<div class='options'>");
                                            for (com.mycompany.tutorhub_enterprise.models.exam.QuestionOption opt : dbOptions) {
                                                String labelText = (opt.optionLabel != null ? opt.optionLabel + ". " : "") + (opt.content != null ? opt.content : "");
                                                String optValue = String.valueOf(opt.id); // Use optionId as value for grading
                                                html.append("<div class='option'><input type='radio' name='q_").append(q.id)
                                                    .append("' id='q_").append(q.id).append("_").append(opt.id).append("' value='").append(optValue).append("'>");
                                                html.append("<label for='q_").append(q.id).append("_").append(opt.id).append("'>").append(labelText).append("</label></div>");
                                            }
                                            html.append("</div>");
                                        } else {
                                            // Fallback: try embedded JSON options
                                            if (q.content != null && q.content.trim().startsWith("{")) {
                                                try {
                                                    java.util.Map<String, Object> cMap = gson.fromJson(q.content, java.util.Map.class);
                                                    if (cMap.containsKey("options") && cMap.get("options") instanceof java.util.List) {
                                                        java.util.List<String> embeddedOpts = (java.util.List<String>) cMap.get("options");
                                                        html.append("<div class='options'>");
                                                        for (int oi = 0; oi < embeddedOpts.size(); oi++) {
                                                            String optVal = embeddedOpts.get(oi);
                                                            html.append("<div class='option'><input type='radio' name='q_").append(q.id)
                                                                .append("' id='q_").append(q.id).append("_").append(oi).append("' value='").append(optVal.replace("'", "&apos;")).append("'>");
                                                            html.append("<label for='q_").append(q.id).append("_").append(oi).append("'>").append(optVal).append("</label></div>");
                                                        }
                                                        html.append("</div>");
                                                    }
                                                } catch (Exception ex) {
                                                    System.err.println("[EXAM] Error parsing embedded options questionId=" + q.id);
                                                }
                                            } else {
                                                html.append("<p><em>(Câu hỏi này chưa có lựa chọn)</em></p>");
                                            }
                                        }
                                    } else if ("ESSAY".equals(q.questionType)) {
                                        html.append("<textarea rows='4' style='width:100%;' placeholder='Nhập câu trả lời...' id='q_").append(q.id).append("'></textarea>");
                                    } else {
                                        html.append("<input type='text' style='width:100%;' placeholder='Nhập câu trả lời...' id='q_").append(q.id).append("'>");
                                    }
                                    
                                    html.append("</div>");
                                }
                            }
                            html.append("</body></html>");
                            finalHtml = html.toString();
                            }
                            
                            java.util.Map<String, Object> data = new java.util.HashMap<>();
                            data.put("sessionId", String.valueOf(sessionId));
                            data.put("examId", exam.id);
                            data.put("examTitle", exam.title);
                            data.put("durationMinutes", exam.durationMins);
                            data.put("htmlContent", finalHtml);
                            
                            Packet res = new Packet(true, "Bắt đầu thi thành công", data);
                            res.action = "EXAM_START_RESPONSE";
                            client.sendPacket(res);
                        } else {
                            Packet res = new Packet();
                            res.action = "EXAM_START_RESPONSE";
                            res.success = false;
                            res.message = "Không thể tạo phiên thi";
                            client.sendPacket(res);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                Packet res = new Packet();
                res.action = "EXAM_START_RESPONSE";
                res.success = false;
                res.message = "examId không hợp lệ";
                client.sendPacket(res);
            }
        } else {
            Packet res = new Packet();
            res.action = "EXAM_START_RESPONSE";
            res.success = false;
            res.message = "Payload không hợp lệ";
            client.sendPacket(res);
        }
    }

    private static void handleExamStartRequestV2(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) {
            Packet res = new Packet(); res.action = "EXAM_START_RESPONSE_V2"; res.success = false; res.message = "Vui lòng đăng nhập"; client.sendPacket(res); return;
        }
        // HF-4: Bật flag V2 cho debug mode (không phải production)
        System.setProperty("tse.paperStartV2.enabled", "true");
        String esv2Role = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getUserRole(userId);
        client.sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamStartV2Service.handleStartRequestV2(userId, esv2Role, packet.payload));
    }

    private static void handleExamSubmit(ClientHandler client, Packet packet, int userId, Gson gson) {
        System.out.println("[TSE_DB] EXAM_SUBMIT action=" + packet.action);
        
        if (userId <= 0) {
            Packet res = new Packet();
            res.action = "EXAM_SUBMIT_ACK";
            res.success = false;
            res.message = "Vui lòng đăng nhập trước khi nộp bài";
            client.sendPacket(res);
            return;
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
            client.sendPacket(res);
            return;
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
                    client.sendPacket(res);
                } else if (session.userId != userId) {
                    Packet res = new Packet();
                    res.action = "EXAM_SUBMIT_ACK";
                    res.success = false;
                    res.message = "Bạn không có quyền nộp bài thi này";
                    client.sendPacket(res);
                } else if ("SUBMITTED".equals(session.status)) {
                    Packet res = new Packet();
                    res.action = "EXAM_SUBMIT_ACK";
                    res.success = false;
                    res.message = "Bạn đã nộp bài trước đó";
                    client.sendPacket(res);
                } else if (!"IN_PROGRESS".equals(session.status)) {
                    Packet res = new Packet();
                    res.action = "EXAM_SUBMIT_ACK";
                    res.success = false;
                    res.message = "Trạng thái phiên thi không hợp lệ: " + session.status;
                    client.sendPacket(res);
                } else {
                    System.out.println("[EXAM_SUBMIT] examId=" + session.examId);
                    System.out.println("[EXAM_SUBMIT] sessionIdPresent=true");
                    
                    try {
                        java.util.Map<String, Object> payloadMap = gson.fromJson(submitPayload, java.util.Map.class);
                        if (payloadMap == null || !payloadMap.containsKey("answers")) {
                            throw new Exception("Missing answers");
                        }
                        
                        java.util.List<java.util.Map<String, Object>> answersList = (java.util.List<java.util.Map<String, Object>>) payloadMap.get("answers");
                        System.out.println("[EXAM_SUBMIT] parsedAnswersCount=" + answersList.size());
                        
                        java.util.Set<Integer> validQuestionIds = new java.util.HashSet<>();
                        
                        // V1 questions
                        java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Question> examQuestions = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getQuestionsByExam(session.examId);
                        for (com.mycompany.tutorhub_enterprise.models.exam.Question q : examQuestions) {
                            validQuestionIds.add(q.id);
                        }
                        
                        // V2 questions
                        Integer paperId = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.getAssignedPaperId(session.examId);
                        if (paperId != null) {
                            java.util.List<com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion> paperQuestions = com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO.listQuestionsByPaper(paperId);
                            for (com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion pq : paperQuestions) {
                                validQuestionIds.add(pq.questionId);
                            }
                        }
                        
                        int savedCount = 0;
                        for (java.util.Map<String, Object> ans : answersList) {
                            if (ans != null && ans.containsKey("questionId")) {
                                int qId = ((Number) ans.get("questionId")).intValue();
                                if (validQuestionIds.contains(qId)) {
                                    String ansJson = gson.toJson(ans);
                                    boolean saved = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.saveAnswer(sessionId, qId, ansJson);
                                    if (saved) {
                                        savedCount++;
                                    } else {
                                        System.err.println("[EXAM_SUBMIT] saveAnswer failed questionId=" + qId + ", reason=DB Update Failed");
                                    }
                                } else {
                                    System.err.println("[EXAM_SUBMIT] saveAnswer failed questionId=" + qId + ", reason=Not in validQuestionIds");
                                }
                            }
                        }
                        System.out.println("[EXAM_SUBMIT] savedAnswersCount=" + savedCount);
                        System.out.println("[EXAM_SUBMIT] errorCode=null");
                        System.out.println("[EXAM_SUBMIT] result=success");
                        
                        boolean updated = com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.updateSessionStatus(sessionId, "SUBMITTED");
                        
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("sessionId", String.valueOf(sessionId));
                        data.put("examId", session.examId);
                        data.put("submitStatus", "RECEIVED");
                        data.put("savedAnswerCount", savedCount);
                        
                        Packet res = new Packet(updated, updated ? "Nộp bài thành công" : "Lỗi cập nhật DB", data);
                        res.action = "EXAM_SUBMIT_ACK";
                        client.sendPacket(res);
                        
                    } catch (Exception ex) {
                        System.err.println("[TSE_DB] Error parsing submit payload: " + ex.getMessage());
                        Packet res = new Packet();
                        res.action = "EXAM_SUBMIT_ACK";
                        res.success = false;
                        res.message = "Dữ liệu bài làm không hợp lệ";
                        client.sendPacket(res);
                    }
                }
            } catch (NumberFormatException e) {
                Packet res = new Packet();
                res.action = "EXAM_SUBMIT_ACK";
                res.success = false;
                res.message = "sessionId không hợp lệ";
                client.sendPacket(res);
            }
        } else {
            Packet res = new Packet();
            res.action = "EXAM_SUBMIT_ACK";
            res.success = false;
            res.message = "Payload không hợp lệ";
            client.sendPacket(res);
        }
    }

    private static void handleImportHtmlQuiz(ClientHandler client, Packet packet, int userId, Gson gson) {
        if (userId <= 0) {
            Packet res = new Packet(false, "Vui lòng đăng nhập");
            res.action = "IMPORT_HTML_QUIZ_FAILED";
            client.sendPacket(res);
            return;
        }

        com.mycompany.tutorhub_enterprise.server.services.HtmlQuizImportService service = 
                new com.mycompany.tutorhub_enterprise.server.services.HtmlQuizImportService();
        
        com.mycompany.tutorhub_enterprise.server.services.HtmlQuizImportService.ImportResult result = 
                service.processImport(packet.payload, userId);

        Packet res = new Packet();
        if (result.success) {
            res.action = "IMPORT_HTML_QUIZ_SUCCESS";
            res.success = true;
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("bankId", result.bankId);
            data.put("questionCount", result.questionCount);
            if (result.paperId > 0) {
                data.put("paperId", result.paperId);
                data.put("paperQuestionCount", result.questionCount);
            }
            res.data = data;
        } else {
            res.action = "IMPORT_HTML_QUIZ_FAILED";
            res.success = false;
            res.message = result.errorMessage;
        }
        client.sendPacket(res);
    }


}
