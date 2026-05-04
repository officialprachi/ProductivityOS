package com.productivityos.app.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentUser(): Flow<FirebaseUser?>
    fun isLoggedIn(): Boolean
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser>
    suspend fun signOut()
}
