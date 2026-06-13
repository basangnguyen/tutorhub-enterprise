package com.mycompany.tutorhub_enterprise.client.exam.ui;

public final class TSEBrightnessStatus {
    public final boolean supported;
    public final boolean writable;
    public final int percent;
    public final String method;
    public final String message;

    public TSEBrightnessStatus(boolean supported, boolean writable, int percent, String method, String message) {
        this.supported = supported;
        this.writable = writable;
        this.percent = percent;
        this.method = method;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("TSEBrightnessStatus{supported=%b, writable=%b, percent=%d, method='%s', message='%s'}",
                supported, writable, percent, method, message);
    }
}
