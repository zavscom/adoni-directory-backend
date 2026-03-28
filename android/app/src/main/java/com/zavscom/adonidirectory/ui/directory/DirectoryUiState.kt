package com.zavscom.adonidirectory.ui.directory

import com.zavscom.adonidirectory.data.local.entity.BusinessEntity

data class DirectoryUiState(
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val businesses: List<BusinessEntity> = emptyList(),
    val lastUpdated: String? = null,
    val isLoading: Boolean = true,
)
