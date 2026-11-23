package com.meh.fire;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The "Not now" screen shown after a blocked screen is closed. */
public class BlockedActivity extends Activity {

    static final String EXTRA_WHAT = "what";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeLeft;
    private TextView what;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!Lock.active(BlockedActivity.this)) {
                timeLeft.setText("Your lock just ended. Everything is unlocked.");
                return;
            }
            timeLeft.setText("Unlocks in " + Lock.fmt(Lock.remaining(BlockedActivity.this)));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#14110F"));
        int pad = (int) (32 * dp);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Not now.");
        title.setTextColor(Color.parseColor("#FF7A3D"));
        title.setTextSize(44);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        what = new TextView(this);
        what.setTextColor(Color.WHITE);
        what.setTextSize(18);
        what.setGravity(Gravity.CENTER);
        what.setPadding(0, (int) (12 * dp), 0, 0);
        root.addView(what);

        timeLeft = new TextView(this);
        timeLeft.setTextColor(Color.parseColor("#B8B0A8"));
        timeLeft.setTextSize(16);
        timeLeft.setGravity(Gravity.CENTER);
        timeLeft.setPadding(0, (int) (8 * dp), 0, (int) (32 * dp));
        root.addView(timeLeft);

        Button close = new Button(this);
        close.setText("Close");
        close.setAllCaps(false);
        close.setOnClickListener(v -> goHome());
        root.addView(close);

        Button unlock = new Button(this);
        unlock.setText("Unlock with fingerprint / face");
        unlock.setAllCaps(false);
        unlock.setOnClickListener(v -> Unlocker.unlock(this, this::finish));
        root.addView(unlock);

        setContentView(root);
        showWhat(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showWhat(intent);
    }

    private void showWhat(Intent intent) {
        String label = intent == null ? null : intent.getStringExtra(EXTRA_WHAT);
        what.setText((label == null ? "This" : label) + " is locked right now.");
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

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }
}
