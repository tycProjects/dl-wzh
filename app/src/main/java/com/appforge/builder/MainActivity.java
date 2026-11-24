package com.appforge.builder;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    int bg=Color.rgb(246,248,247), surface=Color.WHITE, ink=Color.rgb(25,32,30), accent=Color.rgb(0,107,95);
    boolean dark=false;
    LinearLayout root, body;
    ArrayList<String> components=new ArrayList<>(), blocks=new ArrayList<>(), codeBlocks=new ArrayList<>();
    String projectName="Meu primeiro app";

    public void onCreate(Bundle b){ super.onCreate(b); showHome(); }
    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    GradientDrawable shape(int color,int radius){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    TextView text(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ink); if(bold)t.setTypeface(null,Typeface.BOLD); return t; }
    LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(1); l.setPadding(dp(18),dp(12),dp(18),dp(18)); l.setBackgroundColor(bg); return l; }
    void base(String title,String subtitle){
        root=column(); ScrollView scroll=new ScrollView(this); body=column(); body.setPadding(dp(18),dp(8),dp(18),dp(24));
        TextView h=text(title,27,true); body.addView(h); if(subtitle!=null){TextView sub=text(subtitle,14,false); sub.setAlpha(.72f); body.addView(sub);}
        scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
    }
    void card(String title,String desc,Runnable run){
        LinearLayout c=new LinearLayout(this); c.setOrientation(1); c.setPadding(dp(16),dp(15),dp(16),dp(15)); c.setBackground(shape(surface,22));
        TextView a=text(title,17,true), b=text(desc,13,false); b.setAlpha(.7f); c.addView(a); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(10); c.addView(b,p);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.topMargin=dp(12); body.addView(c,cp); c.setOnClickListener(v->run.run());
    }
    void button(String label,Runnable r){
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(10); body.addView(b,p); b.setOnClickListener(v->r.run());
    }
    void field(String hint, java.util.function.Consumer<String> done){
        EditText e=new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setTextColor(ink); body.addView(e);
        button("Adicionar",()->{String s=e.getText().toString().trim(); if(!s.isEmpty())done.accept(s);});
    }
    void showHome(){
        base("AppForge","Crie aplicativos Android com blocos e componentes");
        card("＋  Novo projeto","Comece do zero ou use um modelo",()->newProject());
        card("▦  Componentes e blocos","Biblioteca visual inspirada no Android Studio",()->showEditor());
        card("⚙  Configurações","Tema e informações do AppForge",()->showSettings());
        TextView note=text("Versão 1.4 • Projeto Android Java",12,false); note.setAlpha(.6f); body.addView(note);
    }
    void newProject(){
        base("Novo projeto","Escolha um nome para seu aplicativo");
        field("Nome do projeto",s->{projectName=s; components.clear(); blocks.clear(); codeBlocks.clear(); showEditor();});
        card("Modelo em branco","Um projeto simples para começar",()->{projectName="Meu primeiro app";components.clear();showEditor();});
        card("Lista de tarefas","Modelo demonstrativo",()->{projectName="Lista de tarefas";components.clear();components.add("Título: Minhas tarefas");components.add("Campo de texto");components.add("Botão: Adicionar");showEditor();});
        card("Contador","Modelo demonstrativo",()->{projectName="Contador";components.clear();components.add("Título: Contador");components.add("Botão: +1");showEditor();});
    }
    void showEditor(){
        base(projectName,"Editor visual • Monte a interface do seu aplicativo");
        card("Pré-visualização","Veja os componentes adicionados",()->preview());
        card("＋ Componentes","Layouts, widgets, listas e recursos Android",()->addComponent());
        card("▤ Blocos visuais","Eventos, controle, variáveis e dados",()->showBlocks());
        card("{ } Avançado","Blocos com trechos Java, Kotlin e XML",()->advanced());
        card("Propriedades","Componentes adicionados: "+components.size(),()->properties());
        button("← Início",()->showHome());
        TextView label=text("Componentes no projeto",16,true); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(18); body.addView(label,lp);
        for(String s:components){TextView t=text("• "+s,14,false); body.addView(t);}
    }
    void addComponent(){
        base("Adicionar componente","Escolha um item para inserir no projeto");
        String[][] groups={{"Layouts","LinearLayout","ConstraintLayout","FrameLayout","RelativeLayout","ScrollView"},{"Widgets","TextView","EditText","Button","ImageButton","ImageView","CheckBox","RadioButton","Switch","Slider","ProgressBar","SeekBar","FloatingActionButton","CardView"},{"Listas","RecyclerView","ListView","GridView","Spinner"},{"Navegação","Toolbar","BottomNavigationView","TabLayout","Navigation Drawer"},{"Mídia","VideoView","WebView","Áudio"},{"Sistema Android","Intent","Toast","Snackbar","SharedPreferences","Notificações","Permissões"}};
        for(String[] g:groups){TextView head=text(g[0],18,true); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(15);body.addView(head,hp);
            for(int i=1;i<g.length;i++){final String item=g[i]; card(item,"Toque para adicionar",()->{components.add(item);showEditor();});}}
        button("Voltar",()->showEditor());
    }
    void showBlocks(){
        base("Blocos visuais","Adicione ações ao comportamento do aplicativo");
        String[][] groups={{"Eventos","Ao clicar","Ao iniciar tela","Ao tocar e segurar"},{"Componentes","Definir texto","Mostrar componente","Ocultar componente"},{"Controle","Se / Senão","Repetir","Enquanto"},{"Variáveis","Criar variável","Definir variável","Somar valor"},{"Texto e números","Juntar textos","Converter para número","Comparar valores"},{"Android","Mostrar Toast","Abrir tela","Pedir permissão"},{"Dados e rede","Salvar dado","Ler dado","Requisição web"}};
        for(String[] g:groups){TextView h=text(g[0],18,true);body.addView(h);for(int i=1;i<g.length;i++){String x=g[i];card("🧩 "+x,"Adicionar bloco",()->{blocks.add(x);showBlocks();});}}
        button("Voltar",()->showEditor());
        TextView n=text("Blocos adicionados: "+blocks.size(),13,false);body.addView(n);
    }
    void advanced(){
        base("Avançado","Trechos de código para usuários experientes");
        card("Java","Lógica e eventos Android",()->snippet("Java"));
        card("Kotlin","Trechos Kotlin",()->snippet("Kotlin"));
        card("XML","Layouts e recursos",()->snippet("XML"));
        button("＋ Código personalizado",()->{
            EditText e=new EditText(this);e.setHint("Digite ou cole seu código");e.setMinLines(4);
            new AlertDialog.Builder(this).setTitle("Novo bloco de código").setView(e).setPositiveButton("Adicionar",(d,w)->{codeBlocks.add("Código personalizado: "+e.getText());advanced();}).setNegativeButton("Cancelar",null).show();
        });
        button("Voltar",()->showEditor());
        TextView t=text("Blocos de código salvos: "+codeBlocks.size(),13,false);body.addView(t);
    }
    void snippet(String lang){
        String[] names={"Mostrar Toast","Criar variável","If / Else","Laço For","Evento de clique","Abrir outra tela"};
        String[] snippets={"Toast.makeText(this, \"Olá!\", Toast.LENGTH_SHORT).show();","int contador = 0;","if (condicao) { /* ação */ } else { /* alternativa */ }","for (int i = 0; i < 10; i++) { }","view.setOnClickListener(v -> { });","startActivity(new Intent(this, OutraActivity.class));"};
        base("Blocos "+lang,"Selecione um trecho");
        for(int i=0;i<names.length;i++){final String s=snippets[i];card(names[i],s,()->{codeBlocks.add(lang+": "+s);advanced();});}
        button("Voltar",()->advanced());
    }
    void properties(){
        base("Propriedades","Resumo dos elementos do projeto");
        for(String s:components)card(s,"Elemento adicionado ao projeto",()->new AlertDialog.Builder(this).setMessage("Este item está na lista de componentes.").setPositiveButton("OK",null).show());
        button("Limpar componentes",()->{components.clear();showEditor();});
        button("Voltar",()->showEditor());
    }
    void preview(){
        base("Pré-visualização",projectName);
        TextView title=text(projectName,24,true);body.addView(title);
        for(String s:components){TextView t=text(s,16,false);t.setPadding(dp(12),dp(14),dp(12),dp(14));t.setBackground(shape(surface,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);body.addView(t,p);}
        if(components.isEmpty())body.addView(text("Adicione componentes para visualizar sua tela.",14,false));
        button("Voltar ao editor",()->showEditor());
    }
    void showSettings(){
        base("Configurações","Personalize o AppForge");
        card("☀ Tema branco","Usar aparência clara",()->{dark=false;applyColors();showSettings();});
        card("☾ Tema escuro","Usar aparência escura",()->{dark=true;applyColors();showSettings();});
        card("Sobre o AppForge","Criador visual de aplicativos Android • versão 1.4",()->new AlertDialog.Builder(this).setMessage("AppForge é um protótipo de editor visual. A geração e compilação automática de APK ainda não está implementada.").setPositiveButton("OK",null).show());
        button("Voltar",()->showHome());
    }
    void applyColors(){
        if(dark){bg=Color.rgb(18,22,21);surface=Color.rgb(38,45,43);ink=Color.rgb(235,241,238);accent=Color.rgb(91,213,188);}
        else{bg=Color.rgb(246,248,247);surface=Color.WHITE;ink=Color.rgb(25,32,30);accent=Color.rgb(0,107,95);}
    }
}
