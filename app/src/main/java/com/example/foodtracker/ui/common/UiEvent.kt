package com.example.foodtracker.ui.common

sealed interface UiEvent {
    data class ShowMessage(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : UiEvent
}