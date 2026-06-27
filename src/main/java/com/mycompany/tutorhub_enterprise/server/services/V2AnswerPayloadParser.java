package com.mycompany.tutorhub_enterprise.server.services;

import java.util.Map;

/**
 * Interface to parse the payloadJson into a map of Question ID -> Selected Option ID.
 * This is a server-side only component.
 * Pending schema verification for the actual payloadJson format.
 */
public interface V2AnswerPayloadParser {
    Map<Long, Long> extractAnswers(String payloadJson);
}
