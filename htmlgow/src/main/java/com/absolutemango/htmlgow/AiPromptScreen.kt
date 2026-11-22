package com.absolutemango.htmlgow

import android.app.Activity
import android.app.RemoteInput
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissValue
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.input.RemoteInputIntentHelper
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val CoalBackground = Color(0xFF212121)
private val LightBorderColor = Color(0xFF48484A)
private val HighlightColor = Color(0xFF81D4FA)

@Composable
fun AiPromptScreen(
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val retainedPrompt by WearState.retainedPrompt.collectAsState()
    var promptText by remember { mutableStateOf(retainedPrompt) }
    
    val aiStatus by WearState.aiStatus.collectAsState()
    val generatedPath by WearState.generatedFilePath.collectAsState()

    fun updatePrompt(text: String) {
        promptText = text
        WearState.retainedPrompt.value = text
    }

    // Intercept hardware back button
    BackHandler(enabled = true) {
        if (aiStatus.startsWith("Error")) {
            WearState.aiStatus.value = "Idle"
        }
        onBack()
    }

    // Swipe right to dismiss gesture
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissValue.Dismissed) {
            if (aiStatus.startsWith("Error")) {
                WearState.aiStatus.value = "Idle"
            }
            onBack()
            dismissState.snapTo(SwipeToDismissValue.Default)
        }
    }

    val remoteInputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = RemoteInput.getResultsFromIntent(result.data)
            val input = results?.getCharSequence("input_key")?.toString()
            if (!input.isNullOrBlank()) {
                updatePrompt(input)
            }
        }
    }

    fun launchSystemInput() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val remoteInput = RemoteInput.Builder("input_key")
            .setLabel("Describe your HTML...")
            .build()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        remoteInputLauncher.launch(intent)
    }

    // 35-second client-side watchdog: if phone drops connection or dies, timeout gracefully
    LaunchedEffect(aiStatus) {
        if (aiStatus == "Sending..." || aiStatus.startsWith("Generating") || aiStatus == "Receiving..." || aiStatus.startsWith("Traffic Spike")) {
            delay(35000L)
            WearState.aiStatus.value = "Error: Timed Out"
        }
    }

    // 5-second error dwell: reset to "Idle" without wiping user's typed prompt
    LaunchedEffect(aiStatus) {
        if (aiStatus.startsWith("Error")) {
            delay(5000L)
            WearState.aiStatus.value = "Idle"
        }
    }

    val listState = rememberScalingLazyListState()

    @Composable
    fun PromptBody(contentPadding: PaddingValues) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Indicator Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    val dotColor = when {
                        aiStatus == "Idle" -> Color.Gray
                        aiStatus == "Sending..." -> Color.Yellow
                        aiStatus.startsWith("Generating") -> Color(0xFFFFB74D) // Amber
                        aiStatus.startsWith("Traffic Spike") -> Color(0xFFFF9800) // Deep Orange
                        aiStatus == "Receiving..." -> Color(0xFF81D4FA) // Cyan
                        aiStatus == "Ready!" -> Color.Green
                        aiStatus.startsWith("Error") -> Color.Red
                        else -> Color.Cyan
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = aiStatus,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Prompt Box: Sleek pill shape - tap opens Wear OS full-screen RemoteInput
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF2C2C2E))
                        .border(1.dp, LightBorderColor, RoundedCornerShape(26.dp))
                        .clickable { launchSystemInput() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (promptText.isEmpty()) {
                        Text(
                            text = "Tap to describe HTML...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = promptText,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Clear prompt button
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3A3A3C))
                                    .clickable { updatePrompt("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Send Button transformed from Type / Speak
            item {
                val isSendingAllowed = (aiStatus == "Idle" || aiStatus.startsWith("Error")) && promptText.isNotBlank()
                Button(
                    onClick = {
                        if (isSendingAllowed) {
                            WearState.aiStatus.value = "Sending..."
                            coroutineScope.launch {
                                try {
                                    val phoneNode = WearTransferManager.findPhoneNode(context)
                                    if (phoneNode != null) {
                                        val messageClient = Wearable.getMessageClient(context)
                                        messageClient.sendMessage(phoneNode.id, "/ai_prompt", promptText.toByteArray()).await()
                                    } else {
                                        WearState.aiStatus.value = "Error: No Phone"
                                    }
                                } catch (e: Exception) {
                                    WearState.aiStatus.value = "Error: Send Failed"
                                }
                            }
                        }
                    },
                    enabled = isSendingAllowed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(top = 8.dp)
                        .border(
                            1.dp,
                            if (isSendingAllowed) HighlightColor.copy(alpha = 0.5f) else LightBorderColor,
                            RoundedCornerShape(20.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSendingAllowed) Color(0xFF0288D1) else CoalBackground,
                        contentColor = if (isSendingAllowed) Color.White else Color.Gray
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSendingAllowed) Color.White else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSendingAllowed) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxSize()
    ) { isBackground ->
        if (!isBackground) {
            AppScaffold {
                if (aiStatus == "Ready!" && generatedPath != null) {
                    ScreenScaffold(
                        scrollState = listState,
                        edgeButton = {
                            // Material 3 official semicircle EdgeButton anchored at the bottom curve of the round screen
                            EdgeButton(
                                onClick = {
                                    val path = generatedPath!!
                                    WearState.aiStatus.value = "Idle"
                                    WearState.generatedFilePath.value = null
                                    WearState.retainedPrompt.value = ""
                                    onOpenFile(path)
                                },
                                colors = androidx.wear.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B3A24),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play HTML",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color(0xFF81C784)
                                )
                            }
                        }
                    ) { contentPadding ->
                        PromptBody(contentPadding)
                    }
                } else {
                    ScreenScaffold(
                        scrollState = listState
                    ) { contentPadding ->
                        PromptBody(contentPadding)
                    }
                }
            }
        }
    }
}
