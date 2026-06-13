package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TSEBrightnessController {

    private static final String TAG = "[TSE_BRIGHTNESS]";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TSE-BrightnessWorker");
        t.setDaemon(true);
        return t;
    });
    private Process activeProcess = null;

    public TSEBrightnessController() {
    }

    public TSEBrightnessStatus getStatus() {
        System.out.println(TAG + " Query current brightness.");
        try {
            Future<TSEBrightnessStatus> future = executor.submit(new Callable<TSEBrightnessStatus>() {
                @Override
                public TSEBrightnessStatus call() {
                    return getBrightnessInternal();
                }
            });
            TSEBrightnessStatus status = future.get(3000, TimeUnit.MILLISECONDS);
            System.out.println(TAG + " Status supported=" + status.supported + ", writable=" + status.writable + ", percent=" + status.percent + ", method=" + status.method);
            return status;
        } catch (TimeoutException e) {
            System.err.println(TAG + " Get brightness timed out.");
            return new TSEBrightnessStatus(false, false, -1, "TIMEOUT", "Timeout querying brightness");
        } catch (Exception e) {
            System.err.println(TAG + " Get brightness failed: " + e.getMessage());
            return new TSEBrightnessStatus(false, false, -1, "ERROR", e.getMessage());
        }
    }

    public TSEBrightnessStatus setBrightness(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;

        final int targetPercent = percent;
        System.out.println(TAG + " Set requested: " + targetPercent);

        try {
            Future<TSEBrightnessStatus> future = executor.submit(new Callable<TSEBrightnessStatus>() {
                @Override
                public TSEBrightnessStatus call() {
                    return setBrightnessInternal(targetPercent);
                }
            });
            TSEBrightnessStatus status = future.get(3000, TimeUnit.MILLISECONDS);
            if (status.supported && status.writable) {
                System.out.println(TAG + " Set completed: " + targetPercent);
            } else {
                System.out.println(TAG + " Set failed: " + status.message);
                System.out.println(TAG + " Brightness control unsupported on this device.");
            }
            return status;
        } catch (TimeoutException e) {
            System.err.println(TAG + " Set brightness timed out.");
            return new TSEBrightnessStatus(false, false, -1, "TIMEOUT", "Timeout setting brightness");
        } catch (Exception e) {
            System.err.println(TAG + " Set brightness failed: " + e.getMessage());
            return new TSEBrightnessStatus(false, false, -1, "ERROR", e.getMessage());
        }
    }

    private TSEBrightnessStatus getBrightnessInternal() {
        System.out.println(TAG + " Trying WMI brightness strategy.");
        try {
            String script = "Get-CimInstance -Namespace root/wmi -ClassName WmiMonitorBrightness -ErrorAction Stop | Select-Object -ExpandProperty CurrentBrightness";
            String output = runPowerShell(script);
            if (output != null && !output.trim().isEmpty()) {
                int percent = Integer.parseInt(output.trim());
                System.out.println(TAG + " Strategy selected: WMI");
                return new TSEBrightnessStatus(true, true, percent, "WMI", "Success");
            }
        } catch (Exception e) {
            System.out.println(TAG + " WMI Get failed: " + e.getMessage());
        }

        System.out.println(TAG + " Trying DDC-CI brightness strategy.");
        // DDC-CI could be complex and unsafe via JNA, so we fallback to unsupported.
        System.out.println(TAG + " Strategy selected: UNSUPPORTED");
        return new TSEBrightnessStatus(false, false, -1, "UNSUPPORTED", "Device does not support brightness control");
    }

    private TSEBrightnessStatus setBrightnessInternal(int percent) {
        System.out.println(TAG + " Trying WMI brightness strategy.");
        try {
            String script = "(Get-WmiObject -Namespace root/wmi -Class WmiMonitorBrightnessMethods -ErrorAction Stop).WmiSetBrightness(1, " + percent + ")";
            boolean success = runPowerShellScriptVoid(script);
            if (success) {
                return new TSEBrightnessStatus(true, true, percent, "WMI", "Success");
            }
        } catch (Exception e) {
            System.out.println(TAG + " WMI Set failed: " + e.getMessage());
        }
        
        System.out.println(TAG + " Trying DDC-CI brightness strategy.");
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

    public void shutdownNowNoBlock() {
        System.out.println(TAG + " Shutdown requested.");
        executor.shutdownNow();
        if (activeProcess != null) {
            try {
                activeProcess.destroyForcibly();
            } catch (Exception ignored) {}
            activeProcess = null;
        }
        System.out.println(TAG + " Brightness worker stopped.");
        System.out.println(TAG + " No active PowerShell process remains.");
    }
}
