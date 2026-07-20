package com.example.ui.search

import com.example.core.MmDlpApi
import com.example.core.Track
import com.example.model.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class MockMmDlpApi : MmDlpApi {
        var callCount = 0
        override suspend fun search(query: String, source: SearchSource): List<Track> {
            callCount++
            return emptyList()
        }

        override suspend fun exportPlaylistJson(playlistName: String, tracks: List<com.example.model.Track>): String = ""
        override suspend fun exportPlaylistXml(playlistName: String, tracks: List<com.example.model.Track>): String = ""
        override suspend fun importPlaylistJson(json: String): Pair<String, List<com.example.model.Track>> = "" to emptyList()
        override fun setNetworkConfig(enableQuic: Boolean) {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchQuery debounces correctly`() = runTest {
        val mockApi = MockMmDlpApi()
        val viewModel = SearchViewModel(mockApi)
        
        // Advance to clear any initial flow emissions
        advanceTimeBy(1000)
        mockApi.callCount = 0 // Reset in case something was called

        viewModel.updateSearchQuery("a")
        runCurrent()
        viewModel.updateSearchQuery("ab")
        runCurrent()
        viewModel.updateSearchQuery("abc")
        runCurrent()

        advanceTimeBy(200)
        assertEquals(0, mockApi.callCount)

        advanceTimeBy(200)
        assertEquals(1, mockApi.callCount)
    }
}
