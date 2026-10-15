package com.sscp.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đọc bảng "bead plate" (các hình tròn B / P / T xếp theo hàng) trên màn hình bàn Baccarat.
 * Đọc dữ liệu THẬT trên màn hình bằng phân tích màu của điểm ảnh, không OCR, không đoán.
 * Lớp này không phụ thuộc Android để có thể kiểm thử trên máy tính.
 *
 * Quy ước: ô được sắp theo hàng (trên xuống dưới, trái sang phải); ô cuối = ván mới nhất.
 * Ký tự: 'B' Nhà Cái, 'P' Tay con, 'T' Hòa, 'b'/'p' = ô có biểu tượng Rồng/Hổ
 * (nhận theo màu viền; ngoặc là suy luận từ màu, nên hiển thị riêng).
 */
public final class BeadPlateReader {

    public static final class Result {
        public final String cells;   // chuỗi các ô đã đọc theo thứ tự
        public Result(String cells) { this.cells = cells; }
        public boolean isEmpty() { return cells.isEmpty(); }
        public char last() { return cells.isEmpty() ? 0 : cells.charAt(cells.length() - 1); }
    }

    private static final class Blob {
        int minX, minY, maxX, maxY, area;
        long sumX, sumY;
        int red, blue, green, other;
        char type;
        int row, col;
        int w() { return maxX - minX + 1; }
        int h() { return maxY - minY + 1; }
        float cx() { return (float) sumX / area; }
        float cy() { return (float) sumY / area; }
    }

