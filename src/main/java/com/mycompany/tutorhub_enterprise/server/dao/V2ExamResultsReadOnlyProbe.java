package com.mycompany.tutorhub_enterprise.server.dao;

public interface V2ExamResultsReadOnlyProbe {
    boolean existsResultForAttempt(String attemptId);
}
