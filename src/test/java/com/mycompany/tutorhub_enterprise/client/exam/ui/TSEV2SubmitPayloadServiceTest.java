package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2SubmitPayloadServiceTest {

    private TSEV2SubmitPayloadService service;
    private TSEV2ReadOnlyExamRenderModel model;
    private TSEV2AnswerDraftSnapshot snapshot;

    @BeforeEach
    public void setup() {
        service = new TSEV2SubmitPayloadService();
        
        // Setup mock model
        model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(101);
        model.setPaperId(202);
        model.setPackageHash("HASH-123");
        model.setQuestionCount(3);
        
        List<TSEV2ReadOnlyQuestionView> questions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            TSEV2ReadOnlyQuestionView q = new TSEV2ReadOnlyQuestionView();
            q.setId(i);
            
            List<TSEV2ReadOnlyOptionView> opts = new ArrayList<>();
            TSEV2ReadOnlyOptionView opt1 = new TSEV2ReadOnlyOptionView();
            opt1.setId(i * 10 + 1);
            TSEV2ReadOnlyOptionView opt2 = new TSEV2ReadOnlyOptionView();
            opt2.setId(i * 10 + 2);
            opts.add(opt1);
            opts.add(opt2);
            q.setOptions(opts);
            
            questions.add(q);
        }
        model.setQuestions(questions);
        
        // Setup mock snapshot
        snapshot = new TSEV2AnswerDraftSnapshot();
        snapshot.setExamId(101);
        snapshot.setPaperId(202);
        snapshot.setAttemptId("ATTEMPT-1");
        snapshot.setPackageHash("HASH-123");
        snapshot.setSnapshotHash("SNAP-HASH-1");
    }

    @Test
    public void testEmptySelection() {
        snapshot.setAnswers(new ArrayList<>());
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        assertEquals(0, payload.getAnsweredCount());
        assertEquals(3, payload.getUnansweredCount());
        assertFalse(payload.isComplete());
        assertEquals("ATTEMPT-1", payload.getAttemptId());
        
        assertDoesNotThrow(() -> service.validatePayloadMatchesRenderModel(payload, model));
        assertDoesNotThrow(() -> service.validatePayloadSafe(payload));
    }

    @Test
    public void testPartialSelection() {
        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        answers.add(new TSEV2AnswerDraftItem(1, 11, "time"));
        snapshot.setAnswers(answers);
        
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        assertEquals(1, payload.getAnsweredCount());
        assertEquals(2, payload.getUnansweredCount());
        assertFalse(payload.isComplete());
        
        assertDoesNotThrow(() -> service.validatePayloadMatchesRenderModel(payload, model));
    }

    @Test
    public void testFullSelection() {
        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        answers.add(new TSEV2AnswerDraftItem(1, 11, "time"));
        answers.add(new TSEV2AnswerDraftItem(2, 22, "time"));
        answers.add(new TSEV2AnswerDraftItem(3, 31, "time"));
        snapshot.setAnswers(answers);
        
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        assertEquals(3, payload.getAnsweredCount());
        assertEquals(0, payload.getUnansweredCount());
        assertTrue(payload.isComplete());
        
        assertDoesNotThrow(() -> service.validatePayloadMatchesRenderModel(payload, model));
    }

    @Test
    public void testDuplicateAnswersNormalized() {
        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        answers.add(new TSEV2AnswerDraftItem(1, 11, "time1"));
        answers.add(new TSEV2AnswerDraftItem(1, 12, "time2")); // duplicate question
        snapshot.setAnswers(answers);
        
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        // Should only pick the first one
        assertEquals(1, payload.getAnsweredCount());
        assertEquals(1, payload.getAnswers().size());
        assertEquals(11, payload.getAnswers().get(0).getSelectedOptionId());
        
        assertDoesNotThrow(() -> service.validatePayloadMatchesRenderModel(payload, model));
    }

    @Test
    public void testInvalidOptionRejected() {
        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        // Option 99 doesn't belong to Question 1
        answers.add(new TSEV2AnswerDraftItem(1, 99, "time"));
        snapshot.setAnswers(answers);
        
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.validatePayloadMatchesRenderModel(payload, model)
        );
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_PAYLOAD_INVALID_OPTION"));
    }
    
    @Test
    public void testInvalidQuestionRejected() {
        List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
        // Question 9 doesn't exist
        answers.add(new TSEV2AnswerDraftItem(9, 91, "time"));
        snapshot.setAnswers(answers);
        
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.validatePayloadMatchesRenderModel(payload, model)
        );
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_PAYLOAD_INVALID_OPTION"));
    }

    @Test
    public void testContextMismatch() {
        snapshot.setAnswers(new ArrayList<>());
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        // Mismatch examId
        TSEV2ReadOnlyExamRenderModel mismatchedModel = new TSEV2ReadOnlyExamRenderModel();
        mismatchedModel.setExamId(999);
        mismatchedModel.setPaperId(202);
        mismatchedModel.setPackageHash("HASH-123");
        mismatchedModel.setQuestionCount(3);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.validatePayloadMatchesRenderModel(payload, mismatchedModel)
        );
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: examId"));
    }

    @Test
    public void testSafetyValidationNoLeak() {
        snapshot.setAnswers(new ArrayList<>());
        TSEV2SubmitPayload payload = service.createPayload(model, snapshot);
        
        // Intentionally inject bad string to check if validatePayloadSafe catches it
        payload.setAttemptId("some_sessionToken_leak");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.validatePayloadSafe(payload)
        );
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_PAYLOAD_UNSAFE"));
        
        payload.setAttemptId("some_isCorrect_leak");
        assertThrows(RuntimeException.class, () -> service.validatePayloadSafe(payload));
        
        payload.setAttemptId("some_answerKey_leak");
        assertThrows(RuntimeException.class, () -> service.validatePayloadSafe(payload));
    }

    @Test
    public void testPayloadHashStable() {
        snapshot.setAnswers(new ArrayList<>());
        TSEV2SubmitPayload payload1 = service.createPayload(model, snapshot);
        
        // Make payload2 with exact same inputs
        TSEV2SubmitPayload payload2 = service.createPayload(model, snapshot);
        
        // Except preparedAt which differs by time, let's sync them for the test
        payload2.setPreparedAt(payload1.getPreparedAt());
        payload2.setPayloadHash(service.computePayloadHash(payload2));
        
        assertEquals(payload1.getPayloadHash(), payload2.getPayloadHash());
    }
}
