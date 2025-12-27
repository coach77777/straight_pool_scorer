package com.example.straightpool.data

data class LeagueMatch(
    val week: Int,
    val dateMmDd: String?,
    val aRoster: Int,
    val bRoster: Int,
    val aScore: Int?,              // null if not played
    val bScore: Int?,              // null if not played
    val status: String,            // "played" / "scheduled"
    val note: String?,
    val countsForStandings: Boolean
) {
    val isPlayed: Boolean get() =
        status.equals("played", ignoreCase = true) && aScore != null && bScore != null
}
