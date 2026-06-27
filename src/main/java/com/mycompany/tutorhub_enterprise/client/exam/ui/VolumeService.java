package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.win32.WinDef.BOOL;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VolumeService {

    private static final CLSID MMDeviceEnumerator_CLSID = new CLSID("BCDE0395-E52F-467C-8E3D-C4579291692E");
    private static final IID IMMDeviceEnumerator_IID = new IID("A95664D2-9614-4F35-A746-DE8DB63617E6");
    private static final IID IAudioEndpointVolume_IID = new IID("5CDF2C82-841E-4546-9722-0CF74078229A");

    private static final int CLSCTX_ALL = 23;
    private static final int eRender = 0;
    private static final int eMultimedia = 1;

    public static class IMMDeviceEnumerator extends Unknown {
        public IMMDeviceEnumerator(Pointer p) { super(p); }
        public HRESULT GetDefaultAudioEndpoint(int dataFlow, int role, PointerByReference ppEndpoint) {
            return (HRESULT) _invokeNativeObject(4, new Object[] { this.getPointer(), dataFlow, role, ppEndpoint }, HRESULT.class);
        }
    }

    public static class IMMDevice extends Unknown {
        public IMMDevice(Pointer p) { super(p); }
        public HRESULT Activate(IID iid, int dwClsCtx, Pointer pActivationParams, PointerByReference ppInterface) {
            return (HRESULT) _invokeNativeObject(3, new Object[] { this.getPointer(), iid.getPointer(), dwClsCtx, pActivationParams, ppInterface }, HRESULT.class);
        }
    }

    public static class IAudioEndpointVolume extends Unknown {
        public IAudioEndpointVolume(Pointer p) { super(p); }
        
        public HRESULT SetMasterVolumeLevelScalar(float fLevel, Pointer pguidEventContext) {
            return (HRESULT) _invokeNativeObject(7, new Object[] { this.getPointer(), fLevel, pguidEventContext }, HRESULT.class);
        }
        
        public HRESULT GetMasterVolumeLevelScalar(FloatByReference pfLevel) {
            return (HRESULT) _invokeNativeObject(9, new Object[] { this.getPointer(), pfLevel }, HRESULT.class);
        }
        
        public HRESULT SetMute(boolean bMute, Pointer pguidEventContext) {
            return (HRESULT) _invokeNativeObject(14, new Object[] { this.getPointer(), new BOOL(bMute), pguidEventContext }, HRESULT.class);
        }
        
        public HRESULT GetMute(IntByReference pbMute) {
            return (HRESULT) _invokeNativeObject(15, new Object[] { this.getPointer(), pbMute }, HRESULT.class);
        }
    }

    private final QuickSettingsStateStore stateStore;
    private ScheduledExecutorService scheduler;
    
    private volatile TSEVolumeStatus lastKnownStatus = null;
    private volatile long lastVolumeCacheAt = 0;
    private static final long CACHE_TTL_MS = 2000;
    
    private volatile int lastNonZeroVolume = 30;
    private final AtomicInteger versionCounter = new AtomicInteger(0);

    public VolumeService() {
        this(null);
    }

    public VolumeService(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void initialize() {
        System.out.println("[TSE_VOLUME_SERVICE] initialize");
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TSE-Volume-Poller");
            t.setDaemon(true);
            return t;
        });

        // Run immediately, then every 2 seconds
        scheduler.scheduleAtFixedRate(this::refreshNow, 0, 2, TimeUnit.SECONDS);
    }

    public void terminate() {
        System.out.println("[TSE_VOLUME_SERVICE] terminate");
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }

    public void refreshNow() {
        TSEVolumeStatus status = getStatusUncached();
        if (status.supported) {
            lastKnownStatus = status;
            lastVolumeCacheAt = System.currentTimeMillis();
        }
        updateStore(status, false);
    }

    private void updateStore(TSEVolumeStatus status, boolean pending) {
        if (stateStore != null && status != null) {
            stateStore.updateVolume(
                status.supported, 
                status.writable, 
                status.percent, 
                status.muted, 
                versionCounter.get(), 
                pending, 
                status.supported ? null : status.message
            );
        }
    }

    private IAudioEndpointVolume getEndpointVolume() throws Exception {
        PointerByReference pEnumerator = new PointerByReference();
        HRESULT hr = Ole32.INSTANCE.CoCreateInstance(MMDeviceEnumerator_CLSID, null, CLSCTX_ALL, IMMDeviceEnumerator_IID, pEnumerator);
        if (!com.sun.jna.platform.win32.WinError.S_OK.equals(hr)) {
            throw new Exception("CoCreateInstance failed with HR: " + hr);
        }

        IMMDeviceEnumerator enumerator = new IMMDeviceEnumerator(pEnumerator.getValue());
        PointerByReference pDevice = new PointerByReference();
        hr = enumerator.GetDefaultAudioEndpoint(eRender, eMultimedia, pDevice);
        if (!com.sun.jna.platform.win32.WinError.S_OK.equals(hr)) {
            enumerator.Release();
            throw new Exception("GetDefaultAudioEndpoint failed with HR: " + hr);
        }

        IMMDevice device = new IMMDevice(pDevice.getValue());
        PointerByReference pEndpointVolume = new PointerByReference();
        hr = device.Activate(IAudioEndpointVolume_IID, CLSCTX_ALL, null, pEndpointVolume);
        
        enumerator.Release();
        device.Release();

        if (!com.sun.jna.platform.win32.WinError.S_OK.equals(hr)) {
            throw new Exception("Activate IAudioEndpointVolume failed with HR: " + hr);
        }

        return new IAudioEndpointVolume(pEndpointVolume.getValue());
    }

    private interface COMOperation<T> {
        T execute(IAudioEndpointVolume endpoint) throws Exception;
    }

    private <T> T withCOM(COMOperation<T> operation, T errorValue) {
        boolean initialized = false;
        try {
            HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
            initialized = com.sun.jna.platform.win32.WinError.S_OK.equals(hr) || 
                          com.sun.jna.platform.win32.WinError.S_FALSE.equals(hr);
            
            if (initialized) {
                System.out.println("[TSE_VOLUME_SERVICE] COM initialized");
            }
            
            IAudioEndpointVolume endpoint = getEndpointVolume();
            try {
                return operation.execute(endpoint);
            } finally {
                endpoint.Release();
            }
        } catch (Exception ex) {
            System.err.println("[TSE_VOLUME_SERVICE] error=" + ex.getMessage());
            return errorValue;
        } finally {
            if (initialized) {
                Ole32.INSTANCE.CoUninitialize();
                System.out.println("[TSE_VOLUME_SERVICE] COM uninitialized");
            }
        }
    }

    public TSEVolumeStatus getStatus() {
        System.out.println("[TSE_VOLUME_SERVICE] getStatus");
        if (lastKnownStatus != null && (System.currentTimeMillis() - lastVolumeCacheAt < CACHE_TTL_MS)) {
            return lastKnownStatus;
        }
        
        TSEVolumeStatus status = getStatusUncached();
        if (status.supported) {
            lastKnownStatus = status;
            lastVolumeCacheAt = System.currentTimeMillis();
        }
        return status;
    }
    
    private TSEVolumeStatus getStatusUncached() {
        return withCOM(endpoint -> {
            FloatByReference level = new FloatByReference();
            endpoint.GetMasterVolumeLevelScalar(level);
            IntByReference mutedRef = new IntByReference();
            endpoint.GetMute(mutedRef);
            
            int percent = (int) Math.round(level.getValue() * 100);
            boolean muted = mutedRef.getValue() != 0;
            if (percent == 0) {
                muted = true;
            }
            if (muted) {
                percent = 0;
            }
            
            return new TSEVolumeStatus(true, true, percent, muted, "CoreAudio", "OK");
        }, new TSEVolumeStatus(false, false, 0, false, "ERROR", "Failed to query volume"));
    }

    public TSEVolumeStatus setVolume(int percent, String requestId) {
        System.out.println("[TSE_VOLUME_SERVICE] setVolume percent=" + percent);
        int targetPercent = Math.max(0, Math.min(100, percent));
        versionCounter.incrementAndGet();

        TSEVolumeStatus result = withCOM(endpoint -> {
            if (targetPercent == 0) {
                endpoint.SetMasterVolumeLevelScalar(0f, null);
                endpoint.SetMute(true, null);
                return new TSEVolumeStatus(true, true, 0, true, "CoreAudio", "OK");
            } else {
                lastNonZeroVolume = targetPercent;
                float scalar = targetPercent / 100f;
                endpoint.SetMute(false, null);
                endpoint.SetMasterVolumeLevelScalar(scalar, null);
                return new TSEVolumeStatus(true, true, targetPercent, false, "CoreAudio", "OK");
            }
        }, new TSEVolumeStatus(false, false, 0, false, "ERROR", "Failed to set volume"));

        if (result.supported) {
            lastKnownStatus = result;
            lastVolumeCacheAt = System.currentTimeMillis();
        }
        updateStore(result, false);
        return result;
    }

    public TSEVolumeStatus setMuted(boolean muted, String requestId) {
        System.out.println("[TSE_VOLUME_SERVICE] setMuted muted=" + muted);
        versionCounter.incrementAndGet();

        TSEVolumeStatus result = withCOM(endpoint -> {
            if (muted) {
                endpoint.SetMute(true, null);
                return new TSEVolumeStatus(true, true, 0, true, "CoreAudio", "OK");
            } else {
                endpoint.SetMute(false, null);
                float scalar = Math.max(0f, Math.min(100f, lastNonZeroVolume)) / 100f;
                endpoint.SetMasterVolumeLevelScalar(scalar, null);
                return new TSEVolumeStatus(true, true, lastNonZeroVolume, false, "CoreAudio", "OK");
            }
        }, new TSEVolumeStatus(false, false, 0, false, "ERROR", "Failed to set mute"));

        if (result.supported) {
            lastKnownStatus = result;
            lastVolumeCacheAt = System.currentTimeMillis();
        }
        updateStore(result, false);
        return result;
    }
    
    public int getLastNonZeroVolume() {
        return lastNonZeroVolume;
    }
}
