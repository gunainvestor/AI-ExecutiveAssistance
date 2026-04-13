package com.execos.data.repo

import javax.inject.Inject
import javax.inject.Singleton

private const val LOCAL_USER_ID = "local"

/**
 * MVP: single offline profile. [uid] parameters on other repositories are ignored but kept
 * so the UI layer can switch to multi-user or sync later without wide refactors.
 */
@Singleton
class AuthRepository @Inject constructor() {
    val currentUserId: String? = LOCAL_USER_ID

    suspend fun ensureSignedIn(): Result<String> = Result.success(LOCAL_USER_ID)
}
