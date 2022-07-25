package org.synthesis.auth

interface AuthService {

    /**
     * Token validation.
     */
    suspend fun verifyToken(accessToken: String): Boolean
}
