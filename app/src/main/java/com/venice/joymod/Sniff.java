package com.venice.joymod;

public final class Sniff {

    private static final String[] Q = {
            "fila", "queue", "posicao", "posicao na", "position", "aguard",
            "waiting", "estimad", "estimated", "a frente", "ahead", "minutos",
            "minutes", "vezes a frente", "previsao"
    };

    private static final String[] IGNORE = {
            "joyark", "cloud", "gaming", "servidor", "server", "entrar", "play",
            "jogar", "cancelar", "voltar", "ok", "todos", "pc virtual"
    };

    public static String norm(String s) {
        if (s == null) return "";
        String r = s.toLowerCase();
        r = r.replace('a', 'a');
        r = r.replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a");
        r = r.replace("é", "e").replace("ê", "e").replace("í", "i");
        r = r.replace("ó", "o").replace("ô", "o").replace("õ", "o").replace("ú", "u");
        r = r.replace("ç", "c");
        return r;
    }

    public static boolean hasQueue(String s) {
        String l = norm(s);
        if (l.length() == 0) return false;
        for (String k : Q) if (l.contains(k)) return true;
        return false;
    }

    public static String queueLine(String s) {
        if (s == null) return null;
        String[] lines = s.split("\n");
        for (String ln : lines) {
            String l = norm(ln);
            for (String k : Q) {
                if (l.contains(k)) {
                    String t = ln.trim();
                    return t.length() > 70 ? t.substring(0, 70) : t;
                }
            }
        }
        return null;
    }

    public static String gameTitle(String s) {
        if (s == null) return null;
        String best = null;
        String[] lines = s.split("\n");
        for (String ln : lines) {
            String t = ln.trim();
            if (t.length() < 3 || t.length() > 40) continue;
            if (t.matches("[0-9:.,%s\\-]+")) continue;
            String l = norm(t);
            boolean bad = false;
            for (String k : Q) if (l.contains(k)) { bad = true; break; }
            if (bad) continue;
            for (String k : IGNORE) if (l.equals(k)) { bad = true; break; }
            if (bad) continue;
            if (best == null || t.length() > best.length()) best = t;
        }
        return best;
    }

    public static boolean inStream(String s) {
        ScreenReader r = ScreenReader.get();
        if (r == null) return false;
        if (!QueueEngine.PKG.equals(r.getForeground())) return false;
        if (r.msSinceEvent() > 4000) return false;
        return !hasQueue(s);
    }
}
