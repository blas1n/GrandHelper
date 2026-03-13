package com.family.grandhelper.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class ParameterExtractorTest {

    private val extractor = ParameterExtractor()

    @Test
    fun `extract alarm parameters`() {
        val result = extractor.extractAlarm("내일 아침 6시에 알람 맞춰줘")
        assertEquals(6, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `extract contact from kakaotalk`() {
        val result = extractor.extractKakaoTalk("엄마한테 내일 간다고 카톡 보내줘")
        assertEquals("엄마", result.contactAlias)
        assertEquals("내일 간다", result.message)
    }

    @Test
    fun `extract contact with 에게 postposition`() {
        val result = extractor.extractCall("큰아들에게 전화 걸어줘")
        assertEquals("큰아들", result.contactAlias)
    }

    @Test
    fun `extract destination`() {
        val result = extractor.extractNavigation("서울역까지 네비 틀어줘")
        assertEquals("서울역", result.destination)
    }

    @Test
    fun `extract destination with 길 안내`() {
        val result = extractor.extractNavigation("서울역까지 길 안내해줘")
        assertEquals("서울역", result.destination)
    }

    @Test
    fun `extract kakaotalk message with 다고`() {
        val result = extractor.extractKakaoTalk("큰아들한테 오늘 늦는다고 카톡 보내줘")
        assertEquals("큰아들", result.contactAlias)
        assertEquals("오늘 늦는다", result.message)
    }
}
