package com.example.blockblasthelper

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*

class MainActivity:Activity(){
 private val n=10; private var board=Array(n){BooleanArray(n)}; private val chosen=arrayOfNulls<Shape>(3)
 private val views=Array(n){arrayOfNulls<TextView>(n)}; private lateinit var info:TextView; private lateinit var pieceArea:LinearLayout
 private var hint:Move?=null; private val undo=ArrayDeque<Pair<Array<BooleanArray>,Array<Shape?>>>()
 private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
 private fun txt(s:String,size:Float=14f,color:Int=Color.WHITE)=TextView(this).apply{text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL}
 private fun button(s:String,act:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setOnClickListener{act()}}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=Color.rgb(16,24,39);window.navigationBarColor=Color.rgb(16,24,39);ui();draw();drawPieces()}
 private fun ui(){val scroll=ScrollView(this);val page=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(12),dp(12),dp(16));setBackgroundColor(Color.rgb(16,24,39))}
  page.addView(txt("BLOCK BLAST HELPER",22f).apply{setTypeface(null,Typeface.BOLD)});page.addView(txt("Nhập bàn cờ 10×10 và chọn tối đa 3 khối.",13f,Color.LTGRAY))
  info=txt("Chạm ô để đánh dấu ô đang bị chiếm.",13f,Color.rgb(255,210,120));page.addView(info)
  val grid=GridLayout(this).apply{rowCount=n;columnCount=n;setPadding(dp(2),dp(2),dp(2),dp(2));setBackgroundColor(Color.rgb(55,70,92))}
  for(r in 0 until n)for(c in 0 until n){val v=TextView(this).apply{gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(34,48,70));setOnClickListener{save();board[r][c]=!board[r][c];hint=null;draw()}};views[r][c]=v;grid.addView(v,GridLayout.LayoutParams(GridLayout.spec(r,1f),GridLayout.spec(c,1f)).apply{width=0;height=dp(29);setMargins(dp(1),dp(1),dp(1),dp(1))})}
  page.addView(grid,LinearLayout.LayoutParams(-1,dp(310)));val row=LinearLayout(this).apply{orientation=0};row.addView(button("Xóa bàn cờ"){save();board=Array(n){BooleanArray(n)};hint=null;draw()},LinearLayout.LayoutParams(0,-2,1f));row.addView(button("Hoàn tác"){restore()},LinearLayout.LayoutParams(0,-2,1f));page.addView(row)
  page.addView(txt("CHỌN 3 KHỐI",17f).apply{setPadding(0,dp(12),0,dp(4));setTypeface(null,Typeface.BOLD)});pieceArea=LinearLayout(this).apply{orientation=1};page.addView(pieceArea)
  val actions=LinearLayout(this).apply{orientation=0};actions.addView(button("TÌM NƯỚC ĐẶT"){solve()},LinearLayout.LayoutParams(0,-2,1f));actions.addView(button("ÁP DỤNG GỢI Ý"){applyHint()},LinearLayout.LayoutParams(0,-2,1f));page.addView(actions)
  page.addView(button("Xóa lựa chọn khối"){save();for(i in 0..2)chosen[i]=null;hint=null;drawPieces();draw()})
  page.addView(txt("Lưu ý: bạn tự nhập bàn cờ và chọn khối. Ứng dụng chưa tự nhận diện ảnh hoặc điều khiển game. Gợi ý dựa trên tìm kiếm heuristic, có thể không tối ưu tuyệt đối.",12f,Color.LTGRAY).apply{setPadding(0,dp(12),0,0)})
  scroll.addView(page);setContentView(scroll)
 }
 private fun save(){undo.addLast(Array(n){board[it].copyOf()} to chosen.copyOf());if(undo.size>25)undo.removeFirst()}
 private fun restore(){if(undo.isEmpty()){info.text="Không có thao tác để hoàn tác.";return};val x=undo.removeLast();board=Array(n){x.first[it].copyOf()};for(i in 0..2)chosen[i]=x.second[i];hint=null;draw();drawPieces();info.text="Đã hoàn tác."}
 private fun draw(){for(r in 0 until n)for(c in 0 until n){val v=views[r][c]?:continue;val isHint=hint?.shape?.cells?.any{hint!!.row+it.r==r&&hint!!.col+it.c==c}?:false;v.text=if(isHint)"●" else "";v.setBackgroundColor(when{isHint->Color.rgb(255,190,70);board[r][c]->Color.rgb(54,196,164);else->Color.rgb(34,48,70)})}}
 private fun drawPieces(){pieceArea.removeAllViews();for(i in 0..2){val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL};row.addView(txt("Khối ${i+1}: ${chosen[i]?.name?:"Chuachon"}"),LinearLayout.LayoutParams(0,dp(48),1f));row.addView(button("Chọn"){pick(i)});row.addView(button("Bỏ"){chosen[i]=null;hint=null;drawPieces();draw()});pieceArea.addView(row)}}
 private fun pick(i:Int){AlertDialog.Builder(this).setTitle("Chọn khối ${i+1}").setItems(Shapes.all.map{it.name}.toTypedArray()){_,which->save();chosen[i]=Shapes.all[which];hint=null;drawPieces();draw()}.show()}
 private fun solve(){if(chosen.all{it==null}){info.text="Bạn cần chọn ít nhất một khối.";return};val moves=Solver.best(board,chosen.toList());if(moves.isEmpty()){info.text="Không tìm thấy vị trí đặt phù hợp.";hint=null;draw();return};hint=moves.first();draw();val m=moves.first();info.text="Gợi ý: ${m.shape.name}, hàng ${m.row+1}, cột ${m.col+1}. Tìm được chuỗi ${moves.size} khối."}
 private fun applyHint(){val m=hint;if(m==null){info.text="Hãy bấm TÌM NƯỚC ĐẶT trước.";return};save();val cleared=Solver.put(board,m.shape,m.row,m.col);if(cleared<0){hint=null;info.text="Gợi ý không còn hợp lệ, hãy tìm lại.";return};chosen[m.slot]=null;hint=null;draw();drawPieces();info.text="Đã đặt ${m.shape.name}; xóa ${cleared} hàng/cột. Chọn khối mới nếu cần."}
}
