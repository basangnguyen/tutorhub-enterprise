package com.mycompany.tutorhub_enterprise.client.exam.ui;

public final class TSEVolumeStatus {
    public final boolean supported;
    public final boolean writable;
    public final int percent;
    public final boolean muted;
    public final String method;
    public final String message;

    public TSEVolumeStatus(boolean supported, boolean writable, int percent, boolean muted, String method, String message) {
        this.supported = supported;
        this.writable = writable;
        this.percent = percent;
        this.muted = muted;
        this.method = method;
        this.message = message;
    }
}
