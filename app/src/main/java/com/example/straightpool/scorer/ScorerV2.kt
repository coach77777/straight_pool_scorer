package com.example.straightpool.scorer

// Animations
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

// Foundation + Layout
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

// Material3
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

// Runtime / UI
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// Data
import com.example.straightpool.data.MatchHistoryRepoV2
import com.example.straightpool.data.MatchHistoryRow

// Coroutines
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────────────────────────────────────
// Domain
// ────────────────────────────────────────────────────────────────────────────────

data class Player(
    val id: Int? = null,
    val name: String = "Player",
    val score: Int = 0,
    val foulsInARow: Int = 0
)

data class InningEntry(
    val number: Int,
    val playerIndex: Int,   // 0 = A, 1 = B
    val balls: Int,
    val fouls: Int,
    val breakFouls: Int,
    val pointsDelta: Int    // balls - fouls - 2*breakFouls
)

enum class RackMode { Opening15, Continuous14Plus1 }
enum class GamePhase { Opening, AwaitChoiceAfterBreakFoul, Scoring }

data class GameState(
    val targetScore: Int = 125,
    val targetLocked: Boolean = false,

    val innings: Int = 1,
    val atTableIndex: Int = 0,
    val players: List<Player> = listOf(
        Player(name = "Player A"),
        Player(name = "Player B")
    ),

    // Opening/scoring phase
    val phase: GamePhase = GamePhase.Opening,
    val breakerIndex: Int = 0,

    // 14.1
    val rackNumber: Int = 1,
    val ballsDownInRack: Int = 0,
    val rackBallsRemaining: Int = 15,
    val rackMode: RackMode = RackMode.Opening15,

    // per-turn counters & log
    val inningCounter: Int = 0,
    val currentBalls: Int = 0,
    val currentFouls: Int = 0,
    val currentBreakFouls: Int = 0,
    val log: List<InningEntry> = emptyList(),

    // header + winner
    val weekKey: String? = null,
    val weekLabel: String? = null,
    val winnerIndex: Int? = null,
    val postWin: Boolean = false
)

enum class Action {
    POCKET_BALL,
    FOUL,
    FOUL_BALL_DROPPED,
    DELIBERATE_FOUL,
    SAFETY,
    END_TURN
}

// ────────────────────────────────────────────────────────────────────────────────
// ViewModel
// ────────────────────────────────────────────────────────────────────────────────

class ScorerViewModel : ViewModel() {

    data class MatchResult(
        val weekKey: String?,
        val weekLabel: String?,
        val aName: String, val aScore: Int,
        val bName: String, val bScore: Int,
        val winnerName: String,
        val highRun: Int
    )

    val history = mutableStateListOf<MatchResult>()

    private fun calcHighRun(g: GameState): Int =
        g.log.groupBy { it.playerIndex }
            .values
            .flatMap { entries -> entries.map { e -> e.balls } }
            .maxOrNull() ?: 0

    private fun calcHighRunForPlayer(g: GameState, playerIndex: Int): Int? =
        g.log.filter { it.playerIndex == playerIndex }
            .maxOfOrNull { it.balls }

    private fun checkWinner(g: GameState): GameState {
        if (g.winnerIndex != null) return g
        val i = g.atTableIndex
        val p = g.players[i]
        return if (p.score >= g.targetScore) g.copy(winnerIndex = i, postWin = true) else g
    }

    private fun lockTargetIfNeeded(g: GameState, hadActivity: Boolean = true): GameState =
        if (!g.targetLocked && hadActivity) g.copy(targetLocked = true) else g

    var game by mutableStateOf(GameState())
        private set

    private val undoStack = mutableStateListOf<GameState>()
    private fun pushUndo() { undoStack.add(game) }
    fun undo() { if (undoStack.isNotEmpty()) game = undoStack.removeAt(undoStack.lastIndex) }

