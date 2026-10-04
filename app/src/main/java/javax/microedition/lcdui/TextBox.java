package javax.microedition.lcdui;
import java.util.ArrayList;
public class TextBox extends Displayable {
  public final String title; private String text; public final int max; public final int constraints;
  private final ArrayList<Command> commands=new ArrayList<Command>(); private CommandListener listener;
  public TextBox(String title,String text,int max,int constraints){this.title=title;this.text=text==null?"":text;this.max=max;this.constraints=constraints;}
  public void addCommand(Command c){commands.add(c);} public ArrayList<Command> getCommands(){return commands;}
  public void setCommandListener(CommandListener l){listener=l;} public CommandListener getCommandListener(){return listener;}
  public String getString(){return text;} public void setString(String s){text=s==null?"":s;}
}
