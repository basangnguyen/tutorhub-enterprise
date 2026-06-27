package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TSEV2SubmitPayloadService {
    
    private final Gson gson;

    public TSEV2SubmitPayloadService() {
        this.gson = new GsonBuilder().create();
    }

    public TSEV2SubmitPayload createPayload(
            TSEV2ReadOnlyExamRenderModel model,
            TSEV2AnswerDraftSnapshot snapshot) {

        TSEV2SubmitPayload payload = new TSEV2SubmitPayload();
        payload.setPayloadVersion("1.0");
        payload.setFlow("PAPER_START_V2");
        payload.setExamId(model.getExamId());
        payload.setPaperId(model.getPaperId());
        payload.setAttemptId(snapshot.getAttemptId());
        payload.setPackageHash(model.getPackageHash());
        payload.setQuestionCount(model.getQuestionCount());
        
        List<TSEV2SubmitAnswerItem> answers = new ArrayList<>();
        Set<Integer> answeredQuestions = new HashSet<>();
        
        if (snapshot.getAnswers() != null) {
            for (TSEV2AnswerDraftItem draftItem : snapshot.getAnswers()) {
                if (answeredQuestions.contains(draftItem.getQuestionId())) {
                    continue; // Normalize safely
                }
                answers.add(new TSEV2SubmitAnswerItem(draftItem.getQuestionId(), draftItem.getSelectedOptionId(), draftItem.getAnsweredAt()));
                answeredQuestions.add(draftItem.getQuestionId());
            }
        }
        
        payload.setAnswers(answers);
        payload.setAnsweredCount(answers.size());
        payload.setUnansweredCount(payload.getQuestionCount() - answers.size());
        payload.setComplete(payload.getAnsweredCount() == payload.getQuestionCount());
        payload.setDraftSnapshotHash(snapshot.getSnapshotHash());
        payload.setPreparedAt(Instant.now().toString());
        
        payload.setPayloadHash(computePayloadHash(payload));
        
        return payload;
    }

    public String toJson(TSEV2SubmitPayload payload) {
        return gson.toJson(payload);
    }

    public String computePayloadHash(TSEV2SubmitPayload payload) {
        try {
            String oldHash = payload.getPayloadHash();
            payload.setPayloadHash(null);
            
            String json = gson.toJson(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            payload.setPayloadHash(oldHash);
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_HASH_FAILED", e);
        }
    }

    public void validatePayloadSafe(TSEV2SubmitPayload payload) {
        String json = toJson(payload);
        
        if (json.contains("sessionToken")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains sessionToken");
        if (json.contains("keyB64")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains keyB64");
        if (json.contains("plaintextJson") || json.contains("plaintext")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains plaintext");
        if (json.contains("isCorrect")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains isCorrect");
        if (json.contains("answerKey") || json.contains("correctOption")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains correctOption");
        if (json.contains("password") || json.contains("passwordHash")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains password");
        if (json.contains("score") || json.contains("gradingResult")) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_UNSAFE: contains score");
    }

    public void validatePayloadMatchesRenderModel(
            TSEV2SubmitPayload payload,
            TSEV2ReadOnlyExamRenderModel model) {
        
        if (payload.getExamId() != model.getExamId()) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: examId");
        if (payload.getPaperId() != model.getPaperId()) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: paperId");
        if (model.getPackageHash() != null && !model.getPackageHash().equals(payload.getPackageHash())) {
            throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: packageHash");
        }
        if (payload.getQuestionCount() != model.getQuestionCount()) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: questionCount");
        if (payload.getAnswers() == null) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: answers is null");
        if (payload.getAnsweredCount() != payload.getAnswers().size()) throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: answeredCount");
        if (payload.getUnansweredCount() != (payload.getQuestionCount() - payload.getAnsweredCount())) {
            throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: unansweredCount");
        }
        if (payload.isComplete() != (payload.getAnsweredCount() == payload.getQuestionCount())) {
            throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: complete");
        }
        
        Set<Integer> seenQuestions = new HashSet<>();
        
        for (TSEV2SubmitAnswerItem item : payload.getAnswers()) {
            if (!seenQuestions.add(item.getQuestionId())) {
                throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_CONTEXT_MISMATCH: duplicate answer");
            }
            
            TSEV2ReadOnlyQuestionView qView = null;
            for (TSEV2ReadOnlyQuestionView q : model.getQuestions()) {
                if (q.getId() == item.getQuestionId()) {
                    qView = q;
                    break;
                }
            }
            if (qView == null) {
                throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_INVALID_OPTION: question not found");
            }
            
            boolean validOption = false;
            for (TSEV2ReadOnlyOptionView opt : qView.getOptions()) {
                if (opt.getId() == item.getSelectedOptionId()) {
                    validOption = true;
                    break;
                }
            }
            if (!validOption) {
                throw new RuntimeException("ERROR_SUBMIT_PAYLOAD_INVALID_OPTION: option not found");
            }
        }
    }
}
