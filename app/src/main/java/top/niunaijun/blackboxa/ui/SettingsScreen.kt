package top.niunaijun.blackboxa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: CloneViewModel) {
    val clones by viewModel.clones.collectAsState()
    val gmsSpaces by viewModel.gmsSpaces.collectAsState()
    val gmsInstalling by viewModel.gmsInstalling.collectAsState()
    val crashReport by viewModel.crashReport.collectAsState()
    val collectingCrashReport by viewModel.collectingCrashReport.collectAsState()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "CloneX",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${clones.size} clone${if (clones.size == 1) "" else "s"} running",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "How it works",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Each clone runs in its own private space inside CloneX, " +
                "with its own login and data. No root, and no work profile. " +
                "Long-press a clone to add it to your home screen as \"App 1\", \"App 2\" and so on.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Google Play Services",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Some apps — AI apps especially — need Google Play Services " +
                "inside the clone to work, and crash without it. Install it once " +
                "per clone space; it copies your phone's own Google apps in, " +
                "and can take a minute or two.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (gmsSpaces.isEmpty()) {
            Text(
                text = "No clone spaces yet — clone an app first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (space in gmsSpaces) {
                val installing = gmsInstalling.contains(space.userId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Space ${space.userId + 1}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (space.gmsInstalled) {
                                "Play Services installed"
                            } else {
                                "${space.appCount} app${if (space.appCount == 1) "" else "s"} · Play Services missing"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (space.gmsInstalled) {
                        Text(
                            text = "✓ Installed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (installing) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = { viewModel.installGms(space.userId) }) {
                            Text("Install")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Crash report",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "If a clone crashes, come back here right after and tap the button. " +
                "It copies the exact reason from the phone's log — paste it to me " +
                "and I'll fix that exact crash.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (collectingCrashReport) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.copyCrashReport(context) }) {
                Text("Copy crash report")
            }
        }
        if (crashReport != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = crashReport!!.lineSequence().take(8).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Good to know",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Some apps with strong integrity checks, like banking apps, " +
                "can detect the sandbox and may refuse to run. Most social and " +
                "messaging apps clone just fine.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "CloneX 4.1.2  •  Built on the open-source NewBlackbox engine (Apache 2.0)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
