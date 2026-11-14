package top.niunaijun.blackboxa.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClonesScreen(
    viewModel: CloneViewModel,
    onAddClone: () -> Unit
) {
    val clones by viewModel.clones.collectAsState()
    val icons by viewModel.icons.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val context = LocalContext.current
    var menuClone by remember { mutableStateOf<CloneEntry?>(null) }
    var deleteClone by remember { mutableStateOf<CloneEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading && clones.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (clones.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Text(
                    text = "No clones yet",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Clone an app to run a second copy with its own account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAddClone) {
                    Text("Clone your first app")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(clones, key = { "${it.packageName}_${it.userId}" }) { clone ->
                    LaunchedEffect(clone.packageName) {
                        viewModel.ensureIcon(clone.packageName)
                    }
                    Box(modifier = Modifier.animateItemPlacement()) {
                        CloneCell(
                            bitmap = icons[clone.packageName],
                            label = "${clone.appName} ${clone.number}",
                            badgeNumber = clone.number,
                            onClick = { viewModel.launch(clone) },
                            onLongClick = { menuClone = clone }
                        )
                        DropdownMenu(
                            expanded = menuClone == clone,
                            onDismissRequest = { menuClone = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    menuClone = null
                                    viewModel.launch(clone)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to home screen") },
                                onClick = {
                                    menuClone = null
                                    viewModel.addShortcut(context, clone)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete clone") },
                                onClick = {
                                    menuClone = null
                                    deleteClone = clone
                                }
                            )
                        }
                    }
                }
            }
        }

        deleteClone?.let { clone ->
            AlertDialog(
                onDismissRequest = { deleteClone = null },
                title = { Text("Delete ${clone.appName} ${clone.number}?") },
                text = { Text("Its separate account and data will be gone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(clone)
                        deleteClone = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteClone = null }) { Text("Cancel") }
                }
            )
        }
    }
}
