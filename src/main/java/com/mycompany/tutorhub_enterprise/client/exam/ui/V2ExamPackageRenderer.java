package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;

public class V2ExamPackageRenderer {

    public static String renderHtml(V2ExamHandoffBundle bundle) throws Exception {
        validateSafeForRender(bundle);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; margin: 10px; color: #333;'>");
        
        sb.append("<div style='background-color: #f5f5f5; padding: 10px; border-radius: 5px; margin-bottom: 20px;'>");
        sb.append("<h2 style='margin-top: 0;'>TutorHub Secure Exam V2 - Package Preview</h2>");
        sb.append("<ul>");
        sb.append("<li><b>Exam ID:</b> ").append(bundle.examId).append("</li>");
        sb.append("<li><b>Paper ID:</b> ").append(bundle.paperId).append("</li>");
        if (bundle.attemptId != null) {
            sb.append("<li><b>Attempt ID:</b> ").append(bundle.attemptId).append("</li>");
        }
        if (bundle.deadlineAt != null) {
            sb.append("<li><b>Deadline:</b> ").append(bundle.deadlineAt).append("</li>");
        }
        sb.append("<li><b>Duration:</b> ").append(bundle.durationMinutes).append(" mins</li>");
        sb.append("<li><b>Question Count:</b> ").append(bundle.questionCount).append("</li>");
        sb.append("<li><b>Total Score:</b> ").append(bundle.totalScore).append("</li>");
        sb.append("<li><b>Package Hash:</b> ").append(bundle.packageHash).append("</li>");
        sb.append("</ul>");
        sb.append("</div>");

        sb.append("<h3>Questions:</h3>");
        int index = 1;
        for (V2ExamHandoffQuestion q : bundle.questions) {
            sb.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #ddd;'>");
            sb.append("<b>Question ").append(index++).append(" (ID: ").append(q.questionId).append("):</b><br/>");
            
            String content = q.content != null ? q.content.replace("\n", "<br/>") : "";
            sb.append("<div style='margin-top: 5px; margin-bottom: 10px;'>").append(content).append("</div>");
            
            if (q.options != null && !q.options.isEmpty()) {
                sb.append("<ul style='list-style-type: none; padding-left: 20px;'>");
                for (V2ExamHandoffOption opt : q.options) {
                    sb.append("<li style='margin-bottom: 5px;'><label><input type='radio' disabled> ");
                    sb.append(opt.content != null ? opt.content : "");
                    sb.append("</label></li>");
                }
                sb.append("</ul>");
            }
            sb.append("</div>");
        }
        
        sb.append("</body></html>");
        
        String html = sb.toString();
        validateStringSafe(html, bundle.sessionToken);
        return html;
    }

    public static String renderTextSummary(V2ExamHandoffBundle bundle) throws Exception {
        validateSafeForRender(bundle);

        StringBuilder sb = new StringBuilder();
        sb.append("--- V2 EXAM PACKAGE PREVIEW ---\n");
        sb.append("Exam ID: ").append(bundle.examId).append("\n");
        sb.append("Paper ID: ").append(bundle.paperId).append("\n");
        if (bundle.attemptId != null) sb.append("Attempt ID: ").append(bundle.attemptId).append("\n");
        sb.append("Duration: ").append(bundle.durationMinutes).append(" mins\n");
        sb.append("Questions: ").append(bundle.questionCount).append("\n");
        sb.append("-------------------------------\n");

        int index = 1;
        for (V2ExamHandoffQuestion q : bundle.questions) {
            sb.append("Q").append(index++).append(" [ID:").append(q.questionId).append("]: ");
            sb.append(q.content != null ? q.content.replace("\n", " ") : "").append("\n");
            if (q.options != null) {
                for (V2ExamHandoffOption opt : q.options) {
                    sb.append("  - ").append(opt.content).append("\n");
                }
            }
        }
        
        String text = sb.toString();
        validateStringSafe(text, bundle.sessionToken);
        return text;
    }

    public static void validateSafeForRender(V2ExamHandoffBundle bundle) throws Exception {
        if (bundle == null) throw new Exception("Bundle is null");
        if (bundle.questions == null || bundle.questions.isEmpty()) throw new Exception("questions array is empty");
        if (bundle.questionCount <= 0) throw new Exception("questionCount <= 0");
        if (bundle.packageHash == null || bundle.packageHash.isEmpty()) throw new Exception("packageHash is empty");
    }
    
    private static void validateStringSafe(String content, String rawSessionToken) throws Exception {
        if (content.contains("isCorrect") || content.contains("answerKey") || content.contains("correctOption") || content.contains("grading_config")) {
            throw new Exception("SECURITY VIOLATION: Rendered content contains answer fields");
        }
        if (rawSessionToken != null && !rawSessionToken.isEmpty() && content.contains(rawSessionToken)) {
            throw new Exception("SECURITY VIOLATION: Rendered content contains raw session token");
        }
        if (content.contains("password") || content.contains("passwordHash")) {
            throw new Exception("SECURITY VIOLATION: Rendered content contains password fields");
        }
    }
}
