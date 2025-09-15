package com.example.straightpool.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset



/** Robust CSV splitter that understands quotes and commas inside quotes. */
private fun csvSplit(line: String): List<String> {
    val out = ArrayList<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        when {
            ch == '"' -> {
                // handle escaped quotes ("")
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"'); i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            ch == ',' && !inQuotes -> {
                out += sb.toString().trim()
                sb.clear()
            }
            else -> sb.append(ch)
        }
        i++
    }
    out += sb.toString().trim()
    return out
}

/**
 * Loads players from assets/players_including_bye_phone_emails.csv
 * Expected header (case-insensitive): id, name, phone, email.
 * If an id column is not present, a running index will be assigned.
 */
fun loadRosterFromAssets(ctx: Context): List<RosterPlayer> {
    val fileName = "players_including_bye_phone_emails.csv"
    return try {
        ctx.assets.open(fileName).use { ins ->
            BufferedReader(InputStreamReader(ins, Charset.forName("UTF-8"))).use { br ->
                val lines = br.readLines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return emptyList()

                val header = csvSplit(lines.first()).map { it.lowercase() }
                val idIdx    = header.indexOfFirst { it in setOf("playerid", "player_id", "id") }
                val nameIdx  = header.indexOfFirst { it in setOf("name", "player", "player_name") }
                val phoneIdx = header.indexOfFirst { it.contains("phone") }
                val emailIdx = header.indexOfFirst { it.contains("mail") }

                val hasId = idIdx >= 0
                val body = lines.drop(1)

                body.mapIndexedNotNull { idx, raw ->
                    val cols = csvSplit(raw)
                    if (nameIdx !in cols.indices) return@mapIndexedNotNull null
                    val name  = cols[nameIdx].ifBlank { return@mapIndexedNotNull null }
                    val phone = phoneIdx.takeIf { it in cols.indices }?.let { cols[it].ifBlank { null } }
                    val email = emailIdx.takeIf { it in cols.indices }?.let { cols[it].ifBlank { null } }
                    val id    = if (hasId && idIdx in cols.indices) {
                        cols[idIdx].toIntOrNull() ?: (idx + 1)
                    } else {
                        idx + 1
                    }
                    RosterPlayer(id, name, phone, email)
                }
            }
        }
    } catch (t: Throwable) {
        Log.e("RosterLoader", "Failed loading roster CSV", t)
        emptyList()
    }
}

/** Back-compat alias if old code still imports this name. */
@Deprecated("Use loadRosterFromAssets")
fun loadPlayersFromAssets(ctx: Context) = loadRosterFromAssets(ctx)
