package com.hematcount.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hematcount.app.model.CountItem

/** Counts + undo history + target lock for one keypad (used by every mode except peripheral blood). */
class Tally(val key: String, defaultTarget: Int) {
    val counts = mutableStateMapOf<String, Int>()
    private val undoStack = mutableStateListOf<Pair<String, Int>>()
    var target by mutableStateOf(defaultTarget)
    var locked by mutableStateOf(false)

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    fun count(id: String) = counts[id] ?: 0

    fun total(items: List<CountItem>): Int = items.filter { it.countsInTotal }.sumOf { count(it.id) }

    fun tap(item: CountItem, items: List<CountItem>, correction: Boolean): TapResult {
        if (correction) {
            val c = count(item.id)
            if (c == 0) return TapResult.NOTHING_TO_REMOVE
            counts[item.id] = c - 1
            undoStack.add(item.id to -1)
            if (locked && target != 0 && total(items) < target) locked = false
            return TapResult.REMOVED
        }
        if (locked) return TapResult.LOCKED
        counts[item.id] = count(item.id) + 1
        undoStack.add(item.id to 1)
        while (undoStack.size > 3000) undoStack.removeAt(0)
        if (item.countsInTotal && target != 0 && total(items) >= target) {
            locked = true
            return TapResult.TARGET_REACHED
        }
        return TapResult.ADDED
    }

    /** @return id of the counter that changed, or null. */
    fun undo(items: List<CountItem>): String? {
        val (id, delta) = undoStack.removeLastOrNull() ?: return null
        counts[id] = (count(id) - delta).coerceAtLeast(0)
        if (locked && target != 0 && total(items) < target) locked = false
        return id
    }

    fun setTarget(t: Int, items: List<CountItem>) {
        target = t
        locked = t != 0 && total(items) >= t
    }

    fun clear() { counts.clear(); undoStack.clear(); locked = false }

    fun load(map: Map<String, Int>) {
        counts.clear(); undoStack.clear()
        map.forEach { (k, v) -> if (v > 0) counts[k] = v }
    }

    fun encodeCounts(): String = counts.entries.filter { it.value > 0 }.joinToString(",") { "${it.key}:${it.value}" }
    fun encodeUndo(): String = undoStack.takeLast(3000).joinToString(",") { "${it.first}:${it.second}" }

    fun decode(countsText: String?, undoText: String?) {
        counts.clear(); undoStack.clear()
        countsText?.split(",")?.forEach { e ->
            val p = e.split(":"); val v = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && v != null && v > 0) counts[p[0]] = v
        }
        undoText?.takeIf { it.isNotBlank() }?.split(",")?.forEach { e ->
            val p = e.split(":"); val d = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && d != null) undoStack.add(p[0] to d)
        }
    }
}
