package com.musicstream.app.presentation.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.ui.theme.MusicStreamTheme
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(state.isLoginSuccessful) {
        if (state.isLoginSuccessful) {
            onLoginSuccess()
        }
    }

    AuthContent(
        state = state,
        onLoginClick = { email, password -> viewModel.login(email, password) },
        onSignUpClick = { name, email, password -> viewModel.signUp(name, email, password) },
        onClearMessages = { viewModel.clearMessages() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(
    state: AuthUiState,
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String, String) -> Unit,
    onClearMessages: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val nameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }

    val isNameFocused by nameInteractionSource.collectIsFocusedAsState()
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    LaunchedEffect(state.error, state.successMessage) {
        if (state.error != null || state.successMessage != null) {
            delay(3000)
            onClearMessages()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedPlaceholderColor = Color.Transparent,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(id = R.drawable.audio_tune_icon), null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "MusicStream",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isLoginMode) "Welcome back, you've been missed!" else "Join the community of music lovers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(40.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    label = { Text("Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = if (!isNameFocused) { { Text("Enter your full name") } } else null,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onClearMessages() },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                    interactionSource = nameInteractionSource
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                label = { Text("Email Address", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = if (!isEmailFocused) { { Text("Enter your email") } } else null,
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onClearMessages() },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                interactionSource = emailInteractionSource
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = if (!isPasswordFocused) { { Text("Password") } } else null,
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onClearMessages() },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                interactionSource = passwordInteractionSource
            )

            if (isLoginMode) {
                Box(Modifier.fillMaxWidth(), Alignment.CenterEnd) {
                    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = Color.Gray)) {
                        TextButton(onClick = {}) {
                            Text(
                                "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else { Spacer(Modifier.height(24.dp)) }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (state.successMessage != null) {
                Text(
                    text = state.successMessage,
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                        // Normally we'd handle this in ViewModel, but for simple validation here:
                        // viewModel.showError("...")
                    } else {
                        if (isLoginMode) {
                            onLoginClick(email, password)
                        } else {
                            onSignUpClick(name, email, password)
                        }
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (isLoginMode) "Login" else "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text(" OR ", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(painterResource(R.drawable.google_color_icon), null, Modifier.size(24.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isLoginMode) "Sign Up" else "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        isLoginMode = !isLoginMode
                        onClearMessages()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    MusicStreamTheme {
        AuthContent(
            state = AuthUiState(),
            onLoginClick = { _, _ -> },
            onSignUpClick = { _, _, _ -> },
            onClearMessages = {}
        )
    }
}
