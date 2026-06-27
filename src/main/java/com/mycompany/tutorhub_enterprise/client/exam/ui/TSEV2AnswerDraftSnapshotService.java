package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

public class TSEV2AnswerDraftSnapshotService {

    public static final String SNAPSHOT_VERSION = "tse_v2_answer_draft_snapshot_v1";
    public static final String FLOW = "PAPER_START_V2";

    private static final String[] FORBIDDEN_SNAPSHOT_TOKENS = {
            "sessiontoken",
            "keyb64",
            "aeskey",
            "secretkey",
            "rawkey",
            "plaintextjson",
            "plaintext",
            "iscorrect",
            "answerkey",
            "correctoption",
            "grading_config",
            "passwordhash",
            "password",
            "score",
            "iscorrectanswer"
    };

    private final Supplier<Instant> clock;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public TSEV2AnswerDraftSnapshotService() {
        this(Instant::now);
    }

    public TSEV2AnswerDraftSnapshotService(Supplier<Instant> clock) {
        this.clock = clock == null ? Instant::now : clock;
    }

    public TSEV2AnswerDraftSnapshot createSnapshot(
            TSEV2ReadOnlyExamRenderModel model,
            TSEV2AnswerSelectionState state
    ) {
        validateSelectedOptionsBelongToQuestions(model, state);

        List<TSEV2ReadOnlyQuestionView> questions = model.getQuestions();
        Map<Integer, Integer> selectedOptions = new TreeMap<>(state.snapshot());
        String now = DateTimeFormatter.ISO_INSTANT.format(clock.get());

        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : selectedOptions.entrySet()) {
            answers.add(new TSEV2AnswerDraftItem(entry.getKey(), entry.getValue(), now));
        }

