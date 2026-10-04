/** A bullet or a thrown bomb. */
public class Shot {
    public float x, y, vx, vy, grav;
    public int life, dmg, color, fuse, radius, knock;
    public boolean bomb, rest, slipper;
    public Fighter owner;
    public int width;
    public boolean pierce;
    public java.util.Vector hits = new java.util.Vector();
}
