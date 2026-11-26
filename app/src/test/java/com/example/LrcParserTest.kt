package com.example

import com.example.data.local.AppPreferences
import com.example.model.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseBilingualLrcAndFindActiveLine() {
        val rawLrc = """
            [00:01.00]First line
            [00:05.50][00:20.00]Repeated chorus <00:05.80>word
            [00:12.25]Second verse
        """.trimIndent()

        val rawTranslated = """
            [00:01.10]第一行
            [00:05.50]重复副歌
            [00:20.00]重复副歌
        """.trimIndent()

        val lines = LrcParser.parse(rawLrc, rawTranslated)
        assertEquals(4, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals("第一行", lines[0].translatedText)

        assertEquals(5500L, lines[1].timeMs)
        assertEquals("Repeated chorus word", lines[1].text)
        assertEquals("重复副歌", lines[1].translatedText)

        assertEquals(12250L, lines[2].timeMs)
        assertNull(lines[2].translatedText)

        assertEquals(20000L, lines[3].timeMs)
        assertEquals("Repeated chorus word", lines[3].text)

        assertEquals(0, LrcParser.findActiveIndex(lines, 500L))
        assertEquals(1, LrcParser.findActiveIndex(lines, 7000L))
        assertEquals(2, LrcParser.findActiveIndex(lines, 15000L))
        assertEquals(3, LrcParser.findActiveIndex(lines, 25000L))
    }

    @Test
    fun normalizeServerUrl() {
        assertEquals("http://192.168.1.5:3000/", AppPreferences.normalizeUrl("192.168.1.5:3000"))
        assertEquals("https://api.example.com/", AppPreferences.normalizeUrl("https://api.example.com/"))
        assertEquals("", AppPreferences.normalizeUrl("   "))
    }
}
