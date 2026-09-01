/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.news.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.NewsFeedError
import com.google.samples.apps.nowinandroid.core.data.repository.NewsFeedRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NewsUiState {
    data object Loading : NewsUiState

    data class Success(
        val articles: List<NewsArticle>,
        val isOffline: Boolean,
        val refreshFailed: Boolean,
    ) : NewsUiState

    data class Empty(
        val isOffline: Boolean,
    ) : NewsUiState

    data class Error(
        val error: NewsFeedError,
    ) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsFeedRepository: NewsFeedRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    /** Whether a refresh is currently in progress, e.g. for the pull-to-refresh indicator. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    /** The last refresh failure, or null if the last refresh succeeded. */
    private val refreshError = MutableStateFlow<NewsFeedError?>(null)

    val uiState: StateFlow<NewsUiState> = combine(
        newsFeedRepository.observeArticles(),
        networkMonitor.isOnline,
        _isRefreshing,
        refreshError,
    ) { articles, isOnline, refreshing, error ->
        when {
            // Show a loading indicator while the very first fetch is in progress.
            articles.isEmpty() && refreshing -> NewsUiState.Loading

            // Keep displaying the locally stored news while refreshing, even if the
            // refresh fails.
            articles.isNotEmpty() -> NewsUiState.Success(
                articles = articles,
                isOffline = !isOnline,
                refreshFailed = error != null,
            )

            error != null && error is NewsFeedError.EmptyResponse -> {
                NewsUiState.Empty(isOffline = false)
            }

            error != null -> NewsUiState.Error(error = error)

            !isOnline -> NewsUiState.Empty(isOffline = true)

            // Online, no cached data and no refresh in progress yet.
            else -> NewsUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NewsUiState.Loading,
    )

    private var refreshJob: Job? = null

    fun refresh() {
        // Avoid starting a refresh while one is already in progress.
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                newsFeedRepository.refresh()
                refreshError.value = null
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (error: NewsFeedError) {
                refreshError.value = error
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    init {
        // Fetch the news from the remote source the first time the feature is opened
        // and no news has been successfully fetched and stored before.
        viewModelScope.launch {
            val hasCachedNews = newsFeedRepository.observeArticles().first().isNotEmpty()
            if (!hasCachedNews) {
                refresh()
            }
        }
    }
}
