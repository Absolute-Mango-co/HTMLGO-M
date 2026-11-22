package com.absolutemango.expressiveui.byok

import com.absolutemango.expressiveui.ForgeMessage

object AnthropicMessageSanitizer {

    const val DEFAULT_CONTINUATION_ANCHOR = "Continue the previous generation:"

    /**
     * Non-destructive projection of canonical [ForgeMessage] list for Anthropic API:
     * 1. Extracts system instructions into a separate system string.
     * 2. Maps "model" role to "assistant".
     * 3. Merges consecutive messages of the same role with "\n\n".
     * 4. Synthesizes a leading user turn anchor if the dialogue starts with an assistant turn.
     *
     * @return Pair of optional system instruction and sanitized List of (role, content).
     */
    fun sanitize(messages: List<ForgeMessage>): Pair<String?, List<Pair<String, String>>> {
        val systemMessages = mutableListOf<String>()
        val nonSystemMessages = mutableListOf<Pair<String, String>>()

        for (msg in messages) {
            val trimmedContent = msg.content.trim()
            if (trimmedContent.isEmpty()) continue

            when (msg.role.lowercase()) {
                "system" -> {
                    systemMessages.add(trimmedContent)
                }
                "model", "assistant" -> {
                    nonSystemMessages.add(Pair("assistant", trimmedContent))
                }
                else -> { // "user" and any others
                    nonSystemMessages.add(Pair("user", trimmedContent))
                }
            }
        }

        val systemInstruction = if (systemMessages.isNotEmpty()) {
            systemMessages.joinToString("\n\n")
        } else {
            null
        }

        if (nonSystemMessages.isEmpty()) {
            return Pair(systemInstruction, emptyList())
        }

        // Strict role alternation: merge consecutive identical roles
        val mergedList = mutableListOf<Pair<String, String>>()
        for (turn in nonSystemMessages) {
            if (mergedList.isEmpty()) {
                mergedList.add(turn)
            } else {
                val lastIndex = mergedList.size - 1
                val lastTurn = mergedList[lastIndex]
                if (lastTurn.first == turn.first) {
                    mergedList[lastIndex] = Pair(lastTurn.first, "${lastTurn.second}\n\n${turn.second}")
                } else {
                    mergedList.add(turn)
                }
            }
        }

        // Ensure dialogue starts with user turn: prepend anchor if leading turn is assistant
        if (mergedList.isNotEmpty() && mergedList.first().first == "assistant") {
            mergedList.add(0, Pair("user", DEFAULT_CONTINUATION_ANCHOR))
        }

        return Pair(systemInstruction, mergedList)
    }
}
