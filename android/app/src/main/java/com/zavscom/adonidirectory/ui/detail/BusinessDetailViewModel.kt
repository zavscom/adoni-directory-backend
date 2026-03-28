package com.zavscom.adonidirectory.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import com.zavscom.adonidirectory.data.repository.BusinessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BusinessDetailViewModel(
    repository: BusinessRepository,
    businessId: String,
) : ViewModel() {

    val business: StateFlow<BusinessEntity?> =
        repository.observeBusinessById(businessId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class BusinessDetailViewModelFactory(
    private val repository: BusinessRepository,
    private val businessId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(BusinessDetailViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return BusinessDetailViewModel(repository, businessId) as T
    }
}
