package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2RealSubmitAttemptStatusTransitionGateResult {
    private boolean ready;
    private String statusTransitionGate;
    private List<String> blockingReasons;
    private String errorCode;

    public V2RealSubmitAttemptStatusTransitionGateResult() {
        this.ready = false;
        this.statusTransitionGate = "NOT_READY";
        this.blockingReasons = new ArrayList<>();
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getStatusTransitionGate() {
        return statusTransitionGate;
    }

    public void setStatusTransitionGate(String statusTransitionGate) {
        this.statusTransitionGate = statusTransitionGate;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public void addBlockingReason(String reason) {
        this.blockingReasons.add(reason);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
