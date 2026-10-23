package com.mat.tradehelper.data
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities=[Trade::class,Knowledge::class,AuditLog::class],version=2,exportSchema=false)
abstract class AppDb:RoomDatabase(){
 abstract fun trades():TradeDao
 abstract fun knowledge():KnowledgeDao
 abstract fun audit():AuditDao
 companion object{
  @Volatile private var i:AppDb?=null
  val MIGRATION_1_2=object:Migration(1,2){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, action TEXT NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL)")}}
  fun get(c:Context)=i?:synchronized(this){i?:Room.databaseBuilder(c.applicationContext,AppDb::class.java,"trade-helper-v6.db").addMigrations(MIGRATION_1_2).build().also{i=it}}
 }
}
