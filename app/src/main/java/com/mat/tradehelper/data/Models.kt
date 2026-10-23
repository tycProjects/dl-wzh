package com.mat.tradehelper.data
import androidx.room.*
@Entity(tableName="trades") data class Trade(@PrimaryKey(autoGenerate=true) val id:Long=0,val symbol:String,val side:String,val resultR:Double,val note:String="",val createdAt:Long=System.currentTimeMillis())
@Entity(tableName="knowledge") data class Knowledge(@PrimaryKey(autoGenerate=true) val id:Long=0,val type:String,val title:String,val content:String,val createdAt:Long=System.currentTimeMillis())
@Entity(tableName="audit_log") data class AuditLog(@PrimaryKey(autoGenerate=true) val id:Long=0,val action:String,val detail:String,val createdAt:Long=System.currentTimeMillis())
