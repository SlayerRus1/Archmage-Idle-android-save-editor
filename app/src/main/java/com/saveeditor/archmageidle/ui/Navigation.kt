package com.saveeditor.archmageidle.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen(val title: String) {
    object Home : Screen("Archmage Save Editor")
    object EditorHub : Screen("Editor")
    object Player : Screen("Player")
    object Resources : Screen("Resources")
    object Skills : Screen("Skills")
    object Inventory : Screen("Inventory")
    object Shop : Screen("Shop Upgrades")
    object ConceptUpgrades : Screen("Concept Upgrades")
    object Elixirs : Screen("Elixirs")
    object Chronomancy : Screen("Chronomancy")
    object MapScreenRoute : Screen("Map / Locations")
    object RawJson : Screen("Raw JSON Editor")
}

class NavController {
    var stack by mutableStateOf(listOf<Screen>(Screen.Home))
        private set

    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun popOrNull(): Boolean {
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    fun popToHome() {
        stack = listOf(Screen.Home)
    }

    fun popToEditorHub() {
        stack = listOf(Screen.Home, Screen.EditorHub)
    }
}
