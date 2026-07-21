package com.example.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        songDao = database.songDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAll() = runTest {
        val song = Song(
            id = "1",
            title = "Title",
            artist = "Artist",
            url = "http://example.com",
            filePath = "/path/to/file",
            duration = "3:00",
            imageUrl = "http://image.com"
        )
        songDao.insert(song)

        val songs = songDao.getAll().first()
        assertEquals(1, songs.size)
        assertEquals(song, songs[0])
    }

    @Test
    fun getOfflineOnlyReturnsDownloaded() = runTest {
        val song1 = Song(
            id = "1",
            title = "Downloaded",
            artist = "Artist",
            url = "url1",
            filePath = "path1",
            duration = "3:00",
            imageUrl = "img1",
            isDownloaded = true
        )
        val song2 = Song(
            id = "2",
            title = "Not Downloaded",
            artist = "Artist",
            url = "url2",
            filePath = "path2",
            duration = "3:00",
            imageUrl = "img2",
            isDownloaded = false
        )
        songDao.insert(song1)
        songDao.insert(song2)

        val offlineSongs = songDao.getOffline().first()
        assertEquals(1, offlineSongs.size)
        assertEquals("1", offlineSongs[0].id)
    }

    @Test
    fun existsByUrl() = runTest {
        val url = "http://example.com/song"
        val song = Song(
            id = "1",
            title = "Title",
            artist = "Artist",
            url = url,
            filePath = "path",
            duration = "3:00",
            imageUrl = "img"
        )
        songDao.insert(song)

        assertTrue(songDao.existsByUrl(url))
    }
}
