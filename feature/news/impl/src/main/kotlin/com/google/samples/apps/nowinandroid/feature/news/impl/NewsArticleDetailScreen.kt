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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.designsystem.component.DynamicAsyncImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import com.google.samples.apps.nowinandroid.core.ui.TrackScreenViewEvent
import com.google.samples.apps.nowinandroid.feature.news.api.R as newsR

@Composable
internal fun NewsArticleDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsArticleDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NewsArticleDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun NewsArticleDetailScreen(
    uiState: NewsArticleDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenViewEvent(screenName = "NewsArticleDetail")

    Column(modifier = modifier.fillMaxSize()) {
        NewsArticleDetailTopBar(onBackClick = onBackClick)
        when (uiState) {
            is NewsArticleDetailUiState.Loading -> {
                NiaLoadingWheel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .testTag("news:detail:loading"),
                    contentDesc = stringResource(id = newsR.string.feature_news_api_loading),
                )
            }
            is NewsArticleDetailUiState.Success -> {
                NewsArticleDetailContent(article = uiState.article)
            }
            is NewsArticleDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = newsR.string.feature_news_api_article_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsArticleDetailTopBar(onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = NiaIcons.ArrowBack,
                contentDescription = stringResource(id = newsR.string.feature_news_api_back),
            )
        }
        Text(
            text = stringResource(id = newsR.string.feature_news_api_detail_title),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun NewsArticleDetailContent(article: NewsArticle) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val imageUrl = article.imageUrl
        if (imageUrl != null) {
            DynamicAsyncImage(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        Text(
            text = article.title,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            val sourceIconUrl = article.sourceIconUrl
            if (sourceIconUrl != null) {
                DynamicAsyncImage(
                    imageUrl = sourceIconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = article.source,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Button(
            onClick = { uriHandler.openUri(article.url) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(stringResource(id = newsR.string.feature_news_api_open_article))
        }
    }
}
