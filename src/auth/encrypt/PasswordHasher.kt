package org.synthesis.auth.encrypt

import org.mindrot.jbcrypt.BCrypt

interface PasswordHasher {
    fun hash(password: String): String
    fun check(plain: String, hashed: String): Boolean
}

class BCryptHasher(
    private val rounds: Int = 10 // Default rounds from BCrypt
) : PasswordHasher {
    override fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(rounds))
    override fun check(plain: String, hashed: String): Boolean = BCrypt.checkpw(plain, hashed)
}
