package com.tourverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tourverse.data.model.UpdateUserProfileRequest

@Composable
fun LoginScreen(viewModel: AuthViewModel, onComplete: () -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.complete) { if (state.complete) onComplete() }
    FormScreen("Login") {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button({ viewModel.login(email, password) }, enabled = !state.loading && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.loading) "Signing in..." else "Login")
        }
        TextButton(onClick = onRegister) { Text("Create an account") }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onComplete: () -> Unit, onLogin: () -> Unit) {
    var first by remember { mutableStateOf("") }; var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.complete) { if (state.complete) onComplete() }
    FormScreen("Register") {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(first, { first = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(last, { last = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Text("Use at least 8 characters with uppercase, lowercase and a number.")
        Button({ viewModel.register(first, last, email, password) }, enabled = !state.loading && first.isNotBlank() && last.isNotBlank() && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.loading) "Creating account..." else "Register")
        }
        TextButton(onClick = onLogin) { Text("Already have an account? Login") }
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onDeleted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.accountDeleted) { if (state.accountDeleted) onDeleted() }
    if (state.loading) { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }; return }
    val profile = state.profile
    if (profile == null) { FormScreen("Profile") { Text(state.error ?: "Profile unavailable."); Button(viewModel::load) { Text("Try again") } }; return }
    var first by remember(profile.id) { mutableStateOf(profile.firstName) }; var last by remember(profile.id) { mutableStateOf(profile.lastName) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio.orEmpty()) }; var nationality by remember(profile.id) { mutableStateOf(profile.nationality.orEmpty()) }
    var interests by remember(profile.id) { mutableStateOf(profile.travelInterests.joinToString(", ")) }; var image by remember(profile.id) { mutableStateOf(profile.profileImageUrl.orEmpty()) }
    var public by remember(profile.id) { mutableStateOf(profile.profilePublic) }; var password by remember { mutableStateOf("") }
    FormScreen("Your profile") {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(first, { first = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(last, { last = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bio, { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nationality, { nationality = it }, label = { Text("Nationality") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(interests, { interests = it }, label = { Text("Travel interests") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(image, { image = it }, label = { Text("Profile image URL") }, modifier = Modifier.fillMaxWidth())
        Row { Switch(public, { public = it }); Text("Public profile", Modifier.padding(start = 8.dp)) }
        Button({
            viewModel.save(UpdateUserProfileRequest(first, last, bio, nationality, interests.split(',').map(String::trim).filter(String::isNotEmpty), public), image)
        }, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Saving..." else "Save profile") }
        HorizontalDivider(); Text("Delete account", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(password, { password = it }, label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button({ viewModel.delete(password) }, enabled = !state.saving && password.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Delete account permanently") }
    }
}

@Composable
private fun FormScreen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        content()
    }
}
