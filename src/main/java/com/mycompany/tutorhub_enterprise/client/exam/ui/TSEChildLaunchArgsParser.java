package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEChildLaunchArgsParser {
    public static TSEChildLaunchArgs parse(String[] args) {
        TSEChildLaunchArgs result = new TSEChildLaunchArgs();
        
        for (int i = 0; i < args.length; i++) {
            if ("--context".equals(args[i]) && i + 1 < args.length) {
                result.setLegacyContextPath(args[i + 1]);
            } else if ("--output".equals(args[i]) && i + 1 < args.length) {
                result.setLegacyOutputPath(args[i + 1]);
            } else if ("--key".equals(args[i]) && i + 1 < args.length) {
                result.setLegacyKeyBase64(args[i + 1]);
            } else if ("--v2-handoff-meta".equals(args[i]) && i + 1 < args.length) {
                result.setV2HandoffMetaPath(args[i + 1]);
            } else if ("--v2-handoff-enc".equals(args[i]) && i + 1 < args.length) {
                result.setV2HandoffEncPath(args[i + 1]);
            } else if ("--v2-debug-only".equals(args[i])) {
                result.setV2DebugOnly(true);
            }
        }
        
        if (result.getV2HandoffMetaPath() != null) {
            result.setMode(TSEChildLaunchArgs.Mode.V2_DEBUG);
        } else if (result.getLegacyContextPath() != null && result.getLegacyOutputPath() != null && result.getLegacyKeyBase64() != null) {
            result.setMode(TSEChildLaunchArgs.Mode.LEGACY);
        } else {
            result.setMode(TSEChildLaunchArgs.Mode.INVALID);
            result.setErrorMessage("Missing required arguments for either Legacy or V2 mode. Legacy requires --context, --output, --key. V2 requires --v2-handoff-meta.");
        }
        
        return result;
    }
}
