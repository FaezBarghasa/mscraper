package com.example.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.core.MmDlpApi
import com.example.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlaylistHandlerTest {

    @Test
    fun exportLogicWritesToOutputStream() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockApi = mock(MmDlpApi::class.java)
        
        // We need a real URI that we can write to for SAF test, 
        // or a mock that works in instrumented test.
        // Mocking Uri in instrumented test might still be tricky.
        // Let's use a file provider URI or just a file.
        
        val testFile = File(context.cacheDir, "test_export.json")
        val uri = Uri.fromFile(testFile)

        `when`(mockApi.exportPlaylistJson(anyString(), anyList())).thenReturn("{\"test\": \"data\"}")

        val handler = PlaylistExportHandler(
            context = context,
            scope = this,
            api = mockApi,
            onExportResult = { _, _ -> }
        )

        val pendingDataField = handler.javaClass.getDeclaredField("pendingExportData")
        pendingDataField.isAccessible = true
        pendingDataField.set(handler, "Test" to listOf<Track>())

        handler.onResult(uri)
        
        assertTrue(testFile.exists())
        assertTrue(testFile.readText().contains("test"))
    }
}
