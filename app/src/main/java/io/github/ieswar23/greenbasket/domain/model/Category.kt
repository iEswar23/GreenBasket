package io.github.ieswar23.greenbasket.domain.model

data class Category(
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val tint: Int,
    val productCount: Int = 0,
)
