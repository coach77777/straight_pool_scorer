package com.example.straightpool.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.straightpool.scorer.MatchHistoryScreen
import com.example.straightpool.scorer.ScorerV2Screen
import com.example.straightpool.scorer.ScorerViewModel
import com.example.straightpool.ui.admin.AdminEditPlayerScreen
import com.example.straightpool.ui.admin.AdminExportPlayersScreen
import com.example.straightpool.ui.admin.AdminGateScreen
import com.example.straightpool.ui.admin.AdminImportPlayersScreen
import com.example.straightpool.ui.admin.AdminMenuScreen
import com.example.straightpool.ui.admin.AdminPlayersScreen
import com.example.straightpool.ui.admin.AdminSettingsScreen
import com.example.straightpool.ui.breakintro.BreakIntroScreen
import com.example.straightpool.ui.contacts.ContactsScreen
import com.example.straightpool.ui.help.HelpSpottingScreen
import com.example.straightpool.ui.setup.SetupScreen
import com.example.straightpool.ui.start.StartScreen
import com.example.straightpool.ui.stats.StandingsScreenV2

@Composable
fun AppNav(vm: ScorerViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "start") {

        // START
        composable("start") {
            StartScreen(
                onStart = { nav.navigate("setup") },
                onContacts = { nav.navigate("contacts") },
                onStats = { nav.navigate("stats") },
                onAdmin = { nav.navigate("admin") }
            )
        }

        // CONTACTS
        composable("contacts") {
            ContactsScreen()
        }

        // SETUP
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
                    nav.navigate("break") { popUpTo("setup") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }

        // OPENING BREAK
        composable("break") {
            BreakIntroScreen(
                vm = vm,
                onStart = {
                    nav.navigate("scorer") { popUpTo("break") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }

        // SCORER
        composable("scorer") {
            ScorerV2Screen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onHelp = { nav.navigate("help") },
                onHistory = { nav.navigate("match_history") }
            )
        }

        // MATCH HISTORY
        composable("match_history") {
            MatchHistoryScreen(onBack = { nav.popBackStack() })
        }

        // HELP
        composable("help") {
            HelpSpottingScreen(onBack = { nav.popBackStack() })
        }

        // STANDINGS
        composable("stats") {
            StandingsScreenV2(onBack = { nav.popBackStack() })
        }

        // ADMIN GATE (passcode)
        composable("admin") {
            AdminGateScreen(
                appName = "StraightPool",
                defaultPasscode = "7777",
                onSuccess = { nav.navigate("admin_menu") },
                onBack = { nav.popBackStack() }
            )
        }

        // ADMIN MENU
        composable("admin_menu") {
            AdminMenuScreen(
                onBack = { nav.popBackStack() },
                onAdminSettings = { nav.navigate("admin_settings") },
                onPlayers = { nav.navigate("admin_players") },
                onSchedule = { nav.navigate("admin_schedule") },
                onFixMatchResults = { nav.navigate("admin_fix_results") },
                onPlayerStats = { nav.navigate("admin_player_stats") },
                onImportMatches = { nav.navigate("admin_import_matches") }
            )
        }

        // ADMIN SETTINGS
        composable("admin_settings") {
            AdminSettingsScreen(onBack = { nav.popBackStack() })
        }

        // ADMIN PLAYERS (list)
        composable("admin_players") {
            AdminPlayersScreen(
                onBack = { nav.popBackStack() },
                onEditPlayer = { roster -> nav.navigate("admin_player_edit/$roster") },
                onAddPlayer = {
                    // placeholder until we add a real Add screen
                    nav.navigate("admin_player_edit/999")
                },
                onImport = { nav.navigate("admin_players_import") },
                onExport = { nav.navigate("admin_players_export") }
            )
        }

        // ADMIN PLAYER EDIT
        composable("admin_player_edit/{roster}") { backStack ->
            val roster = backStack.arguments?.getString("roster")?.toIntOrNull() ?: 0
            AdminEditPlayerScreen(
                roster = roster,
                onBack = { nav.popBackStack() }
            )
        }

        // ADMIN PLAYERS IMPORT
        composable("admin_players_import") {
            AdminImportPlayersScreen(onBack = { nav.popBackStack() })
        }

        // ADMIN PLAYERS EXPORT
        composable("admin_players_export") {
            AdminExportPlayersScreen(onBack = { nav.popBackStack() })
        }

        // STUB ROUTES (add screens later, but routes exist so nav doesn't crash)
        composable("admin_schedule") {
            AdminStubScreen(title = "Schedule", onBack = { nav.popBackStack() })
        }
        composable("admin_fix_results") {
            AdminStubScreen(title = "Fix Match Results", onBack = { nav.popBackStack() })
        }
        composable("admin_player_stats") {
            AdminStubScreen(title = "Player Stats", onBack = { nav.popBackStack() })
        }
        composable("admin_import_matches") {
            AdminStubScreen(title = "Import Matches", onBack = { nav.popBackStack() })
        }
    }
}
