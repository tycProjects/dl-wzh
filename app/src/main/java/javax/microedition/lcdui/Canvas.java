package javax.microedition.lcdui;
import android.view.KeyEvent; import android.view.MotionEvent; import android.view.View;
public abstract class Canvas extends View {
 public static final int UP=1, DOWN=6, LEFT=2, RIGHT=5, FIRE=8;
 public Canvas(android.content.Context c){super(c);setFocusable(true);setFocusableInTouchMode(true);}
 public void setFullScreenMode(boolean b){}
 public boolean isDoubleBuffered(){return false;}
 public int getGameAction(int keyCode){switch(keyCode){case KeyEvent.KEYCODE_DPAD_UP:return UP;case KeyEvent.KEYCODE_DPAD_DOWN:return DOWN;case KeyEvent.KEYCODE_DPAD_LEFT:return LEFT;case KeyEvent.KEYCODE_DPAD_RIGHT:return RIGHT;case KeyEvent.KEYCODE_ENTER:case KeyEvent.KEYCODE_DPAD_CENTER:return FIRE;default:throw new IllegalArgumentException();}}
 public void serviceRepaints(){postInvalidate();}
 public void repaint(){postInvalidate();}
 @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);paint(new Graphics(c));}
 protected abstract void paint(Graphics g);
 protected void sizeChanged(int w,int h){}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){sizeChanged(w,h);}
 protected void keyPressed(int kc){} protected void keyReleased(int kc){} protected void keyRepeated(int kc){} protected void hideNotify(){}
 @Override public boolean onKeyDown(int code,KeyEvent e){int k=toKey(code); if(k!=0){keyPressed(k);return true;} return super.onKeyDown(code,e);}
 @Override public boolean onKeyUp(int code,KeyEvent e){int k=toKey(code); if(k!=0){keyReleased(k);return true;} return super.onKeyUp(code,e);}
 private int toKey(int c){switch(c){case KeyEvent.KEYCODE_A:return 'a';case KeyEvent.KEYCODE_B:return 'b';case KeyEvent.KEYCODE_C:return 'c';case KeyEvent.KEYCODE_D:return 'd';case KeyEvent.KEYCODE_E:return 'e';case KeyEvent.KEYCODE_F:return 'f';case KeyEvent.KEYCODE_G:return 'g';case KeyEvent.KEYCODE_H:return 'h';case KeyEvent.KEYCODE_I:return 'i';case KeyEvent.KEYCODE_J:return 'j';case KeyEvent.KEYCODE_K:return 'k';case KeyEvent.KEYCODE_L:return 'l';case KeyEvent.KEYCODE_M:return 'm';case KeyEvent.KEYCODE_N:return 'n';case KeyEvent.KEYCODE_O:return 'o';case KeyEvent.KEYCODE_P:return 'p';case KeyEvent.KEYCODE_Q:return 'q';case KeyEvent.KEYCODE_R:return 'r';case KeyEvent.KEYCODE_S:return 's';case KeyEvent.KEYCODE_T:return 't';case KeyEvent.KEYCODE_U:return 'u';case KeyEvent.KEYCODE_V:return 'v';case KeyEvent.KEYCODE_W:return 'w';case KeyEvent.KEYCODE_X:return 'x';case KeyEvent.KEYCODE_Y:return 'y';case KeyEvent.KEYCODE_Z:return 'z';case KeyEvent.KEYCODE_0:return '0';case KeyEvent.KEYCODE_1:return '1';case KeyEvent.KEYCODE_2:return '2';case KeyEvent.KEYCODE_3:return '3';case KeyEvent.KEYCODE_4:return '4';case KeyEvent.KEYCODE_5:return '5';case KeyEvent.KEYCODE_6:return '6';case KeyEvent.KEYCODE_7:return '7';case KeyEvent.KEYCODE_8:return '8';case KeyEvent.KEYCODE_9:return '9';case KeyEvent.KEYCODE_SPACE:return ' ';case KeyEvent.KEYCODE_STAR:return '*';case KeyEvent.KEYCODE_POUND:return '#';case KeyEvent.KEYCODE_DPAD_UP:return KeyEvent.KEYCODE_DPAD_UP;case KeyEvent.KEYCODE_DPAD_DOWN:return KeyEvent.KEYCODE_DPAD_DOWN;case KeyEvent.KEYCODE_DPAD_LEFT:return KeyEvent.KEYCODE_DPAD_LEFT;case KeyEvent.KEYCODE_DPAD_RIGHT:return KeyEvent.KEYCODE_DPAD_RIGHT;case KeyEvent.KEYCODE_DPAD_CENTER:return KeyEvent.KEYCODE_DPAD_CENTER;default:return 0;}}
 @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_UP){if(this instanceof TouchCanvas){((TouchCanvas)this).androidTouch((int)e.getX(),(int)e.getY(),e.getAction()==MotionEvent.ACTION_DOWN);} else {try{java.lang.reflect.Method m=getClass().getDeclaredMethod("click",int.class,int.class);m.setAccessible(true);m.invoke(this,(int)e.getX(),(int)e.getY());}catch(Throwable ignored){}}return true;}return true;}
}
