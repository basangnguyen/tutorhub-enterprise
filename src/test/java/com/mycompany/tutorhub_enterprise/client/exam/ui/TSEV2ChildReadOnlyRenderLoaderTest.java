package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class TSEV2ChildReadOnlyRenderLoaderTest {

    @Test
    public void testSanitizeForReadOnlyRender_ValidHandoff() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.examId = 1;
        bundle.paperId = 1;
        bundle.questions = new ArrayList<>();
        
        V2ExamHandoffQuestion q = new V2ExamHandoffQuestion();
        q.questionId = 1;
        q.content = "Question content here";
        q.options = new ArrayList<>();
        
        V2ExamHandoffOption opt = new V2ExamHandoffOption();
        opt.optionId = 1;
        opt.content = "Option content here";
        q.options.add(opt);
        
        bundle.questions.add(q);

        TSEV2ReadOnlyExamRenderModel model = TSEV2ChildReadOnlyRenderLoader.sanitizeForReadOnlyRender(bundle);
        
        assertNotNull(model);
        assertEquals(1, model.getExamId());
        assertEquals(1, model.getPaperId());
        assertEquals(1, model.getQuestions().size());
        assertEquals("Question content here", model.getQuestions().get(0).getContent());
        assertEquals(1, model.getQuestions().get(0).getOptions().size());
        assertEquals("Option content here", model.getQuestions().get(0).getOptions().get(0).getContent());
    }

    @Test
    public void testSanitizeForReadOnlyRender_NoIsCorrect() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        V2ExamHandoffQuestion q = new V2ExamHandoffQuestion();
        q.content = "content";
        // bundle is clean, but let's make sure the returned JSON representation doesn't contain answers.
        TSEV2ReadOnlyExamRenderModel model = TSEV2ChildReadOnlyRenderLoader.sanitizeForReadOnlyRender(bundle);
        
        Gson gson = new Gson();
        String json = gson.toJson(model);
        
        assertFalse(json.contains("isCorrect"), "Render model must not contain isCorrect");
        assertFalse(json.contains("answerKey"), "Render model must not contain answerKey");
        assertFalse(json.contains("sessionToken"), "Render model must not contain sessionToken");
    }
}
