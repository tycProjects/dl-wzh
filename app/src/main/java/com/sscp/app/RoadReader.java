package com.sscp.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Đọc 4 bảng cầu ở nửa dưới màn hình bàn Baccarat:
 *   - Big Road (bảng lớn, 6 hàng)             : vòng tròn đỏ = Nhà Cái (B), xanh = Tay con (P), gạch/số xanh lá = Hòa
 *   - Big Eye Boy / Small Road / Cockroach Pig : 3 cầu phụ (đỏ / xanh)
 * Chỉ dựa trên điểm ảnh thật (màu), không OCR, không đoán. Không phụ thuộc Android.
 *
 * Định vị tự động theo các đường kẻ đen của khung (nên chạy được ở nhiều độ phân giải),
 * sau đó tự căn lưới bằng cách tối đa hóa độ khớp với các vòng tròn thực tế.
 */
public final class RoadReader {

    public static final class Snapshot {
        /** Các ván theo thứ tự thời gian: 'B' hoặc 'P' (Hòa không chiếm ô trên Big Road). */
        public final String big;
        /** tie[i] = true nếu ván i có dấu Hòa (gạch / số xanh lá) kèm theo. */
        public final boolean[] tie;
        /** Cầu phụ theo thứ tự thời gian: 'R' đỏ, 'U' xanh. */
        public final String eye, small, roach;
        /** Số ô nhìn thấy nhưng không chắc màu, hoặc ô không xếp được vào cột nào. 0 = đọc sạch. */
        public final int uncertain;
        public final String dump;

        Snapshot(String big, boolean[] tie, String eye, String small, String roach, int uncertain, String dump) {
            this.big = big; this.tie = tie; this.eye = eye; this.small = small; this.roach = roach;
            this.uncertain = uncertain; this.dump = dump;
        }

        /** Khóa so sánh giữa các khung hình. */
        public String signature() {
            StringBuilder t = new StringBuilder();
            for (boolean b : tie) t.append(b ? 't' : '.');
            return big + "|" + t + "|" + eye + "|" + small + "|" + roach;
        }

        public boolean sameAs(Snapshot o) {
            if (o == null) return false;
            return big.equals(o.big) && java.util.Arrays.equals(tie, o.tie)
                    && eye.equals(o.eye) && small.equals(o.small) && roach.equals(o.roach);
        }
    }

    // ------------------------------------------------------------------ API

    /**
     * roi: ARGB của các hàng [roiY0, roiY0 + roiRows) của màn hình, rộng w.
     * Trả null nếu không thấy khung 4 bảng.
     */
    public static Snapshot read(int[] roi, int w, int roiY0, int roiRows) {
        return new RoadReader(roi, w, roiY0, roiRows).run();
    }

    // ------------------------------------------------------------------ nội bộ

    private static final int NONE = 0, RED = 1, BLUE = 2, GREEN = 3, OTHER = 4;
    private final int[] px;
    private final int w, y0, rows;

    private RoadReader(int[] px, int w, int y0, int rows) {
        this.px = px; this.w = w; this.y0 = y0; this.rows = rows;
    }

    private int pix(int x, int y) {
        if (x < 0 || x >= w || y < y0 || y >= y0 + rows) return 0xFFFFFFFF;
        return px[(y - y0) * w + x];
    }

    private static int hue(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        int d = max - min;
        if (d < 35 || max < 70) return NONE;
        float s = d / (float) max;
        if (s < 0.22f) return NONE;
        float h;
        if (max == r) h = 60f * (((g - b) / (float) d) % 6f);
        else if (max == g) h = 60f * (((b - r) / (float) d) + 2f);
        else h = 60f * (((r - g) / (float) d) + 4f);
        if (h < 0) h += 360f;
        if (h < 25f || h >= 335f) return RED;
        if (h >= 75f && h < 170f) return GREEN;
        if (h >= 175f && h < 260f) return BLUE;
        return OTHER;
    }

    private boolean isDark(int x, int y) {
        int c = pix(x, y);
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        return max < 90 && max - min < 40;
    }

    private static final class Run { int s, e; }

