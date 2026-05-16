package com.jumping.gianluigi.sound;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Generates all game sounds procedurally via PCM synthesis – no audio files needed.
 */
public class SoundManager {

    private static final int SAMPLE_RATE = 22050;
    private static volatile boolean muted = false;

    public static void setMuted(boolean m) { muted = m; }
    public static boolean isMuted()        { return muted; }

    // ── Public sound events ────────────────────────────────────────────────

    public static void playJump() {
        play(sweep(380f, 640f, 0.10f, 0.55f));
    }

    public static void playDoubleJump() {
        play(concat(sweep(500f, 850f, 0.08f, 0.4f),
                    sweep(700f, 1050f, 0.08f, 0.4f)));
    }

    public static void playStomp() {
        play(concat(noise(0.06f, 0.8f), tone(120f, 0.12f, 0.6f)));
    }

    public static void playDie() {
        play(sweep(500f, 180f, 0.40f, 0.65f));
    }

    public static void playPowerUp() {
        play(concat(tone(440f, 0.07f, 0.5f),
                    tone(554f, 0.07f, 0.5f),
                    tone(659f, 0.14f, 0.5f)));
    }

    public static void playLevelWin() {
        play(concat(tone(523f, 0.10f, 0.6f),
                    tone(659f, 0.10f, 0.6f),
                    tone(784f, 0.10f, 0.6f),
                    tone(1047f, 0.25f, 0.6f)));
    }

    // ── PCM generators ─────────────────────────────────────────────────────

    /** Pure sine tone with smooth fade-out envelope. */
    private static byte[] tone(float freq, float dur, float vol) {
        int n = (int)(SAMPLE_RATE * dur);
        byte[] d = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double wave = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            float env  = Math.max(0f, 1f - (float) i / n);
            short s = (short)(wave * env * vol * Short.MAX_VALUE);
            d[i * 2]     = (byte)(s & 0xFF);
            d[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return d;
    }

    /** Frequency sweep (portamento effect). */
    private static byte[] sweep(float f1, float f2, float dur, float vol) {
        int n = (int)(SAMPLE_RATE * dur);
        byte[] d = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t    = (float) i / SAMPLE_RATE;
            float freq = f1 + (f2 - f1) * t / dur;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            float env  = Math.max(0f, 1f - t / dur);
            short s = (short)(Math.sin(phase) * env * vol * Short.MAX_VALUE);
            d[i * 2]     = (byte)(s & 0xFF);
            d[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return d;
    }

    /** White noise burst (for impact/stomp). */
    private static byte[] noise(float dur, float vol) {
        int n = (int)(SAMPLE_RATE * dur);
        byte[] d = new byte[n * 2];
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < n; i++) {
            float env  = Math.max(0f, 1f - (float) i / n);
            short s = (short)((rng.nextFloat() * 2f - 1f) * env * vol * Short.MAX_VALUE);
            d[i * 2]     = (byte)(s & 0xFF);
            d[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return d;
    }

    /** Concatenate PCM buffers. */
    @SafeVarargs
    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    /** Play PCM data on a fire-and-forget background thread. */
    private static void play(byte[] data) {
        if (muted) return;
        new Thread(() -> {
            try {
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(data.length)
                        .build();
                track.write(data, 0, data.length);
                track.play();
                long ms = (long)(data.length / 2f / SAMPLE_RATE * 1000) + 80;
                Thread.sleep(ms);
                track.release();
            } catch (Exception ignored) {}
        }, "SoundThread").start();
    }
}
