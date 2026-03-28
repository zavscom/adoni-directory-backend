package com.zavscom.adonidirectory.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import com.zavscom.adonidirectory.data.repository.BusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class DirectoryViewModel(
    private val repository: BusinessRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow("All")
    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)

    private val categoriesFlow: StateFlow<List<String>> =
        repository.observeDistinctCategories()
            .map { listOf("All") + it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("All"))

    private val businessesFlow: StateFlow<List<BusinessEntity>> =
        combine(selectedCategory, searchQuery) { cat, q -> cat to q }
            .flatMapLatest { (cat, q) ->
                when {
                    q.isNotBlank() -> repository.searchBusinesses(q)
                    cat == "All" -> repository.getAllBusinesses()
                    else -> repository.getBusinessesByCategory(cat)
                }
            }
            .onEach { isLoading.value = false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val lastUpdatedFlow: StateFlow<String?> =
        repository.observeLastSyncAt()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<DirectoryUiState> =
        combine(
            categoriesFlow,
            selectedCategory,
            searchQuery,
            businessesFlow,
            lastUpdatedFlow
        ) { categories, sel, query, businesses, last ->
            DirectoryUiState(
                categories = categories,
                selectedCategory = sel,
                searchQuery = query,
                businesses = businesses,
                lastUpdated = last,
                isLoading = false // Will be updated below
            )
        }.combine(isLoading) { state, loading ->
            state.copy(isLoading = loading)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DirectoryUiState(),
        )

    fun onCategorySelected(category: String) {
        selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}

class DirectoryViewModelFactory(
    private val repository: BusinessRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DirectoryViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return DirectoryViewModel(repository) as T
    }
}
