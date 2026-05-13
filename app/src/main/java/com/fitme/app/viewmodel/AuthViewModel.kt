package com.fitme.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitme.app.data.model.User
import com.fitme.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<FirebaseUser>>()
    val loginResult: LiveData<Result<FirebaseUser>> = _loginResult

    private val _registerResult = MutableLiveData<Result<FirebaseUser>>()
    val registerResult: LiveData<Result<FirebaseUser>> = _registerResult

    private val _userProfile = MutableLiveData<Result<User>>()
    val userProfile: LiveData<Result<User>> = _userProfile

    private val _resetResult = MutableLiveData<Result<Unit>>()
    val resetResult: LiveData<Result<Unit>> = _resetResult

    private val _changePasswordResult = MutableLiveData<Result<Unit>>()
    val changePasswordResult: LiveData<Result<Unit>> = _changePasswordResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    val currentUser get() = authRepository.currentUser

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.login(email, password)
            _loginResult.value = result
            _loading.value = false
        }
    }

    fun register(email: String, password: String, name: String, phone: String, role: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.register(email, password, name, phone, role)
            _registerResult.value = result
            _loading.value = false
        }
    }

    fun signInWithGoogle(idToken: String, role: String = "") {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.signInWithGoogle(idToken, role)
            _loginResult.value = result
            _loading.value = false
        }
    }

    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadUserProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        
        // Real-time listener for profile updates (Pro status, etc.)
        profileListener?.remove()
        profileListener = authRepository.listenToUserProfile(uid) { user ->
            if (user != null) {
                _userProfile.value = Result.success(user)
            }
        }

        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.getUserProfile(uid)
            _userProfile.value = result
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.sendPasswordResetEmail(email)
            _resetResult.value = result
            _loading.value = false
        }
    }

    fun clearResetResult() {
        _resetResult.value = null
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.changePassword(oldPass, newPass)
            _changePasswordResult.value = result
            _loading.value = false
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            authRepository.updateUserProfile(user)
            _userProfile.value = Result.success(user)
        }
    }

    fun updateRole(uid: String, role: String) {
        viewModelScope.launch {
            authRepository.updateUserRole(uid, role)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.deleteAccount()
            _deleteResult.value = result
            _loading.value = false
        }
    }
}
