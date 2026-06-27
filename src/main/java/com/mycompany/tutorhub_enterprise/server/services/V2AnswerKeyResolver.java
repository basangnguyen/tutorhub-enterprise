package com.mycompany.tutorhub_enterprise.server.services;

import java.util.Map;

/**
 * Interface to resolve correct options for an exam paper.
 * This is a server-side only component.
 * Pending schema verification for the actual answer key source.
 */
public interface V2AnswerKeyResolver {
    Map<Long, Long> resolveCorrectOptionIds(int paperId);
}
