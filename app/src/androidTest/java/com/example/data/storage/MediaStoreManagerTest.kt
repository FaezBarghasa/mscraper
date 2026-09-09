package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MediaStoreManagerTest {

    private lateinit var context: Context
    private lateinit var mediaStoreManager: MediaStoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mediaStoreManager = MediaStoreManager(context)
    }

    @Test
    fun testMediaStoreLifecycle() = runTest {
        val fileName = "test_song_${System.currentTimeMillis()}.mp3"
        val mimeType = "audio/mpeg"

        // 1. Create pending URI
        val uri = mediaStoreManager.createPendingAudioUri(fileName, mimeType)
        assertNotNull("URI should not be null", uri)
        uri!!

        // 2. Create dummy cache file
        val cacheFile = File(context.cacheDir, "test_cache.mp3")
        FileOutputStream(cacheFile).use { out ->
            out.write("dummy audio data".toByteArray())
        }

        // 3. Write cache file to URI
        mediaStoreManager.writeCacheFileToUri(cacheFile, uri)

        // 4. Finalize file
        mediaStoreManager.finalizeFile(uri)

        // 5. Verify in MediaStore
        val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.MIME_TYPE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            assertTrue("Cursor should have at least one row", cursor.moveToFirst())
            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))
            assertTrue("Filename should match", name.contains("test_song_"))
            assertTrue("MIME type should match", type == mimeType)
        }

        // Cleanup: remove the created file from MediaStore
        context.contentResolver.delete(uri, null, null)
    }
}
