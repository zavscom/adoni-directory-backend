package com.zavscom.adonidirectory.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import com.zavscom.adonidirectory.data.repository.BusinessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BusinessListUiState(
    val businesses: List<BusinessEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class CategoryListViewModel(
    repository: BusinessRepository,
    catalogId: String,
) : ViewModel() {

    val uiState: StateFlow<BusinessListUiState> =
        repository.getAllBusinesses()
            .map { all ->
                BusinessListUiState(
                    businesses = all
                        .filter { CatalogDefinitions.matchesCatalogId(it.category, catalogId) }
                        .sortedBy { it.name },
                    isLoading = false,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BusinessListUiState(),
            )
}

class CategoryListViewModelFactory(
    private val repository: BusinessRepository,
    private val catalogId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CategoryListViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return CategoryListViewModel(repository, catalogId) as T
    }
}

class SearchListViewModel(
    repository: BusinessRepository,
    query: String,
) : ViewModel() {

    private val normalizedQuery = query.trim()

    val uiState: StateFlow<BusinessListUiState> =
        repository.getAllBusinesses()
            .map { all ->
                BusinessListUiState(
                    businesses = all
                        .filter { it.matchesSearch(normalizedQuery) }
                        .sortedBy { it.name },
                    isLoading = false,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BusinessListUiState(),
            )
}

class SearchListViewModelFactory(
    private val repository: BusinessRepository,
    private val query: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SearchListViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return SearchListViewModel(repository, query) as T
    }
}

private fun BusinessEntity.matchesSearch(q: String): Boolean {
    val s = q.trim()
    if (s.isEmpty()) return true
    return name.contains(s, ignoreCase = true) ||
        area.contains(s, ignoreCase = true) ||
        address.contains(s, ignoreCase = true) ||
        category.contains(s, ignoreCase = true)
}
