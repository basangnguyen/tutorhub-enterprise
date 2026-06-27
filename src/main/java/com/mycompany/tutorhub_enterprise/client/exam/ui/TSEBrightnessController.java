package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEBrightnessController {

    private static final BrightnessService service = new BrightnessService();
    private static boolean initialized = false;

    public TSEBrightnessController() {
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            service.initialize();
            initialized = true;
        }
    }

    public TSEBrightnessStatus getStatus() {
        ensureInitialized();
        return service.getStatus();
    }

    public TSEBrightnessStatus setBrightness(int percent) {
        ensureInitialized();
        return service.setBrightness(percent, null);
    }

    public void shutdownNowNoBlock() {
        service.terminate();
        initialized = false;
    }
}
