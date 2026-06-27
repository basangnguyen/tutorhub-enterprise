package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TSEV2ReadOnlyExamPanel extends JPanel {

    @FunctionalInterface
    public interface DraftAutosaveHandler {
        void save(TSEV2ReadOnlyExamRenderModel model, TSEV2AnswerSelectionState state) throws Exception;
    }

    @FunctionalInterface
    public interface DraftRestoreHandler {
        DraftRestoreResult restore(TSEV2ReadOnlyExamRenderModel model, TSEV2AnswerSelectionState state) throws Exception;
    }

    public static final class DraftRestoreResult {
        private final boolean restored;
        private final String statusText;

        private DraftRestoreResult(boolean restored, String statusText) {
            this.restored = restored;
            this.statusText = statusText;
        }

        public static DraftRestoreResult restored() {
            return new DraftRestoreResult(true, "Encrypted draft restored.");
        }

        public static DraftRestoreResult notFound() {
            return new DraftRestoreResult(false, "No encrypted draft found.");
        }

        public boolean isRestored() {
            return restored;
        }

        public String getStatusText() {
            return statusText;
        }
    }

    private static final String[] FORBIDDEN_RENDER_TOKENS = {
            "iscorrect",
            "answerkey",
            "correctoption",
            "sessiontoken",
            "keyb64",
            "plaintext",
            "score",
            "grading",
            "passwordhash",
            "password"
    };

    private final TSEV2ReadOnlyExamRenderModel renderModel;
    private final TSEV2AnswerSelectionState selectionState;
    private final DraftAutosaveHandler draftAutosaveHandler;
    private final DraftRestoreHandler draftRestoreHandler;
    private final Map<Integer, Map<Integer, JRadioButton>> optionButtonsByQuestionId = new LinkedHashMap<>();
    private JLabel progressLabel;
    private JLabel autosaveStatusLabel;

    public TSEV2ReadOnlyExamPanel(TSEV2ReadOnlyExamRenderModel renderModel) {
        this(renderModel, new TSEV2AnswerSelectionState(resolveQuestionCount(renderModel)), null, null);
    }

    public TSEV2ReadOnlyExamPanel(TSEV2ReadOnlyExamRenderModel renderModel, TSEV2AnswerSelectionState selectionState) {
        this(renderModel, selectionState, null, null);
    }

    public TSEV2ReadOnlyExamPanel(
            TSEV2ReadOnlyExamRenderModel renderModel,
            TSEV2AnswerSelectionState selectionState,
            DraftAutosaveHandler draftAutosaveHandler
    ) {
        this(renderModel, selectionState, draftAutosaveHandler, null);
    }

    public TSEV2ReadOnlyExamPanel(
            TSEV2ReadOnlyExamRenderModel renderModel,
            TSEV2AnswerSelectionState selectionState,
            DraftAutosaveHandler draftAutosaveHandler,
            DraftRestoreHandler draftRestoreHandler
    ) {
        this.renderModel = renderModel;
        this.selectionState = selectionState != null
                ? selectionState
                : new TSEV2AnswerSelectionState(resolveQuestionCount(renderModel));
        this.draftAutosaveHandler = draftAutosaveHandler;
        this.draftRestoreHandler = draftRestoreHandler;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        String validationError = validateRenderModel(renderModel);
        if (validationError != null) {
            showSafeError(validationError);
            return;
        }

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 240, 240));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("V2 Package Render - Selection State Prototype");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel metadataPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        metadataPanel.setOpaque(false);
        metadataPanel.add(new JLabel("Exam ID: " + renderModel.getExamId()));
        metadataPanel.add(new JLabel("Paper ID: " + renderModel.getPaperId()));
        metadataPanel.add(new JLabel("Attempt ID: " + renderModel.getAttemptId()));
        metadataPanel.add(new JLabel("Questions: " + renderModel.getQuestionCount()));
        metadataPanel.add(new JLabel("V2 preview"));
        metadataPanel.add(new JLabel("Deadline: " + renderModel.getDeadlineAt()));
        
        headerPanel.add(metadataPanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Body: Questions
        JPanel questionsContainer = new JPanel();
        questionsContainer.setLayout(new BoxLayout(questionsContainer, BoxLayout.Y_AXIS));
        questionsContainer.setBackground(Color.WHITE);
        questionsContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < renderModel.getQuestions().size(); i++) {
            TSEV2ReadOnlyQuestionView q = renderModel.getQuestions().get(i);
            JPanel questionPanel = new JPanel();
            questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
            questionPanel.setBackground(Color.WHITE);
            questionPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    new EmptyBorder(10, 0, 10, 0)
            ));

            JLabel qLabel = new JLabel("<html><b>Question " + (i + 1) + "</b>: " + safeText(q.getContent()) + "</html>");
            qLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            questionPanel.add(qLabel);

            ButtonGroup bg = new ButtonGroup();
            for (TSEV2ReadOnlyOptionView opt : q.getOptions()) {
                JRadioButton optButton = new JRadioButton("<html>" + safeText(opt.getContent()) + "</html>");
                optButton.setEnabled(true);
                optButton.setBackground(Color.WHITE);
                optButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                optButton.addActionListener(e -> {
                    selectionState.selectOption(q.getId(), opt.getId());
                    updateProgress();
                    runDraftAutosave();
                });
                bg.add(optButton);
                optionButtonsByQuestionId
                        .computeIfAbsent(q.getId(), ignored -> new LinkedHashMap<>())
                        .put(opt.getId(), optButton);
                questionPanel.add(optButton);
            }
            questionsContainer.add(questionPanel);
        }

        JScrollPane scrollPane = new JScrollPane(questionsContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        footerPanel.setBackground(new Color(255, 230, 230));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.RED));
        progressLabel = new JLabel();
        progressLabel.setForeground(new Color(120, 35, 35));
        progressLabel.setFont(new Font("Arial", Font.BOLD, 12));
        footerPanel.add(progressLabel);

        JLabel footerLabel = new JLabel("Draft snapshot: in-memory only.");
        footerLabel.setForeground(Color.RED);
        footerLabel.setFont(new Font("Arial", Font.BOLD, 12));
        footerPanel.add(footerLabel);

        autosaveStatusLabel = new JLabel();
        autosaveStatusLabel.setForeground(new Color(120, 35, 35));
        autosaveStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        footerPanel.add(autosaveStatusLabel);

        boolean submitEnabled = Boolean.parseBoolean(System.getProperty(
                com.mycompany.tutorhub_enterprise.client.services.TSEV2ServerNoGradingSubmitBridgeService.FEATURE_FLAG,
                "false"));
        if (submitEnabled) {
            JButton submitBtn = new JButton("Server Submit Dry-run");
            submitBtn.addActionListener(e -> {
                submitBtn.setEnabled(false);
                autosaveStatusLabel.setText("Submitting...");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            com.mycompany.tutorhub_enterprise.client.services.TSEV2ServerSubmitTransport transport = (action, payloadJson) -> {
                                try {
                                    com.mycompany.tutorhub_enterprise.models.Packet packet = new com.mycompany.tutorhub_enterprise.models.Packet();
                                    packet.action = action;
                                    packet.data = payloadJson;
                                    com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(packet);
                                    com.mycompany.tutorhub_enterprise.models.Packet resp = com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().receivePacket(null, null, 15000);
                                    String okAction = action + "_OK";
                                    String errorAction = action + "_ERROR";
                                    if (!okAction.equals(resp.action) && !errorAction.equals(resp.action)) {
                                        return "{\"status\":\"ERROR\",\"errorCode\":\"ERROR_UNEXPECTED_RESPONSE_ACTION\"}";
                                    }
                                    if (resp.data instanceof String) {
                                        return (String) resp.data;
                                    }
                                    if (resp.payload != null && !resp.payload.trim().isEmpty()) {
                                        return resp.payload;
                                    }
                                    if (!resp.success) {
                                        String safeMessage = resp.message == null || resp.message.trim().isEmpty()
                                                ? "UNKNOWN_SERVER_ERROR"
                                                : resp.message.replace("\"", "");
                                        return "{\"status\":\"ERROR\",\"errorCode\":\"" + safeMessage + "\"}";
                                    }
                                    return "{\"status\":\"ERROR\",\"errorCode\":\"ERROR_EMPTY_RESPONSE\"}";
                                } catch (Exception ex) {
                                    return "{\"status\":\"ERROR\",\"errorCode\":\"ERROR_NETWORK_TRANSPORT\"}";
                                }
                            };
                            com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2ClientSubmitPayloadPrepareService prepareService = new com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2ClientSubmitPayloadPrepareService();
                            com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload payload = prepareService.preparePayload(renderModel, selectionState);
                            
                            com.mycompany.tutorhub_enterprise.client.services.TSEV2ServerNoGradingSubmitBridgeService bridgeService = new com.mycompany.tutorhub_enterprise.client.services.TSEV2ServerNoGradingSubmitBridgeService(transport);
                            com.mycompany.tutorhub_enterprise.client.services.TSEV2ServerNoGradingSubmitBridgeResult result = bridgeService.submitNoGrading(payload);
                            
                            SwingUtilities.invokeLater(() -> {
                                if (result.isSuccess()) {
                            autosaveStatusLabel.setText("Submit OK - DraftID: " + result.getClosureDraftId()
                                    + " LedgerID: " + result.getLedgerId()
                                    + " RecordID: " + result.getSubmitRecordId()
                                    + " Hash: " + result.getPayloadHash()
                                    + " Status: " + result.getFinalStatus());
                                } else {
                                    autosaveStatusLabel.setText("Submit Failed: " + result.getErrorCode());
                                }
                                submitBtn.setEnabled(true);
                            });
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                autosaveStatusLabel.setText("Submit Error: VALIDATION_ERROR");
                                submitBtn.setEnabled(true);
                            });
                        }
                        return null;
                    }
                }.execute();
            });
            footerPanel.add(submitBtn);
        }

        updateProgress();
        if (draftRestoreHandler == null) {
            updateAutosaveStatus(draftAutosaveHandler == null
                    ? "Encrypted local draft autosave: disabled"
                    : "Encrypted local draft autosave: ready");
        } else {
            runDraftRestoreOnStartup();
        }
        add(footerPanel, BorderLayout.SOUTH);
    }

    public TSEV2AnswerSelectionState getSelectionState() {
        return selectionState;
    }

    public String renderSafePanelText() {
        StringBuilder sb = new StringBuilder();
        appendText(this, sb);
        return sb.toString();
    }

    private static int resolveQuestionCount(TSEV2ReadOnlyExamRenderModel model) {
        if (model == null || model.getQuestions() == null) {
            return 0;
        }
        return model.getQuestions().size();
    }

    private void updateProgress() {
        if (progressLabel != null) {
            progressLabel.setText("Answered " + selectionState.getAnsweredCount()
                    + " / " + selectionState.getTotalQuestionCount());
        }
    }

    private void runDraftAutosave() {
        if (draftAutosaveHandler == null) {
            updateAutosaveStatus("Encrypted local draft autosave: disabled");
            return;
        }
        try {
            draftAutosaveHandler.save(renderModel, selectionState);
            updateAutosaveStatus("Encrypted local draft autosave: saved");
        } catch (Exception ex) {
            updateAutosaveStatus("Encrypted local draft autosave: failed - " + safeAutosaveErrorCode(ex));
        }
    }

    private void runDraftRestoreOnStartup() {
        try {
            DraftRestoreResult result = draftRestoreHandler.restore(renderModel, selectionState);
            if (result == null) {
                result = DraftRestoreResult.notFound();
            }
            if (result.isRestored()) {
                syncButtonsFromSelectionState();
            }
            updateProgress();
            updateAutosaveStatus(result.getStatusText());
        } catch (Exception ex) {
            updateProgress();
            updateAutosaveStatus("Restore failed - " + safeRestoreErrorCode(ex));
        }
    }

    private void syncButtonsFromSelectionState() {
        for (Map.Entry<Integer, Integer> selection : selectionState.snapshot().entrySet()) {
            Map<Integer, JRadioButton> optionButtons = optionButtonsByQuestionId.get(selection.getKey());
            if (optionButtons == null) {
                continue;
            }
            JRadioButton selectedButton = optionButtons.get(selection.getValue());
            if (selectedButton != null) {
                selectedButton.setSelected(true);
            }
        }
    }

    private void updateAutosaveStatus(String status) {
        if (autosaveStatusLabel != null) {
            autosaveStatusLabel.setText(status);
        }
    }

    private static String safeAutosaveErrorCode(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return "VALIDATION_ERROR";
        }
        if (ex instanceof SecurityException) {
            return "SECURITY_ERROR";
        }
        return "SAVE_FAILED";
    }

    private static String safeRestoreErrorCode(Exception ex) {
        if (ex instanceof TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException) {
            return ((TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException) ex).getErrorCode();
        }
        return TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_CONTEXT_MISMATCH;
    }

    private void showSafeError(String message) {
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(Color.WHITE);
        errorPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel label = new JLabel("<html><b>Selection prototype unavailable.</b><br>" + safeText(message) + "</html>");
        label.setForeground(new Color(150, 40, 40));
        errorPanel.add(label, BorderLayout.CENTER);
        add(errorPanel, BorderLayout.CENTER);
    }

    private static String validateRenderModel(TSEV2ReadOnlyExamRenderModel model) {
        if (model == null) {
            return "Render model is missing.";
        }
        List<TSEV2ReadOnlyQuestionView> questions = model.getQuestions();
        if (questions == null || questions.isEmpty()) {
            return "No safe questions available for debug selection.";
        }
        StringBuilder visibleText = new StringBuilder();
        appendVisibleValue(visibleText, model.getAttemptId());
        appendVisibleValue(visibleText, model.getDeadlineAt());
        for (TSEV2ReadOnlyQuestionView q : questions) {
            if (q == null) {
                return "A question entry is missing.";
            }
            if (q.getOptions() == null) {
                return "A question has no safe option list.";
            }
            appendVisibleValue(visibleText, q.getContent());
            for (TSEV2ReadOnlyOptionView opt : q.getOptions()) {
                if (opt == null) {
                    return "An option entry is missing.";
                }
                appendVisibleValue(visibleText, opt.getContent());
            }
        }
        String lower = visibleText.toString().toLowerCase();
        for (String token : FORBIDDEN_RENDER_TOKENS) {
            if (lower.contains(token)) {
                return "Sensitive marker blocked before rendering.";
            }
        }
        return null;
    }

    private static void appendVisibleValue(StringBuilder sb, String value) {
        if (value != null) {
            sb.append(' ').append(value);
        }
    }

    private static String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static void appendText(Container container, StringBuilder sb) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                sb.append(((JLabel) comp).getText()).append(' ');
            } else if (comp instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) comp;
                sb.append(button.getText()).append(' ');
            }
            if (comp instanceof Container) {
                appendText((Container) comp, sb);
            }
        }
    }
}
