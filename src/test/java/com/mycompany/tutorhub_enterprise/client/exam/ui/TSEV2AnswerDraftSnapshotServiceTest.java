package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2AnswerDraftSnapshotServiceTest {

    private final TSEV2AnswerDraftSnapshotService service =
            new TSEV2AnswerDraftSnapshotService(() -> Instant.parse("2026-06-19T00:00:00Z"));

    @Test
    public void emptySelectionCreatesZeroAnsweredSnapshot() {
        TSEV2AnswerDraftSnapshot snapshot = service.createSnapshot(createSafeModel(), new TSEV2AnswerSelectionState(2));

        assertEquals(0, snapshot.getAnsweredCount());
        assertEquals(2, snapshot.getQuestionCount());
        assertTrue(snapshot.getAnswers().isEmpty());
        assertEquals(TSEV2AnswerDraftSnapshotService.FLOW, snapshot.getFlow());
        assertNotNull(snapshot.getSnapshotHash());
    }

    @Test
    public void oneSelectedAnswerCreatesOneAnswerItem() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 1002);

        TSEV2AnswerDraftSnapshot snapshot = service.createSnapshot(createSafeModel(), state);

        assertEquals(1, snapshot.getAnsweredCount());
        assertEquals(101, snapshot.getAnswers().get(0).getQuestionId());
        assertEquals(1002, snapshot.getAnswers().get(0).getSelectedOptionId());
        assertEquals("2026-06-19T00:00:00Z", snapshot.getAnswers().get(0).getAnsweredAt());
    }

    @Test
    public void changedSelectionKeepsOnlyLatestOption() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 1001);
        state.selectOption(101, 1002);

        TSEV2AnswerDraftSnapshot snapshot = service.createSnapshot(createSafeModel(), state);

        assertEquals(1, snapshot.getAnsweredCount());
        assertEquals(1002, snapshot.getAnswers().get(0).getSelectedOptionId());
    }

    @Test
    public void optionFromAnotherQuestionIsRejected() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 2001);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createSnapshot(createSafeModel(), state)
        );
        assertEquals("Selected option does not belong to the question.", ex.getMessage());
    }

    @Test
    public void snapshotJsonContainsSafeExamMetadata() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(102, 2001);

        String json = service.toJson(service.createSnapshot(createSafeModel(), state));

        assertTrue(json.contains("\"examId\":7"));
        assertTrue(json.contains("\"paperId\":8"));
        assertTrue(json.contains("\"attemptId\":\"attempt-debug-id\""));
        assertTrue(json.contains("\"packageHash\":\"package-hash-123\""));
        assertTrue(json.contains("\"answeredCount\":1"));
        assertTrue(json.contains("\"answers\""));
    }

    @Test
    public void snapshotJsonDoesNotContainSensitiveOrScoringMarkers() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 1001);

        String json = service.toJson(service.createSnapshot(createSafeModel(), state)).toLowerCase();

        assertFalse(json.contains("sessiontoken"));
        assertFalse(json.contains("keyb64"));
        assertFalse(json.contains("plaintext"));
        assertFalse(json.contains("iscorrect"));
        assertFalse(json.contains("answerkey"));
        assertFalse(json.contains("correctoption"));
        assertFalse(json.contains("passwordhash"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("score"));
        assertFalse(json.contains("grading_config"));
    }

    @Test
    public void snapshotHashIsStableWithSameInput() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(102, 2002);

        TSEV2AnswerDraftSnapshot first = service.createSnapshot(createSafeModel(), state);
        TSEV2AnswerDraftSnapshot second = service.createSnapshot(createSafeModel(), state);

        assertEquals(first.getSnapshotHash(), second.getSnapshotHash());
        assertEquals(first.getSnapshotHash(), service.computeSnapshotHash(first));
    }

    @Test
    public void validateSnapshotRejectsSensitiveMarkerInMetadata() {
        TSEV2AnswerDraftSnapshot snapshot =
                service.createSnapshot(createSafeModel(), new TSEV2AnswerSelectionState(2));
        snapshot.setAttemptId("debug-sessionToken-value");
        snapshot.setSnapshotHash(service.computeSnapshotHash(snapshot));

        assertThrows(IllegalArgumentException.class, () -> service.validateSnapshotSafe(snapshot));
    }

    @Test
    public void validateSnapshotRejectsTamperedHash() {
        TSEV2AnswerDraftSnapshot snapshot =
                service.createSnapshot(createSafeModel(), new TSEV2AnswerSelectionState(2));

        snapshot.setAnsweredCount(1);
        snapshot.setSnapshotHash("bad-hash");

        assertThrows(IllegalArgumentException.class, () -> service.validateSnapshotSafe(snapshot));
    }

    @Test
    public void invalidModelMetadataIsRejected() {
        TSEV2ReadOnlyExamRenderModel model = createSafeModel();
        model.setQuestionCount(99);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSnapshot(model, new TSEV2AnswerSelectionState(2))
        );
    }

    @Test
    public void snapshotCreationHasNoAutosaveSubmitOrNetworkContract() {
        TSEV2AnswerDraftSnapshot snapshot =
                service.createSnapshot(createSafeModel(), new TSEV2AnswerSelectionState(2));
        String json = service.toJson(snapshot).toLowerCase();

        assertFalse(json.contains("exam_submit"));
        assertFalse(json.contains("submit_payload"));
        assertFalse(json.contains("autosave_payload"));
        assertFalse(json.contains("http"));
        assertFalse(json.contains("socket"));
    }

    private static TSEV2ReadOnlyExamRenderModel createSafeModel() {
        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(7);
        model.setPaperId(8);
        model.setAttemptId("attempt-debug-id");
        model.setPackageHash("package-hash-123");
        model.setQuestionCount(2);
        model.setQuestions(List.of(
                question(101, "Safe question one", List.of(
                        option(1001, "Safe option A"),
                        option(1002, "Safe option B")
                )),
                question(102, "Safe question two", List.of(
                        option(2001, "True"),
                        option(2002, "False")
                ))
        ));
        return model;
    }

    private static TSEV2ReadOnlyQuestionView question(
            int id,
            String content,
            List<TSEV2ReadOnlyOptionView> options
    ) {
        TSEV2ReadOnlyQuestionView question = new TSEV2ReadOnlyQuestionView();
        question.setId(id);
        question.setContent(content);
        question.setOptions(options);
        return question;
    }

    private static TSEV2ReadOnlyOptionView option(int id, String content) {
        TSEV2ReadOnlyOptionView option = new TSEV2ReadOnlyOptionView();
        option.setId(id);
        option.setContent(content);
        return option;
    }
}
