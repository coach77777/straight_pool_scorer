package com.example.straightpool.data

import android.content.Context
import android.util.Log
import java.nio.charset.Charset

private fun parseBoolLoose(s: String): Boolean =
    when (s.trim().lowercase()) {
        "true", "t", "1", "yes", "y" -> true
        else -> false
    }

private fun parseIntLoose(s: String): Int? {
    val t = s.trim()
    if (t.isEmpty()) return null
    return t.toDoubleOrNull()?.toInt() ?: t.toIntOrNull()
}

fun loadLeagueMatchesFromAssets(
    ctx: Context,
    assetFile: String = "matches_3.csv"
): List<LeagueMatch> {
    return try {
        ctx.assets.open(assetFile).bufferedReader(Charset.forName("UTF-8")).useLines { lines ->
            lines.drop(1)
                .mapNotNull { raw ->
                    if (raw.isBlank()) return@mapNotNull null
                    val p = raw.split(',')  // matches_3.csv has no quoted commas
                    if (p.size < 9) return@mapNotNull null

                    LeagueMatch(
                        week = p[0].trim().toIntOrNull() ?: return@mapNotNull null,
                        dateMmDd = p[1].trim().ifBlank { null },
                        aRoster = p[2].trim().toIntOrNull() ?: return@mapNotNull null,
                        bRoster = p[3].trim().toIntOrNull() ?: return@mapNotNull null,
                        aScore = parseIntLoose(p[4]),
                        bScore = parseIntLoose(p[5]),
                        status = p[6].trim(),
                        note = p[7].trim().ifBlank { null },
                        countsForStandings = parseBoolLoose(p[8])
                    )
                }
                .toList()
        }
    } catch (t: Throwable) {
        Log.e("MatchesRepo", "Failed to load $assetFile", t)
        emptyList()
    }
}


