package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitAnswerItem;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class V2SubmitDryRunValidationServiceTest {

    private TestableValidationService service;
    
    // Test flags for mocking
    private boolean mockExamNotFound = false;
    private boolean mockPaperNotFound = false;

    class TestableValidationService extends V2SubmitDryRunValidationService {
        @Override
        protected Exam getExamById(int examId) {
            if (mockExamNotFound) return null;
            Exam e = new Exam();
            e.id = examId;
            return e;
        }

        @Override
        protected ExamPaper getExamPaperById(int paperId) {
            if (mockPaperNotFound) return null;
            ExamPaper p = new ExamPaper();
            p.id = paperId;
            return p;
        }

        @Override
        protected boolean getAttemptById(String attemptId) {
            return true;
        }

        @Override
        protected List<ExamPaperQuestion> listQuestionsByPaper(int paperId) {
            List<ExamPaperQuestion> mockQuestions = new ArrayList<>();
            ExamPaperQuestion q1 = new ExamPaperQuestion(); q1.questionId = 101;
            ExamPaperQuestion q2 = new ExamPaperQuestion(); q2.questionId = 102;
            mockQuestions.add(q1);
            mockQuestions.add(q2);
            return mockQuestions;
        }
    }

    @BeforeEach
    public void setup() {
        service = new TestableValidationService();
        mockExamNotFound = false;
        mockPaperNotFound = false;
    }

    private TSEV2SubmitPayload createValidPayload() {
        TSEV2SubmitPayload payload = new TSEV2SubmitPayload();
        payload.setFlow("PAPER_START_V2");
        payload.setExamId(1);
        payload.setPaperId(1);
        payload.setAttemptId("test-attempt");
        payload.setPackageHash("hash-xyz");
        payload.setQuestionCount(2);
        payload.setAnsweredCount(2);
        payload.setUnansweredCount(0);
        payload.setComplete(true);
        payload.setPayloadHash("abc-123");
        
        List<TSEV2SubmitAnswerItem> answers = new ArrayList<>();
        TSEV2SubmitAnswerItem a1 = new TSEV2SubmitAnswerItem();
        a1.setQuestionId(101);
        a1.setSelectedOptionId(1001);
        a1.setAnsweredAt("");
        answers.add(a1);
        
        TSEV2SubmitAnswerItem a2 = new TSEV2SubmitAnswerItem();
        a2.setQuestionId(102);
        a2.setSelectedOptionId(1002);
        a2.setAnsweredAt("");
        answers.add(a2);
        
        payload.setAnswers(answers);
        return payload;
    }

    @Test
    public void testValidPayload_Success() {
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertTrue(result.isSuccess(), "Should be success");
        assertEquals("EXAM_SUBMIT_V2_DRYRUN_VALIDATE_OK", result.getErrorCode());
    }

    @Test
    public void testWrongFlow_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.setFlow("EXAM_START_V1");
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_FLOW_INVALID", result.getErrorCode());
    }

    @Test
    public void testExamNotFound_Reject() {
        mockExamNotFound = true;
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_EXAM_NOT_FOUND", result.getErrorCode());
    }

    @Test
    public void testPaperMismatch_Reject() {
        mockPaperNotFound = true;
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_PAPER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testQuestionCountMismatch_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.setQuestionCount(99); // Wrong count
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_QUESTION_COUNT_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testDuplicateQuestion_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        // Add same question twice
        TSEV2SubmitAnswerItem a3 = new TSEV2SubmitAnswerItem();
        a3.setQuestionId(101);
        a3.setAnsweredAt("");
        payload.getAnswers().add(a3);
        payload.setAnsweredCount(3);
        
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_DUPLICATE_QUESTION", result.getErrorCode());
    }

    @Test
    public void testQuestionNotInPaper_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.getAnswers().get(0).setQuestionId(999); // Not in paper
        
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_QUESTION_NOT_IN_PAPER", result.getErrorCode());
    }

    @Test
    public void testUnsafePayloadMarker_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.getAnswers().get(0).setAnsweredAt("my isCorrect true"); // Unsafe keyword
        
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_PAYLOAD_UNSAFE", result.getErrorCode());
    }
    
    @Test
    public void testUnsafePayloadGlobalHash_Reject() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.setPayloadHash("something-score-10"); // Unsafe keyword
        
        V2SubmitDryRunValidationResult result = service.validateDryRun(1, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_PAYLOAD_UNSAFE", result.getErrorCode());
    }
}