    fun startMatch(
        target: Int,
        aId: Int?, aName: String,
        bId: Int?, bName: String,
        weekKey: String?, weekLabel: String?
    ) {
        game = GameState(
            targetScore = target,
            targetLocked = true,
            players = listOf(
                Player(id = aId, name = aName),
                Player(id = bId, name = bName)
            ),
            weekKey = weekKey,
            weekLabel = weekLabel,
            phase = GamePhase.Opening,
            breakerIndex = 0,
            rackNumber = 1,
            ballsDownInRack = 0,
            rackBallsRemaining = 15,
            rackMode = RackMode.Opening15,
            innings = 1,
            atTableIndex = 0,
            winnerIndex = null,
            postWin = false,
            currentBalls = 0,
            currentFouls = 0,
            currentBreakFouls = 0,
            log = emptyList()
        )
        undoStack.clear()
    }

    // ── Opening helpers (used by BreakIntroScreen) ──

    fun openingLegalBreak() {
        val opp = (game.breakerIndex + 1) % 2
        game = lockTargetIfNeeded(
            game.copy(
                phase = GamePhase.Scoring,
                atTableIndex = opp,
                currentBalls = 0,
                currentFouls = 0,
                currentBreakFouls = 0
            ),
            hadActivity = true
        )
    }

    // Legal break with a called ball made: breaker continues
    fun openingLegalBreakWithBall() {
        pushUndo()
        val brk = game.breakerIndex
        game = lockTargetIfNeeded(
            game.copy(
                phase = GamePhase.Scoring,
                atTableIndex = brk,
                currentBalls = 0,
                currentFouls = 0,
                currentBreakFouls = 0
            ),
            hadActivity = true
        )
    }

    fun openingBreakFoul() {
        val i = game.breakerIndex
        val p = game.players[i]
        val before = p.foulsInARow

        if (before >= 2) {
            // 3rd successive breaking foul: -15 only, reset streak, re-rack; still Opening
            val cleared = p.copy(score = p.score - 15, foulsInARow = 0)
            game = lockTargetIfNeeded(
                game.copy(
                    players = game.players.toMutableList().also { it[i] = cleared },
                    phase = GamePhase.Opening,
                    ballsDownInRack = 0,
                    rackBallsRemaining = 15,
                    currentBalls = 0,
                    currentFouls = 0,
                    currentBreakFouls = 0
                ),
                hadActivity = true
            )
        } else {
            // 1st/2nd breaking foul: -2; wait for opponent choice
            val after = p.copy(score = p.score - 2, foulsInARow = before + 1)
            game = lockTargetIfNeeded(
                game.copy(
                    players = game.players.toMutableList().also { it[i] = after },
                    phase = GamePhase.AwaitChoiceAfterBreakFoul,
                    currentBreakFouls = game.currentBreakFouls + 1
                ),
                hadActivity = true
            )
        }
    }

    fun openingOpponentAcceptsTable() {
        val opp = (game.breakerIndex + 1) % 2
        game = lockTargetIfNeeded(
            game.copy(
                phase = GamePhase.Scoring,
                atTableIndex = opp
            ),
            hadActivity = true
        )
    }

    fun openingForceRerack() {
        game = game.copy(
            phase = GamePhase.Opening,
            ballsDownInRack = 0,
            rackBallsRemaining = 15,
            currentBalls = 0,
            currentFouls = 0,
            currentBreakFouls = 0
        )
    }

    fun finalizeTurnIfNeeded() {
        val g = game
        val hadActivity = (g.currentBalls + g.currentFouls + g.currentBreakFouls) > 0
        if (!hadActivity) return
        game = endTurnNow(g, hadActivity = true)
    }


    fun finishMatch() {
        val g = game
        val a = g.players[0]
        val b = g.players[1]
        val winnerName = g.winnerIndex?.let { idx -> g.players[idx].name } ?: "-"

        history += MatchResult(
            weekKey = g.weekKey,
            weekLabel = g.weekLabel,
            aName = a.name, aScore = a.score,
            bName = b.name, bScore = b.score,
            winnerName = winnerName,
            highRun = calcHighRun(g)
        )
    }

