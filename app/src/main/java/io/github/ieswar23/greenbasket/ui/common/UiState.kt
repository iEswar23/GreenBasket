package io.github.ieswar23.greenbasket.ui.common

/** Generic screen state used by list screens. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** One-off UI events (snackbars, navigation) delivered through a Channel. */
sealed interface UiEvent {
    data class Message(val textRes: Int, val args: List<Any> = emptyList()) : UiEvent
}
