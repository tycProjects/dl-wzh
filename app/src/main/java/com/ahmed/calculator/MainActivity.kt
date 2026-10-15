package com.ahmed.calculator

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var display: TextView
    private lateinit var history: TextView
    private lateinit var memoryText: TextView
    private lateinit var scientificPanel: LinearLayout
    private var first=0.0
    private var op=""
    private var fresh=true
    private var memory=0.0
    private var dark=true
    private val records=ArrayList<String>()
    private val accent=Color.rgb(105,90,255)

    override fun onCreate(b:Bundle?){super.onCreate(b); build()}

    private fun build(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,10)}
        setContentView(root)
        top(); screen(); memoryRow(); scientific(); keys(); theme()
    }

    private fun top(){
        val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val t=TextView(this).apply{text="حاسبتي Pro";textSize=22f;setTypeface(null,1)}
        r.addView(t,LinearLayout.LayoutParams(0,60,1f))
        val th=button("☀ / ☾"); th.setOnClickListener{dark=!dark;theme()}
        val hi=button("السجل"); hi.setOnClickListener{showHistory()}
        r.addView(th);r.addView(hi);root.addView(r)
    }

    private fun screen(){
        memoryText=TextView(this).apply{text="M: 0";textSize=14f;gravity=Gravity.RIGHT}
        root.addView(memoryText,LinearLayout.LayoutParams(-1,32))
        history=TextView(this).apply{textSize=16f;gravity=Gravity.RIGHT;maxLines=1}
        root.addView(history,LinearLayout.LayoutParams(-1,42))
        display=TextView(this).apply{text="0";textSize=42f;gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL;setPadding(12,0,12,0);maxLines=1}
        root.addView(display,LinearLayout.LayoutParams(-1,100))
    }

    private fun memoryRow(){
        val r=LinearLayout(this)
        mem(r,"M+"){memory+=value();memText()}
        mem(r,"M−"){memory-=value();memText()}
        mem(r,"MR"){display.text=fmt(memory);fresh=true}
        mem(r,"MC"){memory=0.0;memText()}
        mem(r,"SCI"){scientificPanel.visibility=if(scientificPanel.visibility==View.VISIBLE)View.GONE else View.VISIBLE}
        root.addView(r,LinearLayout.LayoutParams(-1,60))
    }

    private fun mem(r:LinearLayout,s:String,a:()->Unit){
        val b=button(s);b.setOnClickListener{a()};r.addView(b,LinearLayout.LayoutParams(0,-1,1f))
    }

    private fun scientific(){
        scientificPanel=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;visibility=View.GONE}
        sci("sin"){sciCalc("sin"){sin(Math.toRadians(it))}}
        sci("cos"){sciCalc("cos"){cos(Math.toRadians(it))}}
        sci("tan"){sciCalc("tan"){tan(Math.toRadians(it))}}
        sci("√"){sciCalc("√"){sqrt(it)}}
        sci("x²"){sciCalc("x²"){it*it}}
        root.addView(scientificPanel)
    }

    private fun sci(s:String,a:()->Unit){
        val b=button(s);b.setOnClickListener{a()};scientificPanel.addView(b,LinearLayout.LayoutParams(0,60,1f))
    }

    private fun keys(){
        row("AC"){clear()};row("⌫"){del()};row("%"){percent()};row("÷"){setOp("/")}
        row("7","8","9","×",::number,::number,::number){setOp("*")}
        row("4","5","6","−",::number,::number,::number){setOp("-")}
        row("1","2","3","+",::number,::number,::number){setOp("+")}
        val r=LinearLayout(this)
        addKey(r,"0",2f){number("0")};addKey(r,".",1f){dot()};addKey(r,"=",1f){calc()}
        root.addView(r,LinearLayout.LayoutParams(-1,0,1f))
    }

    private fun row(a:String,b:String,c:String,d:String,fa:(String)->Unit,fb:(String)->Unit,fc:(String)->Unit,fd:()->Unit){
        val r=LinearLayout(this)
        addKey(r,a,1f){fa(a)};addKey(r,b,1f){fb(b)};addKey(r,c,1f){fc(c)}
        addKey(r,d,1f){fd()}
        root.addView(r,LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun row(s:String,a:()->Unit){val r=LinearLayout(this);addKey(r,s,1f,a);root.addView(r,LinearLayout.LayoutParams(-1,60))}
    private fun addKey(r:LinearLayout,s:String,w:Float,a:()->Unit){
        val b=if(s in listOf("+","−","×","÷","=")) opButton(s) else button(s)
        b.setOnClickListener{a()};r.addView(b,LinearLayout.LayoutParams(0,-1,w))
    }
    private fun button(s:String)=Button(this).apply{text=s;textSize=18f;setPadding(2,2,2,2)}
    private fun opButton(s:String)=button(s).apply{setTextColor(Color.WHITE);setBackgroundColor(accent)}

    private fun number(s:String){
        if(fresh||display.text.toString()=="0"||display.text.toString()=="خطأ"){display.text=s;fresh=false}else display.append(s)
    }
    private fun dot(){if(fresh){display.text="0.";fresh=false}else if(!display.text.contains("."))display.append(".")}
    private fun value()=display.text.toString().toDoubleOrNull()?:0.0
    private fun setOp(o:String){first=value();op=o;history.text="${fmt(first)} ${sym(o)}";fresh=true}
    private fun calc(){
        if(op.isEmpty())return
        val second=value()
        val result=when(op){"+"->first+second;"-"->first-second;"*"->first*second;"/"->if(second==0.0)Double.NaN else first/second;else->second}
        val out=if(result.isNaN()||result.isInfinite())"خطأ" else fmt(result)
        val rec="${fmt(first)} ${sym(op)} ${fmt(second)} = $out"
        records.add(0,rec);history.text=rec;display.text=out;op="";fresh=true
    }
    private fun sciCalc(name:String,f:(Double)->Double){
        val x=value();val r=f(x);val out=if(r.isNaN()||r.isInfinite())"خطأ" else fmt(r)
        val rec="$name(${fmt(x)}) = $out";records.add(0,rec);history.text=rec;display.text=out;fresh=true
    }
    private fun percent(){display.text=fmt(value()/100.0);fresh=true}
    private fun del(){if(fresh||display.text.length<=1||display.text=="خطأ"){display.text="0";fresh=true}else display.text=display.text.dropLast(1)}
    private fun clear(){first=0.0;op="";display.text="0";history.text="";fresh=true}
    private fun memText(){memoryText.text="M: ${fmt(memory)}"}
    private fun fmt(x:Double):String{if(x==x.toLong().toDouble())return x.toLong().toString();return String.format(Locale.US,"%.10f",x).trimEnd('0').trimEnd('.')}
    private fun sym(o:String)=when(o){"*"->"×";"/"->"÷";"-"->"−";else->"+"}

    private fun showHistory(){
        val s=if(records.isEmpty())"لا توجد عمليات محفوظة." else records.take(100).joinToString("\n\n")
        AlertDialog.Builder(this).setTitle("🧾 سجل العمليات").setMessage(s).setNegativeButton("إغلاق",null)
            .setPositiveButton("مسح السجل"){_,_->records.clear();history.text=""}.show()
    }

    private fun theme(){
        val bg=if(dark)Color.rgb(15,17,23)else Color.rgb(245,245,245)
        val panel=if(dark)Color.rgb(25,28,36)else Color.WHITE
        val bc=if(dark)Color.rgb(37,41,52)else Color.rgb(225,225,230)
        val tc=if(dark)Color.WHITE else Color.BLACK
        root.setBackgroundColor(bg);display.setBackgroundColor(panel);display.setTextColor(tc)
        history.setTextColor(if(dark)Color.LTGRAY else Color.DKGRAY)
        memoryText.setTextColor(if(dark)Color.LTGRAY else Color.DKGRAY)
        paint(root,bc,tc)
    }
    private fun paint(p:ViewGroup,c:Int,t:Int){
        for(i in 0 until p.childCount){
            val v=p.getChildAt(i)
            if(v is Button && v.text !in listOf("+","−","×","÷","=")){v.setBackgroundColor(c);v.setTextColor(t)}
            if(v is ViewGroup)paint(v,c,t)
        }
    }
}
