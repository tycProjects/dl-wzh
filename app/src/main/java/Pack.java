/** A zip file or folder found on the card that may contain add-on (.sbc) files. */
public class Pack {
    public String name;
    public boolean zip;
    public String url;    // full file:/// url (folders end with '/')
    public String root;   // card root this pack was found on
}
