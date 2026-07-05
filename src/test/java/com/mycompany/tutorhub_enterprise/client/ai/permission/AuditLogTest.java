package com.mycompany.tutorhub_enterprise.client.ai.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogTest {

    @TempDir
    Path tempDir;

    @Test
    void readsRecentAuditEntriesNewestFirst() {
        AuditLog auditLog = new AuditLog(tempDir.resolve("audit.log"));
        auditLog.record("apply_patch", "p1", "src/A.java", "applied", "ok");
        auditLog.record("run_command", "c1", ".", "completed", "mvn compile");

        List<AuditLog.Entry> entries = auditLog.readRecent(10);

        assertEquals(2, entries.size());
        assertEquals("run_command", entries.get(0).getAction());
        assertEquals("apply_patch", entries.get(1).getAction());
        assertTrue(entries.get(0).getMessage().contains("mvn compile"));
    }
}
