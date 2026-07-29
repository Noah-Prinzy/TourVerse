package com.tourverse.data.session

interface SessionTokenStore {
    fun readRefreshToken(): String?
    fun writeRefreshToken(token: String)
    fun clear()
}

class InMemorySessionTokenStore(initial: String? = null) : SessionTokenStore {
    private var value = initial
    override fun readRefreshToken() = value
    override fun writeRefreshToken(token: String) { value = token }
    override fun clear() { value = null }
}
