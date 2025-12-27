package com.example.straightpool.data

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MatchHistoryRow(
    val timestampIso: String,
    val week: Int?,
    val rosterA: Int,
    val rosterB: Int,
    val scoreA: Int,
    val scoreB: Int,
    val highRunA: Int?,
    val highRunB: Int?,
    val innings: Int?,
    val countsForStandings: Boolean = false,
    val note: String? = null
)

class MatchHistoryRepoV2(ctx: Context) {

    private val file: File = File(ctx.filesDir, "match_history.csv")

    fun append(row: MatchHistoryRow) {
        ensureHeader()
        file.appendText(row.toCsvLine() + "\n")
    }

    fun readAll(): List<MatchHistoryRow> {
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        if (lines.isEmpty()) return emptyList()

        return lines
            .drop(1)
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("timestampIso,week,rosterA") } // safety
            .mapNotNull { parseLine(it) }
    }

    fun exportText(): String {
        if (!file.exists()) return header() + "\n"
        return file.readText()
    }

    fun clearAll() {
        if (file.exists()) file.delete()
    }

    private fun ensureHeader() {
        if (!file.exists() || file.length() == 0L) {
            file.writeText(header() + "\n")
        }
    }

    private fun header(): String =
        "timestampIso,week,rosterA,rosterB,scoreA,scoreB,highRunA,highRunB,innings,countsForStandings,note"

    private fun MatchHistoryRow.toCsvLine(): String {
        fun esc(s: String): String =
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                "\"" + s.replace("\"", "\"\"") + "\""
            } else s

        return listOf(
            esc(timestampIso),
            week?.toString() ?: "",
            rosterA.toString(),
            rosterB.toString(),
            scoreA.toString(),
            scoreB.toString(),
            highRunA?.toString() ?: "",
            highRunB?.toString() ?: "",
            innings?.toString() ?: "",
            countsForStandings.toString(),
            note?.let { esc(it) } ?: ""
        ).joinToString(",")
    }

    private fun parseLine(line: String): MatchHistoryRow? {
        val parts = splitCsvLine(line)
        if (parts.size < 11) return null

        fun intOrNull(s: String) = s.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        fun boolLoose(s: String) = s.trim().lowercase() in setOf("true", "t", "1", "yes", "y")

        return MatchHistoryRow(
            timestampIso = parts[0].trim(),
            week = intOrNull(parts[1]),
            rosterA = intOrNull(parts[2]) ?: return null,
            rosterB = intOrNull(parts[3]) ?: return null,
            scoreA = intOrNull(parts[4]) ?: return null,
            scoreB = intOrNull(parts[5]) ?: return null,
            highRunA = intOrNull(parts[6]),
            highRunB = intOrNull(parts[7]),
            innings = intOrNull(parts[8]),
            countsForStandings = boolLoose(parts[9]),
            note = parts[10].trim().ifEmpty { null }
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    companion object {
        fun nowIso(): String =
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
