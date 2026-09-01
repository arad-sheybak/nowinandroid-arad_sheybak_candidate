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

package com.google.samples.apps.nowinandroid.feature.news.impl.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.feature.news.api.navigation.NewsArticleNavKey
import com.google.samples.apps.nowinandroid.feature.news.api.navigation.NewsNavKey
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsArticleDetailScreen
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsArticleDetailViewModel
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsScreen

fun EntryProviderScope<NavKey>.newsEntry(navigator: Navigator) {
    entry<NewsNavKey> {
        NewsScreen(
            onArticleClick = { articleId ->
                navigator.navigate(NewsArticleNavKey(articleId = articleId))
            },
        )
    }

    entry<NewsArticleNavKey> { key ->
        val viewModel = hiltViewModel<NewsArticleDetailViewModel, NewsArticleDetailViewModel.Factory> {
            it.create(key)
        }
        NewsArticleDetailScreen(
            viewModel = viewModel,
            onBackClick = { navigator.goBack() },
        )
    }
}
