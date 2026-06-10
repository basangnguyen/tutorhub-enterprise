package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageServiceTest {

    @Test
    void parsePayloadSupportsClientMessageIdProtocol() {
        ChatMessageService.SendChatPayload payload = ChatMessageService.parsePayload(
                "123|TEXT|client-abc|hello|with pipe"
        );

        assertTrue(payload.isValid());
        assertEquals(123, payload.conversationId());
        assertEquals("TEXT", payload.messageType());
        assertEquals("client-abc", payload.clientMessageId());
        assertEquals("hello|with pipe", payload.content());
    }

    @Test
    void parsePayloadKeepsLegacyThreePartProtocolCompatible() {
        ChatMessageService.SendChatPayload payload = ChatMessageService.parsePayload(
                "123|TEXT|legacy content"
        );

        assertTrue(payload.isValid());
        assertEquals(123, payload.conversationId());
        assertEquals("TEXT", payload.messageType());
        assertEquals("", payload.clientMessageId());
        assertEquals("legacy content", payload.content());
    }
}
