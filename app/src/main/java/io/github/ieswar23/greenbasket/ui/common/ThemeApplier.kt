package io.github.ieswar23.greenbasket.ui.common

import androidx.appcompat.app.AppCompatDelegate
import io.github.ieswar23.greenbasket.domain.model.ThemeMode

object ThemeApplier {
    fun nightModeFor(mode: ThemeMode): Int = when (mode) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    fun apply(mode: ThemeMode) {
        val target = nightModeFor(mode)
        if (AppCompatDelegate.getDefaultNightMode() != target) {
            AppCompatDelegate.setDefaultNightMode(target)
        }
    }
}
