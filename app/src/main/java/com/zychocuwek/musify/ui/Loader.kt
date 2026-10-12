package com.zychocuwek.musify.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface Load<out T> {
    data object Busy : Load<Nothing>
    data class Err(val msg: String) : Load<Nothing>
    data class Ok<T>(val v: T) : Load<T>
}

class Loader<T>(private val scope: CoroutineScope, private val block: suspend () -> T) {
    var state by mutableStateOf<Load<T>>(Load.Busy)
        private set

    fun run() {
        state = Load.Busy
        scope.launch {
            state = try { Load.Ok(block()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Load.Err(e.message ?: "Gagal memuat") }
        }
    }
}

@Composable
fun <T> rememberLoader(key: Any?, block: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    val l = remember(key) { Loader(scope, block) }
    LaunchedEffect(l) { l.run() }
    return l
}

/** Menampilkan Loading / Error+Coba lagi / konten. */
@Composable
fun <T> Loader<T>.Show(content: @Composable (T) -> Unit) {
    when (val s = state) {
        is Load.Busy -> Loading()
        is Load.Err -> ErrorBox(s.msg) { run() }
        is Load.Ok -> content(s.v)
    }
}
