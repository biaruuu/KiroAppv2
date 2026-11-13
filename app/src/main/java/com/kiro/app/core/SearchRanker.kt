package com.kiro.app.core

import android.util.Log
import com.kiro.app.data.Anime

/**
 * Re-ranks API search results by title relevance.
 * Uses the Rust engine (libkiro_core.so) when present, otherwise an identical Kotlin implementation.
 */
object SearchRanker {
    private var logged = false

    fun rank(query: String, items: List<Anime>): List<Anime> {
        if (query.isBlank() || items.size < 2) return items

        val texts = Array(items.size) { items[it].searchText() }
        val native = NativeSearch.rankOrNull(query, texts)
        if (!logged) {
            logged = true
            Log.i("Kiro", "Search ranker: " + if (native != null) "Rust (native)" else "Kotlin fallback")
        }
        val order = native ?: kotlinRank(query, texts)
        if (order.isEmpty()) return items // nothing scored: keep the API's own order

        val seen = BooleanArray(items.size)
        val out = ArrayList<Anime>(items.size)
        for (i in order) {
            if (i in items.indices && !seen[i]) {
                seen[i] = true
                out.add(items[i])
            }
        }
        for (i in items.indices) if (!seen[i]) out.add(items[i])
        return out
    }

    // ---- Kotlin port of native/src/ranker.rs (keep both in sync) ----

    internal fun kotlinRank(query: String, texts: Array<String>): IntArray {
        val q = normalize(query)
        if (q.isEmpty()) return IntArray(0)
        return texts.withIndex()
            .map { (i, t) -> score(q, normalize(t)) to i }
            .filter { it.first > 0 }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .map { it.second }
            .toIntArray()
    }

    internal fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        var lastSpace = true
        for (c in s) {
            if (c.isLetterOrDigit()) {
                sb.append(c.lowercaseChar())
                lastSpace = false
            } else if (!lastSpace) {
                sb.append(' ')
                lastSpace = true
            }
        }
        while (sb.isNotEmpty() && sb.last() == ' ') sb.setLength(sb.length - 1)
        return sb.toString()
    }

    internal fun score(query: String, text: String): Int {
        if (query.isEmpty() || text.isEmpty()) return 0
        if (text == query) return 1000

        var total = 0
        total += when {
            text.startsWith(query) -> 800
            text.contains(" $query") -> 600
            text.contains(query) -> 400
            else -> 0
        }

        val words = text.split(' ')
        var matched = 0
        var tokens = 0
        for (t in query.split(' ')) {
            tokens++
            var best = 0
            for (w in words) {
                val s = when {
                    w == t -> 100
                    w.startsWith(t) -> 70
                    t.length >= 3 && w.contains(t) -> 40
                    t.length >= 4 && editDistance(w, t) <= (if (t.length <= 5) 1 else 2) -> 30
                    else -> 0
                }
                if (s > best) best = s
            }
            if (best > 0) {
                matched++
                total += best
            }
        }

        if (total == 0) return 0
        if (matched == tokens) total += 50
        return total - minOf(text.length, 100) / 10
    }

    private fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }
}
