package com.absolutemango.htmlgow

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val CoalBackground = Color(0xFF212121)
private val LightBorderColor = Color(0xFF48484A)
private val HighlightColor = Color(0xFF81D4FA)

@Composable
fun PhoneConnectionScreen(
    onBackClick: () -> Unit,
    onSendFileClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var connectionStatus by remember { mutableStateOf("Checking...") }
    var phoneName by remember { mutableStateOf<String?>("Searching...") }
    val transferStatus by WearTransferManager.transferStatus.collectAsState()

    fun checkStatus() {
        connectionStatus = "Checking..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                com.google.android.gms.wearable.Wearable.getCapabilityClient(context)
                    .addLocalCapability("htmlgow_wear_app")
                    .await()
            } catch (ignored: Exception) {}

            val phoneNode = WearTransferManager.findPhoneNode(context)
            withContext(Dispatchers.Main) {
                if (phoneNode != null) {
                    connectionStatus = "Connected"
                    phoneName = phoneNode.displayName
                } else {
                    connectionStatus = "Disconnected"
                    phoneName = "No Phone Found"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkStatus()
    }

    AppScaffold {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Icon(
                        Icons.Outlined.PhoneAndroid,
                        contentDescription = "Phone",
                        tint = if (connectionStatus == "Connected") HighlightColor else Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item {
                    Text(
                        text = phoneName ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                item { Spacer(modifier = Modifier.height(6.dp)) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when (connectionStatus) {
                            "Connected" -> Color.Green
                            "Checking..." -> Color.Yellow
                            else -> Color.Red
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = connectionStatus,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Live background file transfer indicator
                if (transferStatus != null) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E3A5F))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = transferStatus ?: "",
                                color = HighlightColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
                
                // Send File Button - opens the built-in file manager in picker mode
                item {
                    val isConnected = connectionStatus == "Connected"
                    Button(
                        onClick = onSendFileClick,
                        enabled = isConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .border(
                                1.dp,
                                if (isConnected) HighlightColor else LightBorderColor,
                                RoundedCornerShape(20.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = CoalBackground,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send File",
                                tint = if (isConnected) HighlightColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Send File",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.size(40.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = CoalBackground)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Button(
                            onClick = { checkStatus() },
                            modifier = Modifier
                                .width(90.dp)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = CoalBackground)
                        ) {
                            Text("Refresh", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
