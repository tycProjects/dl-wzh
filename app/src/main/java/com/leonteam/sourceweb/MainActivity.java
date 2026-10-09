package com.leonteam.sourceweb;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(12, 10, 20), PANEL = Color.rgb(24, 19, 37);
    private final int PURPLE = Color.rgb(174, 92, 255), TEXT = Color.rgb(241, 235, 255);
    private final int MUTED = Color.rgb(169, 157, 190);
    private EditText urlInput, searchInput;
    private TextView status, sourceView, resourceView;
    private ScrollView sourceScroll;
    private LinearLayout root, resourceList;
    private String pageUrl = "", html = "", css = "", js = "", currentCode = "";
    private final ArrayList<Resource> resources = new ArrayList<>();
    private String activeTab = "HTML";

    static class Resource {
        String type, url;
        Resource(String t, String u) { type=t; url=u; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    private GradientDrawable bg(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(14));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(TEXT); b.setTextSize(12);
        b.setAllCaps(false); b.setBackground(bg(PANEL, PURPLE)); b.setPadding(dp(10),0,dp(10),0);
        return b;
    }
    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(8)); root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = label("LEON", 22, PURPLE, true);
        TextView title = label("-SOURCE WEB", 18, TEXT, true);
        header.addView(logo); header.addView(title);
        root.addView(header);
        TextView subtitle = label("Web source inspector  •  Neon edition", 12, MUTED, false);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-1,-2); subp.bottomMargin=dp(14);
        root.addView(subtitle,subp);

        LinearLayout urlRow = new LinearLayout(this); urlRow.setGravity(Gravity.CENTER_VERTICAL);
        urlInput = new EditText(this); urlInput.setSingleLine(true); urlInput.setTextColor(TEXT);
        urlInput.setHintTextColor(MUTED); urlInput.setHint("https://example.com");
        urlInput.setTextSize(14); urlInput.setPadding(dp(12),0,dp(12),0); urlInput.setBackground(bg(PANEL,0));
        urlRow.addView(urlInput,new LinearLayout.LayoutParams(0,dp(48),1));
        Button go = button("FETCH"); LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(90),dp(48)); gp.leftMargin=dp(8);
        urlRow.addView(go,gp); root.addView(urlRow);
        go.setOnClickListener(v -> fetchPage());

        status = label("Ready. Enter a public website URL to inspect.", 12, MUTED, false);
        LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,-2); stp.topMargin=dp(10); stp.bottomMargin=dp(10);
        root.addView(status,stp);

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button format=button("Format"), minify=button("Minify"), copy=button("Copy"), zip=button("Export ZIP");
        for(Button b:new Button[]{format,minify,copy,zip}) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1); p.setMargins(dp(2),0,dp(2),0);
            actions.addView(b,p);
        }
        root.addView(actions);
        format.setOnClickListener(v -> transform(true));
        minify.setOnClickListener(v -> transform(false));
        copy.setOnClickListener(v -> { ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("source",currentCode)); toast("Copied source"); });
        zip.setOnClickListener(v -> exportZip());

        LinearLayout tabs = new LinearLayout(this); tabs.setPadding(0,dp(10),0,dp(8));
        for(String tab:new String[]{"HTML","CSS","JS","MEDIA"}) {
            Button b=button(tab); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1); p.setMargins(dp(2),0,dp(2),0);
            tabs.addView(b,p); b.setOnClickListener(v -> showTab(tab));
        }
        root.addView(tabs);

        searchInput = new EditText(this); searchInput.setSingleLine(true); searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(MUTED); searchInput.setHint("Search in current source…");
        searchInput.setTextSize(13); searchInput.setPadding(dp(12),0,dp(12),0); searchInput.setBackground(bg(PANEL,0));
        root.addView(searchInput,new LinearLayout.LayoutParams(-1,dp(42)));
        Button find = button("Find");
        LinearLayout findrow = new LinearLayout(this); findrow.setGravity(Gravity.RIGHT);
        findrow.addView(find,new LinearLayout.LayoutParams(dp(90),dp(38))); root.addView(findrow);
        find.setOnClickListener(v -> searchSource());

        FrameLayout area = new FrameLayout(this); area.setBackground(bg(PANEL,0));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,0,1); ap.topMargin=dp(8); ap.bottomMargin=dp(6); root.addView(area,ap);
        sourceScroll = new ScrollView(this);
        sourceView = label("Fetched source will appear here.",12,TEXT,false);
        sourceView.setTypeface(Typeface.MONOSPACE); sourceView.setTextIsSelectable(true);
        sourceView.setPadding(dp(12),dp(12),dp(12),dp(12));
        sourceScroll.addView(sourceView); area.addView(sourceScroll,new FrameLayout.LayoutParams(-1,-1));
        resourceView = label("",12,TEXT,false); resourceView.setPadding(dp(12),dp(12),dp(12),dp(12));
        ScrollView rs=new ScrollView(this); rs.addView(resourceView); area.addView(rs,new FrameLayout.LayoutParams(-1,-1)); rs.setVisibility(View.GONE);
        resourceList = new LinearLayout(this);
        TextView foot=label("Only inspect websites you are authorized to access. Dynamic JavaScript-rendered content may not appear in raw HTML.",10,MUTED,false);
        root.addView(foot);
    }

    private void fetchPage() {
        String input=urlInput.getText().toString().trim();
        if(input.isEmpty()){toast("Enter a URL first");return;}
        if(!input.matches("(?i)^https?://.*")) input="https://"+input;
        final String target=input;
        try { URL u=new URL(target); if(!u.getProtocol().equals("https")&&!u.getProtocol().equals("http")) throw new Exception(); }
        catch(Exception e){toast("Invalid HTTP/HTTPS URL");return;}
        status.setText("Fetching page…"); hideKeyboard();
        new Thread(() -> {
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent","Leon-Source-web/1.0 (Android source inspector)");
                c.setInstanceFollowRedirects(true);
                int code=c.getResponseCode();
                InputStream in=(code>=400)?c.getErrorStream():c.getInputStream();
                String body=readLimited(in,5*1024*1024);
                String finalUrl=c.getURL().toString(); c.disconnect();
                runOnUiThread(() -> {
                    pageUrl=finalUrl; urlInput.setText(finalUrl); html=body; css=""; js=""; resources.clear();
                    parseResources(body,finalUrl);
                    currentCode=html; activeTab="HTML"; sourceView.setText(html);
                    status.setText("HTTP "+code+"  •  "+html.length()+" chars  •  "+resources.size()+" linked resources");
                    showTab("HTML");
                });
            } catch(Exception e) {
                runOnUiThread(() -> status.setText("Fetch failed: "+e.getMessage()));
            }
        }).start();
    }
    private String readLimited(InputStream in,int max)throws IOException{
        if(in==null)return ""; ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n,total=0;
        while((n=in.read(b))>0){total+=n;if(total>max)throw new IOException("Page exceeds 5 MB limit");out.write(b,0,n);}
        in.close(); return out.toString(StandardCharsets.UTF_8.name());
    }
    private void parseResources(String src,String base) {
        Pattern p=Pattern.compile("(?is)<(script|link|img|source|video|audio|iframe|embed)\\b([^>]+)>?");
        Matcher m=p.matcher(src);
        while(m.find()){
            String tag=m.group(1).toLowerCase(Locale.ROOT), attrs=m.group(2);
            String raw=null, type="";
            Matcher a=Pattern.compile("(?is)(src|href)\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]").matcher(attrs);
            if(a.find()) raw=a.group(2);
            if(raw==null) continue;
            if(tag.equals("link")) {
                if(attrs.toLowerCase(Locale.ROOT).contains("stylesheet")) type="CSS";
                else continue;
            } else if(tag.equals("script")) type="JS";
            else if(tag.equals("img")||tag.equals("source")||tag.equals("video")||tag.equals("audio")||tag.equals("embed")) type="MEDIA";
            else continue;
            try {
                String abs=new URL(new URL(base),raw).toString();
                if(abs.startsWith("http://")||abs.startsWith("https://")) resources.add(new Resource(type,abs));
            } catch(Exception ignored){}
        }
        LinkedHashMap<String,Resource> unique=new LinkedHashMap<>();
        for(Resource r:resources) unique.put(r.type+"|"+r.url,r);
        resources.clear(); resources.addAll(unique.values());
    }
    private void showTab(String tab) {
        activeTab=tab;
        if(tab.equals("MEDIA")) {
            StringBuilder b=new StringBuilder();
            for(Resource r:resources) if(r.type.equals("MEDIA")) b.append("MEDIA  ").append(r.url).append("\n\n");
            sourceView.setText(b.length()==0?"No media links found in static HTML.":b.toString()); currentCode=b.toString();
        } else {
            StringBuilder b=new StringBuilder();
            if(tab.equals("HTML")) b.append(html);
            else {
                for(Resource r:resources) if(r.type.equals(tab)) b.append(r.url).append("\n");
                if(b.length()==0) b.append("No linked ").append(tab).append(" files found.");
                else b.append("\n\nNote: linked resource URLs are listed here. Use Export ZIP to download them.");
            }
            currentCode=b.toString(); sourceView.setText(currentCode);
        }
        sourceScroll.scrollTo(0,0);
    }
    private void searchSource() {
        String q=searchInput.getText().toString();
        if(q.isEmpty()){toast("Type a search term");return;}
        String s=currentCode; String low=s.toLowerCase(Locale.ROOT); int pos=low.indexOf(q.toLowerCase(Locale.ROOT));
        if(pos<0){status.setText("No match for: "+q);return;}
        sourceView.setText(s);
        int line=1; for(int i=0;i<pos;i++) if(s.charAt(i)=='\n')line++;
        status.setText("Found “"+q+"” near character "+pos+" (line "+line+")");
        final int targetLine = line;
        sourceScroll.post(() -> sourceScroll.smoothScrollTo(0,Math.max(0,(targetLine-1)*dp(18))));
    }
    private void transform(boolean pretty) {
        if(currentCode.isEmpty()){toast("Fetch a page first");return;}
        String s=currentCode;
        if(pretty) {
            s=s.replaceAll("(?i)><",">\\n<");
            s=s.replaceAll("(?m)^\\s+","");
        } else {
            s=s.replaceAll(">\\s+<","><").replaceAll("\\s{2,}"," ");
        }
        currentCode=s; sourceView.setText(s); status.setText(pretty?"Basic formatting applied.":"Whitespace minified (basic).");
    }
    private void exportZip() {
        if(pageUrl.isEmpty()){toast("Fetch a page first");return;}
        status.setText("Preparing ZIP…");
        new Thread(() -> {
            File out=new File(getCacheDir(),"leon-source-"+System.currentTimeMillis()+".zip");
            int downloaded=0;
            try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
                addEntry(zip,"index.html",html.getBytes(StandardCharsets.UTF_8));
                HashSet<String> used=new HashSet<>(); int count=0;
                for(Resource r:resources) {
                    if(count>=30) break;
                    String path;
                    try {
                        URL u=new URL(r.url);
                        String name=u.getPath(); if(name==null||name.isEmpty()||name.endsWith("/"))name="/resource-"+count;
                        name=name.substring(name.lastIndexOf('/')+1);
                        name=name.replaceAll("[^A-Za-z0-9._-]","_");
                        if(name.isEmpty())name="resource-"+count;
                        path=r.type.toLowerCase(Locale.ROOT)+"/"+name;
                        int n=1; String candidate=path;
                        while(used.contains(candidate)) candidate=r.type.toLowerCase(Locale.ROOT)+"/"+(n++)+"_"+name;
                        path=candidate; used.add(path);
                        HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(10000);
                        c.setRequestProperty("User-Agent","Leon-Source-web/1.0");
                        if(c.getResponseCode()>=400){c.disconnect();continue;}
                        InputStream in=c.getInputStream(); ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                        byte[] buf=new byte[4096]; int nread,total=0;
                        while((nread=in.read(buf))>0){total+=nread;if(total>2*1024*1024)break;bytes.write(buf,0,nread);}
                        in.close();c.disconnect();
                        addEntry(zip,path,bytes.toByteArray()); downloaded++; count++;
                    } catch(Exception ignored){}
                }
                String readme="Leon-Source web export\nSource URL: "+pageUrl+"\nLinked resources downloaded: "+downloaded+"\nNote: this is a static snapshot; dynamic content and relative paths may need manual adjustment.\n";
                addEntry(zip,"README.txt",readme.getBytes(StandardCharsets.UTF_8));
            } catch(Exception e) {
                runOnUiThread(() -> status.setText("ZIP failed: "+e.getMessage())); return;
            }
            final int done=downloaded;
            runOnUiThread(() -> {
                Intent intent=new Intent(Intent.ACTION_SEND); intent.setType("application/zip");
                Uri zipUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",out);
                intent.putExtra(Intent.EXTRA_STREAM,zipUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { startActivity(Intent.createChooser(intent,"Save or share ZIP")); status.setText("ZIP ready • "+done+" linked files downloaded (max 30)"); }
                catch(Exception e){status.setText("ZIP created at app cache, but no share target is available.");}
            });
        }).start();
    }
    private void addEntry(ZipOutputStream zip,String name,byte[] data)throws IOException{
        zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();
    }
    private void hideKeyboard(){
        try { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(urlInput.getWindowToken(),0); } catch(Exception ignored){}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
