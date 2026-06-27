package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class TSETestSoundPlayer {

    public static void playTestToneAsync() {
        System.out.println("[TSE_AUDIO_TEST] Test sound requested.");
        Thread t = new Thread(() -> {
            try {
                System.out.println("[TSE_AUDIO_TEST] Playing generated tone.");
                // 44100 Hz, 8-bit, mono, unsigned
                AudioFormat af = new AudioFormat(44100, 8, 1, false, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();

                int durationMs = 800; // 800ms
                double freq = 440.0; // 440 Hz
                int length = (int) (44100 * (durationMs / 1000.0));
                byte[] buf = new byte[length];

                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (44100.0 / freq) * 2.0 * Math.PI;
                    // scale down volume by half (64 instead of 127) so it's not too loud
                    buf[i] = (byte) (Math.sin(angle) * 64.0);
                }

                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.stop();
                sdl.close();
                System.out.println("[TSE_AUDIO_TEST] Test sound completed.");
            } catch (Exception e) {
                System.err.println("[TSE_AUDIO_TEST] Test sound failed: " + e.getMessage());
            }
        }, "TSE-TestSoundPlayer");
        t.setDaemon(true);
        t.start();
    }
}
