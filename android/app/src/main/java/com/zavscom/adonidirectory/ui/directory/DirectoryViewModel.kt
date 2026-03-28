package com.zavscom.adonidirectory.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zavscom.adonidirectory.data.repository.BusinessRepository
import com.zavscom.adonidirectory.sync.FULL_URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DirectoryViewModel(
    private val repository: BusinessRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)

    private val allBusinesses = repository.getAllBusinesses()
        .onEach { isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val catalogUiFlow: StateFlow<List<CatalogCategoryUi>> =
        allBusinesses.map { all ->
            val more = CatalogDefinitions.moreEntry()
            CatalogDefinitions.entries.map { e ->
                CatalogCategoryUi(
                    id = e.id,
                    title = e.title,
                    emoji = e.emoji,
                    count = all.count { CatalogDefinitions.matchesCatalogId(it.category, e.id) },
                )
            } + CatalogCategoryUi(
                id = more.id,
                title = more.title,
                emoji = more.emoji,
                count = all.count { CatalogDefinitions.matchesMoreBucket(it.category) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val lastUpdatedFlow: StateFlow<String?> =
        repository.observeLastSyncAt()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Category display names with counts (same data as [DirectoryUiState.catalogCategories]). */
    val categoriesWithCount: StateFlow<List<Pair<String, Int>>> =
        catalogUiFlow.map { list -> list.map { it.title to it.count } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DirectoryUiState> =
        combine(catalogUiFlow, searchQuery, lastUpdatedFlow, isLoading, isRefreshing) { c, q, last, loading, refreshing ->
            DirectoryUiState(
                catalogCategories = c,
                searchQuery = q,
                lastUpdated = last,
                isLoading = loading,
                isRefreshing = refreshing,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DirectoryUiState(),
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun refreshFromRemote() {
        viewModelScope.launch {
            isRefreshing.update { true }
            try {
                repository.syncFromRemote(FULL_URL)
            } finally {
                isRefreshing.update { false }
            }
        }
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
