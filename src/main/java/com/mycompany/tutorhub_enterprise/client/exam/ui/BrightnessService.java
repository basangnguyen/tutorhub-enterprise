package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BrightnessService {

    private static final String TAG = "[TSE_BRIGHTNESS_SERVICE]";
    private ExecutorService executor;
    private Process activeProcess = null;
    
    private boolean isSupported = false;
    private boolean isWritable = false;
    private int currentPercent = -1;
    
    private long lastBrightnessCacheAt = 0;
    private static final long CACHE_TTL_MS = 10000;
    
    private final QuickSettingsStateStore stateStore;
    
    public BrightnessService() {
        this(null);
    }
    
    public BrightnessService(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
    }
    
    public void initialize() {
        System.out.println(TAG + " initialize");
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TSE-BrightnessService");
            t.setDaemon(true);
            return t;
        });
        
        // Adaptive support detection
        TSEBrightnessStatus initialStatus = executeWithTimeout(this::getBrightnessInternal, 3000);
        
        if (initialStatus != null && initialStatus.supported) {
            isSupported = true;
            currentPercent = initialStatus.percent;
            
            // Probe test for writable
            int probeValue = currentPercent;
            if (probeValue < 20) probeValue = 20; // Don't set below 20
            
            final int finalProbe = probeValue;
            TSEBrightnessStatus probeResult = executeWithTimeout(() -> setBrightnessInternal(finalProbe), 3000);
            if (probeResult != null && probeResult.supported && probeResult.writable) {
                isWritable = true;
            } else {
                isWritable = false;
            }
        } else {
            isSupported = false;
            isWritable = false;
        }
        
        System.out.println(TAG + " supported=" + isSupported);
        System.out.println(TAG + " writable=" + isWritable);
        
        updateStore();
    }
    
    public void terminate() {
        System.out.println(TAG + " terminate");
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (activeProcess != null) {
            try {
                activeProcess.destroyForcibly();
            } catch (Exception ignored) {}
            activeProcess = null;
        }
    }
    
    public void refreshNow() {
        if (!isSupported) return;
        
        TSEBrightnessStatus status = executeWithTimeout(this::getBrightnessInternal, 3000);
        if (status != null && status.supported) {
            currentPercent = status.percent;
            lastBrightnessCacheAt = System.currentTimeMillis();
            updateStore();
        }
    }
    
    public TSEBrightnessStatus getStatus() {
        if (!isSupported) {
            return new TSEBrightnessStatus(false, false, -1, "UNSUPPORTED", "Device does not support brightness control");
        }
        
        if (System.currentTimeMillis() - lastBrightnessCacheAt < CACHE_TTL_MS) {
            return new TSEBrightnessStatus(true, isWritable, currentPercent, "WMI", "Success");
        }
        
        System.out.println(TAG + " getStatus");
        TSEBrightnessStatus status = executeWithTimeout(this::getBrightnessInternal, 3000);
        if (status != null && status.supported) {
            currentPercent = status.percent;
            lastBrightnessCacheAt = System.currentTimeMillis();
            updateStore();
            return new TSEBrightnessStatus(true, isWritable, currentPercent, "WMI", "Success");
        }
        return new TSEBrightnessStatus(false, false, -1, "ERROR", "Failed to get brightness");
    }
    
    public TSEBrightnessStatus setBrightness(int percent, String requestId) {
        System.out.println(TAG + " setBrightness percent=" + percent);
        System.out.println("[TSE_BRIGHTNESS_SERVICE] setBrightness percent=" + percent 
            + ", supported=" + isSupported 
            + ", writable=" + isWritable 
            + ", running=" + isRunning()
            + ", requestId=" + requestId);
            
        if (!isSupported || !isWritable) {
            return new TSEBrightnessStatus(isSupported, false, currentPercent, "WMI", "Not writable");
        }
        
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        
        final int targetPercent = percent;
        TSEBrightnessStatus status = executeWithTimeout(() -> setBrightnessInternal(targetPercent), 3000);
        
        System.out.println("[TSE_BRIGHTNESS_SERVICE] set result supported=" + (status != null ? status.supported : false)
            + ", writable=" + (status != null ? status.writable : false)
            + ", percent=" + (status != null ? status.percent : -1)
            + ", method=" + (status != null ? status.method : "null")
            + ", message=" + (status != null ? status.message : "timeout"));
            
        if (status != null && status.supported && status.writable) {
            currentPercent = targetPercent;
            lastBrightnessCacheAt = System.currentTimeMillis();
            
            TSEBrightnessStatus verify = executeWithTimeout(this::getBrightnessInternal, 3000);
            System.out.println("[TSE_BRIGHTNESS_SERVICE] verify after set percent=" + (verify != null ? verify.percent : -1));
            
            updateStore();
            return new TSEBrightnessStatus(true, true, currentPercent, "WMI", "Success");
        }
        
        return new TSEBrightnessStatus(true, false, currentPercent, "ERROR", "Failed to set brightness");
    }
    
    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
    
    private void updateStore() {
        if (stateStore != null) {
            stateStore.updateBrightness(isSupported, isWritable, currentPercent, 0, false, null);
        }
    }
    
    private TSEBrightnessStatus executeWithTimeout(Callable<TSEBrightnessStatus> task, long timeoutMs) {
        if (executor == null || executor.isShutdown()) return null;
        try {
            Future<TSEBrightnessStatus> future = executor.submit(task);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println(TAG + " timeout");
            if (activeProcess != null) {
                activeProcess.destroyForcibly();
            }
            return new TSEBrightnessStatus(false, false, -1, "TIMEOUT", "Timeout");
        } catch (Exception e) {
            return new TSEBrightnessStatus(false, false, -1, "ERROR", e.getMessage());
        }
    }
    
    private TSEBrightnessStatus getBrightnessInternal() {
        try {
            String script = "Get-CimInstance -Namespace root/wmi -ClassName WmiMonitorBrightness -ErrorAction Stop | Select-Object -ExpandProperty CurrentBrightness";
            String output = runPowerShell(script);
            if (output != null && !output.trim().isEmpty()) {
                int percent = Integer.parseInt(output.trim());
                return new TSEBrightnessStatus(true, true, percent, "WMI", "Success");
            }
        } catch (Exception e) {
        }
        return new TSEBrightnessStatus(false, false, -1, "UNSUPPORTED", "Device does not support brightness control");
    }
    
    private TSEBrightnessStatus setBrightnessInternal(int percent) {
        try {
            String script = "(Get-WmiObject -Namespace root/wmi -Class WmiMonitorBrightnessMethods -ErrorAction Stop).WmiSetBrightness(1, " + percent + ")";
            boolean success = runPowerShellScriptVoid(script);
            if (success) {
                return new TSEBrightnessStatus(true, true, percent, "WMI", "Success");
            }
        } catch (Exception e) {
        }
        return new TSEBrightnessStatus(false, false, -1, "UNSUPPORTED", "Device does not support brightness control");
    }
    
    private String runPowerShell(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        activeProcess = process;
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        boolean finished = process.waitFor(2, TimeUnit.SECONDS);
        activeProcess = null;
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("PowerShell process timeout");
        }
        return sb.toString();
    }
    
    private boolean runPowerShellScriptVoid(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        activeProcess = process;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {}
        }
        
        boolean finished = process.waitFor(2, TimeUnit.SECONDS);
        activeProcess = null;
        if (!finished) {
            process.destroyForcibly();
            return false;
        }
        return process.exitValue() == 0;
    }
}
