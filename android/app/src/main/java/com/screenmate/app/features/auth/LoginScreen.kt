package com.screenmate.app.features.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.network.SupabaseModule
import com.screenmate.app.core.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _isSignUp = MutableStateFlow(false)
    val isSignUp = _isSignUp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun updateEmail(value: String) { _email.value = value }
    fun updatePassword(value: String) { _password.value = value }
    fun updateUsername(value: String) { _username.value = value }
    fun toggleSignUp() { _isSignUp.value = !_isSignUp.value; _error.value = null }

    fun submit(context: android.content.Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val app = context.applicationContext as ScreenMateApplication
                val prefs = app.preferences
                val client = app.supabase.getClient() ?: throw Exception("Supabase not configured")
                
                if (_isSignUp.value) {
                    client.auth.signUpWith(Email) {
                        email = _email.value
                        password = _password.value
                        data = kotlinx.serialization.json.buildJsonObject {
                            put("username", kotlinx.serialization.json.JsonPrimitive(_username.value))
                        }
                    }
                } else {
                    client.auth.signInWith(Email) {
                        email = _email.value
                        password = _password.value
                    }
                }
                
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    prefs.userId = user.id
                    prefs.username = user.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "User"
                    prefs.isLoggedIn = true
                    onSuccess()
                } else {
                    _error.value = "Authentication failed."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val username by viewModel.username.collectAsState()
    val isSignUp by viewModel.isSignUp.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentPrimary,
        unfocusedBorderColor = DarkSurfaceVariant,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = AccentPrimary,
        unfocusedLabelColor = TextSecondary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ScreenMate",
            color = AccentPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            singleLine = true,
            trailingIcon = {
                Text(
                    text = if (passwordVisible) "Hide" else "Show",
                    color = AccentPrimary,
                    modifier = Modifier
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(8.dp)
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.submit(context, onNavigateToDashboard) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(24.dp))
            } else {
                Text(text = if (isSignUp) "Sign Up" else "Login", color = DarkBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(context, "Google sign-in coming soon", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
        ) {
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
            color = AccentPrimary,
            modifier = Modifier.clickable { viewModel.toggleSignUp() }
        )
    }
}
