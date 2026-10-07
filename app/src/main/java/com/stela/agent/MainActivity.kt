package com.stela.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stela.agent.domain.AgentStatus
import com.stela.agent.ui.theme.StelaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StelaTheme {
                StelaApp()
            }
        }
    }
}

@Composable
fun StelaApp(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { StelaTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    state = state,
                    onPromptChange = viewModel::updatePrompt,
                    onRun = {
                        viewModel.runPrompt()
                        navController.navigate("execution")
                    },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenDiagnostics = { navController.navigate("about") }
                )
            }
            composable("execution") {
                ExecutionScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onConfirm = viewModel::confirmExecution,
                    onCancel = viewModel::cancelExecution
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onApiKeyChanged = viewModel::updateApiKey,
                    onBack = { navController.popBackStack() },
                    onToggleVoice = viewModel::toggleVoice
                )
            }
            composable("about") {
                AboutScreen(state = state, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StelaTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Stela Agent")
            }
        }
    )
}

@Composable
fun HomeScreen(
    state: AppUiState,
    onPromptChange: (String) -> Unit,
    onRun: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Tell Stela what to do", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Write or speak a natural-language task. Stela interprets it, plans it, and executes the supported steps safely.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            label = { Text("Command") },
            placeholder = { Text("برای من ...") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRun, modifier = Modifier.weight(1f)) {
                Text("Run")
            }
            Button(onClick = onOpenDiagnostics, modifier = Modifier.weight(1f)) {
                Text("Diagnostics")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }
            Button(onClick = { }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Examples", style = MaterialTheme.typography.titleMedium)
                Text("• باز کن برنامه تنظیمات")
                Text("• برای من یک پیام کوتاه آماده کن")
                Text("• این کارها را مرحله به مرحله انجام بده")
            }
        }

        state.latestStep?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Latest plan", style = MaterialTheme.typography.titleMedium)
                    Text(it.title)
                    Text(it.description)
                }
            }
        }
    }
}

@Composable
fun ExecutionScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Execution status", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Command", style = MaterialTheme.typography.labelLarge)
                Text(state.prompt.ifBlank { "No active command" })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Status: ${state.currentStatus.name}")
                state.planSummary.takeIf { it.isNotBlank() }?.let { Text("Plan: $it") }
                if (state.currentTaskId.isNotBlank()) Text("Task: ${state.currentTaskId}")
                if (state.errorMessage != null) Text("Error: ${state.errorMessage}")
            }
        }

        val statusColor = when (state.currentStatus) {
            AgentStatus.SUCCEEDED -> Color(0xFF4CAF50)
            AgentStatus.FAILED -> Color(0xFFE53935)
            AgentStatus.WAITING_FOR_CONFIRMATION -> Color(0xFFFFC107)
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(text = state.currentStatus.name, color = statusColor, fontWeight = FontWeight.Bold)
        }

        if (state.confirmationRequired) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm")
                }
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
        }

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun SettingsScreen(
    state: AppUiState,
    onApiKeyChanged: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Provider settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini API key") },
            placeholder = { Text("Saved securely on-device") }
        )
        Button(onClick = onToggleVoice) {
            Text(if (state.voiceEnabled) "Disable voice" else "Enable voice")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun AboutScreen(state: AppUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Diagnostics", style = MaterialTheme.typography.headlineSmall)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Provider", style = MaterialTheme.typography.titleMedium)
                Text("Status: ${if (state.apiKey.isNotBlank()) "Configured" else "Missing key"}")
                Text("Voice enabled: ${state.voiceEnabled}")
                Text("Accessibility enabled: ${state.accessibilityEnabled}")
            }
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

