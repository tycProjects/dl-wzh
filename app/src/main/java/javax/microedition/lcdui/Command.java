package javax.microedition.lcdui;
public class Command {
    public static final int OK=4, CANCEL=2;
    public final String label; public final int type; public final int priority;
    public Command(String label,int type,int priority){this.label=label;this.type=type;this.priority=priority;}
}
