package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitLegacyFallbackRuntimeGuardService {

    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2FinalAttemptStatusDAO attemptStatusDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ExamResultsReadOnlyProbeImpl examResultDAO;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;
    private final V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO;

    public V2StudentSubmitLegacyFallbackRuntimeGuardService() {
        this(new V2SubmitRecordDAO(), new V2FinalAttemptStatusDAO(), new V2ScoreDraftDAO(),
             new V2OfficialResultDraftDAO(), new V2ExamResultsReadOnlyProbeImpl(),
             new V2ResultPublicationLedgerDAO(), new V2FinalAttemptStatusLedgerDAO());
    }

    public V2StudentSubmitLegacyFallbackRuntimeGuardService(
            V2SubmitRecordDAO submitRecordDAO,
            V2FinalAttemptStatusDAO attemptStatusDAO,
            V2ScoreDraftDAO scoreDraftDAO,
            V2OfficialResultDraftDAO officialResultDraftDAO,
            V2ExamResultsReadOnlyProbeImpl examResultDAO,
            V2ResultPublicationLedgerDAO publicationLedgerDAO,
            V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO) {
        this.submitRecordDAO = submitRecordDAO;
        this.attemptStatusDAO = attemptStatusDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.examResultDAO = examResultDAO;
        this.publicationLedgerDAO = publicationLedgerDAO;
        this.finalStatusLedgerDAO = finalStatusLedgerDAO;
    }

    public V2StudentSubmitLegacyFallbackRuntimeGuardResult checkGuard(int userId, String attemptId) {
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = new V2StudentSubmitLegacyFallbackRuntimeGuardResult();
        result.setCheckedAt(Instant.now());
        List<String> blockingReasons = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentSubmitLegacyFallbackRuntimeGuardEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setFallbackAllowed(false);
            result.setFallbackForbidden(true);
            result.setFallbackReason("FEATURE_DISABLED");
            return result;
        }

        boolean hasWrite = false;
        try {
            if (submitRecordDAO.findLatestByAttemptId(attemptId).isPresent()) hasWrite = true;
            if (!hasWrite && "SUBMITTED".equals(attemptStatusDAO.findAttemptStatus(attemptId).orElse(""))) hasWrite = true;
            if (!hasWrite && "COMPLETED".equals(attemptStatusDAO.findAttemptStatus(attemptId).orElse(""))) hasWrite = true;
            
            if (!hasWrite && examResultDAO.existsResultForAttempt(attemptId)) hasWrite = true;
            if (!hasWrite && finalStatusLedgerDAO.existsByAttemptId(attemptId)) hasWrite = true;
            if (!hasWrite && !publicationLedgerDAO.findByAttemptId(attemptId).isEmpty()) hasWrite = true;
        } catch (Exception e) {
            hasWrite = true; // Safe assumption
        }

        if (hasWrite) {
            result.setSuccess(true);
            result.setReady(true);
            result.setFallbackAllowed(false);
            result.setFallbackForbidden(true);
            result.setFailureZone("POST_WRITE_FAILURE");
            result.setTargetRoute("SAFE_ERROR_RETURN");
            result.setFallbackReason("V2_WRITE_STARTED_FALLBACK_FORBIDDEN");
        } else {
            result.setSuccess(true);
            result.setReady(true);
            result.setFallbackAllowed(true);
            result.setFallbackForbidden(false);
            result.setFailureZone("PRE_WRITE_FAILURE");
            result.setTargetRoute("LEGACY_V1_STUDENT_SUBMIT");
            result.setFallbackReason("PRE_WRITE_FAILURE_FALLBACK_ALLOWED");
        }

        return result;
    }
}
