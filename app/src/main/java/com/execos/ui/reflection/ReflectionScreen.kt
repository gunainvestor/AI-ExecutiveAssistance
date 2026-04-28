package com.execos.ui.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.components.ExecOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionScreen(
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Evaluate your day", fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
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
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 24.dp),
                    ) {
                        Text(
                            "What actually moved your day forward? What didn't?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Be honest. The coach evaluates execution, not activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "AI feedback streams live and includes your recent context automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        ExecOutlinedTextField(
                            value = state.input,
                            onValueChange = viewModel::setInput,
                            label = { Text("Day evaluation") },
                            placeholder = { Text("What outcomes happened? Where did you lose focus? What would make this a 9/10 day?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            maxLines = 14,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.generate() },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            if (state.busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Evaluate my day")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.save() },
                            enabled = state.input.isNotBlank() || state.aiOutput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save evaluation")
                        }
                        if (state.aiOutput.isNotBlank()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "AI performance breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.aiOutput,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (state.recent.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Recent entries",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            state.recent.take(5).forEach { r ->
                                Text(
                                    "${r.date}: ${r.textInput.take(90)}${if (r.textInput.length > 90) "…" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
