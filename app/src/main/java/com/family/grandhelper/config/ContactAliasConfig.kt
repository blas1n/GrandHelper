package com.family.grandhelper.config

import android.content.Context
import org.json.JSONObject
import java.io.File

object ContactAliasConfig {

    private var aliases: Map<String, String> = emptyMap()

    /**
     * assets/contact_aliases.json에서 별명 매핑을 로드한다.
     * JSON 형식: {"엄마": "김OO", "큰아들": "이OO", ...}
     */
    fun load(context: Context) {
        aliases = try {
            val file = File(context.filesDir, "contact_aliases.json")
            val jsonStr = if (file.exists()) {
                file.readText()
            } else {
                // 최초 실행 시 assets에서 복사
                context.assets.open("contact_aliases.json").bufferedReader().use { it.readText() }
                    .also { file.writeText(it) }
            }
            val json = JSONObject(jsonStr)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun resolve(alias: String): String? = aliases[alias]

    fun allAliases(): Set<String> = aliases.keys

    fun findMatchingAlias(text: String): String? {
        return aliases.keys
            .sortedByDescending { it.length }
            .firstOrNull { text.contains(it) }
    }
}
