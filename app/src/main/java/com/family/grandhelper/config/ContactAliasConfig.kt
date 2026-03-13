package com.family.grandhelper.config

object ContactAliasConfig {

    // 가족이 미리 설정해두는 별명 → 실제 연락처 이름 매핑
    // TODO: 실제 가족 관계에 맞게 수정
    private val aliases = mapOf(
        "엄마" to "김순자",
        "아빠" to "김영수",
        "큰아들" to "김민수",
        "작은아들" to "김준호",
        "큰며느리" to "박지현",
        "작은며느리" to "이수연",
        "딸" to "김수연",
        "사위" to "정태호",
        "손녀" to "김하은",
        "손자" to "김하준",
    )

    fun resolve(alias: String): String? = aliases[alias]

    fun allAliases(): Set<String> = aliases.keys

    fun findMatchingAlias(text: String): String? {
        // 긴 별명부터 매칭 (e.g., "큰며느리" vs "며느리")
        return aliases.keys
            .sortedByDescending { it.length }
            .firstOrNull { text.contains(it) }
    }
}
