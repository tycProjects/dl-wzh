import android.app.Activity;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
public class MsvMidlet extends MIDlet {
    private final Activity activity; private MsvCanvas canvas;
    public MsvMidlet(Activity a){activity=a; Display.init(a);}
    public void startApp(){if(canvas==null){canvas=new MsvCanvas(this);Display.getDisplay(this).setCurrent(canvas);canvas.start();}else Display.getDisplay(this).setCurrent(canvas);}
    public void pauseApp(){}
    public void destroyApp(boolean unconditional){if(canvas!=null)canvas.stop();}
    public Display display(){return Display.getDisplay(this);} public void exitApp(){destroyApp(true);activity.finish();notifyDestroyed();}
}
