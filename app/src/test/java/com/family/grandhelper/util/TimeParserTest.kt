package com.family.grandhelper.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeParserTest {

    @Test
    fun `parse explicit morning time`() {
        val result = TimeParser.parse("내일 아침 6시에 알람 맞춰줘")
        assertEquals(6, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse half hour`() {
        val result = TimeParser.parse("5시 반에 알람")
        assertEquals(5, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `parse afternoon time`() {
        val result = TimeParser.parse("오후 3시에 알람 맞춰줘")
        assertEquals(15, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse relative minutes`() {
        val now = Calendar.getInstance()
        val result = TimeParser.parse("30분 뒤에 깨워줘")
        val expected = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) + 30) % (24 * 60)
        val actual = result.hour * 60 + result.minute
        // Allow 1 minute tolerance for test execution time
        assert(kotlin.math.abs(expected - actual) <= 1) {
            "Expected around ${expected / 60}:${expected % 60}, got ${result.hour}:${result.minute}"
        }
    }

    @Test
    fun `parse relative hours`() {
        val now = Calendar.getInstance()
        val result = TimeParser.parse("한 시간 뒤에 알람")
        val expectedHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        assertEquals(expectedHour, result.hour)
    }

    @Test
    fun `parse evening time`() {
        val result = TimeParser.parse("저녁 7시에 알람")
        assertEquals(19, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse day after tomorrow`() {
        val result = TimeParser.parse("모레 아침 8시에 깨워줘")
        assertEquals(8, result.hour)
        assertEquals(0, result.minute)
        // Check it's 2 days from now
        val now = Calendar.getInstance()
        val dayDiff = result.calendar.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR)
        assertEquals(2, dayDiff)
    }

    @Test
    fun `parse with minutes`() {
        val result = TimeParser.parse("오전 7시 45분에 알람")
        assertEquals(7, result.hour)
        assertEquals(45, result.minute)
    }

    @Test
    fun `parse korean number hour`() {
        val result = TimeParser.parse("내일 여섯시에 깨워줘")
        assertEquals(6, result.hour)
    }

    @Test
    fun `parse night time`() {
        val result = TimeParser.parse("밤 11시에 알람")
        assertEquals(23, result.hour)
    }
}
