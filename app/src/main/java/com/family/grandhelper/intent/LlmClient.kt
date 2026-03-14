package com.family.grandhelper.intent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * LLM 폴백 클라이언트.
 * 로컬 키워드 매칭이 UNKNOWN일 때만 호출.
 *
 * 지원 백엔드:
 * - Ollama (Mac Mini via Tailscale/Cloudflare Tunnel)
 * - Claude API (Haiku)
 * - OpenAI API (GPT-4o-mini 등)
 */
class LlmClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val apiType: ApiType = ApiType.OLLAMA,
    private val apiKey: String = "",
    private val model: String = DEFAULT_MODEL
) {

    companion object {
        // TODO: 실제 서버 주소로 변경
        const val DEFAULT_BASE_URL = "http://100.100.100.100:11434"
        const val DEFAULT_MODEL = "llama3.1:8b"
        private const val CONNECT_TIMEOUT = 5000
        private const val READ_TIMEOUT = 15000
    }

    enum class ApiType { OLLAMA, CLAUDE, OPENAI }

    data class LlmResult(
        val intent: String,
        val params: Map<String, String>
    )

    suspend fun classify(transcript: String): LlmResult? = withContext(Dispatchers.IO) {
        try {
            when (apiType) {
                ApiType.OLLAMA -> classifyWithOllama(transcript)
                ApiType.CLAUDE -> classifyWithClaude(transcript)
                ApiType.OPENAI -> classifyWithOpenAI(transcript)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun classifyWithOllama(transcript: String): LlmResult? {
        val url = URL("$baseUrl/api/generate")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        val prompt = buildPrompt(transcript)
        val body = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("stream", false)
            put("format", "json")
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        if (conn.responseCode != 200) return null

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val responseText = json.optString("response", "")
        return parseResponse(responseText)
    }

    private fun classifyWithClaude(transcript: String): LlmResult? {
        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
        }

        val prompt = buildPrompt(transcript)
        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 256)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        if (conn.responseCode != 200) return null

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val content = json.getJSONArray("content").getJSONObject(0).getString("text")
        return parseResponse(content)
    }

    private fun classifyWithOpenAI(transcript: String): LlmResult? {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
        }

        val prompt = buildPrompt(transcript)
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 256)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a Korean voice command classifier. Always respond in JSON only.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        if (conn.responseCode != 200) return null

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
        return parseResponse(content)
    }

    private fun buildPrompt(transcript: String): String {
        return """다음 한국어 음성 명령을 분석하세요.
명령: "$transcript"

가능한 의도: alarm, call, navigation, unknown
- alarm: 알람, 타이머, 깨워달라는 요청
- call: 전화 걸기 요청
- navigation: 길 안내, 네비게이션 요청

JSON으로만 응답하세요:
{"intent": "alarm|call|navigation|unknown", "params": {"recipient": "대상", "destination": "목적지", "hour": "시", "minute": "분"}}

해당하지 않는 params 필드는 빈 문자열로 두세요."""
    }

    private fun parseResponse(text: String): LlmResult? {
        return try {
            // JSON 부분만 추출 (LLM이 추가 텍스트를 붙일 수 있음)
            val jsonStr = text.trim().let { t ->
                val start = t.indexOf('{')
                val end = t.lastIndexOf('}')
                if (start >= 0 && end > start) t.substring(start, end + 1) else t
            }

            val json = JSONObject(jsonStr)
            val intent = json.optString("intent", "unknown")
            val params = mutableMapOf<String, String>()
            json.optJSONObject("params")?.let { p ->
                for (key in p.keys()) {
                    val value = p.optString(key, "")
                    if (value.isNotBlank()) {
                        params[key] = value
                    }
                }
            }
            LlmResult(intent, params)
        } catch (e: Exception) {
            null
        }
    }

    fun toIntentType(result: LlmResult): IntentClassifier.IntentType {
        return when (result.intent.lowercase()) {
            "alarm" -> IntentClassifier.IntentType.ALARM
            "call" -> IntentClassifier.IntentType.CALL
            "navigation" -> IntentClassifier.IntentType.NAVIGATION
            else -> IntentClassifier.IntentType.UNKNOWN
        }
    }
}