    fun saveMatchToHistory(ctx: android.content.Context) {
        val g = game
        if (g.winnerIndex == null) return

        val repo = MatchHistoryRepoV2(ctx)

        fun weekToIntOrNull(key: String?): Int? {
            if (key.isNullOrBlank()) return null
            val digits = key.filter { it.isDigit() }
            return digits.toIntOrNull()
        }

        val a = g.players[0]
        val b = g.players[1]

        val aId = a.id ?: return
        val bId = b.id ?: return

        repo.append(
            MatchHistoryRow(
                timestampIso = MatchHistoryRepoV2.nowIso(),
                week = weekToIntOrNull(g.weekKey),
                rosterA = aId,
                rosterB = bId,
                scoreA = a.score,
                scoreB = b.score,
                highRunA = calcHighRunForPlayer(g, 0),
                highRunB = calcHighRunForPlayer(g, 1),
                innings = g.innings,
                countsForStandings = false,
                note = null
            )
        )
    }

    private fun endTurnNow(g: GameState, hadActivity: Boolean): GameState {
        val nextInning = if (g.atTableIndex == 1) g.innings + 1 else g.innings

        val newLog = if (hadActivity) {
            val delta = g.currentBalls - g.currentFouls - 2 * g.currentBreakFouls
            g.log + InningEntry(
                number = g.inningCounter + 1,
                playerIndex = g.atTableIndex,
                balls = g.currentBalls,
                fouls = g.currentFouls,
                breakFouls = g.currentBreakFouls,
                pointsDelta = delta
            )
        } else g.log

        return g.copy(
            atTableIndex = (g.atTableIndex + 1) % 2,
            innings = nextInning,
            inningCounter = if (hadActivity) g.inningCounter + 1 else g.inningCounter,
            currentBalls = 0,
            currentFouls = 0,
            currentBreakFouls = 0,
            log = newLog
        )
    }

    private fun applyFoul(basePenalty: Int): GameState {
        val g = game
        val i = g.atTableIndex
        val active = g.players[i]

        fun Player.withScore(delta: Int) = copy(score = score + delta)

        val beforeCount = active.foulsInARow

        return if (beforeCount >= 2) {
            val cleared = active.withScore(-15).copy(foulsInARow = 0)
            lockTargetIfNeeded(
                g.copy(
                    players = g.players.toMutableList().also { it[i] = cleared },
                    rackNumber = g.rackNumber + 1,
                    ballsDownInRack = 0,
                    rackBallsRemaining = 15,
                    rackMode = RackMode.Opening15,
                    currentBalls = 0, currentFouls = 0, currentBreakFouls = 0
                ),
                hadActivity = true
            )
        } else {
            val after = active.withScore(-basePenalty).copy(foulsInARow = beforeCount + 1)
            val ng = g.copy(
                players = g.players.toMutableList().also { it[i] = after },
                currentFouls = g.currentFouls + 1
            )
            lockTargetIfNeeded(endTurnNow(ng, hadActivity = true), hadActivity = true)
        }
    }

