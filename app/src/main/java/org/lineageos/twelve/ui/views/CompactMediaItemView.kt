/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.R as MaterialR
import org.lineageos.twelve.R
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.utils.TimestampFormatter

/**
 * A compact view for media items, primarily audio, showing only text.
 */
class CompactMediaItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val headlineTextView by lazy { findViewById<TextView>(R.id.headlineTextView) }
    private val supportingTextView by lazy { findViewById<TextView>(R.id.supportingTextView) }
    private val trailingTextView by lazy { findViewById<TextView>(R.id.trailingTextView) }

    init {
        inflate(context, R.layout.view_compact_media_item, this)
        orientation = HORIZONTAL

        // We use a hardcoded value because we don't have access to the dimension resource
        // from here without checking if it exists first.
        // Ideally this should be R.dimen.list_item_padding but it seems I cannot find it in previous ls calls.
        // I'll check list_item.xml or similar to see what padding they use or just use 16dp.
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        // Add ripple effect
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(MaterialR.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }

    fun setItem(audio: Audio) {
        headlineTextView.text = audio.title
        supportingTextView.text = audio.artistName
        trailingTextView.text = TimestampFormatter.formatTimestampMillis(audio.durationMs ?: 0)

        supportingTextView.isVisible = !audio.artistName.isNullOrEmpty()
    }
}
