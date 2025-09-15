package com.example.straightpool.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.straightpool.scorer.ScorerV2Screen
import com.example.straightpool.scorer.ScorerViewModel
import com.example.straightpool.ui.breakintro.BreakIntroScreen
import com.example.straightpool.ui.contacts.ContactsScreen
import com.example.straightpool.ui.help.HelpSpottingScreen
import com.example.straightpool.ui.setup.SetupScreen
import com.example.straightpool.ui.start.StartScreen
import com.example.straightpool.ui.stub.AdminScreen
import com.example.straightpool.ui.stub.PlayerStatsScreen

@Composable
fun AppNav(vm: ScorerViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "start") {

        // START (logo + 4 buttons)
        composable("start") {
            StartScreen(
                onStart    = { nav.navigate("setup") },
                onContacts = { nav.navigate("contacts") },
                onStats    = { nav.navigate("stats") },
                onAdmin    = { nav.navigate("admin") }
            )
        }

        // CONTACTS (no onBack param in this screen — system back works)
        composable("contacts") {
            ContactsScreen()
        }

        // SETUP (target + players + WEEK)
        composable("setup") {
            SetupScreen(
                vm = vm,
                onStart = { target, aId, aName, bId, bName, weekKey, weekLabel ->
                    vm.startMatch(
                        target = target,
                        aId = aId, aName = aName,
                        bId = bId, bName = bName,
                        weekKey = weekKey, weekLabel = weekLabel
                    )
                    nav.navigate("break") {
                        popUpTo("setup") { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        // OPENING BREAK intro
        composable("break") {
            BreakIntroScreen(
                vm = vm,
                onStart = {                       // <-- renamed from onContinueToScoring
                    nav.navigate("scorer") {
                        popUpTo("break") { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }   // keep this only if your screen defines it
            )
        }

        // MAIN SCORER
        composable("scorer") {
            ScorerV2Screen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onHelp = { nav.navigate("help") }
            )
        }

        // HELP
        composable("help") {
            HelpSpottingScreen(onBack = { nav.popBackStack() })
        }

        // STUB PAGES
        composable("stats") { PlayerStatsScreen(onBack = { nav.popBackStack() }) }
        composable("admin") { AdminScreen(onBack = { nav.popBackStack() }) }
    }
}
