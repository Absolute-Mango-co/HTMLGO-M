package com.absolutemango.htmlgom

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassOptics
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.hazeGlass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// EASTER EGG CONSTANTS & DIALOGUES
// ---------------------------------------------------------------------------

const val PREF_EASTER_EGG_EVER_UNLOCKED = "easter_egg_ever_unlocked"
const val PREF_EASTER_EGG_LAST_COMPLETED_AT = "easter_egg_last_completed_at"

fun resetEasterEggState(prefs: SharedPreferences) {
    prefs.edit()
        .remove(PREF_EASTER_EGG_EVER_UNLOCKED)
        .remove(PREF_EASTER_EGG_LAST_COMPLETED_AT)
        .apply()
}

val EASTER_EGG_DIALOGUES = listOf(
    "Who's there? 😃",
    "Who goes there? 😯",
    "Stop knocking. 🫣",
    "Ouch, that hurt. 😅",
    "Are you lost? 😄",
    "Add a file to get started. 😄",
    "It's me, the wanderer of HTML. 😁",
    "Are you lost? Do you need help? 😀",
    "Get some help. 🤨",
    "Ow, now that hurts. 😔",
    "No, that seriously hurts. 😤",
    "Ouch. 😳",
    "Ouchie! 🫤",
    "Okay. My code hurts. 🫨",
    "When are you going to stop? 🥹",
    "Ouch! I'm going to call my mother! 😠",
    "Mama! He is beating me up! 😭",
    "Please stop. I'm going to call my developer! 😡",
    "Ouch. It hurts. 😖",
    "Okay, you are seriously mind boggled. 🧐",
    "Ouch. Stop doing that. 😩",
    "Haha, that tickles. 🤣",
    "Please use the app. 🥲",
    "Ouch. 😿",
    "Fine, you win. Here is your achievement. 😤"
)

// ---------------------------------------------------------------------------
// HEADLESS EASTER EGG CONTROLLER
// ---------------------------------------------------------------------------

class EasterEggController(
    val prefs: SharedPreferences,
    val coroutineScope: CoroutineScope
) {
    val tapCount: MutableIntState = mutableIntStateOf(0)
    val dialogueIndex: MutableIntState = mutableIntStateOf(0)
    val lastTapTimestamp: MutableLongState = mutableLongStateOf(0L)
    val isToastVisible: MutableState<Boolean> = mutableStateOf(false)
    val isClimaxPlaying: MutableState<Boolean> = mutableStateOf(false)
    val isEverUnlocked: MutableState<Boolean> = mutableStateOf(
        prefs.getBoolean(PREF_EASTER_EGG_EVER_UNLOCKED, false)
    )

    private var dismissJob: Job? = null

    val currentDialogue: String
        get() {
            val idx = dialogueIndex.intValue
            return if (idx in EASTER_EGG_DIALOGUES.indices) EASTER_EGG_DIALOGUES[idx] else ""
        }

    fun reset() {
        tapCount.intValue = 0
        dialogueIndex.intValue = 0
        lastTapTimestamp.longValue = 0L
        isToastVisible.value = false
        isClimaxPlaying.value = false
        isEverUnlocked.value = false
        dismissJob?.cancel()
        resetEasterEggState(prefs)
    }

    fun onTitleTap() {
        if (isClimaxPlaying.value) return

        val isUnlockedInPrefs = prefs.getBoolean(PREF_EASTER_EGG_EVER_UNLOCKED, false)
        if (!isUnlockedInPrefs && isEverUnlocked.value) {
            isEverUnlocked.value = false
            tapCount.intValue = 0
            dialogueIndex.intValue = 0
            isToastVisible.value = false
            dismissJob?.cancel()
        }

        val now = System.currentTimeMillis()
        val lastCompleted = prefs.getLong(PREF_EASTER_EGG_LAST_COMPLETED_AT, 0L)
        val isCooldownActive = (lastCompleted > 0L) && (now - lastCompleted < 24 * 60 * 60 * 1000L) && (now >= lastCompleted)

        if (isCooldownActive) {
            // In 24h cooldown: keep title tap resting
            return
        }

        // Passive 60-second inactivity reset
        if (lastTapTimestamp.longValue > 0L && (now - lastTapTimestamp.longValue > 60_000L)) {
            tapCount.intValue = 1
            dialogueIndex.intValue = 0
            lastTapTimestamp.longValue = now
            isToastVisible.value = false
            dismissJob?.cancel()
            return
        }
        lastTapTimestamp.longValue = now

        tapCount.intValue++

        // Taps 1 through 5: Silent Arming
        if (tapCount.intValue <= 5) {
            return
        }

        // Taps 6 through 30: Dialogue sequence 1..25
        val step = tapCount.intValue - 6
        if (step in EASTER_EGG_DIALOGUES.indices) {
            dialogueIndex.intValue = step
            isToastVisible.value = true

            dismissJob?.cancel()
            dismissJob = coroutineScope.launch {
                delay(2500L)
                isToastVisible.value = false
            }

            // Step 25 Reached (index 24)
            if (step == EASTER_EGG_DIALOGUES.size - 1) {
                isClimaxPlaying.value = true
                isEverUnlocked.value = true
                prefs.edit()
                    .putBoolean(PREF_EASTER_EGG_EVER_UNLOCKED, true)
                    .putLong(PREF_EASTER_EGG_LAST_COMPLETED_AT, now)
                    .apply()

                coroutineScope.launch {
                    delay(4000L)
                    isClimaxPlaying.value = false
                    isToastVisible.value = false
                    tapCount.intValue = 0
                    dialogueIndex.intValue = 0
                }
            }
        }
    }
}

