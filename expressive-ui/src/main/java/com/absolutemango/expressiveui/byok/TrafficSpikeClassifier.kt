package com.absolutemango.expressiveui.byok

import java.net.SocketTimeoutException

enum class RetryPhase {
    COUNTDOWN,
    SENDING,
    LOADING
}

sealed interface SpikeRetryState {
    object Idle : SpikeRetryState
    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int = 10,
        val secondsRemaining: Int,
        val phase: RetryPhase = RetryPhase.COUNTDOWN,
        val errorSnippet: String = "",
        val isBackupKeyEngaged: Boolean = false
    ) : SpikeRetryState
    data class Exhausted(val finalError: String) : SpikeRetryState
}

sealed interface SpikeClassification {
    data class TransientSpike(
        val statusCode: Int,
        val retryAfterMs: Long?,
        val reason: String
    ) : SpikeClassification

    data class TerminalError(
        val statusCode: Int?,
        val error: String,
        val isAuthOrBilling: Boolean
    ) : SpikeClassification

    object Ignore : SpikeClassification
}

object TrafficSpikeClassifier {
    fun classify(throwable: Throwable): SpikeClassification {
        return when (throwable) {
            is QuotaExhaustedException -> {
                val retryAfterMs = throwable.retryAfter?.let { UniversalChatDispatcher.parseRetryAfter(it) }
                SpikeClassification.TransientSpike(
                    statusCode = throwable.code,
                    retryAfterMs = retryAfterMs,
                    reason = throwable.message ?: "Rate limit reached"
                )
            }
            is HttpException -> {
                when (throwable.code) {
                    429 -> SpikeClassification.TransientSpike(429, null, throwable.message ?: "Rate limit reached")
                    503 -> SpikeClassification.TransientSpike(503, null, throwable.message ?: "Service temporarily unavailable")
                    529 -> SpikeClassification.TransientSpike(529, null, throwable.message ?: "Provider site overloaded")
                    400, 401, 403, 404, 422 -> {
                        SpikeClassification.TerminalError(
                            statusCode = throwable.code,
                            error = throwable.message ?: "Client error",
                            isAuthOrBilling = throwable.code == 401 || throwable.code == 403
                        )
                    }
                    else -> classifyByMessage(throwable.message)
                }
            }
            is StreamTimeoutException, is SocketTimeoutException -> {
                SpikeClassification.TransientSpike(0, null, throwable.message ?: "Stream connection timed out")
            }
            else -> classifyByMessage(throwable.message)
        }
    }

    private fun classifyByMessage(rawMessage: String?): SpikeClassification {
        val msg = rawMessage?.lowercase() ?: ""
        val isTransient = msg.contains("overloaded") ||
                msg.contains("traffic") ||
                msg.contains("capacity") ||
                msg.contains("rate_limit") ||
                msg.contains("rate limit") ||
                msg.contains("resource_exhausted") ||
                msg.contains("too many requests") ||
                msg.contains("server busy") ||
                msg.contains("timed out") ||
                msg.contains("timeout")

        val isTerminal = msg.contains("credit balance") ||
                msg.contains("insufficient_quota") ||
                msg.contains("insufficient credits") ||
                msg.contains("context length") ||
                msg.contains("invalid api key") ||
                msg.contains("unauthorized") ||
                msg.contains("payment required")

        return when {
            isTerminal -> SpikeClassification.TerminalError(
                statusCode = null,
                error = rawMessage ?: "Unrecoverable quota or configuration error",
                isAuthOrBilling = true
            )
            isTransient -> SpikeClassification.TransientSpike(
                statusCode = 0,
                retryAfterMs = null,
                reason = rawMessage ?: "Upstream congestion"
            )
            else -> SpikeClassification.Ignore
        }
    }
}
