package com.mycompany.tutorhub_enterprise.client.home;

import com.github.sarxos.webcam.Webcam;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.utils.B2Helper;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocketWebPopupDialog extends JDialog {

    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static LocketWebPopupDialog activeInstance;

    private final JFXPanel fxPanel = new JFXPanel();
    private final List<HomeLocketItem> locketItems;
    private final int initialIndex;
    private final Runnable refreshCallback;
    private final Runnable messageCallback;
    private final LocketBridge bridge = new LocketBridge();
    private final Object cameraLock = new Object();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LocketPopupWorker");
        thread.setDaemon(true);
        return thread;
    });

    private WebEngine webEngine;
    private Webcam webcam;
    private Thread cameraThread;
    private volatile boolean cameraRunning;
    private byte[] selectedImageBytes;
    private String selectedImageExtension = "jpg";
    private String selectedImageName = "locket.jpg";

    public LocketWebPopupDialog(Frame parent) {
        this(parent, new ArrayList<>(), 0, null, null);
    }

    public LocketWebPopupDialog(
            Frame parent,
            List<HomeLocketItem> items,
            int startIndex,
            Runnable refreshCallback,
            Runnable messageCallback
    ) {
        super(parent, "TutorHub Locket", true);
        this.locketItems = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.initialIndex = Math.max(0, startIndex);
        this.refreshCallback = refreshCallback;
        this.messageCallback = messageCallback;

        setMinimumSize(new Dimension(880, 600));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 200)); // Dark overlay
        ((javax.swing.JComponent) getContentPane()).setOpaque(false);
        setLayout(new BorderLayout());
        
        fxPanel.setOpaque(false);
        add(fxPanel, BorderLayout.CENTER);

        if (parent != null) {
            setSize(parent.getSize());
            setLocation(parent.getLocation());
        } else {
            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            setSize(screenSize.width, screenSize.height);
            setLocationRelativeTo(null);
        }

        activeInstance = this;
        Platform.setImplicitExit(false);
        Platform.runLater(this::initFX);
    }

    public static void closeActiveInstance() {
        LocketWebPopupDialog instance = activeInstance;
        if (instance != null) {
            instance.disposeDialog();
        }
    }

    public static void handleUploadError(String msg) {
        LocketWebPopupDialog instance = activeInstance;
        if (instance != null) {
            instance.notifyUploadError(msg == null ? "Khong dang duoc anh." : msg);
        }
    }

    @Override
    public void dispose() {
        cleanupCamera();
        worker.shutdownNow();
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.dispose();
    }

    private void initFX() {
        try {
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webEngine = webView.getEngine();

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("bridge", bridge);
                        webEngine.executeScript("if (window.TutorHubLocketBridgeReady) { window.TutorHubLocketBridgeReady(); }");
                        injectAssets();
                        pushStateToWebView();
                        System.out.println("[LOCKET_POPUP] Web UI loaded");
                    } catch (Exception e) {
                        System.err.println("[LOCKET_POPUP] Bridge setup failed: " + e.getMessage());
                    }
                } else if (newState == Worker.State.FAILED) {
                    Throwable error = webEngine.getLoadWorker().getException();
                    System.err.println("[LOCKET_POPUP] Web UI load failed: " + (error == null ? "unknown" : error.getMessage()));
                }
            });

            URL url = getClass().getResource("/locket-web/locket-popup.html");
            if (url == null) {
                System.err.println("[LOCKET_POPUP] Missing /locket-web/locket-popup.html");
                return;
            }
            Scene scene = new Scene(webView);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            fxPanel.setScene(scene);
            webView.setStyle("-fx-background-color: transparent;");
            webView.setPageFill(javafx.scene.paint.Color.TRANSPARENT);
            webEngine.load(url.toExternalForm());
        } catch (Exception e) {
            System.err.println("[LOCKET_POPUP] initFX failed: " + e.getMessage());
        }
    }

    private void pushStateToWebView() {
        if (webEngine == null) {
            return;
        }
        Map<String, Object> state = new HashMap<>();
        state.put("items", locketItems);
        state.put("startIndex", Math.min(initialIndex, Math.max(0, locketItems.size() - 1)));
        state.put("currentUserName", com.mycompany.tutorhub_enterprise.client.MainDashboard.currentStaticUserName);
        state.put("currentUserAvatarBase64", com.mycompany.tutorhub_enterprise.client.MainDashboard.currentStaticUserAvatarBase64);
        executePopupScript("setState", state);
    }

    private void executePopupScript(String method, Object... args) {
        if (webEngine == null) {
            return;
        }
        Platform.runLater(() -> {
            try {
                StringBuilder script = new StringBuilder();
                script.append("window.TutorHubLocketPopup && window.TutorHubLocketPopup.")
                        .append(method)
                        .append("(");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        script.append(",");
                    }
                    script.append(GSON.toJson(args[i]));
                }
                script.append(");");
                webEngine.executeScript(script.toString());
            } catch (Exception e) {
                System.err.println("[LOCKET_POPUP] JS call failed " + method + ": " + e.getMessage());
            }
        });
    }

    private String resourceToDataUrl(String resourcePath, String mimeType) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url == null) {
                System.err.println("[LOCKET_POPUP][ASSET_MISSING] " + resourcePath);
                return "";
            }
            java.io.InputStream is = url.openStream();
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            is.close();
            return "data:" + mimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.err.println("[LOCKET_POPUP][ASSET_MISSING] " + resourcePath + " - " + e.getMessage());
            return "";
        }
    }

    private void injectAssets() {
        if (webEngine == null) return;
        Map<String, String> icons = new HashMap<>();
        icons.put("play", resourceToDataUrl("/images/icon/phat.png", "image/png"));
        icons.put("camera", resourceToDataUrl("/images/icon/camera.svg", "image/svg+xml"));
        icons.put("tuychon", resourceToDataUrl("/images/icon/tuychon.svg", "image/svg+xml"));
        icons.put("message", resourceToDataUrl("/images/icon/message.svg", "image/svg+xml"));
        icons.put("close", resourceToDataUrl("/images/icon/trash.svg", "image/svg+xml"));
        icons.put("plus", resourceToDataUrl("/images/icon/plus1.svg", "image/svg+xml"));
        icons.put("clock", resourceToDataUrl("/images/icon/clock_color.svg", "image/svg+xml"));
        icons.put("camera_empty", resourceToDataUrl("/images/icon/camera_empty.png", "image/png"));

        Map<String, String> reactions = new HashMap<>();
        reactions.put("LIKE", resourceToDataUrl("/images/reactions/like.gif", "image/gif"));
        reactions.put("HEART", resourceToDataUrl("/images/reactions/love.gif", "image/gif"));
        reactions.put("LOVE", resourceToDataUrl("/images/reactions/care.gif", "image/gif"));
        reactions.put("HAHA", resourceToDataUrl("/images/reactions/haha.gif", "image/gif"));
        reactions.put("WOW", resourceToDataUrl("/images/reactions/wow.gif", "image/gif"));
        reactions.put("SAD", resourceToDataUrl("/images/reactions/sad.gif", "image/gif"));
        reactions.put("ANGRY", resourceToDataUrl("/images/reactions/angry.gif", "image/gif"));

        Map<String, Object> assets = new HashMap<>();
        assets.put("icons", icons);
        assets.put("reactions", reactions);

        executePopupScript("setAssets", assets);
    }

    private void disposeDialog() {
        SwingUtilities.invokeLater(() -> {
            cleanupCamera();
            dispose();
        });
    }

    private void openFileChooser() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chon anh Locket");
            chooser.setFileFilter(new FileNameExtensionFilter("Image files (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = chooser.getSelectedFile();
            worker.submit(() -> loadSelectedImage(file));
        });
    }

    private void loadSelectedImage(File file) {
        try {
            validateImageFile(file);
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IllegalArgumentException("File anh khong doc duoc.");
            }
            selectedImageBytes = toJpegBytes(image);
            selectedImageExtension = "jpg";
            selectedImageName = file.getName();
            cleanupCamera();
            executePopupScript("onPickedImage", toDataUrl(selectedImageBytes), selectedImageName);
            System.out.println("[LOCKET_POPUP] Image selected: " + selectedImageName);
        } catch (Exception e) {
            notifyUploadError(e.getMessage());
        }
    }

    private void validateImageFile(File file) {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("File anh khong hop le.");
        }
        if (file.length() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("File qua lon. Vui long chon anh <= 8MB.");
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        boolean allowed = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
        if (!allowed) {
            throw new IllegalArgumentException("Chi ho tro jpg, jpeg, png hoac webp.");
        }
    }

    private byte[] toJpegBytes(BufferedImage image) throws Exception {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(image, 0, 0, Color.WHITE, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", out);
        return out.toByteArray();
    }

    private String toDataUrl(byte[] bytes) {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private void startCamera() {
        if (cameraRunning) {
            return;
        }
        cleanupCamera();
        cameraRunning = true;
        cameraThread = new Thread(this::runCameraPreview, "LocketCameraPreview");
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void runCameraPreview() {
        try {
            synchronized (cameraLock) {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    cameraRunning = false;
                    notifyUploadError("Khong tim thay webcam. Ban van co the chon anh tu may.");
                    return;
                }
                try {
                    webcam.setViewSize(new Dimension(640, 480));
                } catch (Exception ignored) {
                    // Some webcams only support their default resolution.
                }
                webcam.open();
            }

            while (cameraRunning && !Thread.currentThread().isInterrupted()) {
                BufferedImage frame;
                synchronized (cameraLock) {
                    frame = webcam != null && webcam.isOpen() ? webcam.getImage() : null;
                }
                if (frame != null) {
                    byte[] bytes = toJpegBytes(frame);
                    executePopupScript("onCameraFrame", toDataUrl(bytes));
                }
                Thread.sleep(160);
            }
        } catch (Exception e) {
            cameraRunning = false;
            notifyUploadError("Camera gap loi: " + e.getMessage());
        }
    }

    private void captureCameraPhoto() {
        worker.submit(() -> {
            try {
                BufferedImage image;
                synchronized (cameraLock) {
                    image = webcam != null && webcam.isOpen() ? webcam.getImage() : null;
                }
                if (image == null) {
                    throw new IllegalStateException("Khong lay duoc anh tu camera.");
                }
                selectedImageBytes = toJpegBytes(image);
                selectedImageExtension = "jpg";
                selectedImageName = "locket-camera-" + Instant.now().toEpochMilli() + ".jpg";
                cleanupCamera();
                executePopupScript("onCameraCaptured", toDataUrl(selectedImageBytes), selectedImageName);
                System.out.println("[LOCKET_POPUP] Camera photo captured");
            } catch (Exception e) {
                cleanupCamera();
                notifyUploadError("Khong chup duoc anh: " + e.getMessage());
            }
        });
    }

    private void cleanupCamera() {
        cameraRunning = false;
        if (cameraThread != null) {
            cameraThread.interrupt();
            cameraThread = null;
        }
        synchronized (cameraLock) {
            if (webcam != null) {
                try {
                    if (webcam.isOpen()) {
                        webcam.close();
                    }
                } catch (Exception e) {
                    System.err.println("[LOCKET_POPUP] Camera cleanup failed: " + e.getMessage());
                } finally {
                    webcam = null;
                }
            }
        }
    }

    private void submitSelectedPhoto(String caption) {
        if (selectedImageBytes == null || selectedImageBytes.length == 0) {
            notifyUploadError("Hay chon hoac chup anh truoc khi dang.");
            return;
        }
        executePopupScript("setUploading", true, "Dang upload anh...");
        worker.submit(() -> {
            try {
                if (!B2Helper.isConfigured()) {
                    throw new IllegalStateException("Thieu cau hinh B2. Hay cau hinh bien moi truong B2 tren server/client.");
                }
                String base64 = Base64.getEncoder().encodeToString(selectedImageBytes);
                String imageUrl = B2Helper.uploadBase64Image(base64, selectedImageExtension);
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    throw new IllegalStateException("Upload anh that bai.");
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("imageUrl", imageUrl);
                payload.addProperty("thumbnailUrl", imageUrl);
                payload.addProperty("caption", caption == null ? "" : caption.trim());
                payload.addProperty("mediaType", "image");

                NetworkManager.getInstance().sendPacket(new Packet("LOCKET_POST_CREATE", payload.toString()));
                executePopupScript("onPostAccepted");
                System.out.println("[LOCKET_POPUP] LOCKET_POST_CREATE sent without classId");
            } catch (Exception e) {
                notifyUploadError(e.getMessage());
            }
        });
    }

    private void notifyUploadError(String message) {
        String safeMessage = message == null || message.trim().isEmpty()
                ? "Khong thuc hien duoc thao tac Locket."
                : message.trim();
        executePopupScript("onUploadError", safeMessage);
        System.err.println("[LOCKET_POPUP] " + safeMessage);
    }

    private void openMessages() {
        System.out.println("[LOCKET_POPUP] Open messages requested");
        if (messageCallback != null) {
            SwingUtilities.invokeLater(messageCallback);
        }
    }

    private void reactToCurrentPost(String payloadJson) {
        try {
            JsonObject payload = JsonParser.parseString(payloadJson == null ? "{}" : payloadJson).getAsJsonObject();
            if (!payload.has("postId")) {
                return;
            }
            String rawId = payload.get("postId").getAsString();
            long postId = Long.parseLong(rawId);
            JsonObject requestPayload = new JsonObject();
            requestPayload.addProperty("postId", postId);
            requestPayload.addProperty("reactionType", payload.has("reactionType") ? payload.get("reactionType").getAsString() : "HEART");
            NetworkManager.getInstance().sendPacket(new Packet("LOCKET_POST_REACT", requestPayload.toString()));
        } catch (NumberFormatException ignored) {
            System.out.println("[LOCKET_POPUP] Reaction skipped for local/sample post");
        } catch (Exception e) {
            System.err.println("[LOCKET_POPUP] Reaction failed: " + e.getMessage());
        }
    }

    public class LocketBridge {
        public void onEvent(String type, String payloadJson) {
            String eventType = type == null ? "" : type;
            String payload = payloadJson == null ? "{}" : payloadJson;
            System.out.println("[LOCKET_POPUP] Event " + eventType + " payload=" + payload);

            switch (eventType) {
                case "LOCKET_POPUP_READY":
                    pushStateToWebView();
                    break;
                case "LOCKET_PICK_IMAGE":
                    openFileChooser();
                    break;
                case "LOCKET_CAMERA_START":
                    startCamera();
                    break;
                case "LOCKET_CAMERA_CAPTURE":
                    captureCameraPhoto();
                    break;
                case "LOCKET_POST_SUBMIT":
                    String caption = "";
                    try {
                        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                        caption = json.has("caption") ? json.get("caption").getAsString() : "";
                    } catch (Exception ignored) {
                    }
                    submitSelectedPhoto(caption);
                    break;
                case "LOCKET_MESSAGE_OPEN":
                    openMessages();
                    break;
                case "LOCKET_REACTION":
                    reactToCurrentPost(payload);
                    break;
                case "LOCKET_CLOSE":
                    disposeDialog();
                    break;
                case "LOCKET_SLIDESHOW_TOGGLE":
                case "LOCKET_NEXT":
                case "LOCKET_PREV":
                    // UI state is handled in JavaScript; Java only records the event.
                    break;
                default:
                    System.out.println("[LOCKET_POPUP] Unknown event ignored: " + eventType);
                    break;
            }
        }
    }
}
