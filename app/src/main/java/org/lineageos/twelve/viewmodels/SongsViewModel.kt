/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.repositories.MediaRepository

class SongsViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : TwelveViewModel(application) {

    val sortingRule = savedStateHandle.getStateFlow(
        "sorting_rule",
        MediaRepository.defaultAudiosSortingRule
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val audios = sortingRule
        .flatMapLatest { mediaRepository.audios(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSortingRule(sortingRule: SortingRule) {
        savedStateHandle["sorting_rule"] = sortingRule
    }
}
