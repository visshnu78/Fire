package com.meh.fire;

import android.content.Context;
import android.content.SharedPreferences;

/** All app state lives here, in SharedPreferences, so the service and screens share it. */
final class Lock {
    private Lock() {}

    static final String BLOCK_REELS = "block_reels";
    static final String BLOCK_SHORTS = "block_shorts";
    static final String BLOCK_VIDEOS = "block_videos";
    static final String BLOCK_YT_APP = "block_yt_app";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("fire", Context.MODE_PRIVATE);
    }

    // ---- lock timer ----
    static long until(Context c) { return prefs(c).getLong("until", 0); }

    static boolean active(Context c) { return System.currentTimeMillis() < until(c); }

    static long remaining(Context c) { return Math.max(0, until(c) - System.currentTimeMillis()); }

    /** Starts a lock, or extends the current one if it is already running. */
    static void start(Context c, long durationMs) {
        long now = System.currentTimeMillis();
        boolean running = now < until(c);
        long base = running ? until(c) : now;
        SharedPreferences.Editor e = prefs(c).edit().putLong("until", base + durationMs);
        if (!running) e.putInt("blocks", 0);
        e.apply();
    }

    static void clear(Context c) { prefs(c).edit().putLong("until", 0).apply(); }

    // ---- what to block ----
    static boolean blocks(Context c, String key) { return prefs(c).getBoolean(key, true); }

    static void setBlocks(Context c, String key, boolean on) { prefs(c).edit().putBoolean(key, on).apply(); }

    // ---- stats ----
    static void countBlock(Context c) {
        SharedPreferences p = prefs(c);
        p.edit().putInt("blocks", p.getInt("blocks", 0) + 1)
                .putInt("blocks_total", p.getInt("blocks_total", 0) + 1).apply();
    }

    static int blocksThisLock(Context c) { return prefs(c).getInt("blocks", 0); }

    static int blocksTotal(Context c) { return prefs(c).getInt("blocks_total", 0); }

    // ---- inspector (to fix detection if Instagram/YouTube change their screens) ----
    static boolean inspect(Context c) { return prefs(c).getBoolean("inspect", false); }

    static void setInspect(Context c, boolean on) { prefs(c).edit().putBoolean("inspect", on).apply(); }

    static String lastIds(Context c) { return prefs(c).getString("last_ids", ""); }

    static void setLastIds(Context c, String s) { prefs(c).edit().putString("last_ids", s).apply(); }

    static String fmt(long ms) {
        long s = (ms + 999) / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format(java.util.Locale.US, "%dh %02dm %02ds", h, m, sec);
        return String.format(java.util.Locale.US, "%dm %02ds", m, sec);
    }
}
