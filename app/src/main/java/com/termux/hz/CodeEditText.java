package com.termux.hz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.ArrayList;

/** Code editor view: line numbers, undo/redo, auto-indent and syntax highlighting. */
public class CodeEditText extends AppCompatEditText {

    public interface ChangeListener { void onUserChange(); }

    static final class Edit { int start; String before, after; }
    public static final class History {
        final ArrayList<Edit> undo = new ArrayList<>(), redo = new ArrayList<>();
    }

    private final Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gutterPaint = new Paint();
    private History history = new History();
    private boolean applying;
    private String fileName = "";
    private ChangeListener listener;
    private int gutterDigits = 2;
    private final Runnable highlightJob = () -> Highlighter.apply(getText(), fileName);
    private String pendingBefore = "";
    private int pendingStart;

    public CodeEditText(Context c) {
        super(c);
        setTypeface(Typeface.MONOSPACE);
        setTextSize(13);
        setTextColor(Ui.TEXT);
        setBackgroundColor(Ui.BG);
        setGravity(Gravity.TOP | Gravity.START);
        setHorizontallyScrolling(true);
        setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        setHighlightColor(0x555B8CFF);
        setCursorVisible(true);
        numPaint.setTypeface(Typeface.MONOSPACE);
        numPaint.setColor(0xFF5B6478);
        gutterPaint.setColor(0xFF12151B);
        updateGutter();

        setFilters(new InputFilter[]{ (src, s, e, dest, ds, de) -> {
            if (!applying && e - s == 1 && src.charAt(s) == '\n') {
                int ls = ds;
                while (ls > 0 && dest.charAt(ls - 1) != '\n') ls--;
                int i = ls;
                while (i < ds && (dest.charAt(i) == ' ' || dest.charAt(i) == '\t')) i++;
                String indent = dest.subSequence(ls, i).toString();
                int k = ds - 1;
                while (k >= ls && dest.charAt(k) == ' ') k--;
                boolean open = k >= ls && (dest.charAt(k) == '{' || dest.charAt(k) == '(' || dest.charAt(k) == '[' || dest.charAt(k) == ':');
                return "\n" + indent + (open ? "    " : "");
            }
            return null;
        }});

        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                pendingStart = start;
                pendingBefore = applying ? "" : s.subSequence(start, start + count).toString();
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (applying) return;
                Edit ed = new Edit();
                ed.start = start;
                ed.before = pendingBefore;
                ed.after = s.subSequence(start, start + count).toString();
                // merge consecutive single-character typing into one undo step
                if (!history.undo.isEmpty() && ed.before.isEmpty() && ed.after.length() == 1 && !ed.after.equals("\n")) {
                    Edit last = history.undo.get(history.undo.size() - 1);
                    if (last.before.isEmpty() && last.start + last.after.length() == ed.start && last.after.length() < 40
                            && !last.after.endsWith(" ")) {
                        last.after += ed.after;
                        history.redo.clear();
                        return;
                    }
                }
                history.undo.add(ed);
                if (history.undo.size() > 400) history.undo.remove(0);
                history.redo.clear();
            }
            @Override public void afterTextChanged(Editable s) {
                if (applying) return;
                updateGutter();
                removeCallbacks(highlightJob);
                postDelayed(highlightJob, 300);
                if (listener != null) listener.onUserChange();
            }
        });
    }

    public void setChangeListener(ChangeListener l) { listener = l; }

    public void setFileName(String n) { fileName = n; }

    public History history() { return history; }

    /** Loads text without recording undo history or marking the document dirty. */
    public void load(String text, String name, History h, int cursor) {
        applying = true;
        fileName = name;
        history = h != null ? h : new History();
        setText(text);
        Highlighter.apply(getText(), fileName);
        updateGutter();
        applying = false;
        int c = Math.max(0, Math.min(cursor, getText().length()));
        setSelection(c);
    }

    public boolean undo() {
        if (history.undo.isEmpty()) return false;
        Edit e = history.undo.remove(history.undo.size() - 1);
        applying = true;
        try {
            getText().replace(e.start, e.start + e.after.length(), e.before);
            setSelection(e.start + e.before.length());
        } finally { applying = false; }
        history.redo.add(e);
        afterApply();
        return true;
    }

    public boolean redo() {
        if (history.redo.isEmpty()) return false;
        Edit e = history.redo.remove(history.redo.size() - 1);
        applying = true;
        try {
            getText().replace(e.start, e.start + e.before.length(), e.after);
            setSelection(e.start + e.after.length());
        } finally { applying = false; }
        history.undo.add(e);
        afterApply();
        return true;
    }

    private void afterApply() {
        updateGutter();
        removeCallbacks(highlightJob);
        postDelayed(highlightJob, 150);
        if (listener != null) listener.onUserChange();
    }

    /** Replace a range as a single undoable user edit (used by find/replace). */
    public void replaceRange(int a, int b, CharSequence with) { getText().replace(a, b, with); }

    public void insertAtCursor(String s, int cursorBack) {
        int a = Math.max(0, getSelectionStart()), b = Math.max(0, getSelectionEnd());
        getText().replace(Math.min(a, b), Math.max(a, b), s);
        int pos = Math.min(a, b) + s.length() - cursorBack;
        setSelection(Math.max(0, Math.min(pos, getText().length())));
    }

    public void goToLine(int line) {
        Layout l = getLayout();
        if (l == null) return;
        int ln = Math.max(1, Math.min(line, l.getLineCount())) - 1;
        int off = l.getLineStart(ln);
        requestFocus();
        setSelection(off);
        bringPointIntoView(off);
    }

    public void setCodeTextSize(float sp) {
        setTextSize(sp);
        numPaint.setTextSize(getTextSize() * 0.85f);
        updateGutter();
    }

    private void updateGutter() {
        int lines = Math.max(1, getLineCount());
        int digits = Math.max(2, String.valueOf(lines).length());
        numPaint.setTextSize(getTextSize() * 0.85f);
        gutterDigits = digits;
        int w = (int) (numPaint.measureText("8") * digits) + Ui.dp(getContext(), 14);
        if (getPaddingLeft() != w + Ui.dp(getContext(), 6)) {
            setPadding(w + Ui.dp(getContext(), 6), Ui.dp(getContext(), 6), Ui.dp(getContext(), 24), Ui.dp(getContext(), 120));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Layout layout = getLayout();
        if (layout == null) return;
        int sx = getScrollX();
        int gw = getPaddingLeft() - Ui.dp(getContext(), 6);
        int top = getScrollY(), bottom = getScrollY() + getHeight();
        canvas.drawRect(sx, top, sx + gw, bottom, gutterPaint);
        int first = layout.getLineForVertical(top), last = layout.getLineForVertical(bottom);
        CharSequence t = getText();
        for (int i = first; i <= last && i < layout.getLineCount(); i++) {
            int start = layout.getLineStart(i);
            boolean realLineStart = start == 0 || t.charAt(start - 1) == '\n';
            if (!realLineStart) continue;
            int num = 1;
            // count newlines before start (cheap enough with caching by walking from the first visible line)
            num = lineNumberAt(layout, i, t);
            String s = String.valueOf(num);
            float x = sx + gw - Ui.dp(getContext(), 6) - numPaint.measureText(s);
            canvas.drawText(s, x, layout.getLineBaseline(i) + getPaddingTop() * 0f, numPaint);
        }
    }

    private int cacheLine = -1, cacheNum = 1;

    @Override
    public void setHorizontallyScrolling(boolean whether) {
        super.setHorizontallyScrolling(whether);
        cacheLine = -1;
    }

    private int lineNumberAt(Layout layout, int visualLine, CharSequence t) {
        int from = 0, num = 1;
        if (cacheLine >= 0 && cacheLine <= visualLine && cacheLine < layout.getLineCount()) { from = cacheLine; num = cacheNum; }
        for (int i = from; i < visualLine; i++) {
            int end = layout.getLineEnd(i);
            if (end > 0 && end <= t.length() && t.charAt(end - 1) == '\n') num++;
        }
        cacheLine = visualLine; cacheNum = num;
        return num;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        cacheLine = -1; // invalidate line number cache
    }
}
