package com.example.ui.playlist

import com.example.core.ffi.AudioSource
import com.example.core.ffi.MmDlpApi
import com.example.core.ffi.Playlist
import com.example.core.ffi.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistHandlerTest {
    @Test
    fun testPlaylistJsonExportAndImport() {
        val ffiApi = MmDlpApi()
        val track = Track(
            id = "t1",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            sourceUrl = "https://example.com/m83",
            duration = 243uL
        )
        val originalPlaylist = Playlist(
            id = "pl1",
            name = "Synthwave Classics",
            description = "Best 80s inspired tracks",
            tracks = listOf(track),
            source = AudioSource.YOU_TUBE_MUSIC
        )

        val exportedJson = ffiApi.exportPlaylistJson(originalPlaylist)
        assertTrue(exportedJson.contains("Synthwave Classics"))
        assertTrue(exportedJson.contains("Midnight City"))

        val importedPlaylist = ffiApi.importPlaylistJson(exportedJson)
        assertEquals(originalPlaylist.name, importedPlaylist.name)
        assertEquals(1, importedPlaylist.tracks.size)
        assertEquals("Midnight City", importedPlaylist.tracks[0].title)
        assertEquals("M83", importedPlaylist.tracks[0].artist)
    }

    @Test
    fun testPlaylistXmlExport() {
        val ffiApi = MmDlpApi()
        val track = Track(
            id = "t2",
            title = "Days of Thunder",
            artist = "The Midnight",
            album = "Days of Thunder",
            sourceUrl = "https://example.com/midnight",
            duration = 315uL
        )
        val playlist = Playlist(
            id = "pl2",
            name = "Retrowave",
            description = null,
            tracks = listOf(track),
            source = AudioSource.YOU_TUBE_MUSIC
        )

        val exportedXml = ffiApi.exportPlaylistXml(playlist)
        assertTrue(exportedXml.contains("<playlist>"))
        assertTrue(exportedXml.contains("Retrowave"))
        assertTrue(exportedXml.contains("Days of Thunder"))
    }
}
