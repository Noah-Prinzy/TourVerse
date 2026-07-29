package com.tourverse.data.repository

import com.tourverse.data.model.*
import com.tourverse.data.remote.AuthApi

class AuthRepository(private val api: AuthApi) {
    suspend fun login(email: String, password: String) = api.login(LoginRequest(email, password))
    suspend fun register(firstName: String, lastName: String, email: String, password: String) =
        api.register(RegisterRequest(firstName, lastName, email, password))
    suspend fun restore() = api.restore()
    suspend fun logout(all: Boolean = false) = api.logout(all)
    val user get() = api.currentUser
}

class ProfileRepository(private val api: AuthApi) {
    suspend fun get() = api.profile()
    suspend fun update(request: UpdateUserProfileRequest) = api.updateProfile(request)
    suspend fun updateImage(url: String?) = api.updateProfileImage(UpdateProfileImageRequest(url))
    suspend fun delete(password: String) = api.deleteAccount(DeleteAccountRequest(password))
}
