package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkService {

    private final QuickSettingsStateStore stateStore;
    private ScheduledExecutorService scheduler;

    public NetworkService(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void initialize() {
        System.out.println("[TSE_NETWORK_SERVICE] initialize");
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TSE-Network-Service");
            t.setDaemon(true);
            return t;
        });

        // Run immediately, then every 15 seconds
        scheduler.scheduleAtFixedRate(this::refreshNow, 0, 15, TimeUnit.SECONDS);
    }

    public void terminate() {
        System.out.println("[TSE_NETWORK_SERVICE] terminate");
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void refreshNow() {
        try {
            System.out.println("[TSE_NETWORK_SERVICE] refreshNow");
            QuickSettingsSnapshot snapshot = stateStore.getSnapshot();
            
            if ("DISABLED".equals(snapshot.wifiMode)) {
                System.out.println("[TSE_NETWORK_SERVICE] disabled by policy");
                stateStore.updateWifi("DISABLED", "", 0, "WiFi is disabled by policy");
                return;
            }

            // Using Option A: TSENetworkStatusProvider for safe read-only/scan-only
            TSENetworkStatusProvider.NetworkStatus netStatus = TSENetworkStatusProvider.getStatus();
            
            if (!netStatus.hasWifiAdapter) {
                System.out.println("[TSE_NETWORK_SERVICE] no adapter");
                stateStore.updateWifi("NO_ADAPTER", "", 0, null);
                return;
            }

            if (netStatus.isConnected) {
                System.out.println("[TSE_NETWORK_SERVICE] status connected ssid=" + netStatus.ssid);
                stateStore.updateWifi("CONNECTED", netStatus.ssid, netStatus.signalPercent, null);
            } else {
                System.out.println("[TSE_NETWORK_SERVICE] status disconnected");
                stateStore.updateWifi("DISCONNECTED", "", 0, null);
            }
        } catch (Exception e) {
            System.err.println("[TSE_NETWORK_SERVICE] error=" + e.getMessage());
            e.printStackTrace();
            stateStore.updateWifi("ERROR", "", 0, "Error reading network status");
        }
    }

    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
