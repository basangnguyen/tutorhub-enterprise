package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.mycompany.tutorhub_enterprise.models.exam.ParsedQuizQuestion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlQuizDataParserTest {

    @Test
    public void testParseValidQuizDataString() {
        String html = "<html><body>" +
                "<script>\n" +
                "const quizData = [\n" +
                "  {\n" +
                "    question: \"1. Chip vi điều khiển 8051?\",\n" +
                "    answers: {\n" +
                "      a: 'Đáp án A',\n" +
                "      b: \"Đáp án B\",\n" +
                "      c: `Đáp án C`,\n" +
                "      d: \"Đáp án D\"\n" +
                "    },\n" +
                "    correctAnswer: 'c',\n" +
                "    explanation: 'Giải thích chi tiết',\n" +
                "  },\n" +
                "  // Comment 1\n" +
                "  {\n" +
                "    question: 'Câu 2',\n" +
                "    answers: {a:'A',b:'B'}, /* Comment 2 */\n" +
                "    correctAnswer: \"a\",\n" +
                "    explanation: \"\",\n" +
                "  } // Trailing comma above\n" +
                "];\n" +
                "</script></body></html>";

        HtmlQuizDataParser.ParseResult result = HtmlQuizDataParser.parseFromString(html);

        assertTrue(result.isSuccess(), "Parsing should succeed");
        assertEquals(0, result.getInvalidCount(), "Should have 0 invalid questions");
        assertEquals(2, result.getValidCount(), "Should have 2 valid questions");

        List<ParsedQuizQuestion> qList = result.getQuestions();

        // Check Q1
        ParsedQuizQuestion q1 = qList.get(0);
        assertEquals("1. Chip vi điều khiển 8051?", q1.getQuestion());
        assertEquals("c", q1.getCorrectAnswer());
        assertEquals("Giải thích chi tiết", q1.getExplanation());
        assertEquals(4, q1.getAnswers().size());
        assertEquals("Đáp án A", q1.getAnswers().get("a"));
        assertEquals("Đáp án C", q1.getAnswers().get("c"));

        // Check Q2
        ParsedQuizQuestion q2 = qList.get(1);
        assertEquals("Câu 2", q2.getQuestion());
        assertEquals("a", q2.getCorrectAnswer());
        assertEquals("", q2.getExplanation());
        assertEquals(2, q2.getAnswers().size());
        assertEquals("A", q2.getAnswers().get("a"));
    }

    @Test
    public void testParseInvalidQuestions() {
        String html = "<script>const quizData = [" +
                "{ question: \"Q1\", answers: {a:\"A\"}, correctAnswer: \"a\" }, " + // Invalid: only 1 answer
                "{ question: \"Q2\", answers: {a:\"A\",b:\"B\"}, correctAnswer: \"c\" } " + // Invalid: correctAnswer not in keys
                "];</script>";

        HtmlQuizDataParser.ParseResult result = HtmlQuizDataParser.parseFromString(html);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValidCount());
        assertEquals(2, result.getInvalidCount());

        assertEquals("Less than 2 answer options at index 0", result.getInvalidQuestions().get(0).getValidationError());
        assertTrue(result.getInvalidQuestions().get(1).getValidationError().contains("not found in answer keys"));
    }

    @Test
    public void testParseRealVslHtmlFile() {
        Path vslPath = Paths.get(System.getProperty("user.dir"), "docs", "vsl.html");
        
        // Skip if file doesn't exist (e.g. running in different environment)
        if (!vslPath.toFile().exists()) {
            System.out.println("Skipping testParseRealVslHtmlFile, docs/vsl.html not found.");
            return;
        }

        HtmlQuizDataParser.ParseResult result = HtmlQuizDataParser.parse(vslPath);

        assertTrue(result.isSuccess(), "Failed to parse vsl.html: " + result.getErrorMessage());
        
        if (result.getInvalidCount() > 0) {
            System.out.println("Invalid question error: " + result.getInvalidQuestions().get(0).getValidationError());
            System.out.println("Invalid question object: " + result.getInvalidQuestions().get(0));
        }
        
        assertEquals(234, result.getValidCount(), "Expected exactly 234 valid questions from vsl.html");
        assertEquals(0, result.getInvalidCount(), "Expected 0 invalid questions from vsl.html");

        // Spot check question 1
        ParsedQuizQuestion q1 = result.getQuestions().get(0);
        assertTrue(q1.getQuestion().contains("1. Chip vi điều khiển 8051"), "Q1 content mismatch");
        assertEquals(4, q1.getAnswers().size());
        assertEquals("c", q1.getCorrectAnswer());
        assertNotNull(q1.getExplanation(), "Q1 explanation should not be null");
        assertFalse(q1.getExplanation().isEmpty(), "Q1 explanation should not be empty");
    }

    @Test
    public void testStringLiteralEdgeCases() {
        // Unquoted key containing keyword, single quotes with escaped quotes, double quotes with escaped quotes
        String html = "<script>const quizData = [{\n" +
                "  question: 'Câu 1 có \"nháy kép\" và \\'nháy đơn\\'!',\n" +
                "  answers: {\n" +
                "    a: \"Đáp án A có \\\"nháy kép\\\"\",\n" +
                "    b: \"Đáp án B\"\n" +
                "  },\n" +
                "  correctAnswer: 'a'\n" +
                "}];</script>";

        HtmlQuizDataParser.ParseResult result = HtmlQuizDataParser.parseFromString(html);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getValidCount());
        
        ParsedQuizQuestion q = result.getQuestions().get(0);
        assertEquals("Câu 1 có \"nháy kép\" và 'nháy đơn'!", q.getQuestion());
        assertEquals("Đáp án A có \"nháy kép\"", q.getAnswers().get("a"));
    }
}
