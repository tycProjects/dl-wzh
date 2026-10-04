import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Small immediate-mode UI helpers (buttons, gradients). */
public class Ui {
    public static int mix(int a, int b, int t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = ar + (br - ar) * t / 255;
        int gg = ag + (bg - ag) * t / 255;
        int bl = ab + (bb - ab) * t / 255;
        return (r << 16) | (gg << 8) | bl;
    }

    public static void gradient(Graphics g, int w, int h, int c1, int c2) {
        for (int y = 0; y < h; y += 4) {
            g.setColor(mix(c1, c2, y * 255 / h));
            g.fillRect(0, y, w, 4);
        }
    }

    public static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public static void button(Graphics g, Font f, int x, int y, int w, int h, String label, boolean hover, int col) {
        g.setColor(0x000000);
        g.fillRect(x + 2, y + 2, w, h);
        g.setColor(hover ? mix(col, 0xFFFFFF, 90) : col);
        g.fillRect(x, y, w, h);
        g.setColor(mix(col, 0xFFFFFF, 160));
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(hover ? 0xFFFFFF : mix(col, 0x000000, 120));
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(0xFFFFFF);
        g.drawString(label, x + w / 2, y + (h - f.getHeight()) / 2, Graphics.TOP | Graphics.HCENTER);
    }
}
