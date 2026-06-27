package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2ManualCandidateResultHandoffVerificationResult {

    private final boolean success;
    private final boolean ready;
    private final String errorCode;
    private final int userId;
    private final int examId;
    private final String paperId;
    private final String attemptId;
    private final Long examResultId;
    private final Long publicationLedgerId;
    private final Long finalStatusLedgerId;
    private final String handoffStatus;
    private final List<String> warnings;
    private final List<String> blockingReasons;
    private final Instant checkedAt;

    public V2ManualCandidateResultHandoffVerificationResult(Builder builder) {
        this.success = builder.success;
        this.ready = builder.ready;
        this.errorCode = builder.errorCode;
        this.userId = builder.userId;
        this.examId = builder.examId;
        this.paperId = builder.paperId;
        this.attemptId = builder.attemptId;
        this.examResultId = builder.examResultId;
        this.publicationLedgerId = builder.publicationLedgerId;
        this.finalStatusLedgerId = builder.finalStatusLedgerId;
        this.handoffStatus = builder.handoffStatus;
        this.warnings = builder.warnings;
        this.blockingReasons = builder.blockingReasons;
        this.checkedAt = builder.checkedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isReady() {
        return ready;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getUserId() {
        return userId;
    }

    public int getExamId() {
        return examId;
    }

    public String getPaperId() {
        return paperId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public Long getExamResultId() {
        return examResultId;
    }

    public Long getPublicationLedgerId() {
        return publicationLedgerId;
    }

    public Long getFinalStatusLedgerId() {
        return finalStatusLedgerId;
    }

    public String getHandoffStatus() {
        return handoffStatus;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public static class Builder {
        private boolean success;
        private boolean ready;
        private String errorCode;
        private int userId;
        private int examId;
        private String paperId;
        private String attemptId;
        private Long examResultId;
        private Long publicationLedgerId;
        private Long finalStatusLedgerId;
        private String handoffStatus;
        private List<String> warnings;
        private List<String> blockingReasons;
        private Instant checkedAt;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder ready(boolean ready) {
            this.ready = ready;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder examId(int examId) {
            this.examId = examId;
            return this;
        }

        public Builder paperId(String paperId) {
            this.paperId = paperId;
            return this;
        }

        public Builder attemptId(String attemptId) {
            this.attemptId = attemptId;
            return this;
        }

        public Builder examResultId(Long examResultId) {
            this.examResultId = examResultId;
            return this;
        }

        public Builder publicationLedgerId(Long publicationLedgerId) {
            this.publicationLedgerId = publicationLedgerId;
            return this;
        }

        public Builder finalStatusLedgerId(Long finalStatusLedgerId) {
            this.finalStatusLedgerId = finalStatusLedgerId;
            return this;
        }

        public Builder handoffStatus(String handoffStatus) {
            this.handoffStatus = handoffStatus;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder blockingReasons(List<String> blockingReasons) {
            this.blockingReasons = blockingReasons;
            return this;
        }

        public Builder checkedAt(Instant checkedAt) {
            this.checkedAt = checkedAt;
            return this;
        }

        public V2ManualCandidateResultHandoffVerificationResult build() {
            return new V2ManualCandidateResultHandoffVerificationResult(this);
        }
    }
}
