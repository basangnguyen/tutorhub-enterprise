package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2ClientSubmitPayloadPrepareServiceTest {

    private TSEV2ClientSubmitPayloadPrepareService service;
    private TSEV2ReadOnlyExamRenderModel renderModel;
    private final Gson gson = new GsonBuilder().create();

    @BeforeEach
    public void setUp() {
        service = new TSEV2ClientSubmitPayloadPrepareService();
        renderModel = createMockRenderModel();
    }

    private TSEV2ReadOnlyExamRenderModel createMockRenderModel() {
        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(101);
        model.setPaperId(202);
        model.setAttemptId("ATT-303");
        model.setPackageHash("ABC-123");
        model.setQuestionCount(3);

        List<TSEV2ReadOnlyQuestionView> questions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            TSEV2ReadOnlyQuestionView q = new TSEV2ReadOnlyQuestionView();
            q.setId(i);
            q.setContent("Q" + i);
            List<TSEV2ReadOnlyOptionView> options = new ArrayList<>();
            for (int j = 1; j <= 4; j++) {
                TSEV2ReadOnlyOptionView o = new TSEV2ReadOnlyOptionView();
                o.setId(i * 10 + j);
                o.setContent("Opt " + j);
                options.add(o);
            }
            q.setOptions(options);
            questions.add(q);
        }
        model.setQuestions(questions);
        return model;
    }

    @Test
    public void testPreparePayloadEmptySelection() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        TSEV2SubmitPayload payload = service.preparePayload(renderModel, state);
        
        assertNotNull(payload);
        assertEquals(101L, payload.getExamId());
        assertEquals(0, payload.getAnsweredCount());
        assertEquals(3, payload.getUnansweredCount());
        assertFalse(payload.isComplete());
        assertEquals(64, payload.getPayloadHash().length());
    }

    @Test
    public void testPreparePayloadHashIsSha256Hex() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        state.selectOption(1, 11);

        TSEV2SubmitPayload payload = service.preparePayload(renderModel, state);

        assertTrue(Pattern.matches("^[a-f0-9]{64}$", payload.getPayloadHash()));
    }

    @Test
    public void testPreparePayloadPartialSelection() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        state.selectOption(1, 11); // Q1 -> Opt1
        
        TSEV2SubmitPayload payload = service.preparePayload(renderModel, state);
        
        assertNotNull(payload);
        assertEquals(1, payload.getAnsweredCount());
        assertEquals(2, payload.getUnansweredCount());
        assertFalse(payload.isComplete());
    }

    @Test
    public void testPreparePayloadFullSelection() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        state.selectOption(1, 11);
        state.selectOption(2, 22);
        state.selectOption(3, 33);
        
        TSEV2SubmitPayload payload = service.preparePayload(renderModel, state);
        
        assertNotNull(payload);
        assertEquals(3, payload.getAnsweredCount());
        assertEquals(0, payload.getUnansweredCount());
        assertTrue(payload.isComplete());
    }

    @Test
    public void testPreparePayloadDoesNotContainSensitiveMarkers() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        state.selectOption(1, 11);
        state.selectOption(2, 22);

        TSEV2SubmitPayload payload = service.preparePayload(renderModel, state);
        String json = gson.toJson(payload).toLowerCase();

        assertFalse(json.contains("sessiontoken"));
        assertFalse(json.contains("keyb64"));
        assertFalse(json.contains("plaintext"));
        assertFalse(json.contains("answerkey"));
        assertFalse(json.contains("iscorrect"));
        assertFalse(json.contains("correctoption"));
        assertFalse(json.contains("score"));
    }

    @Test
    public void testPreparePayloadInvalidOption() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);
        // Put invalid option id 99 for question 1 (valid options are 11, 12, 13, 14)
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            state.selectOption(1, 99); // bypass safety
            service.preparePayload(renderModel, state);
        });
        assertTrue(ex.getMessage().contains("Selected option does not belong to the question"));
    }
}