    /** pixels: ARGB, width*height. Trả về null nếu không thấy bảng. */
    public static Result read(int[] px, int width, int height) {
        int x0 = 0, x1 = (int) (width * 0.31f);
        int y0 = (int) (height * 0.45f), y1 = (int) (height * 0.97f);
        int rw = x1 - x0, rh = y1 - y0;
        if (rw < 20 || rh < 20) return null;

        // 0 = nền, 1 = đỏ, 2 = xanh dương, 3 = xanh lá, 4 = màu bão hòa khác, 5 = trắng (chữ)
        byte[] cls = new byte[rw * rh];
        for (int y = 0; y < rh; y++) {
            for (int x = 0; x < rw; x++) {
                cls[y * rw + x] = classify(px[(y + y0) * width + (x + x0)]);
            }
        }

        // Thành phần liên thông của mọi điểm "có màu" (loại nền/đường kẻ xám).
        boolean[] seen = new boolean[rw * rh];
        List<Blob> blobs = new ArrayList<>();
        int[] stack = new int[rw * rh];
        for (int i = 0; i < cls.length; i++) {
            if (seen[i] || cls[i] == 0 || cls[i] == 5) continue;
            Blob b = new Blob();
            b.minX = b.minY = Integer.MAX_VALUE;
            b.maxX = b.maxY = -1;
            int sp = 0;
            stack[sp++] = i;
            seen[i] = true;
            while (sp > 0) {
                int p = stack[--sp];
                int px_ = p % rw, py_ = p / rw;
                b.area++;
                b.sumX += px_; b.sumY += py_;
                if (px_ < b.minX) b.minX = px_;
                if (px_ > b.maxX) b.maxX = px_;
                if (py_ < b.minY) b.minY = py_;
                if (py_ > b.maxY) b.maxY = py_;
                switch (cls[p]) {
                    case 1: b.red++; break;
                    case 2: b.blue++; break;
                    case 3: b.green++; break;
                    default: b.other++;
                }
                if (px_ > 0 && !seen[p - 1] && cls[p - 1] != 0 && cls[p - 1] != 5) { seen[p - 1] = true; stack[sp++] = p - 1; }
                if (px_ < rw - 1 && !seen[p + 1] && cls[p + 1] != 0 && cls[p + 1] != 5) { seen[p + 1] = true; stack[sp++] = p + 1; }
                if (py_ > 0 && !seen[p - rw] && cls[p - rw] != 0 && cls[p - rw] != 5) { seen[p - rw] = true; stack[sp++] = p - rw; }
                if (py_ < rh - 1 && !seen[p + rw] && cls[p + rw] != 0 && cls[p + rw] != 5) { seen[p + rw] = true; stack[sp++] = p + rw; }
            }
            blobs.add(b);
        }

        // Giữ lại các hình tròn đặc (loại vòng rỗng của bảng Big Road và nhiễu).
        List<Blob> discs = new ArrayList<>();
        for (Blob b : blobs) {
            int w = b.w(), h = b.h();
            if (w < 8 || h < 8) continue;
            float aspect = (float) w / h;
            if (aspect < 0.75f || aspect > 1.33f) continue;
            float fill = (float) b.area / (w * h);
            if (fill < 0.55f) continue;
            discs.add(b);
        }
        if (discs.size() < 1) return null;

        // Kích thước chuẩn = trung vị đường kính; loại blob lệch kích thước.
        List<Integer> sizes = new ArrayList<>();
        for (Blob b : discs) sizes.add(b.w());
        Collections.sort(sizes);
        int d = sizes.get(sizes.size() / 2);
        List<Blob> cells = new ArrayList<>();
        for (Blob b : discs) {
            if (Math.abs(b.w() - d) <= Math.max(3, d * 0.25f)) cells.add(b);
        }
        if (cells.isEmpty()) return null;

        // Loại: bảng bead plate nằm ở ngoài cùng bên trái; bỏ blob quá xa cột đầu (nhiễu video).
        float minCx = Float.MAX_VALUE;
        for (Blob b : cells) minCx = Math.min(minCx, b.cx());
        List<Blob> kept = new ArrayList<>();
        for (Blob b : cells) if (b.cx() - minCx < d * 6.5f) kept.add(b);
        cells = kept;

        // Gom hàng theo y để dựng lưới ô.
        Collections.sort(cells, (a, c) -> Float.compare(a.cy(), c.cy()));
        List<Float> rowYs = new ArrayList<>();
        float rowY = cells.get(0).cy();
        rowYs.add(rowY);
        for (Blob b : cells) {
            if (b.cy() - rowY > d * 0.6f) { rowY = b.cy(); rowYs.add(rowY); }
        }
        // Bước ô theo chiều dọc và ngang.
        float pitchY = d * 1.18f;
        if (rowYs.size() >= 2) {
            List<Float> dy = new ArrayList<>();
            for (int i = 1; i < rowYs.size(); i++) dy.add(rowYs.get(i) - rowYs.get(i - 1));
            Collections.sort(dy);
            float m = dy.get(dy.size() / 2);
            if (m > d * 0.8f && m < d * 2f) pitchY = m;
        }
        float pitchX = pitchY;
        float left = Float.MAX_VALUE;
        for (Blob b : cells) left = Math.min(left, b.cx());
        float top = rowYs.get(0);
        // Số hàng: từ hàng đầu đến hết vùng bảng (hàng cuối có chip hoặc thấp hơn tối đa 1 hàng).
        float lastY = rowYs.get(rowYs.size() - 1);
        int rows = Math.round((lastY - top) / pitchY) + 1;
        int cols = 5;
        float bottomLimit = y1 - y0;

        StringBuilder sb = new StringBuilder();
        int stableEmptyRun = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = left + c * pitchX, cy = top + r * pitchY;
                if (cy + d / 2f > bottomLimit) continue;
                char t = sampleCell(cls, rw, rh, cx, cy, d);
                if (t == 0) { stableEmptyRun++; } else { sb.append(t); stableEmptyRun = 0; }
            }
        }
        return new Result(sb.toString());
    }

    /** Đọc 1 ô của lưới: trả 0 nếu trống. */
    private static char sampleCell(byte[] cls, int rw, int rh, float cx, float cy, int d) {
        float rad = d * 0.38f;
        int red = 0, blue = 0, green = 0, total = 0, colored = 0;
        for (int y = Math.round(cy - rad); y <= Math.round(cy + rad); y++) {
            for (int x = Math.round(cx - rad); x <= Math.round(cx + rad); x++) {
                if (x < 0 || y < 0 || x >= rw || y >= rh) continue;
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy > rad * rad) continue;
                total++;
                switch (cls[y * rw + x]) {
                    case 1: red++; colored++; break;
                    case 2: blue++; colored++; break;
                    case 3: green++; colored++; break;
                    case 4: colored++; break;
                    default:
                }
            }
        }
        if (total == 0 || colored < total * 0.45f) return 0;   // ô trống
        int max = Math.max(red, Math.max(blue, green));
        if (max < colored * 0.45f) return 0;                    // không chắc -> không đoán
        return max == red ? 'B' : (max == blue ? 'P' : 'T');
    }

    private static byte classify(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        int delta = max - min;
        if (max < 60) return 4;                       // tối: viền biểu tượng
        if (delta < 45) return (max > 200) ? (byte) 5 : (byte) 0; // trắng/xám: nền, đường kẻ, chữ
        float h;
        if (max == r) h = 60f * (((g - b) / (float) delta) % 6f);
        else if (max == g) h = 60f * (((b - r) / (float) delta) + 2f);
        else h = 60f * (((r - g) / (float) delta) + 4f);
        if (h < 0) h += 360f;
        float s = delta / (float) max;
        if (s < 0.35f) return 0;
        if (h < 22f || h >= 335f) return 1;
        if (h >= 75f && h < 170f) return 3;
        if (h >= 170f && h < 255f) return 2;
        return 4;
    }
}
