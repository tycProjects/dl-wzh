package javax.microedition.lcdui;
public class Font {
 public static final int FACE_PROPORTIONAL=0, STYLE_PLAIN=0, SIZE_SMALL=8, SIZE_MEDIUM=16, SIZE_LARGE=24;
 private final float size; private Font(float s){size=s;} public static Font getFont(int face,int style,int size){return new Font(size==SIZE_LARGE?20:size==SIZE_MEDIUM?16:12);} public float size(){return size;}
}
