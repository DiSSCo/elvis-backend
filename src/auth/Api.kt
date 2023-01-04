package org.synthesis.auth

data class AuthConfig(
    val url: String,
    val realm: String,
    val clientId: String
)
