package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TSEChildLaunchArgsParserTest {

    @Test
    public void testLegacyMode() {
        String[] args = {"--context", "ctx.enc", "--output", "out.enc", "--key", "abcxyz"};
        TSEChildLaunchArgs parsed = TSEChildLaunchArgsParser.parse(args);
        
        assertEquals(TSEChildLaunchArgs.Mode.LEGACY, parsed.getMode());
        assertEquals("ctx.enc", parsed.getLegacyContextPath());
        assertEquals("out.enc", parsed.getLegacyOutputPath());
        assertEquals("abcxyz", parsed.getLegacyKeyBase64());
        assertNull(parsed.getV2HandoffMetaPath());
    }

    @Test
    public void testV2DebugMode() {
        String[] args = {"--v2-handoff-meta", "meta.json", "--v2-handoff-enc", "pkg.enc", "--v2-debug-only"};
        TSEChildLaunchArgs parsed = TSEChildLaunchArgsParser.parse(args);
        
        assertEquals(TSEChildLaunchArgs.Mode.V2_DEBUG, parsed.getMode());
        assertEquals("meta.json", parsed.getV2HandoffMetaPath());
        assertEquals("pkg.enc", parsed.getV2HandoffEncPath());
        assertTrue(parsed.isV2DebugOnly());
        assertNull(parsed.getLegacyContextPath());
        assertNull(parsed.getLegacyKeyBase64()); // Key is not extracted from args in V2
    }

    @Test
    public void testInvalidMode() {
        String[] args = {"--context", "ctx.enc", "--output", "out.enc"}; // Missing key
        TSEChildLaunchArgs parsed = TSEChildLaunchArgsParser.parse(args);
        
        assertEquals(TSEChildLaunchArgs.Mode.INVALID, parsed.getMode());
        assertNotNull(parsed.getErrorMessage());
    }

    @Test
    public void testEmptyArgs() {
        String[] args = {};
        TSEChildLaunchArgs parsed = TSEChildLaunchArgsParser.parse(args);
        
        assertEquals(TSEChildLaunchArgs.Mode.INVALID, parsed.getMode());
    }
}
