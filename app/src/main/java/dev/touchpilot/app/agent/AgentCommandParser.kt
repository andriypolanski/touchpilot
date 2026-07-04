package dev.touchpilot.app.agent

import org.json.JSONObject

object AgentCommandParser {
    fun parse(raw: String): AgentCommand {
        val jsonText = extractJsonObject(raw)
        val json = JSONObject(jsonText)

        val finalAnswer = json.optString("final", "").ifBlank { null }
        val tool = json.optString("tool", "").ifBlank { null }
        val argsJson = json.optJSONObject("args")
        val args = buildMap {
            if (argsJson != null) {
                for (key in argsJson.keys()) {
                    // A JSON null arrives as JSONObject.NULL (a non-null
                    // sentinel), so `?.toString()` does NOT short-circuit —
                    // it yields the literal "null". Treat it as empty/absent,
                    // matching optString()'s handling for `tool`/`final` above.
                    val value = if (argsJson.isNull(key)) "" else argsJson.opt(key)?.toString().orEmpty()
                    put(key, value)
                }
            }
        }

        return AgentCommand(
            tool = tool,
            args = args,
            finalAnswer = finalAnswer
        )
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()

        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
            .find(trimmed)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        // Prefer an object that actually carries a "tool" or "final" key. org.json
        // is lenient about unquoted keys, so a malformed object (e.g. smart-quoted
        // keys) can "parse" into a candidate without the real command keys — try
        // strict first, then the repaired text, before settling for any object.
        findCommandObject(fenced)?.let { return it }
        findCommandObject(trimmed)?.let { return it }
        findCommandObject(fenced?.let(LenientJson::repair))?.let { return it }
        findCommandObject(LenientJson.repair(trimmed))?.let { return it }

        // Last resort: any parseable object (unchanged fallback), strict then repaired.
        findAnyObject(fenced)?.let { return it }
        findAnyObject(trimmed)?.let { return it }
        findAnyObject(fenced?.let(LenientJson::repair))?.let { return it }
        findAnyObject(LenientJson.repair(trimmed))?.let { return it }

        error("Model did not return a JSON object: $raw")
    }

    /** The last parseable object that carries a `tool` or `final` key, if any. */
    private fun findCommandObject(text: String?): String? {
        if (text == null) return null
        return parseObjects(text)
            .lastOrNull { (_, json) -> json.has("tool") || json.has("final") }
            ?.first
    }

    /** The last parseable object of any shape, if any. */
    private fun findAnyObject(text: String?): String? {
        if (text == null) return null
        return parseObjects(text).lastOrNull()?.first
    }

    private fun parseObjects(text: String): List<Pair<String, JSONObject>> {
        val parsedCandidates = mutableListOf<Pair<String, JSONObject>>()
        var i = 0
        while (i < text.length) {
            if (text[i] == '{') {
                val end = findMatchingCloseBrace(text, i)
                if (end < 0) {
                    i++
                    continue
                }
                val candidate = text.substring(i, end + 1)
                runCatching { JSONObject(candidate) }
                    .getOrNull()
                    ?.let { parsedCandidates += candidate to it }
                i = end + 1
            } else {
                i++
            }
        }
        return parsedCandidates
    }

    private fun findMatchingCloseBrace(text: String, startIndex: Int): Int {
        var depth = 0
        var i = startIndex
        var inString = false
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                when (c) {
                    '\\' -> if (i + 1 < text.length) i++
                    '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
            i++
        }
        return -1
    }
}
