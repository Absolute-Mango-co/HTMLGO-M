package com.absolutemango.htmlgom

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.absolutemango.expressiveui.byok.HttpException
import com.absolutemango.expressiveui.byok.QuotaExhaustedException
import com.absolutemango.expressiveui.byok.StreamTimeoutException
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearCommunicationService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val bridge by lazy { WearAiForgeBridge(this) }

    companion object {
        private const val TAG = "WearCommService"
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            try {
                Wearable.getCapabilityClient(this@WearCommunicationService)
                    .addLocalCapability("htmlgom_phone_app")
                    .await()
                Log.d(TAG, "Registered capability htmlgom_phone_app")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register local capability: ${e.message}")
            }
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)
        
        if (channel.path.startsWith("/file_transfer")) {
            scope.launch {
                try {
                    val rawName = channel.path.substringAfter("/file_transfer/").trim()
                    val incomingFileName = if (rawName.isNotBlank() && rawName != "/file_transfer") rawName else "received_${System.currentTimeMillis()}.bin"
                    
                    // Direct receiveFile to a private cache file to avoid SAF Permission Denial in Google Play Services
                    val tempFile = File(cacheDir, "wear_temp_${System.currentTimeMillis()}_${incomingFileName.filter { it.isLetterOrDigit() || it == '.' || it == '_' }}")
                    Wearable.getChannelClient(this@WearCommunicationService)
                        .receiveFile(channel, Uri.fromFile(tempFile), false)
                        .await()
                    
                    val prefs = getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
                    val folderUriString = prefs.getString("permitted_folder_uri", null)
                    var savedToSaf = false
                    
                    if (folderUriString != null) {
                        val rootUri = Uri.parse(folderUriString)
                        val rootDoc = DocumentFile.fromTreeUri(this@WearCommunicationService, rootUri)
                        
                        if (rootDoc != null && rootDoc.exists()) {
                            val timestamp = SimpleDateFormat("ddMMyy_HHmm", Locale.US).format(Date())
                            val newFolder = rootDoc.createDirectory("wear_transfer_$timestamp") ?: rootDoc
                            val mimeType = when {
                                incomingFileName.endsWith(".html", ignoreCase = true) || incomingFileName.endsWith(".htm", ignoreCase = true) -> "text/html"
                                incomingFileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
                                else -> "application/octet-stream"
                            }
                            val newFile = newFolder.createFile(mimeType, incomingFileName)
                            
                            if (newFile != null) {
                                contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                    tempFile.inputStream().use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                savedToSaf = true
                                Log.d(TAG, "File received from watch and saved to SAF workspace: ${newFile.uri}")
                            }
                        }
                    }
                    
                    if (!savedToSaf) {
                        val fallbackDir = File(getExternalFilesDir(null) ?: filesDir, "wear_transfers").apply {
                            if (!exists()) mkdirs()
                        }
                        val destFile = File(fallbackDir, incomingFileName)
                        tempFile.copyTo(destFile, overwrite = true)
                        Log.d(TAG, "File received from watch and saved to fallback storage: ${destFile.absolutePath}")
                    }
                    
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to receive file from channel: ${e.message}", e)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        if (messageEvent.path == "/ai_prompt") {
            val prompt = String(messageEvent.data)
            val sourceNodeId = messageEvent.sourceNodeId
            
            scope.launch {
                val messageClient = Wearable.getMessageClient(this@WearCommunicationService)
                
                suspend fun sendStatus(status: String) {
                    try {
                        messageClient.sendMessage(sourceNodeId, "/ai_status", status.toByteArray()).await()
                        Log.d(TAG, "Emitted AI status to watch ($sourceNodeId): $status")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send status to watch: ${e.message}")
                    }
                }

                try {
                    val result = bridge.generateHtmlForWatch(prompt) { status ->
                        sendStatus(status)
                    }

                    result.fold(
                        onSuccess = { tempFile ->
                            sendStatus("Receiving...")
                            val channelClient = Wearable.getChannelClient(this@WearCommunicationService)
                            val channel = channelClient.openChannel(sourceNodeId, "/ai_reply").await()
                            channelClient.sendFile(channel, Uri.fromFile(tempFile)).await()
                            Log.d(TAG, "AI generated HTML sent to watch successfully: ${tempFile.length()} bytes")
                        },
                        onFailure = { error ->
                            val errorStatus = mapErrorToStatus(error)
                            Log.e(TAG, "AI generation failed: ${error.message}, reporting status: $errorStatus")
                            sendStatus(errorStatus)
                        }
                    )
                } catch (e: Exception) {
                    val errorStatus = mapErrorToStatus(e)
                    Log.e(TAG, "Unhandled exception in AI prompt processing: ${e.message}", e)
                    sendStatus(errorStatus)
                }
            }
        }
    }

    private fun mapErrorToStatus(error: Throwable): String {
        val msg = error.message ?: ""
        return when {
            error is NoApiKeyException -> "Error: Set API Key on Phone"
            error is QuotaExhaustedException -> "Error: Quota 429"
            error is StreamTimeoutException -> "Error: Timed Out"
            error is UnknownHostException || error is SocketException || error is SocketTimeoutException -> "Error: Phone Offline"
            error is HttpException && (error.code == 401 || error.code == 403) -> "Error: Invalid API Key"
            error is HttpException && error.code == 429 -> "Error: Quota 429"
            error is HttpException -> "Error: HTTP ${error.code}"
            msg.contains("API key", ignoreCase = true) || msg.contains("key not valid", ignoreCase = true) || msg.contains("401") || msg.contains("403") -> "Error: Invalid API Key"
            msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("exhausted", ignoreCase = true) -> "Error: Quota 429"
            msg.contains("offline", ignoreCase = true) || msg.contains("resolve host", ignoreCase = true) || msg.contains("network", ignoreCase = true) -> "Error: Phone Offline"
            else -> "Error: ${msg.take(18).ifBlank { "Generation Failed" }}"
        }
    }
}
