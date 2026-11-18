package com.termux.hz;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.ViewGroup;

/** Live build / command output. */
public class ConsolePanel extends LinearLayout {

    public interface Host {
        void stop();
        void askFix(String logTail);
        void openTerminal();
        void runCommand(String cmd);
    }

    private final Activity act;
    private final Host host;
    private TextView out;
    private ScrollView scroll;
    private final SpannableStringBuilder buf = new SpannableStringBuilder();
    private TextView stopBtn, status;
    private int lines;
    private boolean flushPending;
    private final Runnable flush = () -> {
        flushPending = false;
        out.setText(buf);
        scroll.post(() -> scroll.fullScroll(FOCUS_DOWN));
    };

    public ConsolePanel(Activity a, Host h) {
        super(a);
        act = a; host = h;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Ui.SURFACE);
        bar.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 6));
        status = Ui.text(a, "Idle", 13, Ui.MUTED);
        bar.addView(status, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stopBtn = Ui.chip(a, "■ Stop", host::stop);
        stopBtn.setTextColor(Ui.DANGER);
        stopBtn.setVisibility(GONE);
        bar.addView(stopBtn);
        addView(bar);

        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setBackgroundColor(Ui.SURFACE);
        LinearLayout chips = new LinearLayout(a);
        chips.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 8));
        chips.addView(chip("✨ Fix with agent", () -> host.askFix(Sh.tail(buf.toString(), 6000))));
        chips.addView(chip("Copy log", () -> {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("log", buf.toString()));
            Ui.toast(act, "Log copied");
        }));
        chips.addView(chip("Clear", this::clear));
        chips.addView(chip("Open terminal", host::openTerminal));
        hs.addView(chips);
        addView(hs);
        addView(Ui.divider(a));

        out = new TextView(a);
        out.setTypeface(Typeface.MONOSPACE);
        out.setTextSize(11.5f);
        out.setTextColor(0xFFC9D1E0);
        out.setTextIsSelectable(true);
        out.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        scroll = new ScrollView(a);
        scroll.addView(out);
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout in = new LinearLayout(a);
        in.setGravity(Gravity.CENTER_VERTICAL);
        in.setBackgroundColor(Ui.SURFACE);
        in.setPadding(Ui.dp(a, 8), Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 6));
        TextView prompt = Ui.text(a, "$", 15, Ui.ACCENT);
        prompt.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 6), 0);
        EditText cmd = Ui.input(a, "Run a command in the project folder");
        cmd.setSingleLine(true);
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setImeOptions(EditorInfo.IME_ACTION_SEND);
        Runnable go = () -> {
            String c = cmd.getText().toString().trim();
            if (!c.isEmpty()) { cmd.setText(""); host.runCommand(c); }
        };
        cmd.setOnEditorActionListener((v, id, e) -> { go.run(); return true; });
        in.addView(prompt);
        in.addView(cmd, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        in.addView(Ui.icon(a, com.termux.R.drawable.hz_ic_send, "Run", go));
        addView(in);
        append("HZ console ready. Press Build to compile your mod.");
    }

    private TextView chip(String t, Runnable r) {
        TextView c = Ui.chip(act, t, r);
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.rightMargin = Ui.dp(act, 8);
        c.setLayoutParams(p);
        return c;
    }

    public void setStatus(String s, int color, boolean running) {
        status.setText(s);
        status.setTextColor(color);
        stopBtn.setVisibility(running ? VISIBLE : GONE);
    }

    public void clear() { buf.clear(); lines = 0; out.setText(""); }

    public void append(String line) { appendColored(line, colorFor(line)); }

    public void appendColored(String line, int color) {
        int start = buf.length();
        buf.append(line).append('\n');
        if (color != 0) buf.setSpan(new ForegroundColorSpan(color), start, buf.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (++lines > 3000) {
            int cut = buf.toString().indexOf('\n', buf.length() / 3);
            if (cut > 0) { buf.delete(0, cut + 1); lines = 2000; }
        }
        if (!flushPending) { flushPending = true; postDelayed(flush, 100); }
    }

    private static int colorFor(String l) {
        String s = l.toLowerCase();
        if (s.contains("build successful") || s.startsWith("ok:") || s.contains("build success")) return Ui.ACCENT;
        if (s.contains("error:") || s.contains("failed") || s.contains("exception") || s.startsWith("e: ") || s.contains("[error]")) return Ui.DANGER;
        if (s.contains("warning") || s.startsWith("w: ") || s.contains("[warning]")) return Ui.WARN;
        if (s.startsWith("> task") || s.startsWith("$ ") || s.startsWith("==")) return 0xFF7FDBCA;
        return 0;
    }

    public String text() { return buf.toString(); }
}
