package com.mat.tradehelper.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

object BackupEngine {
    const val SCHEMA_VERSION = 1

    suspend fun export(context: Context, db: AppDb, uri: Uri) {
        val root = JSONObject()
            .put("format", "TradeHelperBackup")
            .put("schemaVersion", SCHEMA_VERSION)
            .put("appVersion", "7.0.0")
            .put("createdAt", System.currentTimeMillis())
        val trades = JSONArray()
        db.trades().snapshot().forEach { x ->
            trades.put(JSONObject().put("id",x.id).put("symbol",x.symbol).put("side",x.side).put("resultR",x.resultR).put("note",x.note).put("createdAt",x.createdAt))
        }
        val knowledge = JSONArray()
        db.knowledge().snapshot().forEach { x ->
            knowledge.put(JSONObject().put("id",x.id).put("type",x.type).put("title",x.title).put("content",x.content).put("createdAt",x.createdAt))
        }
        root.put("trades",trades).put("knowledge",knowledge)
        context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { it.write(root.toString(2)) }
        db.audit().add(AuditLog(action="BACKUP_EXPORTED", detail="Schema $SCHEMA_VERSION"))
    }

    suspend fun restore(context: Context, db: AppDb, uri: Uri): Pair<Int,Int> {
        val text=context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val root=JSONObject(text)
        require(root.optString("format")=="TradeHelperBackup") { "Format backup tidak sah" }
        require(root.optInt("schemaVersion")==SCHEMA_VERSION) { "Versi schema tidak disokong" }
        val tradesJson=root.getJSONArray("trades")
        val notesJson=root.getJSONArray("knowledge")
        val trades=buildList { for(i in 0 until tradesJson.length()){val x=tradesJson.getJSONObject(i);add(Trade(symbol=x.getString("symbol"),side=x.getString("side"),resultR=x.getDouble("resultR"),note=x.optString("note"),createdAt=x.getLong("createdAt")))} }
        val notes=buildList { for(i in 0 until notesJson.length()){val x=notesJson.getJSONObject(i);add(Knowledge(type=x.getString("type"),title=x.getString("title"),content=x.getString("content"),createdAt=x.getLong("createdAt")))} }
        db.withTransaction { db.trades().clear();db.knowledge().clear();if(trades.isNotEmpty())db.trades().addAll(trades);if(notes.isNotEmpty())db.knowledge().addAll(notes) }
        db.audit().add(AuditLog(action="BACKUP_RESTORED",detail="${trades.size} trade, ${notes.size} nota"))
        return trades.size to notes.size
    }
}