        TSEV2AnswerDraftSnapshot snapshot = new TSEV2AnswerDraftSnapshot();
        snapshot.setSnapshotVersion(SNAPSHOT_VERSION);
        snapshot.setFlow(FLOW);
        snapshot.setExamId(model.getExamId());
        snapshot.setPaperId(model.getPaperId());
        snapshot.setAttemptId(blankToNull(model.getAttemptId()));
        snapshot.setPackageHash(blankToNull(model.getPackageHash()));
        snapshot.setQuestionCount(questions.size());
        snapshot.setAnsweredCount(answers.size());
        snapshot.setCreatedAt(now);
        snapshot.setUpdatedAt(now);
        snapshot.setAnswers(answers);
        snapshot.setSnapshotHash(computeSnapshotHash(snapshot));
        validateSnapshotSafe(snapshot);
        return snapshot;
    }

    public String toJson(TSEV2AnswerDraftSnapshot snapshot) {
        validateSnapshotSafe(snapshot);
        return gson.toJson(snapshot);
    }

    public String computeSnapshotHash(TSEV2AnswerDraftSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot is missing.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalSnapshotText(snapshot).getBytes(StandardCharsets.UTF_8));
            return toLowerHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Snapshot hash algorithm is unavailable.", ex);
        }
    }

    public void validateSnapshotSafe(TSEV2AnswerDraftSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot is missing.");
        }
        if (!SNAPSHOT_VERSION.equals(snapshot.getSnapshotVersion())) {
            throw new IllegalArgumentException("Snapshot version is invalid.");
        }
        if (!FLOW.equals(snapshot.getFlow())) {
            throw new IllegalArgumentException("Snapshot flow is invalid.");
        }
        if (snapshot.getExamId() <= 0) {
            throw new IllegalArgumentException("Snapshot examId is invalid.");
        }
        if (snapshot.getPaperId() <= 0) {
            throw new IllegalArgumentException("Snapshot paperId is invalid.");
        }
        if (snapshot.getPackageHash() != null && snapshot.getPackageHash().trim().isEmpty()) {
            throw new IllegalArgumentException("Snapshot packageHash is invalid.");
        }
        if (snapshot.getQuestionCount() < 0) {
            throw new IllegalArgumentException("Snapshot questionCount is invalid.");
        }
        List<TSEV2AnswerDraftItem> answers = snapshot.getAnswers();
        if (answers == null) {
            throw new IllegalArgumentException("Snapshot answers are missing.");
        }
        if (snapshot.getAnsweredCount() != answers.size()) {
            throw new IllegalArgumentException("Snapshot answeredCount is inconsistent.");
        }
        if (snapshot.getAnsweredCount() > snapshot.getQuestionCount()) {
            throw new IllegalArgumentException("Snapshot answeredCount exceeds questionCount.");
        }
        if (isBlank(snapshot.getCreatedAt()) || isBlank(snapshot.getUpdatedAt())) {
            throw new IllegalArgumentException("Snapshot timestamp is missing.");
        }
        for (TSEV2AnswerDraftItem answer : answers) {
            if (answer == null) {
                throw new IllegalArgumentException("Snapshot answer item is missing.");
            }
            if (answer.getQuestionId() <= 0 || answer.getSelectedOptionId() <= 0) {
                throw new IllegalArgumentException("Snapshot answer ids are invalid.");
            }
            if (isBlank(answer.getAnsweredAt())) {
                throw new IllegalArgumentException("Snapshot answer timestamp is missing.");
            }
        }
        ensureNoSensitiveMarkers(snapshot);
        if (!isBlank(snapshot.getSnapshotHash())) {
            String expectedHash = computeSnapshotHash(snapshot);
            if (!expectedHash.equals(snapshot.getSnapshotHash())) {
                throw new IllegalArgumentException("Snapshot hash is invalid.");
            }
        }
    }

    public void validateSelectedOptionsBelongToQuestions(
            TSEV2ReadOnlyExamRenderModel model,
            TSEV2AnswerSelectionState state
    ) {
        if (model == null) {
            throw new IllegalArgumentException("Render model is missing.");
        }
        if (state == null) {
            throw new IllegalArgumentException("Selection state is missing.");
        }
        if (model.getExamId() <= 0) {
            throw new IllegalArgumentException("Render model examId is invalid.");
        }
        if (model.getPaperId() <= 0) {
            throw new IllegalArgumentException("Render model paperId is invalid.");
        }
        if (model.getPackageHash() != null && model.getPackageHash().trim().isEmpty()) {
            throw new IllegalArgumentException("Render model packageHash is invalid.");
        }
        List<TSEV2ReadOnlyQuestionView> questions = model.getQuestions();
        if (questions == null) {
            throw new IllegalArgumentException("Render model questions are missing.");
        }
        if (model.getQuestionCount() != questions.size()) {
            throw new IllegalArgumentException("Render model questionCount is inconsistent.");
        }
        if (state.getTotalQuestionCount() != questions.size()) {
            throw new IllegalArgumentException("Selection state questionCount is inconsistent.");
        }

        Map<Integer, Set<Integer>> allowedOptionIds = new HashMap<>();
        for (TSEV2ReadOnlyQuestionView question : questions) {
            if (question == null || question.getId() <= 0) {
                throw new IllegalArgumentException("Render model question is invalid.");
            }
            List<TSEV2ReadOnlyOptionView> options = question.getOptions();
            if (options == null) {
                throw new IllegalArgumentException("Render model options are missing.");
            }
            Set<Integer> optionIds = new HashSet<>();
            for (TSEV2ReadOnlyOptionView option : options) {
                if (option == null || option.getId() <= 0) {
                    throw new IllegalArgumentException("Render model option is invalid.");
                }
                optionIds.add(option.getId());
            }
            allowedOptionIds.put(question.getId(), optionIds);
        }

        for (Map.Entry<Integer, Integer> entry : state.snapshot().entrySet()) {
            Set<Integer> optionIds = allowedOptionIds.get(entry.getKey());
            if (optionIds == null || !optionIds.contains(entry.getValue())) {
                throw new IllegalArgumentException("Selected option does not belong to the question.");
            }
        }
    }

    private void ensureNoSensitiveMarkers(TSEV2AnswerDraftSnapshot snapshot) {
        StringBuilder visible = new StringBuilder();
        appendValue(visible, snapshot.getSnapshotVersion());
        appendValue(visible, snapshot.getFlow());
        appendValue(visible, snapshot.getAttemptId());
        appendValue(visible, snapshot.getPackageHash());
        appendValue(visible, snapshot.getCreatedAt());
        appendValue(visible, snapshot.getUpdatedAt());
        for (TSEV2AnswerDraftItem answer : snapshot.getAnswers()) {
            appendValue(visible, answer.getAnsweredAt());
        }

        String lower = visible.toString().toLowerCase();
        for (String token : FORBIDDEN_SNAPSHOT_TOKENS) {
            if (lower.contains(token)) {
                throw new IllegalArgumentException("Snapshot contains a blocked sensitive marker.");
            }
        }
    }

    private String canonicalSnapshotText(TSEV2AnswerDraftSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, snapshot.getSnapshotVersion());
        appendField(sb, snapshot.getFlow());
        appendField(sb, snapshot.getExamId());
        appendField(sb, snapshot.getPaperId());
        appendField(sb, snapshot.getAttemptId());
        appendField(sb, snapshot.getPackageHash());
        appendField(sb, snapshot.getQuestionCount());
        appendField(sb, snapshot.getAnsweredCount());
        appendField(sb, snapshot.getCreatedAt());
        appendField(sb, snapshot.getUpdatedAt());
        List<TSEV2AnswerDraftItem> answers = snapshot.getAnswers();
        if (answers != null) {
            for (TSEV2AnswerDraftItem answer : answers) {
                if (answer == null) {
                    appendField(sb, "<null-answer>");
                } else {
                    appendField(sb, answer.getQuestionId());
                    appendField(sb, answer.getSelectedOptionId());
                    appendField(sb, answer.getAnsweredAt());
                }
            }
        }
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, Object value) {
        if (sb.length() > 0) {
            sb.append('|');
        }
        if (value == null) {
            sb.append("<null>");
        } else {
            sb.append(String.valueOf(value).replace("\\", "\\\\").replace("|", "\\|"));
        }
    }

    private static void appendValue(StringBuilder sb, String value) {
        if (value != null) {
            sb.append(' ').append(value);
        }
    }

    private static String toLowerHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
