package com.family.grandhelper.util

object KoreanTextUtil {

    private val postpositions = listOf(
        "이한테", "한테서", "에게서", "한테다", "에다가",
        "한테", "에게", "보고", "더러", "께",
    )

    /**
     * "엄마한테" → "엄마", "큰아들에게" → "큰아들"
     */
    fun stripPostposition(text: String): String {
        var result = text.trim()
        for (pp in postpositions) {
            if (result.endsWith(pp)) {
                result = result.dropLast(pp.length)
                break
            }
        }
        return result
    }

    /**
     * 받침 유무에 따라 적절한 조사 선택
     * e.g., addParticle("엄마", "에게", "한테") → "엄마에게"
     */
    fun addParticle(word: String, particleWithBatchim: String, particleWithout: String): String {
        if (word.isBlank()) return word
        val lastChar = word.last()
        return if (hasBatchim(lastChar)) {
            "$word$particleWithBatchim"
        } else {
            "$word$particleWithout"
        }
    }

    /**
     * 한글 마지막 글자에 받침이 있는지 확인
     */
    private fun hasBatchim(char: Char): Boolean {
        if (char.code < 0xAC00 || char.code > 0xD7A3) return false
        return (char.code - 0xAC00) % 28 != 0
    }
}
