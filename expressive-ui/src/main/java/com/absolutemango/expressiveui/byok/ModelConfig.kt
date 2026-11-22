package com.absolutemango.expressiveui.byok

import org.json.JSONObject

data class ModelConfig(
    // 1. Universal / OpenAI / Standard
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxCompletionTokens: Int? = null,
    val maxTokens: Int? = null,
    val reasoningEffort: String? = null,
    val stop: String? = null,
    val stream: Boolean = true,
    val frequencyPenalty: Float? = null,
    val presencePenalty: Float? = null,
    val seed: Long? = null,
    val responseFormat: String? = null,
    val logprobs: Boolean? = null,
    val topLogprobs: Int? = null,
    val logitBias: String? = null,
    val n: Int? = null,

    // 2. Gemini Native (generationConfig)
    val geminiTemperature: Float? = null,
    val geminiTopP: Float? = null,
    val geminiTopK: Int? = null,
    val geminiMaxOutputTokens: Int? = null,
    val geminiStopSequences: String? = null,
    val geminiFrequencyPenalty: Float? = null,
    val geminiPresencePenalty: Float? = null,
    val geminiSeed: Long? = null,
    val geminiCandidateCount: Int? = null,
    val geminiResponseMimeType: String? = null,
    val geminiResponseSchema: String? = null,
    val geminiThinkingLevel: String? = null,
    val geminiThinkingBudget: Int? = null,

    // 3. Local / OpenRouter Extra Samplers
    val minP: Float? = null,
    val repetitionPenalty: Float? = null,
    val typicalP: Float? = null,
    val tfsZ: Float? = null,
    val mirostat: Int? = null,
    val mirostatTau: Float? = null,
    val mirostatEta: Float? = null,
    val repeatLastN: Int? = null,
    val numCtx: Int? = null
) {
    fun hasCustomOverrides(): Boolean {
        return temperature != null ||
                topP != null ||
                topK != null ||
                maxCompletionTokens != null ||
                maxTokens != null ||
                !reasoningEffort.isNullOrBlank() ||
                !stop.isNullOrBlank() ||
                !stream ||
                frequencyPenalty != null ||
                presencePenalty != null ||
                seed != null ||
                !responseFormat.isNullOrBlank() ||
                logprobs != null ||
                topLogprobs != null ||
                !logitBias.isNullOrBlank() ||
                n != null ||
                geminiTemperature != null ||
                geminiTopP != null ||
                geminiTopK != null ||
                geminiMaxOutputTokens != null ||
                !geminiStopSequences.isNullOrBlank() ||
                geminiFrequencyPenalty != null ||
                geminiPresencePenalty != null ||
                geminiSeed != null ||
                geminiCandidateCount != null ||
                !geminiResponseMimeType.isNullOrBlank() ||
                !geminiResponseSchema.isNullOrBlank() ||
                !geminiThinkingLevel.isNullOrBlank() ||
                geminiThinkingBudget != null ||
                minP != null ||
                repetitionPenalty != null ||
                typicalP != null ||
                tfsZ != null ||
                mirostat != null ||
                mirostatTau != null ||
                mirostatEta != null ||
                repeatLastN != null ||
                numCtx != null
    }

    fun toJson(): JSONObject {
        val root = JSONObject()
        temperature?.let { root.put("temperature", it.toDouble()) }
        topP?.let { root.put("top_p", it.toDouble()) }
        topK?.let { root.put("top_k", it) }
        maxCompletionTokens?.let { root.put("max_completion_tokens", it) }
        maxTokens?.let { root.put("max_tokens", it) }
        reasoningEffort?.let { root.put("reasoning_effort", it) }
        stop?.let { root.put("stop", it) }
        root.put("stream", stream)
        frequencyPenalty?.let { root.put("frequency_penalty", it.toDouble()) }
        presencePenalty?.let { root.put("presence_penalty", it.toDouble()) }
        seed?.let { root.put("seed", it) }
        responseFormat?.let { root.put("response_format", it) }
        logprobs?.let { root.put("logprobs", it) }
        topLogprobs?.let { root.put("top_logprobs", it) }
        logitBias?.let { root.put("logit_bias", it) }
        n?.let { root.put("n", it) }

        geminiTemperature?.let { root.put("gemini_temperature", it.toDouble()) }
        geminiTopP?.let { root.put("gemini_topP", it.toDouble()) }
        geminiTopK?.let { root.put("gemini_topK", it) }
        geminiMaxOutputTokens?.let { root.put("gemini_maxOutputTokens", it) }
        geminiStopSequences?.let { root.put("gemini_stopSequences", it) }
        geminiFrequencyPenalty?.let { root.put("gemini_frequencyPenalty", it.toDouble()) }
        geminiPresencePenalty?.let { root.put("gemini_presencePenalty", it.toDouble()) }
        geminiSeed?.let { root.put("gemini_seed", it) }
        geminiCandidateCount?.let { root.put("gemini_candidateCount", it) }
        geminiResponseMimeType?.let { root.put("gemini_responseMimeType", it) }
        geminiResponseSchema?.let { root.put("gemini_responseSchema", it) }
        geminiThinkingLevel?.let { root.put("gemini_thinkingLevel", it) }
        geminiThinkingBudget?.let { root.put("gemini_thinkingBudget", it) }

        minP?.let { root.put("min_p", it.toDouble()) }
        repetitionPenalty?.let { root.put("repetition_penalty", it.toDouble()) }
        typicalP?.let { root.put("typical_p", it.toDouble()) }
        tfsZ?.let { root.put("tfs_z", it.toDouble()) }
        mirostat?.let { root.put("mirostat", it) }
        mirostatTau?.let { root.put("mirostat_tau", it.toDouble()) }
        mirostatEta?.let { root.put("mirostat_eta", it.toDouble()) }
        repeatLastN?.let { root.put("repeat_last_n", it) }
        numCtx?.let { root.put("num_ctx", it) }
        return root
    }

    companion object {
        fun fromJson(json: JSONObject): ModelConfig {
            return ModelConfig(
                temperature = if (json.has("temperature")) json.optDouble("temperature").toFloat() else null,
                topP = if (json.has("top_p")) json.optDouble("top_p").toFloat() else null,
                topK = if (json.has("top_k")) json.optInt("top_k") else null,
                maxCompletionTokens = if (json.has("max_completion_tokens")) json.optInt("max_completion_tokens") else null,
                maxTokens = if (json.has("max_tokens")) json.optInt("max_tokens") else null,
                reasoningEffort = if (json.has("reasoning_effort")) json.optString("reasoning_effort").ifBlank { null } else null,
                stop = if (json.has("stop")) json.optString("stop").ifBlank { null } else null,
                stream = json.optBoolean("stream", true),
                frequencyPenalty = if (json.has("frequency_penalty")) json.optDouble("frequency_penalty").toFloat() else null,
                presencePenalty = if (json.has("presence_penalty")) json.optDouble("presence_penalty").toFloat() else null,
                seed = if (json.has("seed")) json.optLong("seed") else null,
                responseFormat = if (json.has("response_format")) json.optString("response_format").ifBlank { null } else null,
                logprobs = if (json.has("logprobs")) json.optBoolean("logprobs") else null,
                topLogprobs = if (json.has("top_logprobs")) json.optInt("top_logprobs") else null,
                logitBias = if (json.has("logit_bias")) json.optString("logit_bias").ifBlank { null } else null,
                n = if (json.has("n")) json.optInt("n") else null,

                geminiTemperature = if (json.has("gemini_temperature")) json.optDouble("gemini_temperature").toFloat() else null,
                geminiTopP = if (json.has("gemini_topP")) json.optDouble("gemini_topP").toFloat() else null,
                geminiTopK = if (json.has("gemini_topK")) json.optInt("gemini_topK") else null,
                geminiMaxOutputTokens = if (json.has("gemini_maxOutputTokens")) json.optInt("gemini_maxOutputTokens") else null,
                geminiStopSequences = if (json.has("gemini_stopSequences")) json.optString("gemini_stopSequences").ifBlank { null } else null,
                geminiFrequencyPenalty = if (json.has("gemini_frequencyPenalty")) json.optDouble("gemini_frequencyPenalty").toFloat() else null,
                geminiPresencePenalty = if (json.has("gemini_presencePenalty")) json.optDouble("gemini_presencePenalty").toFloat() else null,
                geminiSeed = if (json.has("gemini_seed")) json.optLong("gemini_seed") else null,
                geminiCandidateCount = if (json.has("gemini_candidateCount")) json.optInt("gemini_candidateCount") else null,
                geminiResponseMimeType = if (json.has("gemini_responseMimeType")) json.optString("gemini_responseMimeType").ifBlank { null } else null,
                geminiResponseSchema = if (json.has("gemini_responseSchema")) json.optString("gemini_responseSchema").ifBlank { null } else null,
                geminiThinkingLevel = if (json.has("gemini_thinkingLevel")) json.optString("gemini_thinkingLevel").ifBlank { null } else null,
                geminiThinkingBudget = if (json.has("gemini_thinkingBudget")) json.optInt("gemini_thinkingBudget") else null,

                minP = if (json.has("min_p")) json.optDouble("min_p").toFloat() else null,
                repetitionPenalty = if (json.has("repetition_penalty")) json.optDouble("repetition_penalty").toFloat() else null,
                typicalP = if (json.has("typical_p")) json.optDouble("typical_p").toFloat() else null,
                tfsZ = if (json.has("tfs_z")) json.optDouble("tfs_z").toFloat() else null,
                mirostat = if (json.has("mirostat")) json.optInt("mirostat") else null,
                mirostatTau = if (json.has("mirostat_tau")) json.optDouble("mirostat_tau").toFloat() else null,
                mirostatEta = if (json.has("mirostat_eta")) json.optDouble("mirostat_eta").toFloat() else null,
                repeatLastN = if (json.has("repeat_last_n")) json.optInt("repeat_last_n") else null,
                numCtx = if (json.has("num_ctx")) json.optInt("num_ctx") else null
            )
        }
    }
}
