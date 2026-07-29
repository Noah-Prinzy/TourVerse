package com.tourverse.state

import com.tourverse.data.model.User
import com.tourverse.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SessionState(val user: User? = null, val initializing: Boolean = true) {
    val authenticated get() = user != null
}

class SessionManager(private val repository: AuthRepository) {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState

    suspend fun restore() {
        repository.restore()
        mutableState.value = SessionState(repository.user, false)
    }

    fun authenticated() { mutableState.value = SessionState(repository.user, false) }
    suspend fun logout(all: Boolean = false) { repository.logout(all); mutableState.value = SessionState(null, false) }
    fun clear() { mutableState.value = SessionState(null, false) }
}
