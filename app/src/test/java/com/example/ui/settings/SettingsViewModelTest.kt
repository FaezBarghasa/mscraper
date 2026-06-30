package com.example.ui.settings

import android.app.Application
import com.example.core.MmDlpApi
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mock(SettingsRepository::class.java)
    private val api = mock(MmDlpApi::class.java)

    @Before
    fun setup() {
        whenever(repository.enableQuic).thenReturn(flowOf(true))
        whenever(repository.defaultDownloadFormat).thenReturn(flowOf("MP3"))
        whenever(repository.themeMode).thenReturn(flowOf(ThemeMode.SYSTEM))
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateEnableQuic updates repository and api`() = runTest {
        val viewModel = SettingsViewModel(repository, api)
        
        viewModel.updateEnableQuic(false)
        advanceUntilIdle()

        verify(repository).updateEnableQuic(false)
        verify(api).setNetworkConfig(false)
    }
}
