package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BatteryService {

    private final QuickSettingsStateStore stateStore;
    private ScheduledExecutorService scheduler;

    public BatteryService(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void initialize() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TSE-Battery-Service");
            t.setDaemon(true);
            return t;
        });

        // Run immediately, then every 5 seconds
        scheduler.scheduleAtFixedRate(this::refreshNow, 0, 5, TimeUnit.SECONDS);
    }

    public void terminate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void refreshNow() {
        try {
            TSEBatteryStatusProvider.BatteryStatus batStatus = TSEBatteryStatusProvider.getStatus();
            boolean low = batStatus.hasBattery && batStatus.percent <= 20;
            boolean critical = batStatus.hasBattery && batStatus.percent <= 10;
            
            stateStore.updateBattery(batStatus.hasBattery, batStatus.percent, batStatus.isCharging, low, critical);
        } catch (Exception e) {
            System.err.println("[TSE_BATTERY] Error reading battery: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
