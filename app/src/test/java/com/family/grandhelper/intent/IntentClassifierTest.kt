package com.family.grandhelper.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentClassifierTest {

    private val classifier = IntentClassifier()

    @Test
    fun `classify alarm keywords`() {
        assertEquals(IntentClassifier.IntentType.ALARM, classifier.classify("내일 아침 6시에 알람 맞춰줘"))
        assertEquals(IntentClassifier.IntentType.ALARM, classifier.classify("30분 뒤에 깨워줘"))
        assertEquals(IntentClassifier.IntentType.ALARM, classifier.classify("타이머 5분"))
        assertEquals(IntentClassifier.IntentType.ALARM, classifier.classify("6시에 맞춰줘"))
    }

    @Test
    fun `classify kakaotalk keywords`() {
        assertEquals(IntentClassifier.IntentType.KAKAOTALK, classifier.classify("엄마한테 카톡 보내줘"))
        assertEquals(IntentClassifier.IntentType.KAKAOTALK, classifier.classify("카카오톡으로 메시지 보내줘"))
        assertEquals(IntentClassifier.IntentType.KAKAOTALK, classifier.classify("큰아들한테 문자 보내줘"))
    }

    @Test
    fun `classify navigation keywords`() {
        assertEquals(IntentClassifier.IntentType.NAVIGATION, classifier.classify("서울역까지 네비 틀어줘"))
        assertEquals(IntentClassifier.IntentType.NAVIGATION, classifier.classify("서울역 길 안내해줘"))
        assertEquals(IntentClassifier.IntentType.NAVIGATION, classifier.classify("서울역 가는 길 찾아줘"))
        assertEquals(IntentClassifier.IntentType.NAVIGATION, classifier.classify("서울역 어떻게 가"))
    }

    @Test
    fun `classify call keywords`() {
        assertEquals(IntentClassifier.IntentType.CALL, classifier.classify("큰아들한테 전화 걸어줘"))
        assertEquals(IntentClassifier.IntentType.CALL, classifier.classify("엄마한테 통화해줘"))
        assertEquals(IntentClassifier.IntentType.CALL, classifier.classify("작은아들 연락해줘"))
    }

    @Test
    fun `classify unknown`() {
        assertEquals(IntentClassifier.IntentType.UNKNOWN, classifier.classify("오늘 날씨 어때"))
        assertEquals(IntentClassifier.IntentType.UNKNOWN, classifier.classify(""))
    }
}