    fun apply(action: Action) {
        pushUndo()

        val g = game
        val i = g.atTableIndex
        val active = g.players[i]

        fun Player.clearFouls() = copy(foulsInARow = 0)

        game = when (action) {

            // Score freezes once winner exists, but we still count balls in currentBalls/log for high run.
            Action.POCKET_BALL -> {
                val cur = g.players[i]

                val matchOver = (g.winnerIndex != null) || g.postWin
                val newScore = if (matchOver) cur.score else (cur.score + 1)

                val updated = cur.copy(score = newScore, foulsInARow = 0)

                val newBallsDown = g.ballsDownInRack + 1
                val rackDone = newBallsDown >= 14

                val nextRackNum = if (rackDone) g.rackNumber + 1 else g.rackNumber
                val nextBallsDown2 = if (rackDone) 0 else newBallsDown
                val nextRemain = if (rackDone) 15 else (15 - newBallsDown)
                val nextMode = if (rackDone) RackMode.Continuous14Plus1 else g.rackMode

                val wonNow = (!matchOver && newScore >= g.targetScore)

                val next = g.copy(
                    players = g.players.toMutableList().also { it[i] = updated },
                    ballsDownInRack = nextBallsDown2,
                    rackBallsRemaining = nextRemain,
                    rackNumber = nextRackNum,
                    rackMode = nextMode,
                    currentBalls = g.currentBalls + 1,
                    winnerIndex = if (wonNow) i else g.winnerIndex,
                    postWin = if (wonNow) true else g.postWin
                )

                lockTargetIfNeeded(next, hadActivity = true)
            }

            Action.FOUL_BALL_DROPPED -> applyFoul(basePenalty = 1)
            Action.FOUL -> applyFoul(basePenalty = 1)
            Action.DELIBERATE_FOUL -> applyFoul(basePenalty = 16)

            Action.SAFETY -> {
                val cleared = active.clearFouls()
                val ng = g.copy(players = g.players.toMutableList().also { it[i] = cleared })
                lockTargetIfNeeded(endTurnNow(ng, hadActivity = true), hadActivity = true)
            }

            Action.END_TURN -> {
                val hadActivity = (g.currentBalls + g.currentFouls + g.currentBreakFouls) > 0
                lockTargetIfNeeded(endTurnNow(g, hadActivity), hadActivity)
            }
        }.let { checkWinner(it) }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// UI: Scorer
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun ScorerV2Screen(
    vm: ScorerViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onHelp: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null
) {
    val g = vm.game
    val ctx = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->

            Surface(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title + actions
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Straight Pool Scoring TEST",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            onHistory?.let {
                                OutlinedButton(onClick = it) { Text("onHistory is ${if (onHistory == null) "NULL" else "SET"}") }
                                Spacer(Modifier.width(8.dp))
                            }
                            onHelp?.let {
                                OutlinedButton(onClick = it) { Text("Help") }
                                Spacer(Modifier.width(8.dp))
                            }

                            onBack?.let { OutlinedButton(onClick = it) { Text("Back") } }
                        }
                    }

                    // Scoreboard
                    ScoreBoard(
                        players = g.players,
                        atTable = g.atTableIndex,
                        innings = g.innings,
                        rackRemaining = g.rackBallsRemaining,
                        ballsDownInRack = g.ballsDownInRack,
                        rackNumber = g.rackNumber,
                        rackMode = g.rackMode,
                        onSetAtTable = { /* optional */ },
                        weekKey = g.weekKey,
                        weekLabel = g.weekLabel,
                        winnerIndex = g.winnerIndex
                    )

