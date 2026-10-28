package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.online.YouTubeOEmbedMetadataProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeOEmbedTest {

    private val validBody = """
        {"title":"Site Safety Briefing: Working at Height","author_name":"WorkDaily Safety",
         "author_url":"https://www.youtube.com/@workdailysafety",
         "type":"video","height":113,"width":200,
         "version":"1.0","provider_name":"YouTube",
         "provider_url":"https://www.youtube.com/",
         "thumbnail_height":360,"thumbnail_width":480,
         "thumbnail_url":"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}
    """.trimIndent()

    @Test
    fun `endpoint encodes the canonical watch url`() {
        val url = YouTubeOEmbed.endpointUrl("dQw4w9WgXcQ")
        assertTrue(url.startsWith("https://www.youtube.com/oembed?url="))
        assertTrue(url.contains("watch%3Fv%3DdQw4w9WgXcQ"))
        assertTrue(url.endsWith("&format=json"))
    }

    @Test
    fun `parses title thumbnail and author`() {
        val metadata = YouTubeOEmbed.parse(validBody)!!
        assertEquals("Site Safety Briefing: Working at Height", metadata.title)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", metadata.thumbnailUrl)
        assertEquals("WorkDaily Safety", metadata.authorName)
    }

    @Test
    fun `decodes escaped characters`() {
        val metadata = YouTubeOEmbed.parse("""{"title":"Line\nBreak \"quoted\" \u00e9"}""")!!
        assertEquals("Line Break \"quoted\" é", metadata.title)
    }

    @Test
    fun `rejects malformed bodies`() {
        assertNull(YouTubeOEmbed.parse(null))
        assertNull(YouTubeOEmbed.parse(""))
        assertNull(YouTubeOEmbed.parse("<html>blocked</html>"))
        assertNull(YouTubeOEmbed.parse("{\"title\":\"\"}"))
        assertNull(YouTubeOEmbed.parse("{\"author_name\":\"x\"}"))
        assertNull(YouTubeOEmbed.parse("{\"title\":\"unterminated"))
    }

    @Test
    fun `ignores non https thumbnails`() {
        val metadata = YouTubeOEmbed.parse("""{"title":"T","thumbnail_url":"http://insecure/x.jpg"}""")!!
        assertNull(metadata.thumbnailUrl)
    }

    @Test
    fun `titles are collapsed and bounded`() {
        assertEquals("a b c", YouTubeOEmbed.sanitizeTitle("  a \n b\t c "))
        val long = "x".repeat(300)
        val sanitized = YouTubeOEmbed.sanitizeTitle(long)
        assertEquals(YouTubeOEmbed.MAX_TITLE_LENGTH, sanitized.length)
        assertTrue(sanitized.endsWith("…"))
    }

    @Test
    fun `provider returns metadata from an injected fetcher`() = runBlocking {
        val provider = YouTubeOEmbedMetadataProvider(fetcher = { validBody })
        val metadata = provider.fetch("dQw4w9WgXcQ")
        assertEquals("Site Safety Briefing: Working at Height", metadata?.title)
    }

    @Test
    fun `provider degrades to null on network failure`() = runBlocking {
        val provider = YouTubeOEmbedMetadataProvider(fetcher = { throw java.io.IOException("offline") })
        assertNull(provider.fetch("dQw4w9WgXcQ"))
    }

    @Test
    fun `provider skips invalid video ids without a request`() = runBlocking {
        var called = false
        val provider = YouTubeOEmbedMetadataProvider(fetcher = { called = true; validBody })
        assertNull(provider.fetch("too-short"))
        assertNull(provider.fetch(""))
        assertTrue(!called)
    }
}
