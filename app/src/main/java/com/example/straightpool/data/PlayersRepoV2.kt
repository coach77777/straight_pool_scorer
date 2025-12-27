package com.example.straightpool.data

import android.content.Context
import java.io.File

data class PlayerRow(
    val roster: Int,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val isBye: Boolean = false
)

class PlayersRepoV2(private val ctx: Context) {

    private val file: File = File(ctx.filesDir, "players.csv")

    init {
        ensureHeader()
    }

    fun readAll(): List<PlayerRow> {
        ensureHeader()
        if (!file.exists()) return emptyList()

        val lines = file.readLines()
        if (lines.isEmpty()) return emptyList()

        return lines
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { parseLine(it) }
            .sortedBy { it.roster }
    }

    fun exportText(): String {
        ensureHeader()
        return if (file.exists()) file.readText() else header() + "\n"
    }

    fun upsert(row: PlayerRow) {
        val rows = readAll().toMutableList()
        val idx = rows.indexOfFirst { it.roster == row.roster }
        if (idx >= 0) rows[idx] = row else rows.add(row)
        replaceAll(rows)
    }

    fun delete(roster: Int) {
        val rows = readAll().filterNot { it.roster == roster }
        replaceAll(rows)
    }

    fun clearAll() {
        replaceAll(emptyList())
    }

    // IMPORTANT: Public method that overwrites the file
    fun replaceAll(rows: List<PlayerRow>) {
        val out = buildString {
            append(header()).append("\n")
            rows.sortedBy { it.roster }.forEach { r ->
                append(toCsvLine(r)).append("\n")
            }
        }
        file.writeText(out)
    }

    fun replaceFromCsvText(text: String): Int {
        val lines = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return 0

        val hdr = splitLooseLine(lines.first())
        val idxRoster = hdr.indexOfFirst { it.equals("roster", true) || it.equals("rosterNumber", true) }
        val idxName   = hdr.indexOfFirst { it.equals("name", true) }
        val idxPhone  = hdr.indexOfFirst { it.equals("phone", true) }
        val idxEmail  = hdr.indexOfFirst { it.equals("email", true) }
        val idxBye    = hdr.indexOfFirst { it.equals("isBye", true) }

        if (idxRoster < 0 || idxName < 0) return 0

        val rows = lines.drop(1).mapNotNull { line ->
            val parts = splitLooseLine(line)

            val roster = parts.getOrNull(idxRoster)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val name   = parts.getOrNull(idxName)?.trim().orEmpty()

            val phone = parts.getOrNull(idxPhone)?.trim().takeIf { !it.isNullOrBlank() }
            val email = parts.getOrNull(idxEmail)?.trim().takeIf { !it.isNullOrBlank() }

            val isBye = parts.getOrNull(idxBye)?.trim()?.let {
                it.equals("true", true) || it == "1" || it.equals("yes", true) || it.equals("y", true)
            } ?: false

            PlayerRow(
                roster = roster,
                name = if (name.isBlank()) "Player $roster" else name,
                phone = phone,
                email = email,
                isBye = isBye
            )
        }

        replaceAll(rows)
        return rows.size
    }

    private fun ensureHeader() {
        if (!file.exists() || file.length() == 0L) {
            file.writeText(header() + "\n")
        }
    }

    private fun header(): String = "roster,name,phone,email,isBye"

    private fun toCsvLine(r: PlayerRow): String {
        fun esc(s: String): String =
            if (s.contains(",") || s.contains("\"") || s.contains("\n"))
                "\"" + s.replace("\"", "\"\"") + "\""
            else s

        return listOf(
            r.roster.toString(),
            esc(r.name),
            r.phone?.let { esc(it) } ?: "",
            r.email?.let { esc(it) } ?: "",
            r.isBye.toString()
        ).joinToString(",")
    }

    private fun parseLine(line: String): PlayerRow? {
        val parts = splitCsvLine(line)
        if (parts.size < 5) return null

        fun intOrNull(s: String) = s.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        fun boolLoose(s: String) = s.trim().lowercase() in setOf("true","t","1","yes","y")

        val roster = intOrNull(parts[0]) ?: return null
        val name = parts[1].trim().ifEmpty { "Player $roster" }
        val phone = parts[2].trim().ifEmpty { null }
        val email = parts[3].trim().ifEmpty { null }
        val isBye = boolLoose(parts[4])

        return PlayerRow(
            roster = roster,
            name = name,
            phone = phone,
            email = email,
            isBye = isBye
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                out.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    private fun splitLooseLine(line: String): List<String> {
        val delim = if (line.contains('\t')) '\t' else ','
        return line.split(delim).map { it.trim().trim('"') }
    }
}
