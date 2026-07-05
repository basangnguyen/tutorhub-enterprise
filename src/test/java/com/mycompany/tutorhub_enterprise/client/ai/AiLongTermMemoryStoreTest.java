package com.mycompany.tutorhub_enterprise.client.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiLongTermMemoryStoreTest {

    @Test
    void autoMemoryRejectsSensitiveNotesAndDeduplicates() {
        AiLongTermMemoryStore store = new AiLongTermMemoryStore("phase9_test_user", "phase9_test_conversation");
        store.clear();

        AiLongTermMemoryStore.MemoryWriteResult sensitive =
                store.addAuto("The Hugging Face token is hf_example_secret", "test");
        AiLongTermMemoryStore.MemoryWriteResult saved =
                store.addAuto("User prefers concise Vietnamese engineering updates.", "test");
        AiLongTermMemoryStore.MemoryWriteResult duplicate =
                store.addAuto("User prefers concise Vietnamese engineering updates.", "test");

        assertFalse(sensitive.isSaved());
        assertTrue(saved.isSaved());
        assertFalse(duplicate.isSaved());
        assertEquals(1, store.snapshot().getCount());
        assertTrue(store.snapshot().getContext().contains("concise Vietnamese"));

        store.clear();
    }

    @Test
    void updatesAndRemovesIndividualMemoryNotes() {
        AiLongTermMemoryStore store = new AiLongTermMemoryStore("phase91_test_user", "phase91_test_conversation");
        store.clear();
        AiLongTermMemoryStore.MemoryWriteResult saved =
                store.addAuto("Original stable project fact.", "test");
        String id = saved.getSnapshot().getItems().get(0).getId();

        AiLongTermMemoryStore.MemoryWriteResult updated =
                store.update(id, "Updated stable project fact.");
        AiLongTermMemoryStore.MemoryWriteResult removed = store.remove(id);

        assertTrue(updated.isSaved());
        assertTrue(updated.getSnapshot().getContext().contains("Updated stable project fact"));
        assertTrue(removed.isSaved());
        assertEquals(0, removed.getSnapshot().getCount());

        store.clear();
    }
}
