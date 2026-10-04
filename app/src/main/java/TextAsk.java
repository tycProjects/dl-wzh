import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

/** Opens the phone's text entry screen (full QWERTY) and hands the result back to the canvas. */
public class TextAsk implements CommandListener {
    private MsvCanvas cv;
    private Display disp;
    private int field;
    private Command ok = new Command("OK", Command.OK, 1);
    private Command cancel = new Command("Huy", Command.CANCEL, 2);
    private TextBox box;

    public TextAsk(MsvCanvas cv, Display d, int field, String title, String init, int max) {
        this.cv = cv;
        this.disp = d;
        this.field = field;
        box = new TextBox(title, init, max, TextField.ANY);
        box.addCommand(ok);
        box.addCommand(cancel);
        box.setCommandListener(this);
        d.setCurrent(box);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == ok) {
            cv.textDone(field, box.getString());
        }
        disp.setCurrent(cv);
    }
}
