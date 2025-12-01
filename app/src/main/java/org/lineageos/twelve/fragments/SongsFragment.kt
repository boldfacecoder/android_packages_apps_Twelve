/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.setProgressCompat
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.ui.recyclerview.DisplayAwareGridLayoutManager
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.views.CompactMediaItemView
import org.lineageos.twelve.ui.views.HorizontalMediaItemView
import org.lineageos.twelve.ui.views.MediaItemGridItem
import org.lineageos.twelve.ui.views.SortingChip
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.SongsViewModel

/**
 * View all songs.
 */
class SongsFragment : Fragment(R.layout.fragment_songs) {
    // View models
    private val viewModel by viewModels<SongsViewModel>()

    // Views
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val sortingChip by getViewProperty<SortingChip>(R.id.sortingChip)
    private val viewModeToggleGroup by getViewProperty<MaterialButtonToggleGroup>(R.id.viewModeToggleGroup)

    private enum class ViewMode {
        LIST,
        LIST_WITH_ART,
        GRID
    }

    private var currentViewMode = ViewMode.LIST_WITH_ART

    // Recyclerview adapters
    private val gridAdapter by lazy {
        object : SimpleListAdapter<Audio, MediaItemGridItem>(
            UniqueItemDiffCallback(),
            ::MediaItemGridItem,
        ) {
            override fun ViewHolder.onBindView(item: Audio) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_now_playing,
                        NowPlayingFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    private val listWithArtAdapter by lazy {
        object : SimpleListAdapter<Audio, HorizontalMediaItemView>(
            UniqueItemDiffCallback(),
            ::HorizontalMediaItemView,
        ) {
            override fun ViewHolder.onBindView(item: Audio) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_now_playing,
                        NowPlayingFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    private val listAdapter by lazy {
        object : SimpleListAdapter<Audio, CompactMediaItemView>(
            UniqueItemDiffCallback(),
            ::CompactMediaItemView,
        ) {
            override fun ViewHolder.onBindView(item: Audio) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_now_playing,
                        NowPlayingFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortingChip.setSortingStrategies(
            sortedMapOf(
                SortingStrategy.NAME to R.string.sort_by_title,
                SortingStrategy.ARTIST_NAME to R.string.sort_by_artist_name,
                SortingStrategy.ALBUM_NAME to R.string.sort_by_album_name,
                SortingStrategy.DURATION to R.string.sort_by_duration,
                SortingStrategy.DATE_ADDED to R.string.sort_by_date_added,
                SortingStrategy.CREATION_DATE to R.string.sort_by_release_date,
                SortingStrategy.MODIFICATION_DATE to R.string.sort_by_last_modified,
            )
        )
        sortingChip.setOnSortingRuleSelectedListener {
            viewModel.setSortingRule(it)
        }

        viewModeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.viewModeList -> setViewMode(ViewMode.LIST)
                    R.id.viewModeListWithArt -> setViewMode(ViewMode.LIST_WITH_ART)
                    R.id.viewModeGrid -> setViewMode(ViewMode.GRID)
                }
            }
        }

        // Default View Mode
        viewModeToggleGroup.check(R.id.viewModeListWithArt)
        setViewMode(ViewMode.LIST_WITH_ART)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        recyclerView.layoutManager = null
        super.onDestroyView()
    }

    private fun setViewMode(mode: ViewMode) {
        currentViewMode = mode
        // Context might be null here if view is destroyed, but we are inside onViewCreated or called safely
        val context = context ?: return

        when (mode) {
            ViewMode.LIST -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = listAdapter
            }
            ViewMode.LIST_WITH_ART -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = listWithArtAdapter
            }
            ViewMode.GRID -> {
                recyclerView.layoutManager = DisplayAwareGridLayoutManager(context, 2)
                recyclerView.adapter = gridAdapter
            }
        }

        // Re-submit list if data is already loaded
        val currentList = when (val result = viewModel.audios.value) {
            is FlowResult.Success -> result.data
            else -> emptyList()
        }

        if (currentList.isNotEmpty()) {
             when (mode) {
                ViewMode.LIST -> listAdapter.submitList(currentList)
                ViewMode.LIST_WITH_ART -> listWithArtAdapter.submitList(currentList)
                ViewMode.GRID -> gridAdapter.submitList(currentList)
            }
        }
    }

    private suspend fun loadData() {
        coroutineScope {
            launch {
                viewModel.audios.collectLatest {
                    linearProgressIndicator.setProgressCompat(it)

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            val data = it.data
                             when (currentViewMode) {
                                ViewMode.LIST -> listAdapter.submitList(data)
                                ViewMode.LIST_WITH_ART -> listWithArtAdapter.submitList(data)
                                ViewMode.GRID -> gridAdapter.submitList(data)
                            }

                            val isEmpty = data.isEmpty()
                            recyclerView.isVisible = !isEmpty
                            noElementsLinearLayout.isVisible = isEmpty
                        }

                        is FlowResult.Error -> {
                            Log.e(
                                LOG_TAG,
                                "Failed to load songs, error: ${it.error}",
                                it.throwable
                            )

                             when (currentViewMode) {
                                ViewMode.LIST -> listAdapter.submitList(emptyList())
                                ViewMode.LIST_WITH_ART -> listWithArtAdapter.submitList(emptyList())
                                ViewMode.GRID -> gridAdapter.submitList(emptyList())
                            }

                            recyclerView.isVisible = false
                            noElementsLinearLayout.isVisible = true
                        }
                    }
                }
            }

            launch {
                viewModel.sortingRule.collectLatest {
                    sortingChip.setSortingRule(it)
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = SongsFragment::class.simpleName!!
    }
}
