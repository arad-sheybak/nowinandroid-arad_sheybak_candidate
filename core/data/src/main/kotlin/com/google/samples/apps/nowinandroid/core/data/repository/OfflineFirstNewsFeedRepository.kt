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

import com.google.samples.apps.nowinandroid.core.data.model.asEntity
import com.google.samples.apps.nowinandroid.core.database.dao.NewsArticleDao
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import com.google.samples.apps.nowinandroid.core.network.NewsFeedNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Offline-first [NewsFeedRepository]: reads come exclusively from the local data source,
 * while writes happen when a refresh fetches the latest news from the remote source.
 */
internal class OfflineFirstNewsFeedRepository @Inject constructor(
    private val newsFeedNetworkDataSource: NewsFeedNetworkDataSource,
    private val newsArticleDao: NewsArticleDao,
) : NewsFeedRepository {

    override fun observeArticles(): Flow<List<NewsArticle>> =
        newsArticleDao.observeArticles().map { articles ->
            articles.map { it.asExternalModel() }
        }

    override fun observeArticle(id: String): Flow<NewsArticle?> =
        newsArticleDao.observeArticle(id = id).map { article ->
            article?.asExternalModel()
        }

    override suspend fun refresh() {
        val networkArticles = try {
            newsFeedNetworkDataSource.getNewsFeed().values.flatten()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (ioException: IOException) {
            // Connectivity issues, such as being offline.
            throw NewsFeedError.NetworkError(cause = ioException)
        } catch (exception: Exception) {
            // Remote server errors, such as non-2xx responses or malformed payloads.
            throw NewsFeedError.ServerError(cause = exception)
        }

        if (networkArticles.isEmpty()) {
            throw NewsFeedError.EmptyResponse
        }

        try {
            newsArticleDao.upsertArticles(articles = networkArticles.map { it.asEntity() })
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            throw NewsFeedError.LocalStorageError(cause = exception)
        }
    }
}
