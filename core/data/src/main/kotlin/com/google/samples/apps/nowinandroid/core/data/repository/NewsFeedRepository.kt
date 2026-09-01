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

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import kotlinx.coroutines.flow.Flow

/**
 * Errors that can occur while fetching the news feed, so that callers can distinguish
 * between the different failure modes.
 */
sealed class NewsFeedError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The request failed because the device is not connected to the network. */
    class NetworkError(cause: Throwable) : NewsFeedError(
        message = "Network error while fetching the news feed",
        cause = cause,
    )

    /** The remote server returned an error response. */
    class ServerError(cause: Throwable) : NewsFeedError(
        message = "Server error while fetching the news feed",
        cause = cause,
    )

    /** The fetched news could not be stored in the local data source. */
    class LocalStorageError(cause: Throwable) : NewsFeedError(
        message = "Local storage error while storing the news feed",
        cause = cause,
    )

    /** The remote request succeeded but returned no news articles. */
    data object EmptyResponse : NewsFeedError(
        message = "The news feed returned no articles",
    )
}

/**
 * Data layer implementation for [NewsArticle], backed by a remote and a local data source.
 */
interface NewsFeedRepository {

    /**
     * Observes the latest successfully fetched news articles from the local data source.
     */
    fun observeArticles(): Flow<List<NewsArticle>>

    /**
     * Observes a single news article from the local data source, or null if it is not stored.
     */
    fun observeArticle(id: String): Flow<NewsArticle?>

    /**
     * Fetches the latest news from the remote source and stores it in the local data source.
     *
     * @throws [NewsFeedError] when the remote request or the local storage fails.
     */
    suspend fun refresh()
}
