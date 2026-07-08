package com.example.data.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PublicStorageManagerTest {

    @Test
    fun moveToPublicDownloadsWritesFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "test_audio.mp3")
        FileOutputStream(tempFile).use { it.write("test data".toByteArray()) }

        val uri = PublicStorageManager.moveToPublicDownloads(
            context = context,
            tempFilePath = tempFile.absolutePath,
            fileName = "mscraper_test_audio.mp3",
            mimeType = "audio/mpeg"
        )

        assertNotNull(uri)
        
        // Cleanup MediaStore entry would be good but requires more setup
    }
}
