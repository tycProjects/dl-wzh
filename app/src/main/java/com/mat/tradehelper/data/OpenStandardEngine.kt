package com.mat.tradehelper.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object OpenStandardEngine {
 const val FORMAT="THJ"
 const val SCHEMA_VERSION=1

 suspend fun exportThj(context:Context,db:AppDb,uri:Uri){
  val root=JSONObject().put("format",FORMAT).put("schemaVersion",SCHEMA_VERSION).put("appVersion","9.0.0").put("exportedAt",System.currentTimeMillis())
  val trades=JSONArray();db.trades().snapshot().forEach{x->trades.put(JSONObject().put("uuid",stableId("trade",x.id,x.createdAt)).put("symbol",x.symbol).put("side",x.side).put("resultR",x.resultR).put("note",x.note).put("createdAt",x.createdAt))}
  val notes=JSONArray();db.knowledge().snapshot().forEach{x->notes.put(JSONObject().put("uuid",stableId("knowledge",x.id,x.createdAt)).put("type",x.type).put("title",x.title).put("content",x.content).put("createdAt",x.createdAt))}
  root.put("trades",trades).put("knowledge",notes)
  context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use{it.write(root.toString(2))}
  db.audit().add(AuditLog(action="THJ_EXPORTED",detail="${trades.length()} trade, ${notes.length()} nota"))
 }

 suspend fun importThj(context:Context,db:AppDb,uri:Uri):Pair<Int,Int>{
  val root=JSONObject(context.contentResolver.openInputStream(uri)!!.bufferedReader().use{it.readText()})
  require(root.optString("format")==FORMAT){"Bukan fail THJ"};require(root.optInt("schemaVersion")==SCHEMA_VERSION){"Schema THJ tidak disokong"}
  val tj=root.getJSONArray("trades");val nj=root.getJSONArray("knowledge")
  val trades=buildList{for(i in 0 until tj.length()){val x=tj.getJSONObject(i);add(Trade(symbol=x.getString("symbol"),side=x.getString("side"),resultR=x.getDouble("resultR"),note=x.optString("note"),createdAt=x.getLong("createdAt")))}}
  val notes=buildList{for(i in 0 until nj.length()){val x=nj.getJSONObject(i);add(Knowledge(type=x.getString("type"),title=x.getString("title"),content=x.getString("content"),createdAt=x.getLong("createdAt")))}}
  db.withTransaction{db.trades().clear();db.knowledge().clear();if(trades.isNotEmpty())db.trades().addAll(trades);if(notes.isNotEmpty())db.knowledge().addAll(notes)}
  db.audit().add(AuditLog(action="THJ_IMPORTED",detail="${trades.size} trade, ${notes.size} nota"));return trades.size to notes.size
 }

 suspend fun exportCsv(context:Context,db:AppDb,uri:Uri){
  val out=buildString{appendLine("createdAt,symbol,side,resultR,note");db.trades().snapshot().forEach{x->appendLine(listOf(x.createdAt,csv(x.symbol),csv(x.side),x.resultR,csv(x.note)).joinToString(","))}}
  context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use{it.write(out)}
  db.audit().add(AuditLog(action="CSV_EXPORTED",detail="Trade journal"))
 }
 private fun csv(v:String)="\"${v.replace("\"","\"\"")}\""
 private fun stableId(type:String,id:Long,time:Long)=UUID.nameUUIDFromBytes("$type:$id:$time".toByteArray()).toString()
}
