package com.musicstream.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.ui.theme.MusicStreamTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String, String) -> Unit,
    isEmailRegistered: suspend (String) -> Boolean = { false }
) {
    if (isSystemInDarkTheme()) {
        AuthScreenDark(onLoginClick, onSignUpClick, isEmailRegistered)
    } else {
        AuthScreenLight(onLoginClick, onSignUpClick, isEmailRegistered)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenLight(
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String, String) -> Unit,
    isEmailRegistered: suspend (String) -> Boolean
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Text and isError
    val scope = rememberCoroutineScope()

    val nameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }

    val isNameFocused by nameInteractionSource.collectIsFocusedAsState()
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    // Clear message after 3 seconds
    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            message = null
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.Black,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = Color.Black,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedPlaceholderColor = Color.Transparent,
        unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.6f)
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
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
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("MusicStream", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.Black)
            Text(if (isLoginMode) "Welcome back, you've been missed!" else "Join the community of music lovers.", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
            Spacer(Modifier.height(40.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(color = Color.Black),
                    label = { Text("Full Name", color = Color.Black) },
                    placeholder = if (!isNameFocused) { { Text("Enter your full name") } } else null,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) message = null },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Black) },
                    shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                    interactionSource = nameInteractionSource
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                textStyle = TextStyle(color = Color.Black),
                label = { Text("Email Address", color = Color.Black) },
                placeholder = if (!isEmailFocused) { { Text("Enter your email") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) message = null },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Black) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = fieldColors,
                interactionSource = emailInteractionSource
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                textStyle = TextStyle(color = Color.Black),
                label = { Text("Password", color = Color.Black) },
                placeholder = if (!isPasswordFocused) { { Text("Password") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) message = null },
                leadingIcon = { Icon(Icons.Default.Lock,
                    null, tint = Color.Black) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = fieldColors,
                interactionSource = passwordInteractionSource
            )

            if (isLoginMode) {
                Box(Modifier.fillMaxWidth(), Alignment.CenterEnd) {
                    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = Color.Gray)) {
                        TextButton(onClick = {}) {
                            Text(
                                "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                    }
                }
            } else { Spacer(Modifier.height(24.dp)) }

            message?.let { (text, isError) ->
                Text(
                    text = text,
                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                        message = "Please enter your email and password" to true
                    } else {
                        scope.launch {
                            val registered = isEmailRegistered(email)
                            if (isLoginMode) {
                                if (!registered) {
                                    message = "Email not registered. Please Sign Up." to true
                                } else {
                                    onLoginClick(email, password)
                                }
                            } else {
                                if (registered) {
                                    message = "Email already registered. Please Login." to true
                                } else {
                                    onSignUpClick(name, email, password)
                                    // Switch to log in mode after signup
                                    isLoginMode = true
                                    message = "Registration successful! Please Login." to false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) { Text(if (isLoginMode) "Login" else "Create Account", style = MaterialTheme.typography.titleMedium) }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color.LightGray)
                Text(" OR ", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium, color = Color.Black)
                HorizontalDivider(Modifier.weight(1f), color = Color.LightGray)
            }
            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black))
            { Text("Continue with Google", color = Color.Black) }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isLoginMode) "Sign Up" else "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        isLoginMode = !isLoginMode
                        message = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenDark(
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String, String) -> Unit,
    isEmailRegistered: suspend (String) -> Boolean
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val scope = rememberCoroutineScope()

    val nameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }

    val isNameFocused by nameInteractionSource.collectIsFocusedAsState()
    val isEmailFocused by emailInteractionSource.collectIsFocusedAsState()
    val isPasswordFocused by passwordInteractionSource.collectIsFocusedAsState()

    // Clear message after 3 seconds
    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            message = null
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedPlaceholderColor = Color.Transparent,
        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f)
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
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("MusicStream", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.White)
            Text(if (isLoginMode) "Welcome back, you've been missed!" else "Join the community of music lovers.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(40.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(color = Color.White),
                    label = { Text("Full Name", color = Color.White) },
                    placeholder = if (!isNameFocused) { { Text("Enter your full name") } } else null,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) message = null },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) },
                    shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                    interactionSource = nameInteractionSource
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                textStyle = TextStyle(color = Color.White),
                label = { Text("Email Address", color = Color.White) },
                placeholder = if (!isEmailFocused) { { Text("Enter your email") } } else null,
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) message = null },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White) },
                shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors,
                interactionSource = emailInteractionSource
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                textStyle = TextStyle(color = Color.White),
                label = { Text("Password", color = Color.White) },
                placeholder = if (!isPasswordFocused) { { Text("Password") } } else null,
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) message = null },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
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
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else { Spacer(Modifier.height(24.dp)) }

            message?.let { (text, isError) ->
                Text(
                    text = text,
                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                        message = "Please enter your email and password" to true
                    } else {
                        scope.launch {
                            val registered = isEmailRegistered(email)
                            if (isLoginMode) {
                                if (!registered) {
                                    message = "Email not registered. Please Sign Up." to true
                                } else {
                                    onLoginClick(email, password)
                                }
                            } else {
                                if (registered) {
                                    message = "Email already registered. " +
                                            "Please Login." to true
                                } else {
                                    onSignUpClick(name, email, password)
                                    // Switch to log in mode after signup
                                    isLoginMode = true
                                    message = "Registration successful! " +
                                            "Please Login." to false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) { Text(if (isLoginMode) "Login" else "Create Account", style = MaterialTheme.typography.titleMedium) }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text(" OR ", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }
            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White))
            { Text("Continue with Google", color = Color.White) }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isLoginMode) "Sign Up" else "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        isLoginMode = !isLoginMode
                        message = null
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun AuthScreenDarkPreview() {
    MusicStreamTheme(darkTheme = true) {
        AuthScreenDark(
            onLoginClick = { _, _ -> },
            onSignUpClick = { _, _, _ -> },
            isEmailRegistered = { false }
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun AuthScreenLightPreview() {
    MusicStreamTheme(darkTheme = false) {
        AuthScreenLight(
            onLoginClick = { _, _ -> },
            onSignUpClick = { _, _, _ -> },
            isEmailRegistered = { false }
        )
    }
}
