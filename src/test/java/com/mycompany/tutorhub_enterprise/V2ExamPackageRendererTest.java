package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.exam.ui.V2ExamPackageRenderer;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class V2ExamPackageRendererTest {

    private V2ExamHandoffBundle createValidBundle() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.examId = 100;
        bundle.paperId = 200;
        bundle.attemptId = "ATTEMPT-UUID";
        bundle.durationMinutes = 60;
        bundle.packageHash = "VALID_HASH";
        bundle.questionCount = 1;
        bundle.totalScore = 10;
        
        bundle.questions = new ArrayList<>();
        V2ExamHandoffQuestion q = new V2ExamHandoffQuestion();
        q.questionId = 1;
        q.content = "What is 2+2?";
        q.type = "MCQ";
        
        q.options = new ArrayList<>();
        V2ExamHandoffOption opt1 = new V2ExamHandoffOption();
        opt1.optionLabel = "A";
        opt1.content = "3";
        q.options.add(opt1);
        
        V2ExamHandoffOption opt2 = new V2ExamHandoffOption();
        opt2.optionLabel = "B";
        opt2.content = "4";
        q.options.add(opt2);
        
        bundle.questions.add(q);
        return bundle;
    }

    @Test
    public void testRenderValidBundle() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        String html = V2ExamPackageRenderer.renderHtml(bundle);
        
        assertNotNull(html);
        assertTrue(html.contains("What is 2+2?"));
        assertTrue(html.contains("4"));
        assertTrue(html.contains("Package Preview"));
        assertTrue(html.contains("Exam ID:</b> 100"));
    }
    
    @Test
    public void testRenderEmptyBundleThrowsException() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        Exception e = assertThrows(Exception.class, () -> V2ExamPackageRenderer.renderHtml(bundle));
        assertTrue(e.getMessage().contains("questions array is empty") || e.getMessage().contains("questionCount"));
    }

    @Test
    public void testSecurityRenderBlocksRawToken() {
        V2ExamHandoffBundle bundle = createValidBundle();
        bundle.sessionToken = "SECRET_RAW_TOKEN";
        
        // Let's deliberately inject it into content to simulate a breach
        bundle.questions.get(0).content = "Here is a SECRET_RAW_TOKEN";
        
        Exception e = assertThrows(Exception.class, () -> V2ExamPackageRenderer.renderHtml(bundle));
        assertTrue(e.getMessage().contains("SECURITY VIOLATION"));
    }

    @Test
    public void testSecurityRenderBlocksAnswers() {
        V2ExamHandoffBundle bundle = createValidBundle();
        
        // Deliberately inject isCorrect into content
        bundle.questions.get(0).content = "This content has isCorrect string inside it";
        
        Exception e = assertThrows(Exception.class, () -> V2ExamPackageRenderer.renderHtml(bundle));
        assertTrue(e.getMessage().contains("SECURITY VIOLATION: Rendered content contains answer fields"));
    }
}
