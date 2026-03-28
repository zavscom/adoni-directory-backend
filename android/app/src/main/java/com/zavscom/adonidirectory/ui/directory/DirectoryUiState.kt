package com.zavscom.adonidirectory.ui.directory

data class CatalogCategoryUi(
    val id: String,
    val title: String,
    val emoji: String,
    val count: Int,
)

data class DirectoryUiState(
    val catalogCategories: List<CatalogCategoryUi> = emptyList(),
    val searchQuery: String = "",
    val lastUpdated: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)
