package com.meh.fire;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Watches Instagram and YouTube only. While a lock is running and a blocked screen
 * appears, it presses Back (leaves Reels/Shorts, shrinks a video to the mini-player)
 * and shows the "Not now" screen, which also pauses the video.
 */
public class BlockerService extends AccessibilityService {

    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastBlockAt = 0;
    private long lastInspectAt = 0;
    private long lastPipCheckAt = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (!Targets.isInstagram(pkg) && !Targets.isYouTube(pkg)) return;

        boolean locked = Lock.active(this);
        boolean inspect = Lock.inspect(this);
        if (!locked && !inspect) return;

        // Never act while the screen is off (e.g. background playback in RVX).
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastBlockAt < 1500) return; // give the app time to react to the last Back

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !pkg.equals(root.getPackageName().toString())) {
            // YouTube is sending events but isn't the main screen: it may be floating in
            // picture-in-picture. During a lock, pause it and swipe the window away.
            if (locked && Targets.isYouTube(pkg) && now - lastPipCheckAt > 2000) {
                lastPipCheckAt = now;
                if (dismissYouTubePip()) pauseMedia();
            }
            return;
        }

        if (inspect && now - lastInspectAt > 1500) {
            lastInspectAt = now;
            Lock.setLastIds(this, (Targets.isInstagram(pkg) ? "Instagram" : "YouTube")
                    + " (" + pkg + ") screen IDs:\n" + Targets.collectIds(root));
        }
        if (!locked) return;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        Targets.Kind kind = Targets.detect(this, pkg, root, screenWidth);
        if (kind == null) return;

        lastBlockAt = now;
        Lock.countBlock(this);
        if (kind == Targets.Kind.YT_APP || kind == Targets.Kind.VIDEO) pauseMedia();
        // Whole-app block: leave the app entirely. Otherwise just step back out of the feed.
        performGlobalAction(kind == Targets.Kind.YT_APP ? GLOBAL_ACTION_HOME : GLOBAL_ACTION_BACK);
        main.postDelayed(() -> {
            Intent i = new Intent(this, BlockedActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BlockedActivity.EXTRA_WHAT, kind.label);
            try {
                startActivity(i);
            } catch (RuntimeException ignored) {
                // Back was already pressed; the screen is just a bonus.
            }
        }, 250);
        if (Targets.isYouTube(pkg)) {
            // Leaving YouTube can turn the video into a floating window; clear it once it appears.
            main.postDelayed(this::dismissYouTubePip, 900);
            main.postDelayed(this::dismissYouTubePip, 2000);
        }
    }

    /** Sends a media "pause" key, which stops the YouTube player even if it is floating. */
    private void pauseMedia() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        long t = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
    }

    /**
     * Finds a YouTube picture-in-picture window and drags it to the bottom of the screen,
     * which is Android's "dismiss" spot for floating video. Returns true if one was found.
     */
    private boolean dismissYouTubePip() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return false;
        for (AccessibilityWindowInfo w : windows) {
            if (!w.isInPictureInPictureMode()) continue;
            AccessibilityNodeInfo r = w.getRoot();
            CharSequence p = r == null ? null : r.getPackageName();
            if (p == null || !Targets.isYouTube(p.toString())) continue;

            pauseMedia();
            Rect b = new Rect();
            w.getBoundsInScreen(b);
            int sw = getResources().getDisplayMetrics().widthPixels;
            int sh = getResources().getDisplayMetrics().heightPixels;
            Path path = new Path();
            path.moveTo(b.exactCenterX(), b.exactCenterY());
            path.lineTo(sw / 2f, sh - 40f);
            GestureDescription g = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 700))
                    .build();
            dispatchGesture(g, null, null);
            return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}
