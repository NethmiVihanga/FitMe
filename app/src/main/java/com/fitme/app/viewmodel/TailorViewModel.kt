package com.fitme.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitme.app.data.model.Portfolio
import com.fitme.app.data.model.Review
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.fitme.app.data.repository.TailorRepository
import kotlinx.coroutines.launch

class TailorViewModel : ViewModel() {

    private val tailorRepository = TailorRepository()

    private val _tailorList = MutableLiveData<Result<List<Pair<User, SeamstressProfile>>>>()
    val tailorList: LiveData<Result<List<Pair<User, SeamstressProfile>>>> = _tailorList

    private val _tailorProfile = MutableLiveData<Result<SeamstressProfile>>()
    val tailorProfile: LiveData<Result<SeamstressProfile>> = _tailorProfile

    private val _portfolio = MutableLiveData<Result<List<Portfolio>>>()
    val portfolio: LiveData<Result<List<Portfolio>>> = _portfolio

    private val _reviews = MutableLiveData<Result<List<Review>>>()
    val reviews: LiveData<Result<List<Review>>> = _reviews

    private val _earnings = MutableLiveData<Result<Map<String, Any?>>>()
    val earnings: LiveData<Result<Map<String, Any?>>> = _earnings

    private val _transactions = MutableLiveData<Result<List<Map<String, Any?>>>>()
    val transactions: LiveData<Result<List<Map<String, Any?>>>> = _transactions

    private val _portfolioAdded = MutableLiveData<Result<String>>()
    val portfolioAdded: LiveData<Result<String>> = _portfolioAdded

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private var cachedTailors: List<Pair<User, SeamstressProfile>>? = null

    fun loadAllTailors() {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.getAllSeamstresses()
            if (result.isSuccess) {
                cachedTailors = result.getOrNull()
            }
            _tailorList.value = result
            _loading.value = false
        }
    }

    fun searchTailors(query: String) {
        if (query.isEmpty()) {
            cachedTailors?.let {
                _tailorList.value = Result.success(it)
            } ?: loadAllTailors()
        } else {
            cachedTailors?.let { list ->
                val filtered = list.filter { (user, profile) ->
                    user.name.contains(query, ignoreCase = true) ||
                    user.location.contains(query, ignoreCase = true) ||
                    profile.specialties.any { it.contains(query, ignoreCase = true) }
                }
                _tailorList.value = Result.success(filtered)
            }
        }
    }

    fun loadTailorProfile(uid: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.getSeamstressProfile(uid)
            _tailorProfile.value = result
            _loading.value = false
        }
    }

    fun loadPortfolio(seamstressId: String) {
        viewModelScope.launch {
            val result = tailorRepository.getPortfolio(seamstressId)
            _portfolio.value = result
        }
    }

    private val _portfolioDeleted = MutableLiveData<Result<Unit>>()
    val portfolioDeleted: LiveData<Result<Unit>> = _portfolioDeleted

    private val _portfolioUpdated = MutableLiveData<Result<Unit>>()
    val portfolioUpdated: LiveData<Result<Unit>> = _portfolioUpdated

    fun addPortfolioItem(portfolio: Portfolio) {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.addPortfolioItem(portfolio)
            _portfolioAdded.value = result
            _loading.value = false
        }
    }

    fun deletePortfolioItem(portfolioId: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.deletePortfolioItem(portfolioId)
            _portfolioDeleted.value = result
            _loading.value = false
        }
    }

    fun updatePortfolioItem(portfolio: Portfolio) {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.updatePortfolioItem(portfolio)
            _portfolioUpdated.value = result
            _loading.value = false
        }
    }

    fun loadReviews(seamstressId: String) {
        viewModelScope.launch {
            val result = tailorRepository.getReviews(seamstressId)
            _reviews.value = result
        }
    }

    private var earningsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadEarnings(seamstressId: String) {
        earningsListener?.remove()
        earningsListener = tailorRepository.listenToEarnings(seamstressId) { result ->
            _earnings.value = result
        }

        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.getEarnings(seamstressId)
            _earnings.value = result
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        earningsListener?.remove()
    }

    fun loadTransactions(seamstressId: String) {
        viewModelScope.launch {
            val result = tailorRepository.getTransactions(seamstressId)
            _transactions.value = result
        }
    }

    fun updateProfile(profile: SeamstressProfile) {
        viewModelScope.launch {
            tailorRepository.updateSeamstressProfile(profile)
        }
    }

    private val _paymentMethod = MutableLiveData<Result<Map<String, String>>>()
    val paymentMethod: LiveData<Result<Map<String, String>>> = _paymentMethod

    private val _paymentMethodUpdated = MutableLiveData<Result<Unit>>()
    val paymentMethodUpdated: LiveData<Result<Unit>> = _paymentMethodUpdated

    fun loadPaymentMethod(uid: String) {
        viewModelScope.launch {
            val result = tailorRepository.getPaymentMethod(uid)
            _paymentMethod.value = result
        }
    }

    fun updatePaymentMethod(uid: String, bankName: String, accountNumber: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = tailorRepository.updatePaymentMethod(uid, bankName, accountNumber)
            _paymentMethodUpdated.value = result
            _loading.value = false
        }
    }
}
