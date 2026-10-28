package com.synfusion.attendance.live.watchparty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeLinkParserTest {

    @Test
    fun `parses standard watch url`() {
        val result = YouTubeLinkParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(result is YouTubeParseResult.Success)
        val selection = (result as YouTubeParseResult.Success).selection
        assertEquals("dQw4w9WgXcQ", selection.videoId)
        assertNull(selection.playlistId)
        assertTrue(selection.isVideoPlayable)
    }

    @Test
    fun `parses watch url with playlist keeps both ids`() {
        val result = YouTubeLinkParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890abcdef")
        val selection = (result as YouTubeParseResult.Success).selection
        assertEquals("dQw4w9WgXcQ", selection.videoId)
        assertEquals("PL1234567890abcdef", selection.playlistId)
    }

    @Test
    fun `parses short shared links`() {
        val result = YouTubeLinkParser.parse("https://youtu.be/dQw4w9WgXcQ?si=abc")
        assertEquals("dQw4w9WgXcQ", (result as YouTubeParseResult.Success).selection.videoId)
    }

    @Test
    fun `parses shorts live embed and mobile urls`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeLinkParser.parseVideoId("https://youtube.com/shorts/dQw4w9WgXcQ")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeLinkParser.parseVideoId("https://www.youtube.com/live/dQw4w9WgXcQ")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeLinkParser.parseVideoId("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeLinkParser.parseVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun `parses youtube music links as official youtube videos`() {
        val result = YouTubeLinkParser.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ")
        val selection = (result as YouTubeParseResult.Success).selection
        assertEquals("dQw4w9WgXcQ", selection.videoId)
        assertEquals("RDAMVMdQw4w9WgXcQ", selection.playlistId)
    }

    @Test
    fun `accepts bare video and playlist ids`() {
        assertEquals("dQw4w9WgXcQ", YouTubeLinkParser.parseVideoId("dQw4w9WgXcQ"))
        assertEquals("PL1234567890abcdef", YouTubeLinkParser.parsePlaylistId("PL1234567890abcdef"))
    }

    @Test
    fun `playlist only url is not directly video playable`() {
        val selection = (YouTubeLinkParser.parse("https://www.youtube.com/playlist?list=PL1234567890abcdef") as YouTubeParseResult.Success).selection
        assertTrue(selection.isPlaylistOnly)
        assertFalse(selection.isVideoPlayable)
    }

    @Test
    fun `rejects non youtube hosts even when they contain a v parameter`() {
        val result = YouTubeLinkParser.parse("https://evil.example.com/watch?v=dQw4w9WgXcQ")
        assertTrue(result is YouTubeParseResult.Failure)
        assertEquals(YouTubeParseError.UNSUPPORTED_HOST, (result as YouTubeParseResult.Failure).error)
        assertNull(YouTubeLinkParser.parseVideoId("https://evil.example.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `rejects javascript and file schemes`() {
        assertTrue(YouTubeLinkParser.parse("javascript:alert(1)") is YouTubeParseResult.Failure)
        assertTrue(YouTubeLinkParser.parse("file:///sdcard/video.mp4") is YouTubeParseResult.Failure)
    }

    @Test
    fun `rejects malformed and empty input`() {
        assertTrue(YouTubeLinkParser.parse("") is YouTubeParseResult.Failure)
        assertTrue(YouTubeLinkParser.parse("   ") is YouTubeParseResult.Failure)
        assertTrue(YouTubeLinkParser.parse("https://www.youtube.com/watch?v=tooshort") is YouTubeParseResult.Failure)
        assertTrue(YouTubeLinkParser.parse("https://youtu.be/") is YouTubeParseResult.Failure)
    }

    @Test
    fun `player variables keep official player controls and inline playback`() {
        val selection = (YouTubeLinkParser.parse("https://youtu.be/dQw4w9WgXcQ") as YouTubeParseResult.Success).selection
        val vars = YouTubeLinkParser.playerVariables(selection, "https://www.youtube.com")
        assertEquals("1", vars["playsinline"])
        assertEquals("1", vars["controls"])
        assertEquals("1", vars["enablejsapi"])
        assertEquals("0", vars["rel"])
        assertEquals("https://www.youtube.com", vars["origin"])
    }

    @Test
    fun `thumbnail url points at youtube image cdn`() {
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            YouTubeLinkParser.thumbnailUrl("dQw4w9WgXcQ")
        )
    }
}
