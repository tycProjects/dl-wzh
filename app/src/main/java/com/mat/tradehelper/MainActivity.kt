package com.mat.tradehelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mat.tradehelper.data.*
import com.mat.tradehelper.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{AppTheme{App()}}}}
enum class Tab(val label:String){HOME("Utama"),JOURNAL("Jurnal"),VAULT("Vault"),RISK("Risiko"),DATA("Platform")}
@Composable fun App(){var tab by remember{mutableStateOf(Tab.HOME)};Scaffold(containerColor=Color.Black,bottomBar={NavigationBar(containerColor=Color.Black){Tab.entries.forEach{t->NavigationBarItem(tab==t,{tab=t},{Icon(when(t){Tab.HOME->Icons.Default.Home;Tab.JOURNAL->Icons.Default.Book;Tab.VAULT->Icons.Default.LibraryBooks;Tab.RISK->Icons.Default.Shield;Tab.DATA->Icons.Default.AccountTree},null)},{Text(t.label)})}}}){p->Box(Modifier.padding(p)){when(tab){Tab.HOME->Home();Tab.JOURNAL->Journal();Tab.VAULT->Vault();Tab.RISK->Risk();Tab.DATA->PlatformScreen()}}}}
@Composable fun CardBox(content:@Composable ColumnScope.()->Unit)=Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF080808)),shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,Color(0xFF202020))){Column(Modifier.padding(16.dp),content=content)}
@Composable fun Home(){val d=AppDb.get(LocalContext.current);val trades by d.trades().all().collectAsStateWithLifecycle(emptyList());val total=trades.sumOf{it.resultR};val wins=trades.count{it.resultR>0};val rate=if(trades.isEmpty())0 else wins*100/trades.size;LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Trade Helper V9",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};item{CardBox{Text("Prestasi",color=Color.Gray);Text("${if(total>=0)"+" else ""}$total R",style=MaterialTheme.typography.headlineLarge,color=if(total>=0)Green else Red)}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Card(Modifier.weight(1f),colors=CardDefaults.cardColors(Color(0xFF080808))){Column(Modifier.padding(16.dp)){Text("Win rate");Text("$rate%",fontWeight=FontWeight.Bold)}};Card(Modifier.weight(1f),colors=CardDefaults.cardColors(Color(0xFF080808))){Column(Modifier.padding(16.dp)){Text("Trade");Text(trades.size.toString(),fontWeight=FontWeight.Bold)}}}};item{Text("Data dashboard dikira daripada jurnal sebenar.",color=Color.Gray)}}}
@Composable fun Journal(){val d=AppDb.get(LocalContext.current);val list by d.trades().all().collectAsStateWithLifecycle(emptyList());val scope=rememberCoroutineScope();var symbol by remember{mutableStateOf("")};var rr by remember{mutableStateOf("")};Column(Modifier.padding(16.dp)){Text("Jurnal",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedTextField(symbol,{symbol=it},label={Text("Simbol")},modifier=Modifier.fillMaxWidth());OutlinedTextField(rr,{rr=it},label={Text("Keputusan R")},modifier=Modifier.fillMaxWidth());Button({if(symbol.isNotBlank())scope.launch{d.trades().add(Trade(symbol=symbol,side="BUY",resultR=rr.toDoubleOrNull()?:0.0));d.audit().add(AuditLog(action="TRADE_CREATED",detail="$symbol • ${rr.toDoubleOrNull()?:0.0}R"));symbol="";rr=""}},Modifier.fillMaxWidth()){Text("Simpan trade")};LazyColumn{items(list,key={it.id}){x->ListItem(headlineContent={Text(x.symbol)},supportingContent={Text("${x.side} • ${x.resultR}R")},trailingContent={IconButton({scope.launch{d.trades().delete(x);d.audit().add(AuditLog(action="TRADE_DELETED",detail=x.symbol))}}){Icon(Icons.Default.DeleteOutline,null)}})}}}}
@Composable fun Vault(){val d=AppDb.get(LocalContext.current);val list by d.knowledge().all().collectAsStateWithLifecycle(emptyList());val scope=rememberCoroutineScope();var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};Column(Modifier.padding(16.dp)){Text("Knowledge Vault",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Playbook, peraturan, pengajaran dan kesilapan",color=Color.Gray);OutlinedTextField(title,{title=it},label={Text("Tajuk")},modifier=Modifier.fillMaxWidth());OutlinedTextField(body,{body=it},label={Text("Kandungan")},modifier=Modifier.fillMaxWidth(),minLines=3);Button({if(title.isNotBlank()&&body.isNotBlank())scope.launch{d.knowledge().add(Knowledge(type="Playbook",title=title,content=body));d.audit().add(AuditLog(action="KNOWLEDGE_CREATED",detail=title));title="";body=""}},Modifier.fillMaxWidth()){Text("Simpan nota")};LazyColumn{items(list,key={it.id}){x->ListItem(headlineContent={Text(x.title)},supportingContent={Text(x.content)},trailingContent={IconButton({scope.launch{d.knowledge().delete(x);d.audit().add(AuditLog(action="KNOWLEDGE_DELETED",detail=x.title))}}){Icon(Icons.Default.DeleteOutline,null)}})}}}}
@Composable fun Risk(){var balance by remember{mutableStateOf("")};var pct by remember{mutableStateOf("1")};val amount=(balance.toDoubleOrNull()?:0.0)*(pct.toDoubleOrNull()?:0.0)/100;Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Risk Manager",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedTextField(balance,{balance=it},label={Text("Baki akaun")},modifier=Modifier.fillMaxWidth());OutlinedTextField(pct,{pct=it},label={Text("Risiko %")},modifier=Modifier.fillMaxWidth());CardBox{Text("Had kerugian per trade");Text("%.2f".format(amount),style=MaterialTheme.typography.headlineMedium,color=Green)};Text("Alat pengurusan risiko dan jurnal sahaja. Bukan nasihat kewangan.",color=Color.Gray)}}


@Composable fun PreservationScreen(){
    val context=LocalContext.current
    val db=remember{AppDb.get(context)}
    val scope=rememberCoroutineScope()
    var status by remember{mutableStateOf("Data kekal lokal pada peranti.")}
    var pendingRestore by remember{mutableStateOf<android.net.Uri?>(null)}
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch{runCatching{BackupEngine.export(context,db,uri)}.onSuccess{status="Backup berjaya disimpan."}.onFailure{status="Backup gagal: ${it.message}"}}}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)pendingRestore=uri}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Data Preservation",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
        item{Text("Eksport dan pulihkan jurnal serta Knowledge Vault dalam format JSON berversi.",color=Color.Gray)}
        item{CardBox{Icon(Icons.Default.SaveAlt,null,tint=Green);Spacer(Modifier.height(8.dp));Text("Backup JSON",fontWeight=FontWeight.Bold);Text("Simpan salinan mudah alih menggunakan Android file picker.",color=Color.Gray);Spacer(Modifier.height(12.dp));Button({create.launch("TradeHelper_V6_Backup.json")},Modifier.fillMaxWidth()){Text("Eksport backup")}}}
        item{CardBox{Icon(Icons.Default.Restore,null,tint=Green);Spacer(Modifier.height(8.dp));Text("Restore JSON",fontWeight=FontWeight.Bold);Text("Fail diperiksa sebelum database diganti.",color=Color.Gray);Spacer(Modifier.height(12.dp));OutlinedButton({open.launch(arrayOf("application/json","text/plain"))},Modifier.fillMaxWidth()){Text("Pilih fail backup")}}}
        item{CardBox{Text("Status",fontWeight=FontWeight.Bold);Text(status,color=Color.Gray)}}
        item{Text("Restore akan menggantikan jurnal dan nota semasa selepas pengesahan.",color=Color.Gray,style=MaterialTheme.typography.bodySmall)}
    }
    if(pendingRestore!=null)AlertDialog(onDismissRequest={pendingRestore=null},title={Text("Pulihkan backup?")},text={Text("Data jurnal dan vault semasa akan diganti. Proses menggunakan transaksi database supaya import tidak separuh siap.")},confirmButton={TextButton({val uri=pendingRestore!!;pendingRestore=null;scope.launch{runCatching{BackupEngine.restore(context,db,uri)}.onSuccess{status="Restore berjaya: ${it.first} trade, ${it.second} nota."}.onFailure{status="Restore gagal: ${it.message}"}}}){Text("Pulihkan",color=Green)}},dismissButton={TextButton({pendingRestore=null}){Text("Batal")}})
}


@Composable fun AuditScreen(){
 val context=LocalContext.current
 val db=remember{AppDb.get(context)}
 val logs by db.audit().all().collectAsStateWithLifecycle(emptyList())
 val scope=rememberCoroutineScope()
 var report by remember{mutableStateOf<IntegrityReport?>(null)}
 var showPreservation by remember{mutableStateOf(false)}
 if(showPreservation){Column{Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton({showPreservation=false}){Icon(Icons.Default.ArrowBack,null)};Text("Backup & Restore",fontWeight=FontWeight.Bold)};Box(Modifier.weight(1f)){PreservationScreen()}};return}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Audit & Preservation",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Text("Jejak perubahan penting dan periksa kesihatan data.",color=Color.Gray)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({scope.launch{report=IntegrityEngine.verify(db);db.audit().add(AuditLog(action="INTEGRITY_CHECK",detail=if(report!!.healthy)"Healthy" else "Issues found"))}},Modifier.weight(1f)){Icon(Icons.Default.VerifiedUser,null);Spacer(Modifier.width(6.dp));Text("Semak")};OutlinedButton({showPreservation=true},Modifier.weight(1f)){Icon(Icons.Default.Save,null);Spacer(Modifier.width(6.dp));Text("Backup")}}}
  report?.let{r->item{CardBox{Text(if(r.healthy)"Database sihat" else "Isu dikesan",fontWeight=FontWeight.Bold,color=if(r.healthy)Green else Red);Text("${r.trades} trade • ${r.knowledge} nota • ${r.audit} log",color=Color.Gray);r.issues.take(5).forEach{Text("• $it",color=Red)}}}}
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Aktiviti",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("${logs.size} rekod",color=Color.Gray)}}
  if(logs.isEmpty())item{CardBox{Text("Belum ada aktiviti direkod.",color=Color.Gray)}}
  else items(logs,key={it.id}){x->CardBox{Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.History,null,tint=Green);Spacer(Modifier.width(10.dp));Column{Text(x.action.replace('_',' '),fontWeight=FontWeight.SemiBold);Text(x.detail,color=Color.Gray,style=MaterialTheme.typography.bodySmall)}}}}
 }
}


