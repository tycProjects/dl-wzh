package com.example.chatzipai;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

/** Hiển thị markdown cơ bản: **đậm**, `code`, ```khối code```, # tiêu đề, danh sách, bảng. */
final class Md {
    private static final int FLAG = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;

    private Md() { }

    static CharSequence render(String src) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String[] lines = src.split("\n", -1);
        boolean code = false;
        for (String ln : lines) {
            String tr = ln.trim();
            if (tr.startsWith("```")) { code = !code; continue; }
            if (sb.length() > 0) sb.append('\n');
            if (code) {
                int st = sb.length();
                sb.append(ln.isEmpty() ? " " : ln);
                codeSpans(sb, st, sb.length());
                continue;
            }
            if (tr.matches("^#{1,6}\\s+.*")) {
                int lvl = 0;
                while (lvl < tr.length() && tr.charAt(lvl) == '#') lvl++;
                int st = sb.length();
                inline(sb, tr.substring(lvl).trim());
                sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), FLAG);
                sb.setSpan(new RelativeSizeSpan(lvl <= 2 ? 1.25f : 1.1f), st, sb.length(), FLAG);
                continue;
            }
            if (tr.matches("^(-{3,}|\\*{3,}|_{3,})$")) { sb.append("──────────"); continue; }
            if (tr.startsWith("|")) {
                if (tr.matches("^[|\\-:\\s]+$")) { removeTrailingNewline(sb); continue; }
                String row = tr.replaceAll("^\\|", "").replaceAll("\\|$", "").replace("|", "  │  ").trim();
                int st = sb.length();
                inline(sb, row);
                sb.setSpan(new TypefaceSpan("monospace"), st, sb.length(), FLAG);
                continue;
            }
            int lead = 0;
            while (lead < ln.length() && ln.charAt(lead) == ' ') lead++;
            if (tr.startsWith("- ") || tr.startsWith("* ") || tr.startsWith("• ")) {
                for (int i = 0; i < lead; i++) sb.append(' ');
                sb.append("•  ");
                inline(sb, tr.substring(2).trim());
                continue;
            }
            if (tr.startsWith(">")) {
                int st = sb.length();
                inline(sb, tr.substring(1).trim());
                sb.setSpan(new ForegroundColorSpan(0xFF6B7280), st, sb.length(), FLAG);
                continue;
            }
            inline(sb, ln);
        }
        return sb;
    }

    private static void removeTrailingNewline(SpannableStringBuilder sb) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.delete(sb.length() - 1, sb.length());
    }

    private static void codeSpans(SpannableStringBuilder sb, int st, int en) {
        sb.setSpan(new TypefaceSpan("monospace"), st, en, FLAG);
        sb.setSpan(new BackgroundColorSpan(0xFFF1F3F5), st, en, FLAG);
        sb.setSpan(new RelativeSizeSpan(0.88f), st, en, FLAG);
        sb.setSpan(new ForegroundColorSpan(0xFF1F2937), st, en, FLAG);
    }

    private static void inline(SpannableStringBuilder sb, String s) {
        int i = 0, n = s.length();
        while (i < n) {
            if (s.startsWith("**", i)) {
                int e = s.indexOf("**", i + 2);
                if (e > i + 2) {
                    int st = sb.length();
                    inline(sb, s.substring(i + 2, e));
                    sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), FLAG);
                    i = e + 2;
                    continue;
                }
            }
            char c = s.charAt(i);
            if (c == '`') {
                int e = s.indexOf('`', i + 1);
                if (e > i + 1) {
                    int st = sb.length();
                    sb.append(s, i + 1, e);
                    codeSpans(sb, st, sb.length());
                    i = e + 1;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
    }
}
