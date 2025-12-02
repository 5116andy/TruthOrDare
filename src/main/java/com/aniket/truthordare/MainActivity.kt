package com.aniket.truthordare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope as KCoroutineScope

// ---------- models ----------
data class Prompt(val text: String, val durationSec: Int, val isTruth: Boolean)

sealed class Screen {
    object Start : Screen()
    object Players : Screen()
    data class Game(val players: List<String>) : Screen()
}

// ---------- Activity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TruthOrDareApp() }
    }
}

// ---------- App shell ----------
@Composable
fun TruthOrDareApp() {
    MaterialTheme {
        Scaffold(topBar = { TopBarSimple() }, containerColor = Color(0xFFFAF3FF)) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFDF7FF), Color(0xFFF2EAFB))))
            ) {
                BackgroundFloatingBlobs()
                AppRoot()
            }
        }
    }
}

@Composable
fun TopBarSimple() {
    Surface(color = Color(0xFFF8F2FF), tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.CenterStart) {
            Text("TruthOrDare", modifier = Modifier.padding(start = 16.dp), fontWeight = FontWeight.SemiBold, color = Color(0xFF3C2A6D))
        }
    }
}

@Composable
fun BackgroundFloatingBlobs() {
    val anim1 = remember { Animatable(-80f) }
    val anim2 = remember { Animatable(80f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            anim1.animateTo(120f, animationSpec = tween(7000))
            anim1.animateTo(-120f, animationSpec = tween(7000))
        }
    }
    LaunchedEffect(Unit) {
        delay(900)
        while (isActive) {
            anim2.animateTo(-140f, animationSpec = tween(9000))
            anim2.animateTo(140f, animationSpec = tween(9000))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawCircle(color = Color(0x22A68AFF), center = Offset(w * 0.2f + anim1.value / 3f, h * 0.25f + anim1.value / 5f), radius = min(w, h) * 0.28f, alpha = 0.08f)
        drawCircle(color = Color(0x22986BFF), center = Offset(w * 0.8f + anim2.value / 4f, h * 0.7f + anim2.value / 6f), radius = min(w, h) * 0.2f, alpha = 0.06f)
    }
}

// ---------- Navigation ----------
@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Start) }
    when (val s = screen) {
        is Screen.Start -> StartScreen(onStart = { screen = Screen.Players })
        is Screen.Players -> PlayersScreen(onBack = { screen = Screen.Start }, onPlay = { players -> if (players.size >= 2) screen = Screen.Game(players) })
        is Screen.Game -> GameScreen(players = s.players, onExit = { screen = Screen.Start })
    }
}

// ---------- Start & Players ----------
@Composable
fun StartScreen(onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 10.dp, modifier = Modifier.padding(24.dp)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Truth or Dare", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3C2A6D))
                Spacer(Modifier.height(6.dp))
                Text("Bottle Game — Friends • Fun • Secrets", color = Color(0xFF7A66A3))
                Spacer(Modifier.height(18.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AA3))) {
                    Text("Start Game", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("Tip: Add 2–8 players for the best experience", color = Color(0xFF6A5AA3))
            }
        }
    }
}