@Composable fun AnalyticsScreen(){
 val context=LocalContext.current
 val db=remember{AppDb.get(context)}
 val trades by db.trades().all().collectAsStateWithLifecycle(emptyList())
 var showAudit by remember{mutableStateOf(false)}
 if(showAudit){Column{Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton({showAudit=false}){Icon(Icons.Default.ArrowBack,null)};Text("Audit & Preservation",fontWeight=FontWeight.Bold)};Box(Modifier.weight(1f)){AuditScreen()}};return}
 val chronological=trades.reversed()
 val wins=trades.filter{it.resultR>0};val losses=trades.filter{it.resultR<0};val net=trades.sumOf{it.resultR}
 val winRate=if(trades.isEmpty())0.0 else wins.size*100.0/trades.size
 val grossWin=wins.sumOf{it.resultR};val grossLoss=kotlin.math.abs(losses.sumOf{it.resultR})
 val pf=if(grossLoss>0)grossWin/grossLoss else if(grossWin>0)grossWin else 0.0
 val expectancy=if(trades.isEmpty())0.0 else net/trades.size
 var running=0.0;var peak=0.0;var drawdown=0.0
 val raw=buildList{add(0.0);chronological.forEach{x->running+=x.resultR;peak=maxOf(peak,running);drawdown=maxOf(drawdown,peak-running);add(running)}}
 val low=raw.minOrNull()?:0.0;val high=raw.maxOrNull()?:1.0;val range=(high-low).takeIf{it>0}?:1.0
 val curve=raw.map{0.12f+((it-low)/range).toFloat()*0.76f}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Analytics Warehouse",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Dikira daripada jurnal lokal",color=Color.Gray)};IconButton({showAudit=true}){Icon(Icons.Default.History,null,tint=Green)}}}
  item{CardBox{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Net performance",color=Color.Gray);Text("${if(net>=0)"+" else ""}${"%.2f".format(net)}R",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,color=if(net>=0)Green else Red)};Icon(Icons.Default.AutoGraph,null,tint=if(net>=0)Green else Red)}}}
  item{CardBox{Text("Equity Curve",fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));AnalyticsCurve(curve);if(trades.isEmpty())Text("Tambah trade untuk menjana carta.",color=Color.Gray,style=MaterialTheme.typography.bodySmall)}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AnalyticsTile("Win rate","${"%.0f".format(winRate)}%",Modifier.weight(1f));AnalyticsTile("Profit factor","%.2f".format(pf),Modifier.weight(1f))}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AnalyticsTile("Expectancy","${"%.2f".format(expectancy)}R",Modifier.weight(1f));AnalyticsTile("Max DD","${"%.2f".format(drawdown)}R",Modifier.weight(1f))}}
  item{CardBox{Text("Outcome Distribution",fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));OutcomeDistribution(wins.size,losses.size,trades.count{it.resultR==0.0});Spacer(Modifier.height(10.dp));Text("${wins.size} menang • ${losses.size} kalah • ${trades.count{it.resultR==0.0}} breakeven",color=Color.Gray)}}
  item{CardBox{Text("Ringkasan",fontWeight=FontWeight.Bold);Text("Jumlah trade: ${trades.size}",color=Color.Gray);Text("Gross win: +${"%.2f".format(grossWin)}R",color=Green);Text("Gross loss: -${"%.2f".format(grossLoss)}R",color=Red)}}
 }
}

