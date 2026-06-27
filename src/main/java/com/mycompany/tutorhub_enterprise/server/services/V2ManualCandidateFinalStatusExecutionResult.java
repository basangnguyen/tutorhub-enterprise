package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2ManualCandidateFinalStatusExecutionResult {

    private final boolean success;
    private final boolean ready;
    private final boolean idempotent;
    private final String errorCode;
    private final int userId;
    private final int examId;
    private final String paperId;
    private final String attemptId;
    private final String finalStatus;
    private final Long finalStatusLedgerId;
    private final String executionStatus;
    private final List<String> warnings;
    private final List<String> blockingReasons;
    private final Instant executedAt;

    public V2ManualCandidateFinalStatusExecutionResult(Builder builder) {
        this.success = builder.success;
        this.ready = builder.ready;
        this.idempotent = builder.idempotent;
        this.errorCode = builder.errorCode;
        this.userId = builder.userId;
        this.examId = builder.examId;
        this.paperId = builder.paperId;
        this.attemptId = builder.attemptId;
        this.finalStatus = builder.finalStatus;
        this.finalStatusLedgerId = builder.finalStatusLedgerId;
        this.executionStatus = builder.executionStatus;
        this.warnings = builder.warnings;
        this.blockingReasons = builder.blockingReasons;
        this.executedAt = builder.executedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isIdempotent() {
        return idempotent;
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

    public String getFinalStatus() {
        return finalStatus;
    }

    public Long getFinalStatusLedgerId() {
        return finalStatusLedgerId;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public static class Builder {
        private boolean success;
        private boolean ready;
        private boolean idempotent;
        private String errorCode;
        private int userId;
        private int examId;
        private String paperId;
        private String attemptId;
        private String finalStatus;
        private Long finalStatusLedgerId;
        private String executionStatus;
        private List<String> warnings;
        private List<String> blockingReasons;
        private Instant executedAt;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder ready(boolean ready) {
            this.ready = ready;
            return this;
        }

        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
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

        public Builder finalStatus(String finalStatus) {
            this.finalStatus = finalStatus;
            return this;
        }

        public Builder finalStatusLedgerId(Long finalStatusLedgerId) {
            this.finalStatusLedgerId = finalStatusLedgerId;
            return this;
        }

        public Builder executionStatus(String executionStatus) {
            this.executionStatus = executionStatus;
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

        public Builder executedAt(Instant executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public V2ManualCandidateFinalStatusExecutionResult build() {
            return new V2ManualCandidateFinalStatusExecutionResult(this);
        }
    }
}
