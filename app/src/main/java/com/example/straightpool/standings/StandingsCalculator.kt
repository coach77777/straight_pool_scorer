package com.example.straightpool.standings

import com.example.straightpool.data.LeagueMatch
import com.example.straightpool.data.RosterPlayer

data class StandingRow(
    val roster: Int,
    val name: String,
    val wins: Int,
    val losses: Int,
    val played: Int,
    val pointsFor: Int,
    val pointsAgainst: Int
)

fun calculateStandings(
    players: List<RosterPlayer>,
    matches: List<LeagueMatch>
): List<StandingRow> {

    val nameByRoster = players.associateBy({ it.playerId }, { it.name })

    data class Acc(
        var wins: Int = 0,
        var losses: Int = 0,
        var played: Int = 0,
        var pf: Int = 0,
        var pa: Int = 0
    )

    val acc = mutableMapOf<Int, Acc>()

    fun get(roster: Int) = acc.getOrPut(roster) { Acc() }

    matches.asSequence()
        .filter { it.isPlayed && it.countsForStandings }
        .forEach { m ->
            val a = m.aRoster
            val b = m.bRoster
            val aScore = m.aScore ?: return@forEach
            val bScore = m.bScore ?: return@forEach

            val aAcc = get(a)
            val bAcc = get(b)

            aAcc.played += 1
            bAcc.played += 1

            aAcc.pf += aScore; aAcc.pa += bScore
            bAcc.pf += bScore; bAcc.pa += aScore

            if (aScore > bScore) {
                aAcc.wins += 1
                bAcc.losses += 1
            } else if (bScore > aScore) {
                bAcc.wins += 1
                aAcc.losses += 1
            } else {
                // ties: you told me “no ties going forward”
                // so for now: treat tie as no-op for W/L but still counts played + points
            }
        }

    return acc.entries
        .map { (roster, a) ->
            StandingRow(
                roster = roster,
                name = nameByRoster[roster] ?: "Player $roster",
                wins = a.wins,
                losses = a.losses,
                played = a.played,
                pointsFor = a.pf,
                pointsAgainst = a.pa
            )
        }
        .sortedWith(compareByDescending<StandingRow> { it.wins }.thenBy { it.losses }.thenBy { it.roster })
}