@Composable fun AnalyticsTile(label:String,value:String,modifier:Modifier)=Card(modifier,colors=CardDefaults.cardColors(Color(0xFF080808)),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Color(0xFF202020))){Column(Modifier.padding(15.dp)){Text(label,color=Color.Gray,style=MaterialTheme.typography.labelMedium);Spacer(Modifier.height(5.dp));Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}}

@Composable fun AnalyticsCurve(values:List<Float>)=Canvas(Modifier.fillMaxWidth().height(180.dp)){
 if(values.size<2)return@Canvas
 repeat(4){i->val y=size.height*i/3f;drawLine(Color(0xFF181818),androidx.compose.ui.geometry.Offset(0f,y),androidx.compose.ui.geometry.Offset(size.width,y),1f)}
 val step=size.width/(values.size-1);val line=Path();val fill=Path()
 values.forEachIndexed{i,v->val x=i*step;val y=size.height-v*size.height;if(i==0){line.moveTo(x,y);fill.moveTo(x,size.height);fill.lineTo(x,y)}else{line.lineTo(x,y);fill.lineTo(x,y)}}
 fill.lineTo(size.width,size.height);fill.close();drawPath(fill,Brush.verticalGradient(listOf(Green.copy(alpha=.28f),Color.Transparent)));drawPath(line,Green,style=Stroke(5f,cap=StrokeCap.Round))
}

