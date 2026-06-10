package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * NetworkTSEExamService - Triển khai TSEExamService kết nối tới NetworkManager thật.
 * Gửi nhận dữ liệu bằng các packet theo chuẩn của TutorHub Core.
 */
public class NetworkTSEExamService implements TSEExamService {

    @Override
    public CompletableFuture<TSELoginResult> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuthRequest authRequest = AuthRequest.login(username, password);
                Packet reqPacket = new Packet(AuthProtocol.LOGIN, authRequest);
                
                // Gửi request
                NetworkManager.getInstance().sendPacket(reqPacket);

                // Đợi phản hồi với timeout 10000ms
                Packet resPacket = NetworkManager.getInstance().receivePacket(
                        AuthProtocol.RESPONSE, authRequest.getRequestId(), 10000);

                if (resPacket != null && resPacket.data instanceof AuthResponse) {
                    AuthResponse authRes = (AuthResponse) resPacket.data;
                    if (authRes.isSuccess()) {
                        TSEExamContext ctx = new TSEExamContext();
                        ctx.username = username;
                        ctx.userId = 1; // Need to extract from response
                        ctx.token = "token_" + UUID.randomUUID().toString();
                        return new TSELoginResult(true, "Login success", ctx);
                    } else {
                        return new TSELoginResult(false, authRes.getMessage(), null);
                    }
                }
                
