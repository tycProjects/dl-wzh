package com.aiapkbuilder.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(16,16,16), PANEL=Color.rgb(30,30,30), TEAL=Color.rgb(73,185,159), WHITE=Color.rgb(239,239,239), MUTED=Color.rgb(145,145,145);
    private LinearLayout root, body, buildsBox;
    private EditText prompt, serverUrl;
    private TextView status;
    private String tab="Build";
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("builder",0); showBuild(); }
    private GradientDrawable bg(int color,int radius,int stroke){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); if(stroke!=0)d.setStroke(2,stroke); return d; }
    private TextView text(String s,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); return t; }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v,int a){v.setPadding(a,a,a,a);} 
    private TextView button(String label,Runnable r){ TextView t=text(label,15,WHITE,true); t.setGravity(Gravity.CENTER); t.setPadding(16,14,16,14); t.setBackground(bg(PANEL,18,Color.rgb(48,48,48))); t.setOnClickListener(v->r.run()); return t; }
    private void base(String title){
        root=column(); root.setBackgroundColor(BG);
        LinearLayout top=row(); top.setPadding(22,20,22,18);
        TextView settings=text("⚙",25,WHITE,true); top.addView(settings);
        TextView name=text(title,20,WHITE,true); name.setGravity(Gravity.CENTER); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1); top.addView(name,np);
        TextView profile=text("●",22,MUTED,true); top.addView(profile); root.addView(top);
        View line=new View(this); line.setBackgroundColor(Color.rgb(45,45,45)); root.addView(line,new LinearLayout.LayoutParams(-1,1));
        body=column(); body.setPadding(22,20,22,12); ScrollView scroll=new ScrollView(this); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(nav()); setContentView(root);
    }
    private View nav(){
        LinearLayout nav=row(); nav.setPadding(8,12,8,12); nav.setBackground(bg(Color.rgb(24,24,24),28,Color.rgb(42,42,42)));
        String[] tabs={"⌂\nBuild","‹ ›\nCode","☁\nHost","♧\nShare"};
        for(String s:tabs){ String key=s.substring(s.indexOf('\n')+1); TextView t=text(s,14,key.equals(tab)?TEAL:MUTED,true); t.setGravity(Gravity.CENTER); nav.addView(t,new LinearLayout.LayoutParams(0,-2,1)); t.setOnClickListener(v->{tab=key; if(key.equals("Build"))showBuild(); else showPage(key);}); }
        return nav;
    }
    private void showBuild(){
        base("AI APK Builder");
        TextView build=text("＋   Build your APK",22,WHITE,true); build.setGravity(Gravity.CENTER); build.setPadding(12,28,12,28); build.setBackground(bg(BG,26,TEAL)); body.addView(build); build.setOnClickListener(v->submitPrompt());
        TextView recent=text("Recent builds",23,WHITE,true); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.topMargin=26; body.addView(recent,rp);
        TextView count=text(buildHistory().size()+" builds · local history",13,MUTED,false); body.addView(count);
        buildsBox=column(); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2); bp.topMargin=12; body.addView(buildsBox,bp); refreshBuilds();
        TextView note=text("Masukkan prompt untuk mulai. AI akan menyiapkan project, lalu layanan build server mengompilasinya menjadi APK.",13,MUTED,false); note.setPadding(4,18,4,4); body.addView(note);
    }
    private void submitPrompt(){
        if(prompt==null || prompt.getText().toString().trim().isEmpty()){ showPromptDialog(); return; }
        String p=prompt.getText().toString().trim(); String endpoint=prefs.getString("endpoint","");
        if(endpoint.isEmpty()){ Toast.makeText(this,"Atur alamat Build Server di menu Host dulu.",Toast.LENGTH_LONG).show(); tab="Host"; showPage("Host"); return; }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(),0);
        status.setText("Mengirim prompt ke Build Server...");
        new Thread(()->{
            HttpURLConnection c=null;
            try{
                URL u=new URL(endpoint.replaceAll("/$","")+"/v1/build"); c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                JSONObject data=new JSONObject(); data.put("prompt",p); data.put("app_name","AI APK Builder"); data.put("platform","android"); data.put("language","java");
                try(OutputStream os=c.getOutputStream()){os.write(data.toString().getBytes("UTF-8"));}
                int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[2048]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n);
                JSONObject result=new JSONObject(out.toString("UTF-8")); String id=result.optString("build_id",UUID.randomUUID().toString()); String state=result.optString("status",code<300?"queued":"failed");
                addBuild(p,id,state,result.optString("message",code<300?"Project masuk antrean build.":"Build server menolak permintaan."));
                runOnUiThread(()->{ if(code>=200&&code<300){Toast.makeText(this,"Prompt berhasil dikirim.",Toast.LENGTH_LONG).show(); showBuild();}else{status.setText("Build gagal: "+result.optString("message","Periksa server."));} });
            }catch(Exception e){ addBuild(p,UUID.randomUUID().toString(),"failed",e.getMessage()==null?"Tidak bisa terhubung ke server.":e.getMessage()); runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Belum bisa mengirim prompt").setMessage("Periksa alamat server dan koneksi internet. Detail: "+e.getMessage()).setPositiveButton("OK",null).show()); }
            finally{if(c!=null)c.disconnect();}
        }).start();
    }
    private void showPromptDialog(){
        LinearLayout box=column(); box.setPadding(4,4,4,4); prompt=new EditText(this); prompt.setTextColor(WHITE); prompt.setHintTextColor(MUTED); prompt.setHint("Contoh: Buat aplikasi kasir dengan login, daftar produk, dan laporan..."); prompt.setMinLines(5); prompt.setGravity(Gravity.TOP); prompt.setBackground(bg(PANEL,16,Color.DKGRAY)); prompt.setPadding(14,14,14,14); box.addView(prompt);
        new AlertDialog.Builder(this).setTitle("Buat aplikasi dari prompt").setView(box).setNegativeButton("Batal",null).setPositiveButton("Mulai Build",(d,w)->{ if(prompt.getText().toString().trim().isEmpty())Toast.makeText(this,"Prompt masih kosong.",Toast.LENGTH_SHORT).show(); else submitPrompt(); }).show();
    }
    private ArrayList<String> buildHistory(){ return new ArrayList<>(prefs.getStringSet("history",new HashSet<>())); }
    private void addBuild(String p,String id,String state,String msg){ ArrayList<String> h=buildHistory(); h.add(0,state+"|"+id+"|"+p+"|"+msg); while(h.size()>30)h.remove(h.size()-1); prefs.edit().putStringSet("history",new HashSet<>(h)).apply(); }
    private void refreshBuilds(){ buildsBox.removeAllViews(); ArrayList<String> h=buildHistory(); if(h.isEmpty()){ TextView empty=text("Belum ada build. Tekan Build your APK untuk mulai.",14,MUTED,false); empty.setPadding(8,20,8,20); buildsBox.addView(empty); return; }
        for(String item:h){ String[] a=item.split("\\|",4); String state=a.length>0?a[0]:"queued", id=a.length>1?a[1]:"", p=a.length>2?a[2]:"Project", msg=a.length>3?a[3]:""; LinearLayout card=column(); card.setPadding(16,14,16,14); card.setBackground(bg(PANEL,22,Color.rgb(43,43,43))); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=10; buildsBox.addView(card,cp); card.addView(text(p,16,WHITE,true)); TextView st=text(state.toUpperCase(Locale.ROOT)+"   ·   "+msg,12,state.equals("failed")?Color.rgb(230,100,100):TEAL,false); st.setPadding(0,8,0,0); card.addView(st); card.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Detail build").setMessage("ID: "+id+"\nStatus: "+state+"\n"+msg).setPositiveButton("Tutup",null).show()); }
    }
    private void showPage(String page){
        base("AI APK Builder");
        if(page.equals("Code")){ body.addView(text("Code Editor",23,WHITE,true)); body.addView(text("Panel ini akan menampilkan file project yang dikirim oleh Build Server. Editor penuh dan sinkronisasi file membutuhkan endpoint project pada server.",14,MUTED,false)); body.addView(button("Lihat kontrak API",()->showInfo("Kontrak API ada di folder backend/ dan docs/ pada ZIP project."))); }
        else if(page.equals("Host")){ body.addView(text("Build Host",23,WHITE,true)); body.addView(text("Masukkan URL HTTPS server yang menjalankan AI coding agent dan Android SDK/Gradle build worker.",14,MUTED,false)); serverUrl=new EditText(this); serverUrl.setSingleLine(true); serverUrl.setTextColor(WHITE); serverUrl.setHintTextColor(MUTED); serverUrl.setHint("https://server-kamu.example"); serverUrl.setText(prefs.getString("endpoint","")); serverUrl.setPadding(14,14,14,14); serverUrl.setBackground(bg(PANEL,14,Color.DKGRAY)); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2); ep.topMargin=16; body.addView(serverUrl,ep); body.addView(button("Simpan alamat server",()->{String s=serverUrl.getText().toString().trim(); if(!s.startsWith("https://")){Toast.makeText(this,"Gunakan alamat HTTPS.",Toast.LENGTH_LONG).show();return;} prefs.edit().putString("endpoint",s).apply(); Toast.makeText(this,"Alamat server disimpan.",Toast.LENGTH_SHORT).show();}),lpTop()); body.addView(text("API Key AI tidak dimasukkan ke APK. Simpan rahasia provider di environment server.",13,MUTED,false)); }
        else { body.addView(text("Bagikan project",23,WHITE,true)); body.addView(text("Bagikan informasi AI APK Builder kepada teman.",14,MUTED,false)); body.addView(button("Bagikan",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,"AI APK Builder — aplikasi pembuat project Android dari prompt.");startActivity(Intent.createChooser(i,"Bagikan"));})); }
    }
    private LinearLayout.LayoutParams lpTop(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=14;return p;}
    private void showInfo(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
}
