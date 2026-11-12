package au.com.canberraexplorer.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AdminActivity extends Activity {
    private static final int BLACK = Color.rgb(13, 13, 15);
    private static final int PANEL = Color.rgb(29, 29, 33);
    private static final int RED = Color.rgb(220, 35, 48);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(180, 180, 187);
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        showDashboard();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void showDashboard() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BLACK);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(28), dp(22), dp(28));
        scroll.addView(content);

        TextView eyebrow = text("CANBERRA EXPLORER", 12, RED, true);
        content.addView(eyebrow);
        TextView title = text("Admin Control", 30, WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        content.addView(title, titleParams);
        TextView subtitle = text("Private administration app", 15, MUTED, false);
        content.addView(subtitle);

        LinearLayout identity = panel();
        addPanelText(identity, "APPROVED ADMIN EMAIL", 11, RED, true);
        addPanelText(identity, "Dominichuddleston299@gmail.com", 15, WHITE, true);
        addPanelText(identity, "Approval is pending secure Firebase setup. This address is not yet granted admin access.", 13, MUTED, false);
        addPanel(identity, dp(26));

        addSection("USER MANAGEMENT");
        addStatusCard("Registered users", "Not connected yet", "A live list will appear after secure admin access is configured.");
        addStatusCard("Currently active", "Not connected yet", "Active status will use app presence signals; it won't be a perfect real-time guarantee.");
        addSection("ADMIN ACTIONS");
        addStatusCard("Block an account", "Unavailable in preview", "The finished control will disable the selected account on the server.");
        addStatusCard("Force sign-out", "Unavailable in preview", "The finished control will revoke sessions and make the app sign the user out.");
        addStatusCard("Admin activity log", "Not connected yet", "Changes will be recorded with a timestamp and the affected account.");

        Button setup = new Button(this);
        setup.setText("SECURE ADMIN CONNECTION NOT SET UP");
        setup.setAllCaps(false);
        setup.setTextColor(WHITE);
        setup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED));
        setup.setEnabled(false);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(52));
        buttonParams.topMargin = dp(18);
        content.addView(setup, buttonParams);

        TextView note = text("PREVIEW BUILD  •  No user data is shown or changed. Sign-in, account controls and live activity will only be enabled after the trusted backend is configured and tested.", 12, MUTED, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(18);
        content.addView(note, noteParams);
        setContentView(scroll);
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(15));
        box.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        box.setBackground(getDrawable(android.R.drawable.dialog_holo_light_frame));
        box.setBackgroundColor(PANEL);
        return box;
    }

    private void addPanelText(LinearLayout box, String value, int size, int color, boolean bold) {
        TextView v = text(value, size, color, bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(4);
        box.addView(v, p);
    }

    private void addPanel(LinearLayout box, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = top;
        content.addView(box, p);
    }

    private void addSection(String label) {
        TextView heading = text(label, 12, RED, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(25);
        p.bottomMargin = dp(10);
        content.addView(heading, p);
    }

    private void addStatusCard(String heading, String status, String detail) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        box.setBackgroundColor(PANEL);
        TextView h = text(heading, 16, WHITE, true);
        box.addView(h);
        TextView s = text(status, 13, RED, true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(6);
        box.addView(s, sp);
        TextView d = text(detail, 12, MUTED, false);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.topMargin = dp(6);
        box.addView(d, dpv);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        content.addView(box, p);
    }
}
