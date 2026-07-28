package com.tourverse

import android.content.Context
import com.tourverse.data.remote.AuthApi
import com.tourverse.data.repository.AuthRepository
import com.tourverse.data.repository.ProfileRepository
import com.tourverse.data.repository.CommunityRepository
import com.tourverse.data.repository.TripRepository
import com.tourverse.data.remote.TourismApi
import com.tourverse.data.session.EncryptedSessionTokenStore
import com.tourverse.state.SessionManager

class AppContainer(context: Context) {
    private val authApi = AuthApi(EncryptedSessionTokenStore(context.applicationContext))
    val authRepository = AuthRepository(authApi)
    val profileRepository = ProfileRepository(authApi)
    val communityRepository = CommunityRepository(TourismApi(), authApi)
    val tripRepository = TripRepository(authApi)
    val sessionManager = SessionManager(authRepository)
}
