package com.mat.tradehelper.data

data class IntegrityReport(val healthy:Boolean,val trades:Int,val knowledge:Int,val audit:Int,val issues:List<String>)
object IntegrityEngine {
 suspend fun verify(db:AppDb):IntegrityReport {
  val issues=mutableListOf<String>()
  val trades=db.trades().snapshot()
  val notes=db.knowledge().snapshot()
  trades.forEachIndexed{i,x->if(x.symbol.isBlank())issues.add("Trade ${i+1}: simbol kosong")}
  notes.forEachIndexed{i,x->if(x.title.isBlank()||x.content.isBlank())issues.add("Nota ${i+1}: kandungan tidak lengkap")}
  return IntegrityReport(issues.isEmpty(),trades.size,notes.size,db.audit().count(),issues)
 }
}
