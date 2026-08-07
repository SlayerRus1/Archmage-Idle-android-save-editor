package com.saveeditor.archmageidle.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saveeditor.archmageidle.data.ItemNameRepository
import com.saveeditor.archmageidle.data.LoadState
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.ui.screens.ChronomancyScreen
import com.saveeditor.archmageidle.ui.screens.ConceptUpgradesScreen
import com.saveeditor.archmageidle.ui.screens.EditorHubScreen
import com.saveeditor.archmageidle.ui.screens.ElixirsScreen
import com.saveeditor.archmageidle.ui.screens.HomeScreen
import com.saveeditor.archmageidle.ui.screens.InventoryScreen
import com.saveeditor.archmageidle.ui.screens.MapLocationsScreen
import com.saveeditor.archmageidle.ui.screens.PlayerScreen
import com.saveeditor.archmageidle.ui.screens.RawJsonScreen
import com.saveeditor.archmageidle.ui.screens.ResourcesScreen
import com.saveeditor.archmageidle.ui.screens.ShopScreen
import com.saveeditor.archmageidle.ui.screens.SkillsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: SaveViewModel,
    navController: NavController,
    itemNames: ItemNameRepository,
    onPickOpenFile: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val current = navController.current
    val hasSave = viewModel.root != null

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.statusMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(current.title) },
                navigationIcon = {
                    if (current != Screen.Home) {
                        IconButton(onClick = { navController.popOrNull() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (hasSave && current != Screen.Home) {
                        IconButton(onClick = onSave) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (current) {
            Screen.Home -> HomeScreen(
                modifier = modifier,
                viewModel = viewModel,
                onOpenFile = onPickOpenFile,
                onContinueToEditor = { navController.push(Screen.EditorHub) }
            )
            Screen.EditorHub -> EditorHubScreen(
                modifier = modifier,
                viewModel = viewModel,
                onNavigate = { navController.push(it) },
                onSave = onSave,
                onSaveAs = onSaveAs,
            )
            Screen.Player -> PlayerScreen(modifier, viewModel)
            Screen.Resources -> ResourcesScreen(modifier, viewModel)
            Screen.Skills -> SkillsScreen(modifier, viewModel)
            Screen.Inventory -> InventoryScreen(modifier, viewModel, itemNames)
            Screen.Shop -> ShopScreen(modifier, viewModel)
            Screen.ConceptUpgrades -> ConceptUpgradesScreen(modifier, viewModel)
            Screen.Elixirs -> ElixirsScreen(modifier, viewModel)
            Screen.Chronomancy -> ChronomancyScreen(modifier, viewModel)
            Screen.MapScreenRoute -> MapLocationsScreen(modifier, viewModel)
            Screen.RawJson -> RawJsonScreen(modifier, viewModel)
        }
    }
}
