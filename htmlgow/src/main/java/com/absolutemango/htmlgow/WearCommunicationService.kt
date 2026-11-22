package com.absolutemango.htmlgow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import org.json.JSONArray

object WearState {
    val aiStatus = kotlinx.coroutines.flow.MutableStateFlow("Idle")
    val generatedFilePath = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val retainedPrompt = kotlinx.coroutines.flow.MutableStateFlow("")
}

class WearCommunicationService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        scope.launch {
            try {
                Wearable.getCapabilityClient(this@WearCommunicationService)
                    .addLocalCapability("htmlgow_wear_app")
                    .await()
                Log.d("WearCommServiceWatch", "Registered capability htmlgow_wear_app")
            } catch (e: Exception) {
                Log.e("WearCommServiceWatch", "Failed to register local capability: ${e.message}")
            }
        }
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/ai_status") {
            WearState.aiStatus.value = String(messageEvent.data)
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)
        
        if (channel.path.startsWith("/file_transfer") || channel.path == "/ai_reply") {
            scope.launch {
                try {
                    val sharedDir = File(filesDir, "shared").apply {
                        if (!exists()) mkdirs()
                    }
                    
                    val fileName = if (channel.path == "/ai_reply") {
                        "ai_response_${System.currentTimeMillis()}.html"
                    } else {
                        val passedName = channel.path.substringAfter("/file_transfer/")
                        if (passedName.isNotBlank() && passedName != "/file_transfer") passedName else "received_${System.currentTimeMillis()}.html"
                    }
                    val newFile = File(sharedDir, fileName)
                    
                    Wearable.getChannelClient(this@WearCommunicationService)
                        .receiveFile(channel, Uri.fromFile(newFile), false)
                        .await()
                        
                    Log.d("WearCommServiceWatch", "File received from phone: ${newFile.absolutePath}")
                    
                    if (channel.path == "/ai_reply") {
                        WearState.generatedFilePath.value = newFile.absolutePath
                        WearState.aiStatus.value = "Ready!"
                    }
                    
                    // Update Recents List so it pops up on the watch home screen
                    val prefs = getSharedPreferences("htmlgow_prefs", Context.MODE_PRIVATE)
                    val jsonString = prefs.getString("recent_files", null)
                    val list = mutableListOf<String>()
                    if (jsonString != null) {
                        val array = JSONArray(jsonString)
                        for (i in 0 until array.length()) {
                            list.add(array.getString(i))
                        }
                    }
                    
                    list.remove(newFile.absolutePath)
                    list.add(0, newFile.absolutePath)
                    
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it) }
                    prefs.edit().putString("recent_files", newArray.toString()).apply()
                    
                } catch (e: Exception) {
                    Log.e("WearCommServiceWatch", "Failed to receive file from channel: ${e.message}")
                }
            }
        }
    }
}