@Composable
fun PlayersScreen(onBack: () -> Unit, onPlay: (List<String>) -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    val players = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack, shape = CircleShape, modifier = Modifier.size(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AA3))) { Text("←", color = Color.White) }
            Spacer(Modifier.width(12.dp))
            Text("Add Players", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("Players: ${players.size}", color = Color(0xFF6A5AA3))
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, placeholder = { Text("Enter player name") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { val t = nameInput.trim(); if (t.isNotEmpty() && players.size < 12) { players.add(t); nameInput = "" } }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AA3))) { Text("Add", color = Color.White) }
        }

        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)) {
            if (players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No players yet — add friends to play", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(players) { idx, p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF2E9FF), modifier = Modifier.weight(1f), tonalElevation = 2.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text("${idx + 1}.", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(p, fontSize = 16.sp)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { players.removeAt(idx) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8D6CEB), contentColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.height(44.dp)) { Text("Remove") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPlay(players.toList()) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = players.size >= 2, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AA3))) {
            Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(8.dp))
        Text("Tip: 3–8 players looks best on a phone", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

// ---------- Game screen ----------
@Composable
fun GameScreen(players: List<String>, onExit: () -> Unit) {
    if (players.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No players found. Go back and add players.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onExit) { Text("Back") }
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    var rotation by remember { mutableFloatStateOf(0f) }
    var spinning by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    var showWinnerDialog by remember { mutableStateOf(false) }
    var showChoiceDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var currentPrompt by remember { mutableStateOf<Prompt?>(null) }

    var countdown by remember { mutableIntStateOf(60) }
    var countdownRunning by remember { mutableStateOf(false) }

    val truthPrompts = remember {
        listOf(
            Prompt("What's your biggest secret?", 60, true),
            Prompt("Who's your crush?", 45, true),
            Prompt("Have you ever lied to your best friend?", 50, true),
            Prompt("What's your most embarrassing moment?", 60, true)
        )
    }
    val darePrompts = remember {
        listOf(
            Prompt("Do 10 push-ups", 15, false),
            Prompt("Sing the chorus of your favourite song", 30, false),
            Prompt("Dance for 20 seconds", 20, false),
            Prompt("Imitate your favourite celebrity for 30s", 30, false)
        )
    }

    val n = players.size
    val sector = 360f / n
    val startAngle = -90f + sector / 2f
    val playerAngles = remember(n) { (0 until n).map { i -> startAngle + i * sector } }

    // responsive badge sizing
    val badgeWidthDp = when {
        n <= 3 -> 140.dp
        n == 4 -> 130.dp
        n <= 6 -> 110.dp
        else -> 95.dp
    }
    val badgeHeightDp = 44.dp
    val fontSizeSp = when {
        n <= 3 -> 15.sp
        n <= 4 -> 14.sp
        n <= 6 -> 12.sp
        else -> 10.sp
    }

    // Reserve bottom prompt height to prevent overlap (fixed safe height)
    val bottomPromptHeight = 220.dp

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onExit, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AA3))) { Text("Exit", color = Color.White) }
            Text("Players: ${players.size}", color = Color(0xFF6A5AA3))
            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Top area (bottle + badges) measured separately from bottom prompt
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(12.dp)
            .onGloballyPositioned { boxSize = it.size },
            contentAlignment = Alignment.Center
        ) {
            // radial bg
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height / 2f
                val r = min(size.width, size.height) * 0.36f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFBF5FF), Color(0xFFEDE1FF))), radius = r + 36f, center = Offset(cx, cy))
            }

            // pointer arrow at top
            Canvas(modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(28.dp)) {
                val w = size.width; val h = size.height
                val path = Path().apply { moveTo(w*0.5f, 0f); lineTo(w, h); lineTo(0f, h); close() }
                drawPath(path = path, color = Color(0xFF4B3E93))
            }

            // compute center and radius from measured box (box excludes bottom prompt because bottom has fixed height)
            val boxWidthF = boxSize.width.toFloat()
            val boxHeightF = boxSize.height.toFloat()
            val centerX = boxWidthF / 2f
            val centerY = boxHeightF / 2f

            val badgeWPx = with(LocalDensity.current) { badgeWidthDp.toPx() }
            val badgeHPx = with(LocalDensity.current) { badgeHeightDp.toPx() }

            // radius calculation - ensure badges stay inside measured box area
            val minDim = min(boxWidthF, boxHeightF)
            val margin = 12f + max(badgeWPx, badgeHPx) * 0.1f
            val radiusPx = (minDim / 2f - max(badgeWPx, badgeHPx) / 2f - margin).coerceAtLeast(60f)

            // Draw badges evenly on the circle
            if (boxSize.width > 0 && boxSize.height > 0) {
                for ((i, name) in players.withIndex()) {
                    val angleDeg = playerAngles[i]
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val badgeCx = centerX + radiusPx * cos(rad).toFloat()
                    val badgeCy = centerY + radiusPx * sin(rad).toFloat()

                    // clamp so entire badge stays inside box
                    val left = (badgeCx - badgeWPx / 2f).coerceIn(0f, (boxWidthF - badgeWPx).coerceAtLeast(0f))
                    val top = (badgeCy - badgeHPx / 2f).coerceIn(0f, (boxHeightF - badgeHPx).coerceAtLeast(0f))

                    val isSelected = selectedIndex == i
                    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 1f, animationSpec = tween(280))

                    Box(modifier = Modifier
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                        .scale(scale)
                        .width(badgeWidthDp)
                        .height(badgeHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = if (isSelected) 10.dp else 3.dp, color = if (isSelected) Color(0xFF6A5AA3) else Color(0xFFFDF7FF), modifier = Modifier.fillMaxSize().border(2.dp, if (isSelected) Color(0xFF3C2A6D) else Color(0xFFE6DFF6), RoundedCornerShape(22.dp))) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text(text = name.uppercase(), color = if (isSelected) Color.White else Color(0xFF231F3A), fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Bottle centered - use graphicsLayer rotationZ to ensure center pivot
            val bottleSizeDp = 150.dp
            Box(modifier = Modifier
                .size(bottleSizeDp)
                .align(Alignment.Center)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bottle),
                    contentDescription = "Bottle",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation }
                        .pointerInput(spinning) {
                            detectTapGestures {
                                if (!spinning) {
                                    startSpinCoroutine(coroutineScope = coroutineScope, initialRotation = rotation, onStart = {
                                        spinning = true
                                        selectedIndex = null
                                        showChoiceDialog = false
                                        showPromptDialog = false
                                        currentPrompt = null
                                        countdownRunning = false
                                        countdown = 60
                                    }, setRotation = { rotation = it }, onFinish = { finalRotation ->
                                        val normalized = ((-finalRotation) % 360f + 360f) % 360f
                                        val rawIndex = ((normalized + sector / 2f) / sector).toInt()
                                        selectedIndex = ((rawIndex % n) + n) % n
                                        spinning = false
                                        showWinnerDialog = true
                                    })
                                }
                            }
                        }
                )
            }
        }

        // bottom prompt area - fixed height (keeps top box measurement stable)
        Surface(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), tonalElevation = 12.dp, modifier = Modifier.fillMaxWidth().height(bottomPromptHeight)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedIndex != null) {
                    Text("${players[selectedIndex!!].uppercase()} — Spin result", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose Truth or Dare", color = Color.Gray)
                } else {
                    Text("Tap the bottle to spin", color = Color.Gray)
                }

                Spacer(Modifier.height(8.dp))

                if (currentPrompt != null) {
                    Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Prompt", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(currentPrompt!!.text)
                            Spacer(Modifier.height(6.dp))
                            if (countdownRunning) Text("Time left: $countdown s", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) else Text("Press Start to begin", color = Color.Gray)
                        }
                    }
                }
            }
        }

        // winner / selection dialogs
        if (showWinnerDialog && selectedIndex != null) {
            AlertDialog(onDismissRequest = {}, title = { Text("Winner!") }, text = { Text("${players[selectedIndex!!].uppercase()} won the spin!") }, confirmButton = {
                Button(onClick = { showWinnerDialog = false; showChoiceDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Continue") }
            })
        }

        if (showChoiceDialog && selectedIndex != null) {
            AlertDialog(onDismissRequest = { showChoiceDialog = false }, title = { Text("Select") }, text = { Text("Choose Truth or Dare for ${players[selectedIndex!!]}") }, confirmButton = {
                Button(onClick = { currentPrompt = truthPrompts.random(); showChoiceDialog = false; showPromptDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Truth") }
            }, dismissButton = {
                Button(onClick = { currentPrompt = darePrompts.random(); showChoiceDialog = false; showPromptDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Dare") }
            })
        }

        if (showPromptDialog && currentPrompt != null) {
            AlertDialog(onDismissRequest = { showPromptDialog = false }, title = { Text(if (currentPrompt!!.isTruth) "Truth" else "Dare") }, text = {
                Column {
                    Text(currentPrompt!!.text)
                    Spacer(Modifier.height(8.dp))
                    Text("Duration: ${currentPrompt!!.durationSec} sec", color = Color.Gray)
                }
            }, confirmButton = {
                Button(onClick = { countdown = currentPrompt!!.durationSec; countdownRunning = true; showPromptDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Start") }
            }, dismissButton = { TextButton(onClick = { showPromptDialog = false }) { Text("Cancel") } })
        }

        LaunchedEffect(countdownRunning) {
            if (countdownRunning) {
                while (countdown > 0 && countdownRunning) {
                    delay(1000)
                    countdown -= 1
                }
                countdownRunning = false
            }
        }
    }
}

// ---------- Spin coroutine helper ----------
private fun startSpinCoroutine(
    coroutineScope: KCoroutineScope,
    initialRotation: Float = 0f,
    onStart: () -> Unit,
    setRotation: (Float) -> Unit,
    onFinish: (Float) -> Unit
) {
    coroutineScope.launch {
        try {
            onStart()
            val start = initialRotation
            val extraSpins = Random.nextInt(5, 11) // 5..10
            val targetAngle = Random.nextFloat() * 360f
            val target = extraSpins * 360f + targetAngle
            val anim = Animatable(start)
            anim.animateTo(
                target,
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            ) {
                setRotation(value % 360f)
            }
            setRotation(anim.value % 360f)
            onFinish(anim.value % 360f)
        } catch (_: Exception) {
            onFinish(0f)
        }
    }
}