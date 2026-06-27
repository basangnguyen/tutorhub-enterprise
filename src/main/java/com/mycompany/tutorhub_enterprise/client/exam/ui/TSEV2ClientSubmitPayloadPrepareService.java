package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;

public class TSEV2ClientSubmitPayloadPrepareService {

    private final TSEV2AnswerDraftSnapshotService snapshotService;
    private final TSEV2SubmitPayloadService payloadService;

    public TSEV2ClientSubmitPayloadPrepareService() {
        this.snapshotService = new TSEV2AnswerDraftSnapshotService();
        this.payloadService = new TSEV2SubmitPayloadService();
    }

    public TSEV2ClientSubmitPayloadPrepareService(
            TSEV2AnswerDraftSnapshotService snapshotService,
            TSEV2SubmitPayloadService payloadService) {
        this.snapshotService = snapshotService;
        this.payloadService = payloadService;
    }

    public TSEV2SubmitPayload preparePayload(TSEV2ReadOnlyExamRenderModel model, TSEV2AnswerSelectionState state) {
        if (model == null) {
            throw new IllegalArgumentException("Render model is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("Selection state is required");
        }

        // 1. Create RAM-only snapshot
        TSEV2AnswerDraftSnapshot snapshot = snapshotService.createSnapshot(model, state);
        
        // 2. Create and hash payload
        TSEV2SubmitPayload payload = payloadService.createPayload(model, snapshot);
        
        // 3. Strict safe check
        payloadService.validatePayloadSafe(payload);
        payloadService.validatePayloadMatchesRenderModel(payload, model);
        
        return payload;
    }
}
