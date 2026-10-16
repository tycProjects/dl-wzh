package com.sscp.app;

import java.util.ArrayList;
import java.util.List;

/**
 * "Bộ não" phân tích cầu. Làm 3 việc, tất cả dựa trên dữ liệu THẬT đọc từ màn hình:
 *  1. Giữ lịch sử ván của ghế hiện tại, ghép các lần đọc (kể cả khi Big Road cuộn sang trái).
 *  2. Nhận dạng dạng cầu đang chạy: bệt, 1-1, 2-2, 3-3, a-b.
 *  3. Thống kê trong CHÍNH ghế này: chuỗi hiện tại trước đây đã tiếp tục hay đã gãy bao nhiêu lần.
 *
 * Không dự đoán ván sau: mỗi ván độc lập; nhận dạng cầu chỉ MÔ TẢ quá khứ.
 */
public final class RoadBrain {

    public static final class Display {
        public final String headline;   // "P · Tay con"
        public final int color;         // màu chữ headline
        public final String pattern;    // dạng cầu
        public final String subRoads;   // 3 cầu phụ
        public final String stats;      // thống kê ghế
        public final String history;    // các ván cuối
        public final boolean newHand;   // ván mới vừa được ghi nhận ở lần đọc này
        Display(String headline, int color, String pattern, String subRoads, String stats, String history, boolean newHand) {
            this.headline = headline; this.color = color; this.pattern = pattern;
            this.subRoads = subRoads; this.stats = stats; this.history = history; this.newHand = newHand;
        }
    }

    private final StringBuilder hist = new StringBuilder();   // 'B' / 'P'
    private final StringBuilder tie = new StringBuilder();    // 't' hoặc '.', cùng độ dài hist

    public String history() { return hist.toString(); }

    public void reset() { hist.setLength(0); tie.setLength(0); }

    /** Ghép 1 lần đọc ổn định vào lịch sử. Trả về số ván mới được thêm (-1 nếu bắt đầu ghế mới). */
    public int merge(RoadReader.Snapshot s) {
        String w = s.big;
        if (w.isEmpty()) return 0;
        if (hist.length() == 0) { load(s); return 0; }
        String h = hist.toString();
        for (int drop = 0; drop <= h.length(); drop++) {
            String keep = h.substring(drop);
            if (keep.length() < Math.min(4, h.length())) break;   // phần trùng quá ngắn thì không đáng tin
            if (!w.startsWith(keep)) continue;
            int added = w.length() - keep.length();
            if (added > 3) break;   // nhảy quá nhiều ván cùng lúc: coi như đọc lại từ đầu
            // cập nhật cờ Hòa của phần trùng (Hòa có thể xuất hiện sau, trên ô cuối)
            for (int i = 0; i < keep.length(); i++) tie.setCharAt(drop + i, s.tie[i] ? 't' : '.');
            for (int i = keep.length(); i < w.length(); i++) { hist.append(w.charAt(i)); tie.append(s.tie[i] ? 't' : '.'); }
            return added;
        }
        if (w.length() <= 6 && h.length() >= 12) { load(s); return -1; }   // ghế mới
        load(s);
        return 0;
    }

    private void load(RoadReader.Snapshot s) {
        reset();
        hist.append(s.big);
        for (boolean b : s.tie) tie.append(b ? 't' : '.');
    }

    public Display describe(RoadReader.Snapshot s, boolean newHand) {
        String h = hist.toString();
        if (h.isEmpty()) return null;
        char last = h.charAt(h.length() - 1);
        boolean lastTie = tie.charAt(tie.length() - 1) == 't';
        String headline = (last == 'B' ? "B · Nhà Cái" : "P · Tay con") + (lastTie ? " + Hòa" : "");
        int color = last == 'B' ? 0xFFFF526B : 0xFF36A7FF;

        List<int[]> runs = runs(h);
        int streak = runs.get(runs.size() - 1)[1];
        String pattern = "Cầu: " + pattern(runs) + " · chuỗi " + streak;

        String subRoads = "Cầu phụ  Mắt " + name(s.eye) + " · Nhỏ " + name(s.small) + " · Gián " + name(s.roach);

        int b = 0, p = 0, t = 0;
        for (int i = 0; i < h.length(); i++) {
            if (h.charAt(i) == 'B') b++; else p++;
            if (tie.charAt(i) == 't') t++;
        }
        String cont = continuation(runs, streak);
        String stats = "Ghế này: B " + b + " · P " + p + " · ô Hòa " + t + " (" + h.length() + " ván)" + cont;

        String tail = h.length() > 14 ? h.substring(h.length() - 14) : h;
        return new Display(headline, color, pattern, subRoads, stats, "Gần nhất: " + tail, newHand);
    }

    private static String name(String seq) {
        if (seq == null || seq.isEmpty()) return "—";
        return seq.charAt(seq.length() - 1) == 'R' ? "đỏ" : "xanh";
    }

    /** {bên, độ dài} theo thứ tự; bên: 0 = B, 1 = P. */
    private static List<int[]> runs(String h) {
        List<int[]> r = new ArrayList<>();
        for (int i = 0; i < h.length(); ) {
            int j = i;
            while (j < h.length() && h.charAt(j) == h.charAt(i)) j++;
            r.add(new int[]{h.charAt(i) == 'B' ? 0 : 1, j - i});
            i = j;
        }
        return r;
    }

    static String pattern(List<int[]> runs) {
        int n = runs.size();
        int[] last = runs.get(n - 1);
        String side = last[0] == 0 ? "B" : "P";
        int k3 = trailing(runs, 3);
        if (k3 >= 2) return "3-3 (" + k3 + " cặp)";
        if (last[1] >= 3) return "bệt " + side + " ×" + last[1];
        int k1 = trailing(runs, 1);
        if (k1 >= 4) return "1-1 ping-pong (" + k1 + " nhịp)";
        int k2 = trailing(runs, 2);
        if (k2 >= 2) return "2-2 (" + k2 + " cặp)";
        if (n >= 4) {
            int a = runs.get(n - 1)[1], b = runs.get(n - 2)[1];
            if (a != b && runs.get(n - 3)[1] == a && runs.get(n - 4)[1] == b) return b + "-" + a + " lặp";
        }
        return "chưa thành cầu rõ";
    }

    private static int trailing(List<int[]> runs, int len) {
        int k = 0;
        for (int i = runs.size() - 1; i >= 0 && runs.get(i)[1] == len; i--) k++;
        return k;
    }

    /**
     * Trong chính ghế này: các chuỗi ĐÃ KẾT THÚC mà độ dài >= chuỗi hiện tại thì bao nhiêu chuỗi dài hơn
     * (tiếp tục) so với đúng bằng (gãy ngay sau đó). Chỉ là thống kê quá khứ.
     */
    private static String continuation(List<int[]> runs, int streak) {
        int reached = 0, went = 0;
        for (int i = 0; i < runs.size() - 1; i++) {   // bỏ chuỗi đang chạy
            int len = runs.get(i)[1];
            if (len >= streak) { reached++; if (len > streak) went++; }
        }
        if (reached < 3) return "";
        return "\nChuỗi " + streak + " trước đây: tiếp tục " + went + "/" + reached + " lần (thống kê, không phải dự đoán)";
    }
}
