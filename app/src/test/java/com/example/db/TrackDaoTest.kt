package com.example.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrackDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        trackDao = database.trackDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleTrack(id: String, title: String, isFav: Boolean = false, playCount: Int = 0): TrackEntity {
        return TrackEntity(
            id = id,
            title = title,
            artist = "Test Artist",
            album = "Test Album",
            albumArtist = "Test Artist",
            genre = "Electronic",
            year = 2024,
            trackNumber = 1,
            discNumber = 1,
            duration = 180,
            bitrate = 320,
            sampleRate = 44100,
            format = "MP3",
            filePath = "/path/to/$id.mp3",
            fileSize = 5000000L,
            artworkPath = null,
            playCount = playCount,
            lastPlayed = null,
            dateAdded = System.currentTimeMillis(),
            isFavorite = isFav,
            rating = 5.0f,
            customTags = null
        )
    }

    @Test
    fun insertAndGetAllTracks() = runTest {
        val track = createSampleTrack("track_1", "Test Song")
        trackDao.insertTrack(track)

        val tracks = trackDao.getAllTracks().first()
        assertEquals(1, tracks.size)
        assertEquals(track, tracks[0])
    }

    @Test
    fun getTrackByIdAndDelete() = runTest {
        val track = createSampleTrack("track_2", "Delete Me")
        trackDao.insertTrack(track)

        val fetched = trackDao.getTrackById("track_2")
        assertEquals("Delete Me", fetched?.title)

        trackDao.deleteTrackById("track_2")
        val deleted = trackDao.getTrackById("track_2")
        assertNull(deleted)
    }

    @Test
    fun updateFavoriteAndPlayCount() = runTest {
        val track = createSampleTrack("track_3", "Fav Track")
        trackDao.insertTrack(track)

        trackDao.updateFavoriteStatus("track_3", true)
        trackDao.incrementPlayCount("track_3", 123456L)

        val updated = trackDao.getTrackById("track_3")
        assertEquals(true, updated?.isFavorite)
        assertEquals(1, updated?.playCount)
        assertEquals(123456L, updated?.lastPlayed)
    }
}
