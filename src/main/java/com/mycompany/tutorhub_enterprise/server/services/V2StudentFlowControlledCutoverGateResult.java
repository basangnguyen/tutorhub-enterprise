package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2StudentFlowControlledCutoverGateResult {

    private final boolean ready;
    private final boolean defaultFlowEnabled;
    private final String errorCode;
    private final String status;
    private final List<String> pendingChecks;
    private final List<String> blockingReasons;
    private final Instant checkedAt;

    public V2StudentFlowControlledCutoverGateResult(Builder builder) {
        this.ready = builder.ready;
        this.defaultFlowEnabled = builder.defaultFlowEnabled;
        this.errorCode = builder.errorCode;
        this.status = builder.status;
        this.pendingChecks = builder.pendingChecks;
        this.blockingReasons = builder.blockingReasons;
        this.checkedAt = builder.checkedAt;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isDefaultFlowEnabled() {
        return defaultFlowEnabled;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getPendingChecks() {
        return pendingChecks;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public static class Builder {
        private boolean ready;
        private boolean defaultFlowEnabled;
        private String errorCode;
        private String status;
        private List<String> pendingChecks;
        private List<String> blockingReasons;
        private Instant checkedAt;

        public Builder ready(boolean ready) {
            this.ready = ready;
            return this;
        }

        public Builder defaultFlowEnabled(boolean defaultFlowEnabled) {
            this.defaultFlowEnabled = defaultFlowEnabled;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder pendingChecks(List<String> pendingChecks) {
            this.pendingChecks = pendingChecks;
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

        public V2StudentFlowControlledCutoverGateResult build() {
            return new V2StudentFlowControlledCutoverGateResult(this);
        }
    }
}
