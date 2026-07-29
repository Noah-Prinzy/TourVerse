package com.tourverse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourverse.data.model.UpdateUserProfileRequest
import com.tourverse.data.model.UserProfile
import com.tourverse.data.repository.AuthRepository
import com.tourverse.data.repository.ProfileRepository
import com.tourverse.state.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null, val complete: Boolean = false)

class AuthViewModel(
    private val repository: AuthRepository,
    private val session: SessionManager
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState

    fun login(email: String, password: String) = submit {
        repository.login(email.trim(), password)
    }

    fun register(first: String, last: String, email: String, password: String) = submit {
        repository.register(first.trim(), last.trim(), email.trim(), password)
    }

    private fun submit(block: suspend () -> Unit) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = AuthUiState(loading = true)
            try {
                block()
                session.authenticated()
                mutableState.value = AuthUiState(complete = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.value = AuthUiState(error = exception.message ?: "Request failed.")
            }
        }
    }
}

data class ProfileUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val accountDeleted: Boolean = false
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val session: SessionManager
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState
    init { load() }

    fun load() = viewModelScope.launch {
        mutableState.value = ProfileUiState()
        try { mutableState.value = ProfileUiState(loading = false, profile = repository.get()) }
        catch (exception: Exception) { mutableState.value = ProfileUiState(loading = false, error = exception.message) }
    }

    fun save(request: UpdateUserProfileRequest, imageUrl: String?) = viewModelScope.launch {
        val current = mutableState.value.profile ?: return@launch
        mutableState.value = mutableState.value.copy(saving = true, error = null)
        try {
            repository.update(request)
            val updated = repository.updateImage(imageUrl?.trim()?.takeIf(String::isNotEmpty))
            mutableState.value = ProfileUiState(loading = false, profile = updated)
        } catch (exception: Exception) {
            mutableState.value = ProfileUiState(loading = false, profile = current, error = exception.message)
        }
    }

    fun delete(password: String) = viewModelScope.launch {
        mutableState.value = mutableState.value.copy(saving = true, error = null)
        try {
            repository.delete(password)
            session.clear()
            mutableState.value = ProfileUiState(loading = false, accountDeleted = true)
        } catch (exception: Exception) {
            mutableState.value = mutableState.value.copy(saving = false, error = exception.message)
        }
    }
}
