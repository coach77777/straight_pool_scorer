package com.example.straightpool.data

import android.content.Context

data class WeekEntry(
    val key: String,   // e.g. "Wk-1"
    val label: String  // e.g. "3-Sep"
)

fun loadWeeksFromAssets(
    context: Context,
    assetFile: String = "weeks_extracted.csv"
): List<WeekEntry> {
    return context.assets.open(assetFile).bufferedReader().useLines { lines ->
        lines.drop(1) // header: week_key,week_label
            .mapNotNull { line ->
                val p = line.split(',')
                if (p.size < 2) return@mapNotNull null
                val k = p[0].trim()
                val v = p[1].trim()
                WeekEntry(k, v)
            }.toList()
    }
}