                return new TSELoginResult(false, "Phản hồi từ server không hợp lệ.", null);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new TSELoginResult(false, "Lỗi kết nối hoặc quá hạn: " + ex.getMessage(), null);
            }
        });
    }

    @Override
    public CompletableFuture<List<TSEExamConfig>> getConfigList(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Action dự kiến "TSE_GET_CONFIG_LIST"
                String reqId = UUID.randomUUID().toString();
                Packet reqPacket = new Packet("TSE_GET_CONFIG_LIST", reqId);
                System.out.println("[TSE_NETWORK] Gửi request: action=TSE_GET_CONFIG_LIST, requestId=" + reqId);
                
                NetworkManager.getInstance().sendPacket(reqPacket);
                
                System.out.println("[TSE_NETWORK] Đang chờ response action=TSE_GET_CONFIG_LIST_RESPONSE...");
                Packet resPacket = NetworkManager.getInstance().receivePacket(
                        "TSE_GET_CONFIG_LIST_RESPONSE", null, 5000);
                
                if (resPacket != null) {
                    System.out.println("[TSE_NETWORK] Nhận response: action=" + resPacket.action + ", dataType=" + (resPacket.data != null ? resPacket.data.getClass().getName() : "null"));
                }
                        
                // Xử lý dữ liệu trả về nếu server support
                if (resPacket != null && resPacket.data instanceof List) {
                    List<TSEExamConfig> configs = new ArrayList<>();
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<?> rawList = (List<?>) resPacket.data;
                    for (Object obj : rawList) {
                        try {
                            String json = gson.toJson(obj);
                            TSEExamConfig config = gson.fromJson(json, TSEExamConfig.class);
                            configs.add(config);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return configs;
                }
            } catch (Exception ex) {
                System.err.println("[TSE_NETWORK] Lỗi lấy Config List: " + ex.getMessage());
            }
            // Fallback khi chưa có server API hoặc timeout -> không return dummy ở skeleton
            throw new RuntimeException("TSE_GET_CONFIG_LIST not implemented on server yet");
        });
    }

    @Override
    public CompletableFuture<TSEStartExamResult> verifyPasswordAndStart(int userId, int examId, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Action dự kiến "EXAM_START_REQUEST"
                String reqId = UUID.randomUUID().toString();
                String payload = examId + "|" + (password == null ? "" : password);
                Packet reqPacket = new Packet("EXAM_START_REQUEST", payload);
                reqPacket.data = payload; // Để cả payload và data cho server parse
                
                System.out.println("[TSE_NETWORK] Starting exam: userId=" + userId + ", examId=" + examId + ", passwordLength=" + (password == null ? 0 : password.length()));
                System.out.println("[TSE_NETWORK] EXAM_START_REQUEST data=" + reqPacket.data);
                System.out.println("[TSE_NETWORK] EXAM_START_REQUEST payload=" + reqPacket.payload);
                System.out.println("[TSE_NETWORK] Gửi request: action=EXAM_START_REQUEST, reqId=" + reqId);
                NetworkManager.getInstance().sendPacket(reqPacket);
                
                System.out.println("[TSE_NETWORK] Đang chờ response action=EXAM_START_RESPONSE...");
                Packet resPacket = NetworkManager.getInstance().receivePacket(
                        "EXAM_START_RESPONSE", null, 10000);
                        
                if (resPacket != null) {
                    System.out.println("[TSE_NETWORK] Nhận response: action=" + resPacket.action + ", success=" + resPacket.success);
                }
                
                if (resPacket != null && resPacket.success) {
                    TSEStartExamResult res = new TSEStartExamResult(true, "Bắt đầu thi thành công");
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String jsonString = gson.toJson(resPacket.data);
                        String preview = jsonString.length() > 200 ? jsonString.substring(0, 200) + "..." : jsonString;
                        System.out.println("[TSE_NETWORK] Debug jsonString: " + preview);
                        java.util.Map<String, Object> map = gson.fromJson(jsonString, new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType());
                        if (map != null) {
                            if (map.containsKey("sessionId")) {
                                res.sessionId = String.valueOf(map.get("sessionId"));
                            }
                            if (map.containsKey("htmlContent")) {
                                res.htmlContent = (String) map.get("htmlContent");
                            }
                            if (map.containsKey("questionCount")) {
                                res.questionCount = ((Double) map.get("questionCount")).intValue();
                            } else {
                                // Default to 1 for dummy mocks unless explicitly 0
                                res.questionCount = 1;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[TSE_NETWORK] Lỗi parse data từ EXAM_START_RESPONSE: " + e.getMessage());
                    }
                    
                    if (res.sessionId == null || res.sessionId.isEmpty()) {
                        res.sessionId = "sess-" + java.util.UUID.randomUUID().toString();
                    }
                    if (res.htmlContent == null || res.htmlContent.isEmpty()) {
                        res.htmlContent = "<html><body><h1>Network Exam Started</h1></body></html>";
                    }
                    return res;
                } else {
                    return new TSEStartExamResult(false, resPacket != null && resPacket.message != null ? resPacket.message : "Mật khẩu không hợp lệ hoặc lỗi kỳ thi.");
                }
            } catch (Exception ex) {
                return new TSEStartExamResult(false, "Lỗi kết nối: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<TSESubmitResult> submitExam(String sessionId, int examId, String submitPayload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Action dự kiến "EXAM_SUBMIT"
                String reqId = UUID.randomUUID().toString();
                String serverPayload = sessionId + "|" + submitPayload;
                Packet reqPacket = new Packet("EXAM_SUBMIT", serverPayload);
                System.out.println("[TSE_NETWORK] Gửi request: action=EXAM_SUBMIT, reqId=" + reqId);
                
                NetworkManager.getInstance().sendPacket(reqPacket);
                
                System.out.println("[TSE_NETWORK] Đang chờ response action=EXAM_SUBMIT_ACK...");
                Packet resPacket = NetworkManager.getInstance().receivePacket(
                        "EXAM_SUBMIT_ACK", null, 10000);
                        
                if (resPacket != null) {
                    System.out.println("[TSE_NETWORK] Nhận response: action=" + resPacket.action + ", success=" + resPacket.success);
                }
                
                if (resPacket != null && resPacket.success) {
                    TSESubmitResult res = new TSESubmitResult(true, "Đã nộp bài thành công trên Server.");
                    res.submitStatus = "RECEIVED";
                    return res;
                } else {
                    return new TSESubmitResult(false, "Nộp bài thất bại trên Server.");
                }
            } catch (Exception ex) {
                return new TSESubmitResult(false, "Lỗi nộp bài: " + ex.getMessage());
            }
        });
    }
}
