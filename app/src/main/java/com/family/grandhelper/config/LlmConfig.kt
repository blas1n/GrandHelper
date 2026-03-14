package com.family.grandhelper.config

import android.content.Context
import com.family.grandhelper.intent.LlmClient
import org.json.JSONObject
import java.io.File

object LlmConfig {

    private var apiType: LlmClient.ApiType = LlmClient.ApiType.OPENAI
    private var apiKey: String = ""
    private var model: String = ""
    private var baseUrl: String? = null

    fun load(context: Context) {
        try {
            val file = File(context.filesDir, "llm_config.json")
            val jsonStr = if (file.exists()) {
                file.readText()
            } else {
                context.assets.open("llm_config.json").bufferedReader().use { it.readText() }
                    .also { file.writeText(it) }
            }
            val json = JSONObject(jsonStr)
            apiType = when (json.optString("api_type", "openai").lowercase()) {
                "claude" -> LlmClient.ApiType.CLAUDE
                "ollama" -> LlmClient.ApiType.OLLAMA
                else -> LlmClient.ApiType.OPENAI
            }
            apiKey = json.optString("api_key", "")
            model = json.optString("model", "")
            baseUrl = json.optString("base_url", "").ifEmpty { null }
        } catch (_: Exception) {
            // 설정 파일이 없거나 파싱 실패 시 LLM 비활성화
        }
    }

    fun createClient(): LlmClient? {
        if (apiKey.isEmpty() && apiType != LlmClient.ApiType.OLLAMA) return null
        if (model.isEmpty()) return null

        return if (baseUrl != null) {
            LlmClient(
                baseUrl = baseUrl!!,
                apiType = apiType,
                model = model,
                apiKey = apiKey
            )
        } else {
            LlmClient(
                apiType = apiType,
                apiKey = apiKey,
                model = model
            )
        }
    }
}
