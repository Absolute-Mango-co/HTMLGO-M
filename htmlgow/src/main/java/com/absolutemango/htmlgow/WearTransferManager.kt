package com.absolutemango.htmlgow

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object WearTransferManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _transferStatus = MutableStateFlow<String?>(null)
    val transferStatus = _transferStatus.asStateFlow()

    suspend fun findPhoneNode(context: Context): Node? {
        try {
            Wearable.getCapabilityClient(context).addLocalCapability("htmlgow_wear_app").await()
        } catch (ignored: Exception) {}
        return try {
            val capabilityInfo = Wearable.getCapabilityClient(context)
                .getCapability("htmlgom_phone_app", CapabilityClient.FILTER_REACHABLE)
                .await()
            val phoneNode = capabilityInfo.nodes.firstOrNull()
            phoneNode ?: Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()
        } catch (e: Exception) {
            try {
                Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()
            } catch (ex: Exception) {
                null
            }
        }
    }

    fun sendFileToPhone(context: Context, file: File) {
        scope.launch {
            try {
                _transferStatus.value = "Connecting..."
                val phoneNode = findPhoneNode(context)
                if (phoneNode == null) {
                    _transferStatus.value = "No Phone Connected"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No Phone Connected", Toast.LENGTH_SHORT).show()
                    }
                    kotlinx.coroutines.delay(3000L)
                    _transferStatus.value = null
                    return@launch
                }

                _transferStatus.value = "Sending ${file.name}..."
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sending ${file.name}...", Toast.LENGTH_SHORT).show()
                }

                val channelClient = Wearable.getChannelClient(context)
                val channelPath = "/file_transfer/${file.name}"
                val channel = channelClient.openChannel(phoneNode.id, channelPath).await()
                channelClient.sendFile(channel, Uri.fromFile(file)).await()

                _transferStatus.value = "Sent!"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sent to phone!", Toast.LENGTH_SHORT).show()
                }
                kotlinx.coroutines.delay(3000L)
                _transferStatus.value = null
            } catch (e: Exception) {
                Log.e("WearTransferManager", "File transfer failed: ${e.message}", e)
                _transferStatus.value = "Send Failed"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Transfer failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                kotlinx.coroutines.delay(3000L)
                _transferStatus.value = null
            }
        }
    }
}
