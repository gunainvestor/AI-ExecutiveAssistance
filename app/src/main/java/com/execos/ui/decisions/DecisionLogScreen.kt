package com.execos.ui.decisions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.execos.data.model.DecisionItem
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.components.ExecOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionLogScreen(
    viewModel: DecisionLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Decision log", fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            if (!state.loading && state.error == null) {
                FloatingActionButton(
                    onClick = { viewModel.openNew() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add decision")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExecGradientBackground {
            when {
                state.loading -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                    ) {
                        Text(state.error ?: "")
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.decisions, key = { it.id }) { item ->
                            DecisionRow(item) { viewModel.openEdit(item) }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (state.editorOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEditor() },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (state.draft.id.isBlank()) "New decision" else "Edit decision",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.dismissEditor() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fill in the context, then run AI for risks and trade-offs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                ExecOutlinedTextField(
                    value = state.draft.title,
                    onValueChange = { t -> viewModel.updateDraft { d -> d.copy(title = t) } },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                ExecOutlinedTextField(
                    value = state.draft.context,
                    onValueChange = { t -> viewModel.updateDraft { d -> d.copy(context = t) } },
                    label = { Text("Context") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8,
                )
                Spacer(Modifier.height(12.dp))
                ExecOutlinedTextField(
                    value = state.draft.options,
                    onValueChange = { t -> viewModel.updateDraft { d -> d.copy(options = t) } },
                    label = { Text("Options considered") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8,
                )
                Spacer(Modifier.height(12.dp))
                ExecOutlinedTextField(
                    value = state.draft.finalDecision,
                    onValueChange = { t -> viewModel.updateDraft { d -> d.copy(finalDecision = t) } },
                    label = { Text("Final decision") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Confidence · ${state.draft.confidence}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.draft.confidence.toFloat(),
                    onValueChange = { v ->
                        viewModel.updateDraft { d ->
                            d.copy(confidence = v.toInt().coerceIn(1, 5))
                        }
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                TextButton(
                    onClick = { viewModel.runAiOnDraft() },
                    enabled = !state.aiBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.aiBusy) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Run AI · risks, trade-offs, blind spots")
                    }
                }
                if (!state.draft.aiAnalysis.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("AI analysis", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.draft.aiAnalysis ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { viewModel.dismissEditor() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.saveDraft()
                        },
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionRow(item: DecisionItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Confidence ${item.confidence}/5 · ${item.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.finalDecision.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.finalDecision, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
