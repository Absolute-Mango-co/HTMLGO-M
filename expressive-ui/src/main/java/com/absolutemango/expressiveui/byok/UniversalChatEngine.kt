package com.absolutemango.expressiveui.byok

import android.util.Log
import com.absolutemango.expressiveui.ForgeMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

open class QuotaExhaustedException(
    val code: Int,
    val retryAfter: String?,
    message: String
) : IOException(message)

open class HttpException(
    val code: Int,
    message: String
) : IOException(message)

open class StreamTimeoutException(
    message: String
) : IOException(message)

interface ChatEngineAdapter {
    fun streamChat(
        messages: List<ForgeMessage>,
        model: String,
        apiKey: String,
        baseUrl: String?,
        modelConfig: ModelConfig = ModelConfig(),
        customApiJson: String = ""
    ): Flow<String>
}

open class UniversalChatEngine(
    private val okHttpClient: OkHttpClient = defaultOkHttpClient()
) {

    companion object {
        private const val TAG = "UniversalChatEngine"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val PRE_STREAM_TIMEOUT_MS = 90_000L
        const val INTER_CHUNK_TIMEOUT_MS = 15_000L

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // Accommodates reasoning TTFT
                .pingInterval(30, TimeUnit.SECONDS)   // Detects half-open dead cellular sockets
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        fun mergeCustomJson(target: JSONObject, customJsonStr: String, isGemini: Boolean = false) {
            val trimmed = customJsonStr.trim().removePrefix("\uFEFF").trim()
            if (trimmed.isBlank() || !trimmed.startsWith("{")) return
            try {
                val custom = JSONObject(trimmed)
                val keys = custom.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (isGemini && key == "generationConfig") {
                        val customGen = custom.optJSONObject("generationConfig")
                        if (customGen != null) {
                            val currentGen = target.optJSONObject("generationConfig")
                                ?: JSONObject().also { target.put("generationConfig", it) }
                            val genKeys = customGen.keys()
                            while (genKeys.hasNext()) {
                                val gk = genKeys.next()
                                currentGen.put(gk, customGen.get(gk))
                            }
                        } else {
                            target.put(key, custom.get(key))
                        }
                    } else {
                        target.put(key, custom.get(key))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to merge custom API JSON: ${e.message}")
            }
        }
    }

    val geminiAdapter: ChatEngineAdapter = GeminiRestAdapter(okHttpClient)
    val openAiAdapter: ChatEngineAdapter = OpenAiCompatibleAdapter(okHttpClient)
    val anthropicAdapter: ChatEngineAdapter = AnthropicAdapter(okHttpClient)

    open fun streamChat(
        providerType: ProviderType,
        messages: List<ForgeMessage>,
        model: String,
        apiKey: String,
        baseUrl: String?,
        modelConfig: ModelConfig = ModelConfig(),
        customApiJson: String = ""
    ): Flow<String> {
        val adapter = when (providerType) {
            ProviderType.GEMINI -> geminiAdapter
            ProviderType.OPENROUTER, ProviderType.GROQ, ProviderType.OPENAI, ProviderType.CUSTOM -> openAiAdapter
            ProviderType.ANTHROPIC -> anthropicAdapter
        }
        return adapter.streamChat(messages, model, apiKey, baseUrl, modelConfig, customApiJson)
    }

    open fun streamChat(
        providerType: ProviderType,
        messages: List<ForgeMessage>,
        model: String,
        apiKey: String,
        baseUrl: String?
    ): Flow<String> = streamChat(providerType, messages, model, apiKey, baseUrl, ModelConfig(), "")

    open fun streamChat(
        providerType: ProviderType,
        messages: List<ForgeMessage>,
        model: String,
        apiKey: String,
        baseUrl: String?,
        modelConfig: ModelConfig
    ): Flow<String> = streamChat(providerType, messages, model, apiKey, baseUrl, modelConfig, "")

    /**
     * Executes SSE request with a dual-phase TTFT watchdog:
     * - Phase 1: 90s pre-stream timeout for first token
     * - Phase 2: 15s inter-chunk timeout for successive tokens
     */
    internal fun executeSse(
        request: Request,
        parser: (type: String?, data: String) -> String?
    ): Flow<String> = callbackFlow {
        var hasEmittedFirstToken = false
        var watchdogJob: Job? = null

        fun startInterChunkWatchdog() {
            watchdogJob?.cancel()
            watchdogJob = launch {
                delay(INTER_CHUNK_TIMEOUT_MS)
                close(StreamTimeoutException("Inter-chunk streaming timeout (15s) exceeded"))
            }
        }

        // Start Phase 1 Watchdog (Pre-stream 90s)
        watchdogJob = launch {
            delay(PRE_STREAM_TIMEOUT_MS)
            if (!hasEmittedFirstToken) {
                close(StreamTimeoutException("Pre-stream Time-To-First-Token timeout (90s) exceeded"))
            }
        }

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection open
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    watchdogJob?.cancel()
                    close()
                    return
                }

                try {
                    val token = parser(type, data)
                    if (!token.isNullOrEmpty()) {
                        hasEmittedFirstToken = true
                        startInterChunkWatchdog()
                        trySend(token)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing SSE chunk: $data", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                watchdogJob?.cancel()
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                watchdogJob?.cancel()
                val error = when {
                    response != null && response.code == 429 -> {
                        val retryAfter = response.header("Retry-After")
                        val errorBody = try { response.body?.string() } catch (_: Exception) { null }
                        QuotaExhaustedException(response.code, retryAfter, errorBody ?: "HTTP 429 Quota Exhausted")
                    }
                    response != null -> {
                        val errorBody = try { response.body?.string() } catch (_: Exception) { null }
                        HttpException(response.code, errorBody ?: "HTTP ${response.code}: ${response.message}")
                    }
                    t != null -> t
                    else -> IOException("Unknown SSE connection failure")
                }
                close(error)
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, eventSourceListener)

        awaitClose {
            watchdogJob?.cancel()
            eventSource.cancel()
        }
    }

    inner class GeminiRestAdapter(private val client: OkHttpClient) : ChatEngineAdapter {
        override fun streamChat(
            messages: List<ForgeMessage>,
            model: String,
            apiKey: String,
            baseUrl: String?,
            modelConfig: ModelConfig,
            customApiJson: String
        ): Flow<String> {
            val base = (baseUrl ?: ProviderType.GEMINI.defaultBaseUrl ?: "https://generativelanguage.googleapis.com").trimEnd('/')
            val url = "$base/v1beta/models/$model:streamGenerateContent?alt=sse"

            val payload = JSONObject()
            val contentsArray = JSONArray()

            var systemPrompt: String? = null
            for (msg in messages) {
                if (msg.role.equals("system", ignoreCase = true)) {
                    systemPrompt = msg.content.trim()
                    continue
                }
                val contentObj = JSONObject()
                val role = if (msg.role.equals("user", ignoreCase = true)) "user" else "model"
                contentObj.put("role", role)
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.content)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            if (!systemPrompt.isNullOrBlank()) {
                val sysObj = JSONObject()
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", systemPrompt)
                partsArray.put(partObj)
                sysObj.put("parts", partsArray)
                payload.put("systemInstruction", sysObj)
            }
            payload.put("contents", contentsArray)

            // Inject Gemini Native generationConfig
            val generationConfig = JSONObject()
            val temp = modelConfig.geminiTemperature ?: modelConfig.temperature
            temp?.let { generationConfig.put("temperature", it.toDouble()) }

            val topP = modelConfig.geminiTopP ?: modelConfig.topP
            topP?.let { generationConfig.put("topP", it.toDouble()) }

            val topK = modelConfig.geminiTopK ?: modelConfig.topK
            topK?.let { generationConfig.put("topK", it) }

            val maxOut = modelConfig.geminiMaxOutputTokens ?: modelConfig.maxCompletionTokens ?: modelConfig.maxTokens
            maxOut?.let { generationConfig.put("maxOutputTokens", it) }

            val stopSeq = modelConfig.geminiStopSequences ?: modelConfig.stop
            if (!stopSeq.isNullOrBlank()) {
                val stopArr = JSONArray()
                val trimmed = stopSeq.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    try {
                        val parsed = JSONArray(trimmed)
                        for (i in 0 until parsed.length()) stopArr.put(parsed.getString(i))
                    } catch (_: Exception) {
                        stopArr.put(trimmed)
                    }
                } else if (trimmed.contains(",")) {
                    trimmed.split(",").forEach {
                        val item = it.trim()
                        if (item.isNotEmpty()) stopArr.put(item)
                    }
                } else {
                    stopArr.put(trimmed)
                }
                if (stopArr.length() > 0) {
                    generationConfig.put("stopSequences", stopArr)
                }
            }

            val freqPenalty = modelConfig.geminiFrequencyPenalty ?: modelConfig.frequencyPenalty
            freqPenalty?.let { generationConfig.put("frequencyPenalty", it.toDouble()) }

            val presPenalty = modelConfig.geminiPresencePenalty ?: modelConfig.presencePenalty
            presPenalty?.let { generationConfig.put("presencePenalty", it.toDouble()) }

            val seed = modelConfig.geminiSeed ?: modelConfig.seed
            seed?.let { generationConfig.put("seed", it) }

            val candCount = modelConfig.geminiCandidateCount ?: modelConfig.n
            candCount?.let { generationConfig.put("candidateCount", it) }

            if (!modelConfig.geminiResponseMimeType.isNullOrBlank()) {
                generationConfig.put("responseMimeType", modelConfig.geminiResponseMimeType.trim())
            } else if (!modelConfig.responseFormat.isNullOrBlank() && modelConfig.responseFormat.contains("json", ignoreCase = true)) {
                generationConfig.put("responseMimeType", "application/json")
            }

            if (!modelConfig.geminiResponseSchema.isNullOrBlank()) {
                val schemaStr = modelConfig.geminiResponseSchema.trim()
                try {
                    generationConfig.put("responseSchema", JSONObject(schemaStr))
                } catch (_: Exception) {
                    generationConfig.put("responseSchema", schemaStr)
                }
            }

            // thinkingConfig (thinkingLevel / thinkingBudget)
            if (!modelConfig.geminiThinkingLevel.isNullOrBlank() || modelConfig.geminiThinkingBudget != null) {
                val thinkingConfig = JSONObject()
                if (!modelConfig.geminiThinkingLevel.isNullOrBlank()) {
                    thinkingConfig.put("thinkingLevel", modelConfig.geminiThinkingLevel.trim())
                }
                modelConfig.geminiThinkingBudget?.let {
                    thinkingConfig.put("thinkingBudget", it)
                }
                generationConfig.put("thinkingConfig", thinkingConfig)
            }

            if (generationConfig.length() > 0) {
                payload.put("generationConfig", generationConfig)
            }

            if (customApiJson.isNotBlank()) {
                mergeCustomJson(payload, customApiJson, isGemini = true)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.header("x-goog-api-key", apiKey)
            }

            return executeSse(requestBuilder.build()) { _, data ->
                val json = JSONObject(data)
                val candidates = json.optJSONArray("candidates") ?: return@executeSse null
                if (candidates.length() == 0) return@executeSse null
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content") ?: return@executeSse null
                val parts = content.optJSONArray("parts") ?: return@executeSse null
                if (parts.length() == 0) return@executeSse null
                parts.getJSONObject(0).optString("text", "")
            }
        }
    }

    inner class OpenAiCompatibleAdapter(private val client: OkHttpClient) : ChatEngineAdapter {
        override fun streamChat(
            messages: List<ForgeMessage>,
            model: String,
            apiKey: String,
            baseUrl: String?,
            modelConfig: ModelConfig,
            customApiJson: String
        ): Flow<String> {
            val base = (baseUrl ?: ProviderType.OPENROUTER.defaultBaseUrl ?: "https://openrouter.ai/api").trimEnd('/')
            val url = "$base/v1/chat/completions"

            val payload = JSONObject()
            payload.put("model", model)
            payload.put("stream", modelConfig.stream)

            // General / OpenAI / Universal parameters
            modelConfig.temperature?.let { payload.put("temperature", it.toDouble()) }
            modelConfig.topP?.let { payload.put("top_p", it.toDouble()) }
            modelConfig.topK?.let { payload.put("top_k", it) }
            modelConfig.maxCompletionTokens?.let { payload.put("max_completion_tokens", it) }
            modelConfig.maxTokens?.let { payload.put("max_tokens", it) }
            if (!modelConfig.reasoningEffort.isNullOrBlank()) {
                payload.put("reasoning_effort", modelConfig.reasoningEffort.trim())
            }
            if (!modelConfig.stop.isNullOrBlank()) {
                val trimmed = modelConfig.stop.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    try {
                        payload.put("stop", JSONArray(trimmed))
                    } catch (_: Exception) {
                        payload.put("stop", trimmed)
                    }
                } else if (trimmed.contains(",")) {
                    val arr = JSONArray()
                    trimmed.split(",").forEach {
                        val item = it.trim()
                        if (item.isNotEmpty()) arr.put(item)
                    }
                    payload.put("stop", arr)
                } else {
                    payload.put("stop", trimmed)
                }
            }
            modelConfig.frequencyPenalty?.let { payload.put("frequency_penalty", it.toDouble()) }
            modelConfig.presencePenalty?.let { payload.put("presence_penalty", it.toDouble()) }
            modelConfig.seed?.let { payload.put("seed", it) }
            if (!modelConfig.responseFormat.isNullOrBlank()) {
                val trimmed = modelConfig.responseFormat.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        payload.put("response_format", JSONObject(trimmed))
                    } catch (_: Exception) {
                        payload.put("response_format", trimmed)
                    }
                } else {
                    val fmt = JSONObject()
                    fmt.put("type", trimmed)
                    payload.put("response_format", fmt)
                }
            }
            modelConfig.logprobs?.let { payload.put("logprobs", it) }
            modelConfig.topLogprobs?.let { payload.put("top_logprobs", it) }
            if (!modelConfig.logitBias.isNullOrBlank()) {
                val trimmed = modelConfig.logitBias.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        payload.put("logit_bias", JSONObject(trimmed))
                    } catch (_: Exception) {
                        payload.put("logit_bias", trimmed)
                    }
                } else {
                    payload.put("logit_bias", trimmed)
                }
            }
            modelConfig.n?.let { payload.put("n", it) }

            // Local / OpenRouter Extra Samplers
            modelConfig.minP?.let { payload.put("min_p", it.toDouble()) }
            modelConfig.repetitionPenalty?.let { payload.put("repetition_penalty", it.toDouble()) }
            modelConfig.typicalP?.let { payload.put("typical_p", it.toDouble()) }
            modelConfig.tfsZ?.let { payload.put("tfs_z", it.toDouble()) }
            modelConfig.mirostat?.let { payload.put("mirostat", it) }
            modelConfig.mirostatTau?.let { payload.put("mirostat_tau", it.toDouble()) }
            modelConfig.mirostatEta?.let { payload.put("mirostat_eta", it.toDouble()) }
            modelConfig.repeatLastN?.let { payload.put("repeat_last_n", it) }
            modelConfig.numCtx?.let { payload.put("num_ctx", it) }

            val messagesArray = JSONArray()
            for (msg in messages) {
                val role = when (msg.role.lowercase()) {
                    "user" -> "user"
                    "model", "assistant" -> "assistant"
                    "system" -> "system"
                    else -> "user"
                }
                val msgObj = JSONObject()
                msgObj.put("role", role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }
            payload.put("messages", messagesArray)

            if (customApiJson.isNotBlank()) {
                mergeCustomJson(payload, customApiJson, isGemini = false)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            return executeSse(requestBuilder.build()) { _, data ->
                val json = JSONObject(data)
                val choices = json.optJSONArray("choices") ?: return@executeSse null
                if (choices.length() == 0) return@executeSse null
                val choice = choices.getJSONObject(0)
                val delta = choice.optJSONObject("delta") ?: return@executeSse null

                // Extract content first, fallback to reasoning_content for DeepSeek-R1 / QwQ models
                if (delta.has("content") && !delta.isNull("content")) {
                    delta.optString("content")
                } else if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                    delta.optString("reasoning_content")
                } else if (delta.has("reasoning") && !delta.isNull("reasoning")) {
                    delta.optString("reasoning")
                } else {
                    null
                }
            }
        }
    }

    inner class AnthropicAdapter(private val client: OkHttpClient) : ChatEngineAdapter {
        override fun streamChat(
            messages: List<ForgeMessage>,
            model: String,
            apiKey: String,
            baseUrl: String?,
            modelConfig: ModelConfig,
            customApiJson: String
        ): Flow<String> {
            val base = (baseUrl ?: ProviderType.ANTHROPIC.defaultBaseUrl ?: "https://api.anthropic.com").trimEnd('/')
            val url = "$base/v1/messages"

            val (systemInstruction, sanitizedList) = AnthropicMessageSanitizer.sanitize(messages)

            val payload = JSONObject()
            payload.put("model", model)
            val maxTokens = modelConfig.maxTokens ?: modelConfig.maxCompletionTokens ?: 4096
            payload.put("max_tokens", maxTokens)
            payload.put("stream", modelConfig.stream)

            modelConfig.temperature?.let { payload.put("temperature", it.toDouble()) }
            modelConfig.topP?.let { payload.put("top_p", it.toDouble()) }
            modelConfig.topK?.let { payload.put("top_k", it) }

            if (!modelConfig.stop.isNullOrBlank()) {
                val stopArr = JSONArray()
                val trimmed = modelConfig.stop.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    try {
                        val parsed = JSONArray(trimmed)
                        for (i in 0 until parsed.length()) stopArr.put(parsed.getString(i))
                    } catch (_: Exception) {
                        stopArr.put(trimmed)
                    }
                } else if (trimmed.contains(",")) {
                    trimmed.split(",").forEach {
                        val item = it.trim()
                        if (item.isNotEmpty()) stopArr.put(item)
                    }
                } else {
                    stopArr.put(trimmed)
                }
                if (stopArr.length() > 0) {
                    payload.put("stop_sequences", stopArr)
                }
            }

            if (!systemInstruction.isNullOrBlank()) {
                payload.put("system", systemInstruction)
            }

            val messagesArray = JSONArray()
            for ((role, content) in sanitizedList) {
                val msgObj = JSONObject()
                msgObj.put("role", role)
                msgObj.put("content", content)
                messagesArray.put(msgObj)
            }
            payload.put("messages", messagesArray)

            if (customApiJson.isNotBlank()) {
                mergeCustomJson(payload, customApiJson, isGemini = false)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .header("anthropic-version", "2023-06-01")

            if (apiKey.isNotBlank()) {
                requestBuilder.header("x-api-key", apiKey)
            }

            return executeSse(requestBuilder.build()) { type, data ->
                val json = JSONObject(data)
                val eventType = type ?: json.optString("type", "")
                if (eventType == "content_block_delta" || json.optString("type") == "content_block_delta") {
                    val delta = json.optJSONObject("delta") ?: return@executeSse null
                    val deltaType = delta.optString("type", "")
                    if (deltaType == "text_delta") {
                        delta.optString("text", "")
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
}
