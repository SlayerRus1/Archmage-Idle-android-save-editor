package com.saveeditor.archmageidle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saveeditor.archmageidle.data.ItemNameRepository
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.ui.AppRoot
import com.saveeditor.archmageidle.ui.NavController
import com.saveeditor.archmageidle.ui.Screen
import com.saveeditor.archmageidle.ui.theme.ArchmageSaveEditorTheme

class MainActivity : ComponentActivity() {

    private var pendingOpenCallback: ((Uri) -> Unit)? = null
    private var pendingSaveCallback: ((Uri) -> Unit)? = null

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some providers don't support persistable permissions; ignore.
                }
                pendingOpenCallback?.invoke(uri)
            }
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                pendingSaveCallback?.invoke(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArchmageSaveEditorTheme {
                val viewModel: SaveViewModel = viewModel()
                val navController = remember { NavController() }
                val savedAsUri = remember { mutableStateOf<Uri?>(null) }
                val itemNames = remember { ItemNameRepository(applicationContext) }

                AppRoot(
                    viewModel = viewModel,
                    navController = navController,
                    itemNames = itemNames,
                    onPickOpenFile = {
                        pendingOpenCallback = { uri ->
                            savedAsUri.value = uri
                            if (viewModel.loadFrom(contentResolver, uri)) {
                                navController.push(Screen.EditorHub)
                            }
                        }
                        openDocumentLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                    },
                    onSave = {
                        val uri = savedAsUri.value
                        if (uri != null) {
                            viewModel.saveTo(contentResolver, uri)
                        } else {
                            pendingSaveCallback = { newUri ->
                                savedAsUri.value = newUri
                                viewModel.saveTo(contentResolver, newUri)
                            }
                            createDocumentLauncher.launch(defaultSaveName(viewModel))
                        }
                    },
                    onSaveAs = {
                        pendingSaveCallback = { newUri ->
                            savedAsUri.value = newUri
                            viewModel.saveTo(contentResolver, newUri)
                        }
                        createDocumentLauncher.launch(defaultSaveName(viewModel))
                    }
                )
            }
        }
    }

    private fun defaultSaveName(viewModel: SaveViewModel): String {
        val loaded = viewModel.loadState
        val base = if (loaded is com.saveeditor.archmageidle.data.LoadState.Loaded) {
            loaded.fileName.removeSuffix(".txt")
        } else {
            "archmage_save_edited"
        }
        return "$base.txt"
    }
}
