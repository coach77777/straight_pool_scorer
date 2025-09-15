package com.example.straightpool.data

import android.content.Context
import java.io.File

data class MatchRecord(
    val weekKey: String,
    val weekLabel: String?,
    val aId: Int?, val aName: String,
    val bId: Int?, val bName: String,
    val aScore: Int, val bScore: Int,
    val aInnings: Int, val bInnings: Int,
    val aHighRun: Int, val bHighRun: Int,
    val winnerId: Int?,   // null if tie/unknown
    val winnerName: String?
)

private fun file(ctx: Context) = File(ctx.filesDir, "match_history.csv")

fun addMatchRecord(ctx: Context, record: MatchRecord) {
    val f = file(ctx)
    val header = "week_key,week_label,a_id,a_name,b_id,b_name,a_score,b_score,a_innings,b_innings,a_high_run,b_high_run,winner_id,winner_name"
    if (!f.exists()) f.writeText(header + "\n")
    val line = listOf(
        record.weekKey,
        (record.weekLabel ?: ""),
        (record.aId?.toString() ?: ""),
        record.aName,
        (record.bId?.toString() ?: ""),
        record.bName,
        record.aScore.toString(),
        record.bScore.toString(),
        record.aInnings.toString(),
        record.bInnings.toString(),
        record.aHighRun.toString(),
        record.bHighRun.toString(),
        (record.winnerId?.toString() ?: ""),
        (record.winnerName ?: "")
    ).joinToString(",")
    f.appendText(line + "\n")
}

fun readAllMatchRecords(ctx: Context): List<MatchRecord> {
    val f = file(ctx)
    if (!f.exists()) return emptyList()
    return f.readLines().drop(1).mapNotNull { line ->
        val p = line.split(',')
        if (p.size < 14) return@mapNotNull null
        MatchRecord(
            weekKey = p[0],
            weekLabel = p[1].ifBlank { null },
            aId = p[2].toIntOrNull(),
            aName = p[3],
            bId = p[4].toIntOrNull(),
            bName = p[5],
            aScore = p[6].toIntOrNull() ?: 0,
            bScore = p[7].toIntOrNull() ?: 0,
            aInnings = p[8].toIntOrNull() ?: 0,
            bInnings = p[9].toIntOrNull() ?: 0,
            aHighRun = p[10].toIntOrNull() ?: 0,
            bHighRun = p[11].toIntOrNull() ?: 0,
            winnerId = p[12].toIntOrNull(),
            winnerName = p[13].ifBlank { null }
        )
    }
}


