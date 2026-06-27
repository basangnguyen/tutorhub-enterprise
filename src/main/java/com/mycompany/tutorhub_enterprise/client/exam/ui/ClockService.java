package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClockService {

    private final QuickSettingsStateStore stateStore;
    private ScheduledExecutorService scheduler;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ClockService(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void initialize() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TSE-Clock-Service");
            t.setDaemon(true);
            return t;
        });

        // Run immediately, then every 60 seconds
        scheduler.scheduleAtFixedRate(this::updateClock, 0, 60, TimeUnit.SECONDS);
    }

    public void terminate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void updateClock() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String timeStr = now.format(timeFormatter);
            String dateStr = now.format(dateFormatter);
            
            stateStore.updateClock(timeStr, dateStr);
        } catch (Exception e) {
            System.err.println("[TSE_CLOCK] Error updating clock: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
