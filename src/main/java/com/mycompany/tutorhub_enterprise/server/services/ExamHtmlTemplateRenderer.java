package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.exam.ExamOptionViewDTO;
import com.mycompany.tutorhub_enterprise.models.exam.ExamQuestionViewDTO;
import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.models.exam.QuestionOption;
import com.mycompany.tutorhub_enterprise.server.ServerConfig;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExamHtmlTemplateRenderer {

    private static String templateHtmlCache = null;

    private static boolean isNewTemplateEnabled() {
        return ServerConfig.isEnabled("TSE_EXAM_NEW_TEMPLATE", "tse.exam.newHtmlTemplate.enabled", true);
    }

    private static String loadTemplate() throws Exception {
        if (templateHtmlCache != null) {
            return templateHtmlCache;
        }
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("tse/exam-template.html")) {
            if (is == null) {
                throw new Exception("Template file tse/exam-template.html not found on classpath");
            }
            byte[] bytes = is.readAllBytes();
            templateHtmlCache = new String(bytes, StandardCharsets.UTF_8);
            return templateHtmlCache;
        }
    }

    /**
     * Attempts to render the new HTML template.
     * Returns null if the template feature is disabled or if loading/rendering fails.
     */
    public static String renderExam(int sessionId, int examId, String examTitle, List<Question> questions) {
        if (!isNewTemplateEnabled()) {
            return null;
        }

        try {
            String template = loadTemplate();

            // Prepare DTOs for JSON serialization
            List<ExamQuestionViewDTO> dtoList = new ArrayList<>();
            for (Question q : questions) {
                ExamQuestionViewDTO dto = new ExamQuestionViewDTO();
                dto.questionId = q.id;
                // Parse question text from q.content (which might be JSON)
                String qText = "Câu hỏi " + (dtoList.size() + 1) + ": ";
                if (q.content != null && !q.content.trim().isEmpty()) {
                    try {
                        if (q.content.trim().startsWith("{")) {
                            java.util.Map<String, Object> contentMap = new Gson().fromJson(q.content, java.util.Map.class);
                            if (contentMap.containsKey("text")) {
                                qText += contentMap.get("text");
                            } else {
                                qText += q.content;
                            }
                        } else {
                            qText += q.content;
                        }
                    } catch (Exception ex) {
                        qText += q.content;
                    }
                } else {
                    qText += "(Nội dung trống)";
                }
                dto.question = qText;
                dto.questionType = q.questionType;
                
                List<QuestionOption> dbOptions = QuestionDAO.getOptionsByQuestionId(q.id);
                if (dbOptions != null && !dbOptions.isEmpty()) {
                    dto.options = new ArrayList<>();
                    for (QuestionOption o : dbOptions) {
                        ExamOptionViewDTO optDto = new ExamOptionViewDTO();
                        optDto.optionId = o.id;
                        optDto.label = o.optionLabel;
                        optDto.text = o.content;
                        dto.options.add(optDto);
                    }
                }
                dtoList.add(dto);
            }

            Gson gson = new Gson();
            String quizDataJson = gson.toJson(dtoList);

            // Inject into template
            String html = template.replace("__SESSION_ID__", String.valueOf(sessionId))
                                  .replace("__EXAM_ID__", String.valueOf(examId))
                                  .replace("__EXAM_TITLE__", examTitle != null ? examTitle : "Bài thi")
                                  .replace("__QUIZ_DATA_JSON__", quizDataJson);

            return html;

        } catch (Exception ex) {
            System.err.println("[EXAM_RENDERER] Failed to render exam template: " + ex.getMessage());
            ex.printStackTrace();
            return null; // Fallback will be triggered in ExamController
        }
    }


}
