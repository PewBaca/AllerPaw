package com.allerpaw.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UndoItem<T>(
    val item: T,
    val label: String
)

/**
 * Generischer Undo-Stack für Soft-Delete.
 * Max. 5 Einträge · Banner 8 Sekunden · dann endgültig löschen via [onExpired].
 */
class UndoManager<T>(
    private val scope: CoroutineScope,
    private val onExpired: suspend (T) -> Unit   // z.B. echtes Hard-Delete oder nichts
) {
    private val _stack = MutableStateFlow<List<UndoItem<T>>>(emptyList())
    val stack: StateFlow<List<UndoItem<T>>> = _stack

    private val jobs = mutableMapOf<T, Job>()

    companion object {
        const val UNDO_DELAY_MS = 8_000L
        const val MAX_STACK = 5
    }

    fun push(item: T, label: String) {
        val current = _stack.value.toMutableList()
        // Wenn Stack voll → ältesten sofort verfallen lassen
        if (current.size >= MAX_STACK) {
            val oldest = current.removeAt(0)
            jobs[oldest.item]?.cancel()
            scope.launch { onExpired(oldest.item) }
        }
        current.add(UndoItem(item, label))
        _stack.value = current

        jobs[item] = scope.launch {
            delay(UNDO_DELAY_MS)
            remove(item)
            onExpired(item)
        }
    }

    fun undo(item: T) {
        jobs[item]?.cancel()
        jobs.remove(item)
        remove(item)
    }

    private fun remove(item: T) {
        _stack.value = _stack.value.filter { it.item != item }
    }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _stack.value = emptyList()
    }
}