@Composable fun OutcomeDistribution(w:Int,l:Int,b:Int){val total=maxOf(1,w+l+b);Row(Modifier.fillMaxWidth().height(18.dp)){if(w>0)Box(Modifier.weight(w.toFloat()/total).fillMaxHeight().background(Green));if(l>0)Box(Modifier.weight(l.toFloat()/total).fillMaxHeight().background(Red));if(b>0)Box(Modifier.weight(b.toFloat()/total).fillMaxHeight().background(Color.Gray));if(w+l+b==0)Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF222222)))}}


@Composable fun PlatformScreen(){
 val context=LocalContext.current;val db=remember{AppDb.get(context)};val scope=rememberCoroutineScope()
 var status by remember{mutableStateOf("Format terbuka THJ bersedia.")};var pending by remember{mutableStateOf<android.net.Uri?>(null)};var showAnalytics by remember{mutableStateOf(false)};var query by remember{mutableStateOf("")}
 val trades by db.trades().all().collectAsStateWithLifecycle(emptyList());val notes by db.knowledge().all().collectAsStateWithLifecycle(emptyList())
 val thjOut=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){u->if(u!=null)scope.launch{runCatching{OpenStandardEngine.exportThj(context,db,u)}.onSuccess{status="THJ berjaya dieksport."}.onFailure{status="Eksport gagal: ${it.message}"}}}
 val csvOut=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){u->if(u!=null)scope.launch{runCatching{OpenStandardEngine.exportCsv(context,db,u)}.onSuccess{status="CSV berjaya dieksport."}.onFailure{status="CSV gagal: ${it.message}"}}}
 val thjIn=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null)pending=u}
 if(showAnalytics){Column{Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton({showAnalytics=false}){Icon(Icons.Default.ArrowBack,null)};Text("Analitik",fontWeight=FontWeight.Bold)};Box(Modifier.weight(1f)){AnalyticsScreen()}};return}
 val foundTrades=if(query.isBlank())emptyList() else trades.filter{it.symbol.contains(query,true)||it.note.contains(query,true)}
 val foundNotes=if(query.isBlank())emptyList() else notes.filter{it.title.contains(query,true)||it.content.contains(query,true)||it.type.contains(query,true)}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Open Data Platform",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("User owns data • THJ Schema 1",color=Color.Gray)};IconButton({showAnalytics=true}){Icon(Icons.Default.Analytics,null,tint=Green)}}}
  item{OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Cari jurnal dan vault")},singleLine=true)}
  if(query.isNotBlank())item{CardBox{Text("Hasil carian",fontWeight=FontWeight.Bold);Text("${foundTrades.size} trade • ${foundNotes.size} nota",color=Color.Gray);foundTrades.take(3).forEach{Text("Trade: ${it.symbol} ${it.resultR}R")};foundNotes.take(3).forEach{Text("Nota: ${it.title}")}}}
  item{CardBox{Icon(Icons.Default.DataObject,null,tint=Green);Text("THJ Open Format",fontWeight=FontWeight.Bold);Text("Eksport lengkap dengan schema version dan UUID stabil.",color=Color.Gray);Spacer(Modifier.height(10.dp));Button({thjOut.launch("TradeHelper_V9.thj.json")},Modifier.fillMaxWidth()){Text("Eksport THJ")};OutlinedButton({thjIn.launch(arrayOf("application/json","text/plain"))},Modifier.fillMaxWidth()){Text("Import THJ")}}}
  item{CardBox{Icon(Icons.Default.TableView,null,tint=Green);Text("CSV Portability",fontWeight=FontWeight.Bold);Text("Eksport jurnal untuk spreadsheet dan sistem lain.",color=Color.Gray);Spacer(Modifier.height(10.dp));OutlinedButton({csvOut.launch("TradeHelper_Trades.csv")},Modifier.fillMaxWidth()){Text("Eksport CSV")}}}
  item{CardBox{Text("Status",fontWeight=FontWeight.Bold);Text(status,color=Color.Gray);Text("${trades.size} trade • ${notes.size} nota",color=Color.Gray)}}
 }
 if(pending!=null)AlertDialog(onDismissRequest={pending=null},title={Text("Import THJ?")},text={Text("Data jurnal dan vault semasa akan diganti selepas format dan schema disahkan.")},confirmButton={TextButton({val u=pending!!;pending=null;scope.launch{runCatching{OpenStandardEngine.importThj(context,db,u)}.onSuccess{status="Import berjaya: ${it.first} trade, ${it.second} nota."}.onFailure{status="Import gagal: ${it.message}"}}}){Text("Import",color=Green)}},dismissButton={TextButton({pending=null}){Text("Batal")}})
}
