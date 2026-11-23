package com.meh.fire;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private float dp;

    private TextView serviceStatus;
    private Button serviceButton;
    private TextView lockStatus;
    private TextView lockStats;
    private LinearLayout startPanel;
    private LinearLayout runningPanel;
    private CheckBox reels, shorts, videos, ytApp, inspector;
    private TextView inspectorOut;
    private EditText customMinutes;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#14110F"));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = px(20);
        col.setPadding(pad, px(28), pad, px(40));
        scroll.addView(col);

        TextView title = text("Fire", 34, "#FF7A3D");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        col.addView(text("Stop the scroll. Keep the useful parts.", 15, "#B8B0A8"));

        // 1. Blocker switch
        col.addView(header("1 · Blocker"));
        serviceStatus = text("", 15, "#FFFFFF");
        col.addView(serviceStatus);
        serviceButton = button("Turn on in Accessibility settings", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        col.addView(serviceButton);

        // 2. What to block
        col.addView(header("2 · What to block"));
        reels = check("Instagram Reels", Lock.BLOCK_REELS);
        shorts = check("YouTube Shorts", Lock.BLOCK_SHORTS);
        videos = check("YouTube videos (long-form)", Lock.BLOCK_VIDEOS);
        col.addView(reels);
        col.addView(shorts);
        col.addView(videos);
        ytApp = check("Whole YouTube app (can't open it at all)", Lock.BLOCK_YT_APP);
        col.addView(ytApp);
        col.addView(text("DMs, search and profiles stay usable, unless you block the whole YouTube app.", 13, "#8A827B"));

        // 3. Lock timer
        col.addView(header("3 · Lock timer"));
        lockStatus = text("", 20, "#FFFFFF");
        lockStatus.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(lockStatus);
        lockStats = text("", 13, "#8A827B");
        col.addView(lockStats);

        startPanel = new LinearLayout(this);
        startPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.addView(preset("30m", 30));
        presets.addView(preset("1h", 60));
        presets.addView(preset("2h", 120));
        presets.addView(preset("4h", 240));
        startPanel.addView(presets);
        LinearLayout custom = new LinearLayout(this);
        custom.setOrientation(LinearLayout.HORIZONTAL);
        customMinutes = new EditText(this);
        customMinutes.setHint("Minutes");
        customMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        customMinutes.setTextColor(Color.WHITE);
        customMinutes.setHintTextColor(Color.parseColor("#6E6660"));
        custom.addView(customMinutes, new LinearLayout.LayoutParams(0, -2, 1));
        custom.addView(button("Start lock", v -> {
            String s = customMinutes.getText().toString().trim();
            if (TextUtils.isEmpty(s)) {
                toast("Type how many minutes first");
                return;
            }
            try {
                startLock(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                toast("That number is too big");
            }
        }));
        startPanel.addView(custom);
        col.addView(startPanel);

        runningPanel = new LinearLayout(this);
        runningPanel.setOrientation(LinearLayout.VERTICAL);
        runningPanel.addView(button("Add 30 minutes", v -> startLock(30)));
        runningPanel.addView(button("Unlock with fingerprint / face", v -> Unlocker.unlock(this, this::refresh)));
        col.addView(runningPanel);

        // 4. Inspector
        col.addView(header("4 · Inspector (only if blocking stops working)"));
        inspector = new CheckBox(this);
        inspector.setText("Record screen IDs from Instagram / YouTube");
        inspector.setTextColor(Color.WHITE);
        inspector.setChecked(Lock.inspect(this));
        inspector.setOnCheckedChangeListener((b, on) -> Lock.setInspect(this, on));
        col.addView(inspector);
        col.addView(text("Turn this on, open the Reel / Short / video that slipped through, "
                + "come back here, and the IDs on that screen show below. Add the right one to "
                + "Targets.java and rebuild.", 13, "#8A827B"));
        inspectorOut = text("", 12, "#C9C2BB");
        inspectorOut.setTypeface(Typeface.MONOSPACE);
        inspectorOut.setTextIsSelectable(true);
        col.addView(inspectorOut);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void refresh() {
        boolean on = serviceEnabled();
        serviceStatus.setText(on ? "Blocker is ON" : "Blocker is OFF: nothing gets blocked until you turn it on.");
        serviceStatus.setTextColor(Color.parseColor(on ? "#7CD992" : "#FF8A80"));
        serviceButton.setVisibility(on ? View.GONE : View.VISIBLE);

        boolean locked = Lock.active(this);
        if (locked) {
            lockStatus.setText("Locked · " + Lock.fmt(Lock.remaining(this)) + " left");
            lockStats.setText("Blocked " + Lock.blocksThisLock(this) + " times this lock · "
                    + Lock.blocksTotal(this) + " all time");
        } else {
            lockStatus.setText("Unlocked");
            lockStats.setText(Lock.blocksTotal(this) > 0
                    ? "Blocked " + Lock.blocksTotal(this) + " times all time" : "Pick how long to lock for.");
        }
        startPanel.setVisibility(locked ? View.GONE : View.VISIBLE);
        runningPanel.setVisibility(locked ? View.VISIBLE : View.GONE);
        // No changing what's blocked mid-lock.
        reels.setEnabled(!locked);
        shorts.setEnabled(!locked);
        videos.setEnabled(!locked);
        ytApp.setEnabled(!locked);

        if (inspector.isChecked()) {
            String ids = Lock.lastIds(this);
            inspectorOut.setText(ids.isEmpty() ? "(open Instagram or YouTube, then come back)" : ids);
            inspectorOut.setVisibility(View.VISIBLE);
        } else {
            inspectorOut.setVisibility(View.GONE);
        }
    }

    private void startLock(int minutes) {
        if (minutes <= 0 || minutes > 24 * 60) {
            toast("Pick between 1 and 1440 minutes");
            return;
        }
        if (!serviceEnabled()) toast("Lock started, but turn the blocker on or nothing will be blocked");
        Lock.start(this, minutes * 60_000L);
        customMinutes.setText("");
        refresh();
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(this, BlockerService.class);
        for (String s : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }

    // ---- small UI helpers ----
    private int px(int v) { return (int) (v * dp); }

    private TextView text(String s, int sp, String color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.parseColor(color));
        t.setPadding(0, px(4), 0, px(4));
        return t;
    }

    private TextView header(String s) {
        TextView t = text(s, 13, "#FF7A3D");
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, px(24), 0, px(6));
        t.setAllCaps(true);
        return t;
    }

    private Button button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private Button preset(String label, int minutes) {
        Button b = button(label, v -> startLock(minutes));
        b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return b;
    }

    private CheckBox check(String label, String key) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextColor(Color.WHITE);
        c.setChecked(Lock.blocks(this, key));
        c.setOnCheckedChangeListener((b, on) -> Lock.setBlocks(this, key, on));
        return c;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
