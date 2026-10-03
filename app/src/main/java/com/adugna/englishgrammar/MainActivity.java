package com.adugna.englishgrammar;

import android.app.*;import android.os.*;import android.graphics.*;import android.graphics.pdf.PdfRenderer;import android.view.*;import android.widget.*;import android.content.*;import java.io.*;import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list; TextView title; PdfRenderer renderer; ParcelFileDescriptor pfd; int currentPage=0; String currentUnit="";
    final int PURPLE=Color.rgb(126,84,147), DARK=Color.rgb(49,28,88), LIGHT=Color.rgb(244,238,250), WHITE=Color.WHITE;
    static class Unit {String name; int page; String sub; Unit(String n,int p,String s){name=n;page=p;sub=s;}}
    Unit[] units={
      new Unit("Collocations",7,"Collocations • As and like • As if / As though • Make and do"),
      new Unit("Conditionals",23,"Types 0–3 • Implied conditionals • Wish / Regret • Review exercises"),
      new Unit("Degrees of Comparison",38,"Positive • Comparative • Superlative • Review exercises"),
      new Unit("Phrasal Verbs and Idioms",54,"Basic rules • English idioms • Similes • Idioms by topic"),
      new Unit("Discourse Markers",81,"Clause markers • Contrast • Cause/result • Time • Place • Choice"),
      new Unit("Relative Clauses",105,"Defining / non-defining • Relative pronouns • Review exercises"),
      new Unit("Modal Auxiliaries",113,"Ability • Permission • Possibility • Deduction • Advice / necessity"),
      new Unit("Active and Passive Pattern",130,"Basic rules • Passive with two objects • Prepositions • Get passive"),
      new Unit("Participles",149,"Adjective participles • Present participle • Perfect participle • Past participle"),
      new Unit("Quantifiers",156,"Quantifier table • Much / many • Few / little • Distributive and graded quantifiers"),
      new Unit("Tenses",161,"Present • Past • Future • Perfect • Progressive • Review questions"),
      new Unit("Direct and Reported Speech",166,"Direct speech • Reported speech • Reported questions • Special cases")
    };
    @Override public void onCreate(Bundle b){super.onCreate(b); showHome();}
    TextView tv(String s,float sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.WHITE);t.setPadding(22,18,22,18);return t;}
    void base(String heading){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LIGHT); setContentView(root);
      LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(PURPLE); TextView menu=tv("☰",28); menu.setOnClickListener(v->showHome()); title=tv(heading,22); title.setTypeface(null,1); bar.addView(menu,new LinearLayout.LayoutParams(64,72));bar.addView(title,new LinearLayout.LayoutParams(0,72,1)); TextView bag=tv("▣",28);bar.addView(bag,new LinearLayout.LayoutParams(64,72));root.addView(bar);
    }
    void showHome(){base("English Grammar"); ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
      TextView intro=tv("Study from your EXTREME ENGLISH PDF\n\n📚 11 grammar units in this scanned edition\n🔥 Entrance-exam questions are marked when verified from the source\n\nTap a unit to open its source pages.",18);intro.setTextColor(DARK);intro.setPadding(24,28,24,28);list.addView(intro);
      for(int i=0;i<units.length;i++){final int k=i;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(20,18,20,18);card.setBackgroundColor(Color.WHITE);TextView n=tv("○  "+units[i].name,20);n.setTextColor(DARK);n.setTypeface(null,1);TextView s=tv(units[i].sub,14);s.setTextColor(Color.DKGRAY);card.addView(n);card.addView(s);card.setOnClickListener(v->openUnit(k));list.addView(card,new LinearLayout.LayoutParams(-1,130)); View line=new View(this);line.setBackgroundColor(Color.LTGRAY);list.addView(line,new LinearLayout.LayoutParams(-1,2));}
      TextView exam=tv("🔥  ENTRANCE EXAM QUESTIONS\n\nUse this section for verified entrance-exam questions from the PDF.\n\nIf a question's source is not explicitly identifiable in the scanned PDF, it will NOT be falsely labeled as an entrance question.",19);exam.setTextColor(WHITE);exam.setBackgroundColor(DARK);exam.setPadding(24,30,24,30);exam.setOnClickListener(v->showExamInfo());list.addView(exam,new LinearLayout.LayoutParams(-1,180));sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));}
    void showExamInfo(){base("🔥 Entrance Exam");TextView t=tv("🔥 ENTRANCE-EXAM INDICATOR\n\nThe PDF is a scanned book, so the app does not guess whether an MCQ is an Ethiopian entrance-exam question.\n\nVerified exam questions will use the red/orange 🔥 badge.\nOrdinary review/practice questions remain unmarked.\n\nThis prevents the app from incorrectly calling a normal exercise an entrance-exam question.",18);t.setTextColor(DARK);root.addView(t);}
    void openUnit(int idx){currentUnit=units[idx].name;base(currentUnit);LinearLayout controls=new LinearLayout(this);controls.setPadding(10,8,10,8);Button prev=new Button(this);prev.setText("‹");Button next=new Button(this);next.setText("›");TextView page=tv("Page "+units[idx].page,16);page.setTextColor(DARK);controls.addView(prev,new LinearLayout.LayoutParams(60,60));controls.addView(page,new LinearLayout.LayoutParams(0,60,1));controls.addView(next,new LinearLayout.LayoutParams(60,60));root.addView(controls);ImageView img=new ImageView(this);img.setAdjustViewBounds(true);img.setScaleType(ImageView.ScaleType.FIT_CENTER);root.addView(img,new LinearLayout.LayoutParams(-1,0,1));
      final int start=idx==units.length-1?166:units[idx].page; currentPage=start-1; render(img,page); prev.setOnClickListener(v->{if(currentPage>0){currentPage--;render(img,page);}});next.setOnClickListener(v->{if(currentPage<renderer.getPageCount()-1){currentPage++;render(img,page);}});
    }
    void render(ImageView img,TextView page){
      try{
        if(renderer==null){
          File f=new File(getCacheDir(),"extreme_english.pdf");
          if(!f.exists()){
            try(InputStream in=getAssets().open("extreme_english.pdf"); FileOutputStream out=new FileOutputStream(f)){
              byte[] b=new byte[8192]; int n;
              while((n=in.read(b))>0) out.write(b,0,n);
            }
          }
          pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
          renderer=new PdfRenderer(pfd);
        }
        if(currentPage<0) currentPage=0;
        if(currentPage>=renderer.getPageCount()) currentPage=renderer.getPageCount()-1;
        PdfRenderer.Page p=renderer.openPage(currentPage);
        Bitmap bm=Bitmap.createBitmap(p.getWidth(),p.getHeight(),Bitmap.Config.ARGB_8888);
        bm.eraseColor(Color.WHITE);
        p.render(bm,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        p.close();
        img.setImageBitmap(bm);
        page.setText("Page "+(currentPage+1)+(isExamPage(currentPage+1)?"   🔥 EXAM":""));
      }catch(Exception e){
        page.setText("Unable to open PDF page");
      }
    }
    boolean isExamPage(int p){return false; /* populated only after explicit exam-source verification */}
    @Override protected void onDestroy(){try{if(renderer!=null)renderer.close();if(pfd!=null)pfd.close();}catch(Exception ignored){}super.onDestroy();}
}
