package com.mbz71.gptchess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnalyzerScreen() }
    }
}

private val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

@Composable
fun AnalyzerScreen() {
    var fen by remember { mutableStateOf(startFen) }
    var status by remember { mutableStateOf("Pronto. Insira uma posição para analisar.") }
    var depth by remember { mutableStateOf(12f) }
    var result by remember { mutableStateOf("Nenhuma análise executada.") }
    val scope = rememberCoroutineScope()
    val engine = remember { StockfishUci() }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF35D07F),
        background = Color(0xFF080B0A),
        surface = Color(0xFF111816)
    )) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF080B0A)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text("GPT CHESS", color = Color(0xFF35D07F), fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("ANALYZER  •  MB.Z71", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(18.dp))
                    Text("ANÁLISE DE POSIÇÃO", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    ChessBoard(fen)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = fen, onValueChange = { fen = it },
                        label = { Text("FEN da posição") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF35D07F),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Profundidade: ${depth.toInt()}", color = Color.White)
                    Slider(value = depth, onValueChange = { depth = it }, valueRange = 6f..20f)
                    Button(
                        onClick = {
                            scope.launch {
                                status = "Analisando..."
                                result = withContext(Dispatchers.IO) { engine.analyze(fen.trim(), depth.toInt()) }
                                status = if (result.startsWith("ERRO:")) "Engine não disponível" else "Análise concluída"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF167A4B))
                    ) { Text("ANALISAR POSIÇÃO") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        fen = startFen
                        result = "Posição inicial carregada."
                        status = "Pronto."
                    }, modifier = Modifier.fillMaxWidth()) { Text("RESTAURAR TABULEIRO") }
                    Spacer(Modifier.height(14.dp))
                    Text(status, color = Color(0xFF35D07F), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111816)), modifier = Modifier.fillMaxWidth()) {
                        Text(result, color = Color.White, modifier = Modifier.padding(14.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Uso responsável: análise de posições próprias, estudo e revisão pós-partida. " +
                        "Este app não se conecta nem interfere em jogos de terceiros.",
                        color = Color.LightGray, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ChessBoard(fen: String) {
    val board = remember(fen) { parseBoard(fen) }
    Column(Modifier.fillMaxWidth().aspectRatio(1f)) {
        for (r in 0..7) {
            Row(Modifier.weight(1f)) {
                for (c in 0..7) {
                    val light = (r + c) % 2 == 0
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .background(if (light) Color(0xFFE5DCCB) else Color(0xFF63866F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(board[r][c], fontSize = 27.sp, color = if (board[r][c].firstOrNull()?.isUpperCase() == true) Color(0xFF17221B) else Color(0xFF101010))
                    }
                }
            }
        }
    }
}

private fun parseBoard(fen: String): List<List<String>> {
    val rows = fen.substringBefore(' ').split('/')
    if (rows.size != 8) return List(8) { List(8) { "" } }
    return rows.map { row ->
        val cells = mutableListOf<String>()
        row.forEach { ch ->
            if (ch.isDigit()) repeat(ch.digitToInt()) { cells.add("") }
            else cells.add(
                when (ch) {
                    'K' -> "♔"; 'Q' -> "♕"; 'R' -> "♖"; 'B' -> "♗"; 'N' -> "♘"; 'P' -> "♙"
                    'k' -> "♚"; 'q' -> "♛"; 'r' -> "♜"; 'b' -> "♝"; 'n' -> "♞"; 'p' -> "♟"
                    else -> ""
                }
            )
        }
        (cells + List((8 - cells.size).coerceAtLeast(0)) { "" }).take(8)
    }
}

private class StockfishUci {
    // Coloque um executável Stockfish compatível em app/src/main/assets/stockfish.
    // O binário não é incluído neste projeto.
    suspend fun analyze(fen: String, depth: Int): String {
        return try {
            val context = AppContextHolder.context ?: return "ERRO: contexto Android indisponível."
            val engineFile = java.io.File(context.filesDir, "stockfish")
            if (!engineFile.exists()) {
                context.assets.open("stockfish").use { input ->
                    engineFile.outputStream().use { output -> input.copyTo(output) }
                }
                engineFile.setExecutable(true)
            }
            val process = ProcessBuilder(engineFile.absolutePath).redirectErrorStream(true).start()
            val writer = process.outputStream.bufferedWriter()
            val reader = process.inputStream.bufferedReader()
            writer.write("uci\n"); writer.flush()
            var line: String?
            var ready = false
            while (reader.readLine().also { line = it } != null) {
                if (line == "uciok") { ready = true; break }
            }
            if (!ready) { process.destroy(); return "ERRO: Stockfish não iniciou corretamente." }
            writer.write("isready\n"); writer.flush()
            while (reader.readLine().also { line = it } != null) if (line == "readyok") break
            writer.write("position fen $fen\ngo depth ${depth.coerceIn(1, 20)}\n"); writer.flush()
            var best = "Análise sem melhor lance retornado."
            while (reader.readLine().also { line = it } != null) {
                val s = line ?: continue
                if (s.startsWith("info depth") && s.contains("score")) best = s
                if (s.startsWith("bestmove")) { best = "$best\n$s"; break }
            }
            writer.write("quit\n"); writer.flush(); process.destroy()
            best
        } catch (e: Exception) {
            "ERRO: ${e.message ?: "não foi possível iniciar Stockfish"}. Adicione o binário compatível em app/src/main/assets/stockfish."
        }
    }
}

private object AppContextHolder {
    var context: android.content.Context? = null
}
