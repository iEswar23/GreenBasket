package io.github.ieswar23.greenbasket

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.ui.common.ThemeApplier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class GreenBasketApp : Application() {

    @Inject lateinit var preferences: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        // Apply the persisted theme before the first Activity is created to avoid a visible recreate.
        // This is a single small DataStore read, done once at process start.
        val mode = runBlocking { preferences.themeMode.first() }
        AppCompatDelegate.setDefaultNightMode(ThemeApplier.nightModeFor(mode))
    }
}
