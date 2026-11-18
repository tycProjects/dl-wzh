package com.termux.hz;

import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regex based syntax colouring for the editor. Cheap and good enough for mod source files. */
public final class Highlighter {
    private Highlighter() {}

    static final int KEYWORD = 0xFFC792EA, STRING = 0xFFC3E88D, COMMENT = 0xFF6B7385, NUMBER = 0xFFF78C6C,
            ANNOT = 0xFFFFCB6B, TYPE = 0xFF82AAFF, KEY = 0xFF7FDBCA, TAG = 0xFFF07178;

    static final class Rule {
        final Pattern p; final int color; final int group;
        Rule(String re, int color, int flags, int group) { this.p = Pattern.compile(re, flags); this.color = color; this.group = group; }
    }

    private static final String JAVA_KW = "\\b(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|var|record|sealed|permits|yield|true|false|null|fun|val|object|when|is|in|override|data|companion|def|task|plugins|dependencies|repositories|apply)\\b";

    private static final List<Rule> CODE = new ArrayList<>();
    private static final List<Rule> JSON = new ArrayList<>();
    private static final List<Rule> PROPS = new ArrayList<>();
    private static final List<Rule> XML = new ArrayList<>();
    private static final List<Rule> MD = new ArrayList<>();
    static {
        CODE.add(new Rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE, 0, 0));
        CODE.add(new Rule("\\b\\d+(?:\\.\\d+)?[fFdDlL]?\\b|\\b0x[0-9a-fA-F]+\\b", NUMBER, 0, 0));
        CODE.add(new Rule(JAVA_KW, KEYWORD, 0, 0));
        CODE.add(new Rule("@[A-Za-z_][A-Za-z0-9_.]*", ANNOT, 0, 0));
        CODE.add(new Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'", STRING, 0, 0));
        CODE.add(new Rule("//[^\\n]*|/\\*[\\s\\S]*?\\*/", COMMENT, 0, 0));

        JSON.add(new Rule("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|\\b(?:true|false|null)\\b", NUMBER, 0, 0));
        JSON.add(new Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"", STRING, 0, 0));
        JSON.add(new Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"(?=\\s*:)", KEY, 0, 0));
        JSON.add(new Rule("//[^\\n]*|/\\*[\\s\\S]*?\\*/", COMMENT, 0, 0));

        PROPS.add(new Rule("^\\s*\\[[^\\]\\n]+\\]", ANNOT, Pattern.MULTILINE, 0));
        PROPS.add(new Rule("^\\s*[A-Za-z0-9_.\\-\"]+(?=\\s*[=:])", KEY, Pattern.MULTILINE, 0));
        PROPS.add(new Rule("\\b\\d+(?:\\.\\d+)*\\b|\\b(?:true|false)\\b", NUMBER, 0, 0));
        PROPS.add(new Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"|'[^'\\n]*'", STRING, 0, 0));
        PROPS.add(new Rule("^\\s*[#;][^\\n]*|\\s#[^\\n]*", COMMENT, Pattern.MULTILINE, 0));

        XML.add(new Rule("</?[A-Za-z_][\\w:.\\-]*|/?>", TAG, 0, 0));
        XML.add(new Rule("\\b[\\w:.\\-]+(?==)", KEY, 0, 0));
        XML.add(new Rule("\"[^\"]*\"|'[^']*'", STRING, 0, 0));
        XML.add(new Rule("<!--[\\s\\S]*?-->", COMMENT, 0, 0));

        MD.add(new Rule("^#{1,6} [^\\n]*", KEYWORD, Pattern.MULTILINE, 0));
        MD.add(new Rule("`[^`\\n]+`|```[\\s\\S]*?```", STRING, 0, 0));
        MD.add(new Rule("\\*\\*[^*\\n]+\\*\\*|\\[[^\\]\\n]+\\]\\([^)\\n]+\\)", TYPE, 0, 0));
    }

    static List<Rule> rulesFor(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".json") || n.endsWith(".mcmeta") || n.endsWith(".json5")) return JSON;
        if (n.endsWith(".properties") || n.endsWith(".toml") || n.endsWith(".cfg") || n.endsWith(".ini")
                || n.endsWith(".yml") || n.endsWith(".yaml") || n.endsWith(".conf") || n.endsWith(".sh")) return PROPS;
        if (n.endsWith(".xml") || n.endsWith(".html") || n.endsWith(".pom")) return XML;
        if (n.endsWith(".md") || n.endsWith(".txt")) return n.endsWith(".md") ? MD : null;
        if (n.endsWith(".java") || n.endsWith(".kt") || n.endsWith(".kts") || n.endsWith(".gradle")
                || n.endsWith(".groovy") || n.endsWith(".js") || n.endsWith(".ts") || n.endsWith(".scala")
                || n.endsWith(".mixins.json") || n.endsWith(".c") || n.endsWith(".cpp") || n.endsWith(".h")) return CODE;
        return null;
    }

    static final class HlSpan extends ForegroundColorSpan {
        HlSpan(int c) { super(c); }
    }

    /** Re-colours the whole editable. Later rules override earlier ones. */
    public static void apply(Editable e, String fileName) {
        HlSpan[] old = e.getSpans(0, e.length(), HlSpan.class);
        for (HlSpan s : old) e.removeSpan(s);
        List<Rule> rules = rulesFor(fileName);
        if (rules == null || e.length() == 0 || e.length() > 300_000) return;
        String s = e.toString();
        int[] col = new int[s.length()];
        for (Rule r : rules) {
            Matcher m = r.p.matcher(s);
            while (m.find()) {
                int a = m.start(r.group), b = m.end(r.group);
                for (int i = a; i < b; i++) col[i] = r.color;
            }
        }
        int i = 0, n = col.length;
        while (i < n) {
            int c = col[i];
            if (c == 0) { i++; continue; }
            int j = i + 1;
            while (j < n && col[j] == c) j++;
            e.setSpan(new HlSpan(c), i, j, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = j;
        }
    }
}