    private Snapshot run() {
        // 1) Các dải ngang đen: [thanh trên của bảng] [đường giữa Big Road / cầu phụ] [đáy khung]
        int xa = (int) (w * 0.31f), xb = (int) (w * 0.97f);
        List<Run> hr = new ArrayList<>();
        Run cur = null;
        for (int y = y0; y < y0 + rows; y++) {
            int dark = 0, tot = 0;
            for (int x = xa; x < xb; x += 2) { tot++; if (isDark(x, y)) dark++; }
            boolean on = dark > tot * 0.6f;
            if (on) {
                if (cur == null) { cur = new Run(); cur.s = y; hr.add(cur); }
                cur.e = y;
            } else cur = null;
        }
        Run top = null, mid = null, bot = null;
        for (int i = 0; i + 2 < hr.size(); i++) {
            Run a = hr.get(i), b = hr.get(i + 1), c = hr.get(i + 2);
            int bigH = b.s - a.e - 1, smallH = c.s - b.e - 1;
            if (bigH < 60 || smallH < 30) continue;
            float ratio = bigH / (float) smallH;
            if (ratio > 1.7f && ratio < 2.3f) { top = a; mid = b; bot = c; break; }
        }
        if (top == null) return null;

        float pitchY = (mid.s - top.e - 1) / 6f;      // ô Big Road
        float pitchS = (bot.s - mid.s) / 6f;          // ô cầu phụ
        if (pitchY < 8 || pitchS < 4) return null;

        // 2) Các đường dọc đen trong vùng cầu phụ
        List<Run> vr = new ArrayList<>();
        cur = null;
        int ya = mid.e + 2, yb = bot.s - 2;
        for (int x = (int) (w * 0.22f); x < w; x++) {
            int dark = 0, tot = 0;
            for (int y = ya; y <= yb; y += 2) { tot++; if (isDark(x, y)) dark++; }
            boolean on = dark > tot * 0.6f;
            if (on) {
                if (cur == null) { cur = new Run(); cur.s = x; vr.add(cur); }
                cur.e = x;
            } else cur = null;
        }
        float v0, v1, v2;
        if (vr.size() >= 3) { v0 = vr.get(0).s; v1 = vr.get(1).s; v2 = vr.get(2).s; }
        else { v0 = w * 0.2917f; v1 = w * 0.528f; v2 = w * 0.765f; }

        StringBuilder dump = new StringBuilder();

        // 3) Big Road
        int bigCols = Math.max(1, Math.min(40, (int) ((w - (v0 + 1)) / (pitchY * 1.01f))));
        float[] best = refineBig(v0 + 1, top.e + 1, pitchY, bigCols);
        float bx = best[0], bpx = best[1], by = best[2];
        int[][] bigG = new int[bigCols][6];
        boolean[][] tieG = new boolean[bigCols][6];
        int unc = 0;
        for (int c = 0; c < bigCols; c++) {
            for (int r = 0; r < 6; r++) {
                float cx = bx + (c + 0.5f) * bpx, cy = by + (r + 0.5f) * pitchY;
                int[] rc = ringVotes(cx, cy, pitchY);
                int red = rc[0], blue = rc[1];
                int m = Math.max(red, blue);
                if (m >= 9) {
                    bigG[c][r] = red >= blue ? 'B' : 'P';
                } else if (m >= 4) {
                    // Có vành nhưng yếu (biểu tượng Rồng/Hổ): lấy màu chiếm ưu thế trong ô
                    int dr, db;
                    int[] dc = new int[2];
                    discCount(cx, cy, pitchY * 0.42f, dc);
                    dr = dc[0]; db = dc[1];
                    if (dr + db > 0) { bigG[c][r] = dr >= db ? 'B' : 'P'; unc++; }
                }
                if (bigG[c][r] != 0) {
                    tieG[c][r] = greenFraction(cx, cy, pitchY * 0.42f) > 0.045f;
                }
            }
        }
        List<int[]> bigEntries = parse(bigG, bigCols, 6);
        int used = bigEntries.size(), occ = 0;
        for (int c = 0; c < bigCols; c++) for (int r = 0; r < 6; r++) if (bigG[c][r] != 0) occ++;
        unc += Math.abs(occ - used);
        StringBuilder big = new StringBuilder();
        boolean[] ties = new boolean[bigEntries.size()];
        for (int i = 0; i < bigEntries.size(); i++) {
            int[] e = bigEntries.get(i);
            big.append((char) e[0]);
            ties[i] = tieG[e[1]][e[2]];
        }
        dump.append("BIG ROAD\n").append(gridText(bigG, bigCols, 6, tieG));

        // 4) Ba cầu phụ
        float[] starts = {v0 + 1, v1 + 1, v2 + 1};
        String[] seqs = new String[3];
        String[] names = {"BIG EYE", "SMALL", "COCKROACH"};
        for (int k = 0; k < 3; k++) {
            float xEnd = (k < 2) ? (k == 0 ? v1 : v2) : w;
            int cols = Math.max(1, Math.min(30, (int) ((xEnd - starts[k]) / pitchS + 0.3f)));
            float[] bs = refineSmall(starts[k], mid.s, pitchS, cols);
            int[][] g = new int[cols][6];
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < 6; r++) {
                    float cx = bs[0] + (c + 0.5f) * bs[1], cy = mid.s + (r + 0.5f) * pitchS;
                    int[] dc = new int[2];
                    int tot = discCount(cx, cy, pitchS * 0.42f, dc);
                    if (dc[0] + dc[1] >= Math.max(3, tot * 0.12f)) g[c][r] = dc[0] >= dc[1] ? 'R' : 'U';
                }
            }
            List<int[]> es = parse(g, cols, 6);
            int o = 0;
            for (int c = 0; c < cols; c++) for (int r = 0; r < 6; r++) if (g[c][r] != 0) o++;
            unc += Math.abs(o - es.size());
            StringBuilder sb = new StringBuilder();
            for (int[] e : es) sb.append((char) e[0]);
            seqs[k] = sb.toString();
            dump.append(names[k]).append('\n').append(gridText(g, cols, 6, null));
        }
        return new Snapshot(big.toString(), ties, seqs[0], seqs[1], seqs[2], unc, dump.toString());
    }

    // ---------------------------------------------------------------- lấy mẫu

    private static final int ANG = 24;
    private static final double[] COS = new double[ANG], SIN = new double[ANG];
    static {
        for (int i = 0; i < ANG; i++) { COS[i] = Math.cos(2 * Math.PI * i / ANG); SIN[i] = Math.sin(2 * Math.PI * i / ANG); }
    }

    /** Đếm số hướng có điểm đỏ / xanh trên vành tròn quanh (cx,cy). */
    private int[] ringVotes(float cx, float cy, float p) {
        int red = 0, blue = 0;
        for (int a = 0; a < ANG; a++) {
            int found = NONE;
            for (float f = 0.30f; f <= 0.47f && found == NONE; f += 0.03f) {
                int x = Math.round((float) (cx + COS[a] * p * f)), y = Math.round((float) (cy + SIN[a] * p * f));
                int h = hue(pix(x, y));
                if (h == RED || h == BLUE) found = h;
            }
            if (found == RED) red++; else if (found == BLUE) blue++;
        }
        return new int[]{red, blue};
    }

    private int ringScore(float cx, float cy, float p) {
        int s = 0;
        for (int a = 0; a < ANG; a++) {
            for (float f = 0.37f; f <= 0.44f; f += 0.035f) {
                int x = Math.round((float) (cx + COS[a] * p * f)), y = Math.round((float) (cy + SIN[a] * p * f));
                int h = hue(pix(x, y));
                if (h == RED || h == BLUE) { s++; break; }
            }
        }
        return s;
    }

    /** Đếm điểm đỏ/xanh trong hình tròn bán kính rad; trả về tổng số điểm xét. */
    private int discCount(float cx, float cy, float rad, int[] out) {
        int tot = 0, red = 0, blue = 0;
        int x0 = Math.round(cx - rad), x1 = Math.round(cx + rad), ya = Math.round(cy - rad), yb = Math.round(cy + rad);
        for (int y = ya; y <= yb; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy > rad * rad) continue;
                tot++;
                int h = hue(pix(x, y));
                if (h == RED) red++; else if (h == BLUE) blue++;
            }
        }
        out[0] = red; out[1] = blue;
        return tot;
    }

    private float greenFraction(float cx, float cy, float rad) {
        int tot = 0, g = 0;
        int x0 = Math.round(cx - rad), x1 = Math.round(cx + rad), ya = Math.round(cy - rad), yb = Math.round(cy + rad);
        for (int y = ya; y <= yb; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy > rad * rad) continue;
                tot++;
                if (hue(pix(x, y)) == GREEN) g++;
            }
        }
        return tot == 0 ? 0 : g / (float) tot;
    }

    // ---------------------------------------------------------------- tự căn lưới

    private float[] refineBig(float x0, float yTop, float pitchY, int cols) {
        float bestS = -1, bx = x0 + 0, bp = pitchY * 1.01f, by = yTop;
        float sc = pitchY / 28f;
        for (float dp = -0.04f; dp <= 0.041f; dp += 0.01f) {
            float pX = pitchY * (1.01f + dp);
            for (float dx = -4 * sc; dx <= 4.01f * sc; dx += Math.max(1f, sc)) {
                for (float dy = -3 * sc; dy <= 3.01f * sc; dy += Math.max(1f, sc)) {
                    int s = 0;
                    for (int c = 0; c < cols; c++) {
                        for (int r = 0; r < 6; r++) {
                            int v = ringScore(x0 + dx + (c + 0.5f) * pX, yTop + dy + (r + 0.5f) * pitchY, pitchY);
                            if (v >= 9) s += v;   // chỉ tính ô thật sự có vành
                        }
                    }
                    if (s > bestS) { bestS = s; bx = x0 + dx; bp = pX; by = yTop + dy; }
                }
            }
        }
        return new float[]{bx, bp, by};
    }

    private float[] refineSmall(float x0, float yTop, float pitchS, int cols) {
        float bestS = -1, bx = x0, bp = pitchS;
        int[] dc = new int[2];
        for (float dp = -0.04f; dp <= 0.041f; dp += 0.02f) {
            float pX = pitchS * (1f + dp);
            for (float dx = -2; dx <= 2.01f; dx += 1f) {
                float s = 0;
                for (int c = 0; c < cols; c++) {
                    for (int r = 0; r < 6; r++) {
                        int tot = discCount(x0 + dx + (c + 0.5f) * pX, yTop + (r + 0.5f) * pitchS, pitchS * 0.42f, dc);
                        if (tot > 0 && (dc[0] + dc[1]) >= Math.max(3, tot * 0.12f)) s += (dc[0] + dc[1]) / (float) tot;
                    }
                }
                if (s > bestS) { bestS = s; bx = x0 + dx; bp = pX; }
            }
        }
        return new float[]{bx, bp};
    }

    // ---------------------------------------------------------------- ghép lưới thành chuỗi thời gian

    /**
     * Quy tắc Big Road: cùng màu đi xuống; khi hết chỗ (hàng 6) hoặc bị chặn thì "đuôi rồng" đi ngang;
     * đổi màu thì sang cột kế tiếp (cột vật lý của ván đầu chuỗi + 1) ở hàng trên cùng.
     * Trả về danh sách {màu, cột, hàng} theo thứ tự thời gian.
     */
    static List<int[]> parse(int[][] g, int cols, int rowsN) {
        List<int[]> out = new ArrayList<>();
        int start = 0, prev = 0;
        while (start < cols && g[start][0] != 0) {
            int color = g[start][0];
            if (color == prev) break;
            int r = 0;
            while (r < rowsN && g[start][r] == color) { out.add(new int[]{color, start, r}); r++; }
            int lastRow = r - 1, lastCol = start;
            while (lastCol + 1 < cols && g[lastCol + 1][lastRow] == color) {
                lastCol++;
                out.add(new int[]{color, lastCol, lastRow});
            }
            prev = color;
            start++;
        }
        return out;
    }

    private static String gridText(int[][] g, int cols, int rowsN, boolean[][] tie) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rowsN; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = g[c][r] == 0 ? '.' : (char) g[c][r];
                if (tie != null && tie[c][r]) ch = Character.toLowerCase(ch);
                sb.append(ch);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
