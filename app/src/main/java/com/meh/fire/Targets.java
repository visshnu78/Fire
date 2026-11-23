package com.meh.fire;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.List;
import java.util.TreeSet;

/**
 * Which screens count as "doomscrolling", detected by Android view IDs.
 *
 * Instagram and YouTube rename these IDs now and then. If blocking stops working
 * after an app update, turn on Inspector in Fire, open the screen, and copy the
 * new ID from the list Fire shows into the arrays below.
 */
final class Targets {
    private Targets() {}

    static final String IG = "com.instagram.android";
    static final String YT = "com.google.android.youtube";

    /** Official Instagram app. */
    static boolean isInstagram(String pkg) {
        return IG.equals(pkg);
    }

    /**
     * Official YouTube and patched builds like ReVanced / RVX
     * (app.rvx.android.youtube, app.revanced.android.youtube, ...). YouTube Music is left alone.
     */
    static boolean isYouTube(String pkg) {
        if (pkg == null) return false;
        if (YT.equals(pkg)) return true;
        String p = pkg.toLowerCase(java.util.Locale.US);
        return p.contains("youtube") && !p.contains("music") && !p.equals("com.meh.fire");
    }

    enum Kind {
        REELS("Instagram Reels", Lock.BLOCK_REELS),
        SHORTS("YouTube Shorts", Lock.BLOCK_SHORTS),
        VIDEO("YouTube videos", Lock.BLOCK_VIDEOS),
        YT_APP("YouTube", Lock.BLOCK_YT_APP);

        final String label;
        final String prefKey;

        Kind(String label, String prefKey) {
            this.label = label;
            this.prefKey = prefKey;
        }
    }

    /** The full-screen Reels viewer (Reels tab, or a reel opened from a DM / profile). */
    static final String[] IG_REELS = {
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "root_clips_layout",
    };

    /** The Shorts player. */
    static final String[] YT_SHORTS = {
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player",
    };

    /**
     * The normal video watch page. Only counted when it is nearly full width,
     * so the small mini-player and home-feed previews don't trigger it.
     */
    static final String[] YT_VIDEO = {
            "watch_player",
            "watch_list",
    };

    static Kind detect(Context c, String pkg, AccessibilityNodeInfo root, int screenWidth) {
        if (isInstagram(pkg)) {
            if (Lock.blocks(c, Lock.BLOCK_REELS) && anyVisible(root, pkg, IG_REELS, 0)) return Kind.REELS;
        } else if (isYouTube(pkg)) {
            if (Lock.blocks(c, Lock.BLOCK_YT_APP)) return Kind.YT_APP;
            if (Lock.blocks(c, Lock.BLOCK_SHORTS) && anyVisible(root, pkg, YT_SHORTS, 0)) return Kind.SHORTS;
            if (Lock.blocks(c, Lock.BLOCK_VIDEOS)
                    && anyVisible(root, pkg, YT_VIDEO, (int) (screenWidth * 0.7f))) return Kind.VIDEO;
        }
        return null;
    }

    /**
     * Patched apps (ReVanced/RVX) change the app's package name, but their screen IDs may keep the
     * original prefix, so check the IDs under both the running package and the official one.
     */
    private static boolean anyVisible(AccessibilityNodeInfo root, String pkg, String[] ids, int minWidth) {
        String official = isYouTube(pkg) ? YT : IG;
        String[] prefixes = pkg.equals(official) ? new String[]{pkg} : new String[]{pkg, official};
        Rect r = new Rect();
        for (String prefix : prefixes) {
            for (String id : ids) {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(prefix + ":id/" + id);
                if (nodes == null) continue;
                for (AccessibilityNodeInfo n : nodes) {
                    if (n == null || !n.isVisibleToUser()) continue;
                    n.getBoundsInScreen(r);
                    if (r.width() >= minWidth && r.height() > 0) return true;
                }
            }
        }
        return false;
    }

    /** For Inspector: every visible view ID on the current screen. */
    static String collectIds(AccessibilityNodeInfo root) {
        TreeSet<String> ids = new TreeSet<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < 1500) {
            AccessibilityNodeInfo n = queue.poll();
            seen++;
            if (n == null) continue;
            String id = n.getViewIdResourceName();
            if (id != null && n.isVisibleToUser()) {
                int slash = id.indexOf(":id/");
                ids.add(slash >= 0 ? id.substring(slash + 4) : id);
            }
            for (int i = 0; i < n.getChildCount(); i++) queue.add(n.getChild(i));
        }
        return String.join("\n", ids);
    }
}
