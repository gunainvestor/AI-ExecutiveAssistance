package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.entity.UserAccountEntity
import com.execos.util.Dates
import com.execos.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val db: ExecOsDatabase,
    private val sessionStore: SessionStore,
) {
    private val dao get() = db.userAccountDao()

    private val legacyLocalUserId = "local"

    /**
     * Local-first: if no account is selected yet, we treat the device as a legacy single-user
     * profile ("local") so existing installs keep working after the v2 migration.
     */
    val currentUserIdFlow: Flow<String> = sessionStore.uidFlow.map { it ?: legacyLocalUserId }

    suspend fun currentUserId(): String = sessionStore.uidFlow.first() ?: legacyLocalUserId

    suspend fun ensureSignedIn(): Result<String> {
        return Result.success(currentUserId())
    }

    suspend fun register(emailRaw: String, password: CharArray): Result<String> {
        val email = emailRaw.trim().lowercase()
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email"))
        }
        if (password.size < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        val existing = dao.findByEmail(email)
        if (existing != null) {
            return Result.failure(IllegalStateException("Account already exists"))
        }
        val uid = UUID.randomUUID().toString()
        val entity = UserAccountEntity(
            id = uid,
            email = email,
            passwordHash = PasswordHasher.hashPassword(password),
            createdAt = Dates.nowIso(),
        )
        return runCatching {
            dao.insert(entity)
            sessionStore.setUid(uid)
            uid
        }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it) })
    }

    suspend fun login(emailRaw: String, password: CharArray): Result<String> {
        val email = emailRaw.trim().lowercase()
        val acct = dao.findByEmail(email) ?: return Result.failure(IllegalStateException("Invalid credentials"))
        val ok = PasswordHasher.verifyPassword(password, acct.passwordHash)
        if (!ok) return Result.failure(IllegalStateException("Invalid credentials"))
        return runCatching {
            sessionStore.setUid(acct.id)
            acct.id
        }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it) })
    }

    suspend fun logout() {
        sessionStore.setUid(null)
    }

    suspend fun currentUserEmail(): String? {
        val uid = currentUserId()
        return dao.findById(uid)?.email
    }
}
