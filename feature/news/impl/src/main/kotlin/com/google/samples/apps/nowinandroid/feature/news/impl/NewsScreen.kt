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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.data.repository.NewsFeedError
import com.google.samples.apps.nowinandroid.core.designsystem.component.DynamicAsyncImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import com.google.samples.apps.nowinandroid.core.ui.TrackScreenViewEvent
import com.google.samples.apps.nowinandroid.feature.news.api.R as newsR

@Composable
internal fun NewsScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    NewsScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onArticleClick = onArticleClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewsScreen(
    uiState: NewsUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenViewEvent(screenName = "News")

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when (uiState) {
            is NewsUiState.Loading -> {
                LoadingState()
            }
            is NewsUiState.Success -> {
                NewsFeedContent(
                    articles = uiState.articles,
                    isOffline = uiState.isOffline,
                    refreshFailed = uiState.refreshFailed,
                    onArticleClick = onArticleClick,
                )
            }
            is NewsUiState.Empty -> {
                EmptyState(
                    isOffline = uiState.isOffline,
                    onRefresh = onRefresh,
                )
            }
            is NewsUiState.Error -> {
                ErrorState(
                    message = errorMessage(uiState.error),
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun errorMessage(error: NewsFeedError): String = when (error) {
    is NewsFeedError.NetworkError -> stringResource(id = newsR.string.feature_news_api_error_network)
    is NewsFeedError.ServerError -> stringResource(id = newsR.string.feature_news_api_error_server)
    is NewsFeedError.LocalStorageError -> stringResource(id = newsR.string.feature_news_api_error_storage)
    is NewsFeedError.EmptyResponse -> stringResource(id = newsR.string.feature_news_api_empty_title)
}

@Composable
private fun LoadingState() {
    NiaLoadingWheel(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("news:loading"),
        contentDesc = stringResource(id = newsR.string.feature_news_api_loading),
    )
}

@Composable
private fun EmptyState(
    isOffline: Boolean,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = newsR.string.feature_news_api_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (isOffline) {
            Text(
                text = stringResource(id = newsR.string.feature_news_api_offline_message),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = stringResource(id = newsR.string.feature_news_api_empty_message),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(id = newsR.string.feature_news_api_refresh))
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = newsR.string.feature_news_api_error_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(id = newsR.string.feature_news_api_retry))
        }
    }
}

@Composable
private fun NewsFeedContent(
    articles: List<NewsArticle>,
    isOffline: Boolean,
    refreshFailed: Boolean,
    onArticleClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isOffline) {
            StatusBanner(
                text = stringResource(id = newsR.string.feature_news_api_offline_message),
            )
        } else if (refreshFailed) {
            StatusBanner(
                text = stringResource(id = newsR.string.feature_news_api_refresh_failed_message),
            )
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            contentPadding = PaddingValues(16.dp),
            verticalItemSpacing = 24.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("news:feed"),
        ) {
            items(
                items = articles,
                key = { it.id },
                contentType = { "newsArticle" },
            ) { article ->
                NewsArticleCard(
                    article = article,
                    onClick = { onArticleClick(article.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column {
            val imageUrl = article.imageUrl
            if (imageUrl != null) {
                DynamicAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.source,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