@Composable
fun rememberEasterEggController(prefs: SharedPreferences): EasterEggController {
    val scope = rememberCoroutineScope()
    return remember(prefs) {
        EasterEggController(prefs, scope)
    }
}

// ---------------------------------------------------------------------------
// IN-APP LIQUID GLASS TOAST OVERLAY (COMPOSE LAYER 5)
// ---------------------------------------------------------------------------

@Composable
fun LiquidGlassToastHost(
    controller: EasterEggController,
    hazeState: HazeState,
    displayState: DeviceDisplayLayoutState,
    modifier: Modifier = Modifier
) {
    val bottomPadding = when (displayState) {
        DeviceDisplayLayoutState.LARGE_DISPLAY -> 32.dp
        DeviceDisplayLayoutState.PHONE_LANDSCAPE -> 24.dp
        DeviceDisplayLayoutState.PHONE_PORTRAIT -> 120.dp
    }

    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val isLightMode = canvasColor.luminance() > 0.5f
    val toastMonetTint = MaterialTheme.colorScheme.primaryContainer

    val toastGlassStyle = remember(toastMonetTint) {
        GlassStyle.regular.then {
            shape(RoundedCornerShape(18.dp))
            tint(toastMonetTint.copy(alpha = if (isLightMode) 0.85f else 0.50f))
            edgeShadow(Color.Transparent)
            optics {
                this.copy(
                    refractionStrength = (this.refractionStrength * 1.10f).coerceIn(0f, 1f),
                    blurRadius = OpticalSizeValue.Fixed(1.dp)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = controller.isToastVisible.value && !controller.isClimaxPlaying.value,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.90f, animationSpec = tween(180)) + slideInVertically(tween(180)) { it / 3 },
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.90f, animationSpec = tween(200))
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 4.dp,
                border = if (isLightMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .hazeGlass(
                        input = HazeInput.Sources(hazeState),
                        style = toastGlassStyle
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                AnimatedContent(
                    targetState = controller.currentDialogue,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.96f))
                            .togetherWith(fadeOut(tween(100)))
                    },
                    label = "ToastDialogueMorph"
                ) { dialogueText ->
                    Text(
                        text = dialogueText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MINECRAFT ACHIEVEMENT BANNER COMPOSABLE (3-PHASE ANIMATION)
// ---------------------------------------------------------------------------

@Composable
fun MinecraftAchievementBanner(
    controller: EasterEggController,
    modifier: Modifier = Modifier
) {
    if (!controller.isClimaxPlaying.value) return

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }

    val offsetX = remember { Animatable(screenWidthPx) }

    LaunchedEffect(controller.isClimaxPlaying.value) {
        if (controller.isClimaxPlaying.value) {
            // Phase 1: Slide In from right to center (0.4s)
            offsetX.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            // Phase 2: Dwell stationary at center for readability (2.8s)
            delay(2800L)
            // Phase 3: Slide Out to left (0.8s)
            offsetX.animateTo(-screenWidthPx, animationSpec = tween(800, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp)
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
            border = BorderStroke(2.dp, Color(0xFF3C3C3C)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_htmlgom_title_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp, 24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Challenge Complete!",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFF55),
                            shadow = Shadow(
                                color = Color(0xFF3F3F15),
                                offset = Offset(2f, 2f),
                                blurRadius = 0f
                            )
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "How Did We Get Here?",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            shadow = Shadow(
                                color = Color(0xFF3F3F3F),
                                offset = Offset(2f, 2f),
                                blurRadius = 0f
                            )
                        )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LIFECYCLE-BOUND AUDIO PLAYER
// ---------------------------------------------------------------------------

@Composable
fun EasterEggAudioPlayer(
    isClimaxPlaying: Boolean
) {
    val context = LocalContext.current

    DisposableEffect(isClimaxPlaying) {
        val player = if (isClimaxPlaying) {
            try {
                MediaPlayer.create(context, R.raw.victory_royale)?.apply {
                    setOnCompletionListener { release() }
                    setOnErrorListener { _, what, extra ->
                        Log.w("EasterEggAudio", "MediaPlayer error: what=$what extra=$extra")
                        release()
                        true
                    }
                    start()
                }
            } catch (e: Exception) {
                Log.w("EasterEggAudio", "Failed to initialize MediaPlayer: ${e.message}")
                null
            }
        } else null

        onDispose {
            try {
                if (player?.isPlaying == true) {
                    player.stop()
                }
                player?.release()
            } catch (e: Exception) {
                Log.w("EasterEggAudio", "Error releasing MediaPlayer: ${e.message}")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SETTINGS SCREEN COMMEMORATION CARD
// ---------------------------------------------------------------------------

@Composable
fun EasterEggCommemorationCard(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isUnlocked) return

    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val isLightMode = canvasColor.luminance() > 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "EASTER EGG ACHIEVEMENT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = if (isLightMode) 1.00f else 0.25f
                )
            ),
            border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_htmlgom_title_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp, 24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Challenge Complete!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "How Did We Get Here? • Wanderer of HTML",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
