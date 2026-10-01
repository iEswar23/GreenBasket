package io.github.ieswar23.greenbasket.domain.model

data class Banner(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val startColor: Int,
    val endColor: Int,
    val cta: String,
    val categoryId: String,
)
