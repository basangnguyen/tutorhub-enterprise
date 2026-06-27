package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class QuickSettingsStateStore {

    public interface StateChangeListener {
        void onStateChanged(QuickSettingsSnapshot newSnapshot);
    }

    private final AtomicReference<QuickSettingsSnapshot> snapshotRef;
    private final AtomicInteger versionCounter;
    private final List<StateChangeListener> listeners;

    public QuickSettingsStateStore(TSESecurityPolicy initialPolicy) {
        this.snapshotRef = new AtomicReference<>(QuickSettingsSnapshot.initial(initialPolicy));
        this.versionCounter = new AtomicInteger(0);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public QuickSettingsSnapshot getSnapshot() {
        return snapshotRef.get();
    }

    public void addListener(StateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public void shutdown() {
        listeners.clear();
    }

    private void notifyListeners(QuickSettingsSnapshot snapshot) {
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChanged(snapshot);
            } catch (Exception e) {
                System.err.println("[TSE_QS_STORE] Listener threw exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateState(Consumer<QuickSettingsSnapshot.Builder> mutator) {
        QuickSettingsSnapshot nextSnapshot = snapshotRef.updateAndGet(current -> {
            QuickSettingsSnapshot.Builder builder = new QuickSettingsSnapshot.Builder(current);
            builder.setSnapshotVersion(versionCounter.incrementAndGet());
            builder.setTimestamp(System.currentTimeMillis());
            mutator.accept(builder);
            return builder.build();
        });
        notifyListeners(nextSnapshot);
    }

    public void updateWifi(String status, String ssid, int signal, String error) {
        updateState(b -> {
            b.setWifiStatus(status);
            b.setWifiSsid(ssid);
            b.setWifiSignal(signal);
            b.setWifiError(error);
        });
    }

    public void updateVolume(boolean supported, boolean writable, int percent, boolean muted, int version, boolean pending, String error) {
        updateState(b -> {
            b.setVolumeSupported(supported);
            b.setVolumeWritable(writable);
            b.setVolumePercent(percent);
            b.setVolumeMuted(muted);
            b.setVolumeVersion(version);
            b.setVolumePending(pending);
            b.setVolumeError(error);
        });
    }

    public void updateBrightness(boolean supported, boolean writable, int percent, int version, boolean pending, String error) {
        System.out.println("[TSE_QS_STORE] updateBrightness supported=" + supported + " writable=" + writable + " percent=" + percent);
        updateState(b -> {
            b.setBrightnessSupported(supported);
            b.setBrightnessWritable(writable);
            b.setBrightnessPercent(percent);
            b.setBrightnessVersion(version);
            b.setBrightnessPending(pending);
            b.setBrightnessError(error);
        });
    }

    public void updateBattery(boolean hasBattery, int percent, boolean charging, boolean low, boolean critical) {
        updateState(b -> {
            b.setHasBattery(hasBattery);
            b.setBatteryPercent(percent);
            b.setBatteryCharging(charging);
            b.setBatteryLow(low);
            b.setBatteryCritical(critical);
        });
    }

    public void updateClock(String time, String date) {
        updateState(b -> {
            b.setClockTime(time);
            b.setClockDate(date);
        });
    }

    public void updateInputMode(String mode) {
        updateState(b -> b.setInputMode(mode));
    }

    public void updatePolicy(TSESecurityPolicy policy) {
        updateState(b -> {
            b.setWifiMode(policy.getWifiMode().name());
            b.setRefreshEnabled(policy.getRefreshPolicy() == TSESecurityPolicy.RefreshPolicy.ENABLED);
            b.setExitAllowed(policy.getExitPolicy() == TSESecurityPolicy.ExitPolicy.ALLOWED);
        });
    }
}
