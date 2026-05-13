package com.fitme.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitme.app.data.model.DressAnnotation
import com.fitme.app.data.repository.AnnotationRepository
import kotlinx.coroutines.launch

class AnnotationViewModel : ViewModel() {

    private val repository = AnnotationRepository()

    private val _annotations = MutableLiveData<List<DressAnnotation>>(emptyList())
    val annotations: LiveData<List<DressAnnotation>> = _annotations

    private val _saveResult = MutableLiveData<Result<DressAnnotation>>()
    val saveResult: LiveData<Result<DressAnnotation>> = _saveResult

    fun loadAnnotations(customerId: String) {
        repository.listenToAnnotations(customerId) { list ->
            _annotations.postValue(list)
        }
    }

    fun saveAnnotation(annotation: DressAnnotation) {
        viewModelScope.launch {
            val result = repository.saveAnnotation(annotation)
            _saveResult.postValue(result)
        }
    }

    fun deleteAnnotation(annotationId: String) {
        viewModelScope.launch {
            repository.deleteAnnotation(annotationId)
        }
    }

    fun clearAllAnnotations(customerId: String) {
        viewModelScope.launch {
            repository.deleteAllUserAnnotations(customerId)
        }
    }
}
