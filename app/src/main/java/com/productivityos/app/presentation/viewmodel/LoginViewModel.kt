package com.productivityos.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivityos.app.domain.model.UserProfile
import com.productivityos.app.domain.repository.AuthRepository
import com.productivityos.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithEmail(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        workType: String,
        focusPref: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUpWithEmail(email, password, name)
                .onSuccess { firebaseUser ->
                    // Save profile to Room so ProfileScreen shows it immediately
                    val profile = UserProfile(
                        name = name,
                        email = firebaseUser.email ?: email,
                        workerType = workType,          // badge on profile card
                        workCategory = workType,        // stat grid "Work Type"
                        focusPreference = focusPref,    // stat grid "Focus Pref." (editable)
                        avgScore = 0,
                        bestStreak = 0,
                        currentStreak = 0,
                        primaryDevice = android.os.Build.MODEL
                    )
                    profileRepository.updateProfile(profile)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }

    private fun friendlyMessage(t: Throwable): String = when {
        t.message?.contains("password", ignoreCase = true) == true ->
            "Incorrect password. Please try again."
        t.message?.contains("no user", ignoreCase = true) == true ||
        t.message?.contains("identifier", ignoreCase = true) == true ->
            "No account found with that email."
        t.message?.contains("already in use", ignoreCase = true) == true ->
            "An account with this email already exists."
        t.message?.contains("badly formatted", ignoreCase = true) == true ->
            "Please enter a valid email address."
        t.message?.contains("network", ignoreCase = true) == true ->
            "Network error. Check your connection."
        else -> t.message ?: "Something went wrong. Please try again."
    }
}
