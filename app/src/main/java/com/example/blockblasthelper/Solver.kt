package com.example.blockblasthelper

data class Cell(val r:Int,val c:Int)
data class Shape(val name:String,val cells:List<Cell>)
data class Move(val slot:Int,val row:Int,val col:Int,val shape:Shape,val cleared:Int)

object Shapes {
 val all=listOf(
  Shape("Ô đơn",listOf(Cell(0,0))),
  Shape("Thanh 2 ngang",(0..1).map{Cell(0,it)}), Shape("Thanh 3 ngang",(0..2).map{Cell(0,it)}),
  Shape("Thanh 4 ngang",(0..3).map{Cell(0,it)}), Shape("Thanh 5 ngang",(0..4).map{Cell(0,it)}),
  Shape("Thanh 2 dọc",(0..1).map{Cell(it,0)}), Shape("Thanh 3 dọc",(0..2).map{Cell(it,0)}),
  Shape("Thanh 4 dọc",(0..3).map{Cell(it,0)}), Shape("Thanh 5 dọc",(0..4).map{Cell(it,0)}),
  Shape("Vuông 2x2",listOf(Cell(0,0),Cell(0,1),Cell(1,0),Cell(1,1))),
  Shape("Vuông 3x3",(0..2).flatMap{r->(0..2).map{c->Cell(r,c)}}),
  Shape("L nhỏ",listOf(Cell(0,0),Cell(1,0),Cell(1,1))),
  Shape("L dài",listOf(Cell(0,0),Cell(1,0),Cell(2,0),Cell(2,1))),
  Shape("L ngược",listOf(Cell(0,1),Cell(1,1),Cell(1,0))),
  Shape("T",listOf(Cell(0,0),Cell(0,1),Cell(0,2),Cell(1,1))),
  Shape("Z",listOf(Cell(0,0),Cell(0,1),Cell(1,1),Cell(1,2))),
  Shape("S",listOf(Cell(0,1),Cell(0,2),Cell(1,0),Cell(1,1))),
  Shape("Dấu cộng",listOf(Cell(0,1),Cell(1,0),Cell(1,1),Cell(1,2),Cell(2,1)))
 )
}
object Solver {
 private const val N=10
 fun fits(b:Array<BooleanArray>,s:Shape,r:Int,c:Int)=s.cells.all{r+it.r in 0 until N&&c+it.c in 0 until N&&!b[r+it.r][c+it.c]}
 fun put(b:Array<BooleanArray>,s:Shape,r:Int,c:Int):Int { if(!fits(b,s,r,c))return -1; s.cells.forEach{b[r+it.r][c+it.c]=true}; return clear(b) }
 fun best(board:Array<BooleanArray>,pieces:List<Shape?>):List<Move>{
  val active=pieces.mapIndexedNotNull{i,s->s?.let{i to it}}; if(active.isEmpty())return emptyList()
  var bestPath=emptyList<Move>(); var bestScore=Int.MIN_VALUE
  fun dfs(b:Array<BooleanArray>,left:List<Pair<Int,Shape>>,path:List<Move>,score:Int){
   if(left.isEmpty()){val v=score+evaluate(b);if(v>bestScore){bestScore=v;bestPath=path};return}
   var any=false
   for((slot,s) in left){val rest=left.filterNot{it.first==slot};val mr=s.cells.maxOf{it.r};val mc=s.cells.maxOf{it.c}
    for(r in 0..(N-1-mr))for(c in 0..(N-1-mc))if(fits(b,s,r,c)){any=true;val nb=Array(N){b[it].copyOf()};s.cells.forEach{nb[r+it.r][c+it.c]=true};val cleared=clear(nb);dfs(nb,rest,path+Move(slot,r,c,s,cleared),score+s.cells.size*2+cleared*35)}
   }
   if(!any){val v=score-left.size*1000+evaluate(b);if(v>bestScore){bestScore=v;bestPath=path}}
  }
  dfs(Array(N){board[it].copyOf()},active,emptyList(),0);return bestPath
 }
 private fun clear(b:Array<BooleanArray>):Int{val rs=(0 until N).filter{r->(0 until N).all{c->b[r][c]}};val cs=(0 until N).filter{c->(0 until N).all{r->b[r][c]}};rs.forEach{r->for(c in 0 until N)b[r][c]=false};cs.forEach{c->for(r in 0 until N)b[r][c]=false};return rs.size+cs.size}
 private fun evaluate(b:Array<BooleanArray>):Int{var count=0;var holes=0;for(r in 0 until N)for(c in 0 until N)if(b[r][c])count++ else {val near=listOf(r-1 to c,r+1 to c,r to c-1,r to c+1).count{(x,y)->x in 0 until N&&y in 0 until N&&b[x][y]};if(near==4)holes++};return -count/3-holes*3}
}
