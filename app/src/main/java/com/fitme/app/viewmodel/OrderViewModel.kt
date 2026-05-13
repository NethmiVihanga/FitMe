package com.fitme.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitme.app.data.model.Order
import com.fitme.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {

    private val orderRepository = OrderRepository()

    private val _customerOrders = MutableLiveData<Result<List<Order>>>()
    val customerOrders: LiveData<Result<List<Order>>> = _customerOrders

    private val _seamstressOrders = MutableLiveData<Result<List<Order>>>()
    val seamstressOrders: LiveData<Result<List<Order>>> = _seamstressOrders

    private val _newOrders = MutableLiveData<Result<List<Order>>>()
    val newOrders: LiveData<Result<List<Order>>> = _newOrders

    private val _orderDetail = MutableLiveData<Result<Order>>()
    val orderDetail: LiveData<Result<Order>> = _orderDetail

    private val _orderAction = MutableLiveData<Result<Unit>>()
    val orderAction: LiveData<Result<Unit>> = _orderAction

    private val _createOrderResult = MutableLiveData<Result<String>>()
    val createOrderResult: LiveData<Result<String>> = _createOrderResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun createOrder(order: Order) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.createOrder(order)
            _createOrderResult.value = result
            _loading.value = false
        }
    }

    private var customerOrdersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var seamstressOrdersListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadCustomerOrders(customerId: String) {
        customerOrdersListener?.remove()
        customerOrdersListener = orderRepository.listenToCustomerOrders(customerId) { result ->
            _customerOrders.value = result
        }

        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.getCustomerOrders(customerId)
            _customerOrders.value = result
            _loading.value = false
        }
    }

    fun loadSeamstressOrders(seamstressId: String) {
        seamstressOrdersListener?.remove()
        seamstressOrdersListener = orderRepository.listenToSeamstressOrders(seamstressId) { result ->
            _seamstressOrders.value = result
        }

        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.getSeamstressOrders(seamstressId)
            _seamstressOrders.value = result
            _loading.value = false
        }
    }

    fun loadNewOrders(seamstressId: String) {
        viewModelScope.launch {
            val result = orderRepository.getNewOrdersForSeamstress(seamstressId)
            _newOrders.value = result
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.getOrderById(orderId)
            _orderDetail.value = result
            _loading.value = false
        }
    }

    private var orderListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningToOrder(orderId: String) {
        orderListener?.remove()
        orderListener = orderRepository.listenToOrderDetails(orderId) { result ->
            _orderDetail.value = result
        }
    }

    override fun onCleared() {
        super.onCleared()
        orderListener?.remove()
        customerOrdersListener?.remove()
        seamstressOrdersListener?.remove()
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.updateOrderStatus(orderId, status)
            _orderAction.value = result
            _loading.value = false
        }
    }

    fun acceptOrder(orderId: String, seamstressId: String, seamstressName: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.acceptOrder(orderId, seamstressId, seamstressName)
            _orderAction.value = result
            _loading.value = false
        }
    }

    fun sendQuote(orderId: String, seamstressId: String, seamstressName: String, quotedPrice: Double) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.sendQuote(orderId, seamstressId, seamstressName, quotedPrice)
            _orderAction.value = result
            _loading.value = false
        }
    }

    fun approveQuote(orderId: String, finalPrice: Double) {
        viewModelScope.launch {
            _loading.value = true
            val result = orderRepository.approveQuote(orderId, finalPrice)
            _orderAction.value = result
            _loading.value = false
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            val result = orderRepository.cancelOrder(orderId)
            _orderAction.value = result
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            val result = orderRepository.deleteOrder(orderId)
            _orderAction.value = result
        }
    }

    fun resendOrder(orderId: String) {
        viewModelScope.launch {
            val result = orderRepository.resendOrder(orderId)
            _orderAction.value = result
        }
    }
}