                    // Game Over row (only after someone hits target)
                    if (g.winnerIndex != null) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Continue scoring for high run",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Continue") }


                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Discarded (not saved)",
                                            duration = SnackbarDuration.Short
                                        )
                                        onBack?.invoke()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Discard") }

                            Button(
                                onClick = {
                                    vm.finalizeTurnIfNeeded()
                                    vm.finishMatch()
                                    vm.saveMatchToHistory(ctx)

                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Congrats! Match recorded.",
                                            duration = SnackbarDuration.Short
                                        )
                                        onBack?.invoke()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Finish Match") }
                        }
                    }

                    // Controls remain enabled (post-win high-run allowed)
                    TurnControls(
                        onPocket = { vm.apply(Action.POCKET_BALL) },
                        onFoul = { vm.apply(Action.FOUL) },
                        onDeliberate = { vm.apply(Action.DELIBERATE_FOUL) },
                        onSafety = { vm.apply(Action.SAFETY) },
                        onEndTurn = { vm.apply(Action.END_TURN) }
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// UI: Match History Viewer
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun MatchHistoryScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { MatchHistoryRepoV2(ctx) }

    var text by remember { mutableStateOf(repo.exportText()) }

    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->

            Surface(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Match History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                text = repo.exportText()
                                scope.launch { snackbarHostState.showSnackbar("Refreshed", duration = SnackbarDuration.Short) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Refresh") }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(text))
                                scope.launch { snackbarHostState.showSnackbar("Copied", duration = SnackbarDuration.Short) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Copy") }

                        Button(
                            onClick = {
                                repo.clearAll()
                                text = repo.exportText()
                                scope.launch { snackbarHostState.showSnackbar("Cleared", duration = SnackbarDuration.Short) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear") }
                    }

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(text)
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Pieces
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreBoard(
    players: List<Player>,
    atTable: Int,
    innings: Int,
    rackRemaining: Int,
    ballsDownInRack: Int,
    rackNumber: Int,
    rackMode: RackMode,
    onSetAtTable: (Int) -> Unit,
    weekKey: String? = null,
    weekLabel: String? = null,
    winnerIndex: Int? = null
) {
    val rackLabel =
        if (rackNumber >= 2 && rackMode == RackMode.Continuous14Plus1 && ballsDownInRack == 0)
            "14 racked + 1 break ball"
        else
            "Rack balls remaining: ${15 - ballsDownInRack}"

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        if (winnerIndex != null && winnerIndex in players.indices) {
            Text(
                text = "Winner: ${players[winnerIndex].name} — post-win high-run mode",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
        }

        if (!weekLabel.isNullOrBlank() || !weekKey.isNullOrBlank()) {
            Text(
                text = buildString {
                    append("Week: ")
                    if (!weekLabel.isNullOrBlank()) append(weekLabel)
                    if (!weekKey.isNullOrBlank()) {
                        if (!weekLabel.isNullOrBlank()) append("  •  ")
                        append(weekKey)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Innings: $innings   •   Rack: $rackNumber   •   $rackLabel",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            players.forEachIndexed { index, p ->
                val isAt = index == atTable
                val twoFouls = isAt && p.foulsInARow >= 2

                val pulseTrans = rememberInfiniteTransition(label = "twoFoulsPulse")
                val pulseScale by pulseTrans.animateFloat(
                    initialValue = 1f,
                    targetValue = if (twoFouls) 1.04f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                val borderColor by animateColorAsState(
                    targetValue = when {
                        twoFouls -> Color(0xFFFFA000)
                        isAt -> Color(0xFF2E7D32)
                        else -> Color.Transparent
                    },
                    label = "borderColor"
                )

                val scoreColor by animateColorAsState(
                    targetValue = when {
                        twoFouls -> Color(0xFFFFA000)
                        isAt -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    label = "scoreColor"
                )

                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .scale(pulseScale)
                        .border(3.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onSetAtTable(index) }
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (index == 0) "${p.name} (A)" else "${p.name} (B)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isAt) FontWeight.ExtraBold else FontWeight.Bold
                        )
                        Text(
                            text = "Score: ${p.score}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = scoreColor,
                            textAlign = TextAlign.Start
                        )
                        Text("Fouls in a row: ${p.foulsInARow}")
                        if (twoFouls) {
                            Text(
                                "Warning: 2 fouls — avoid the 3rd!",
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isAt) {
                            Text("At table", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TurnControls(
    onPocket: () -> Unit,
    onFoul: () -> Unit,
    onDeliberate: () -> Unit,
    onSafety: () -> Unit,
    onEndTurn: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Actions this turn")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPocket, modifier = Modifier.weight(1f)) { Text("Pocket Ball +1") }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSafety()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Safety") }

            OutlinedButton(onClick = onEndTurn, modifier = Modifier.weight(1f)) { Text("End Turn") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFoul()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Foul −1") }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeliberate()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Deliberate Foul −16") }
        }
    }
}
