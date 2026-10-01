package io.github.ieswar23.greenbasket.ui.common

import androidx.core.view.isVisible
import io.github.ieswar23.greenbasket.databinding.LayoutEmptyStateBinding

object EmptyStateBinder {
    fun bind(
        binding: LayoutEmptyStateBinding,
        emoji: String,
        title: CharSequence,
        message: CharSequence,
        actionText: CharSequence? = null,
        onAction: (() -> Unit)? = null,
    ) {
        binding.emptyEmoji.text = emoji
        binding.emptyTitle.text = title
        binding.emptyMessage.text = message
        binding.emptyAction.isVisible = actionText != null
        binding.emptyAction.text = actionText
        binding.emptyAction.setOnClickListener { onAction?.invoke() }
    }
}
