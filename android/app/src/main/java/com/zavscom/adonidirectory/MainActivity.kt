package com.zavscom.adonidirectory

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zavscom.adonidirectory.di.DirectoryAppServices
import com.zavscom.adonidirectory.ui.detail.BusinessDetailScreen
import com.zavscom.adonidirectory.ui.detail.BusinessDetailViewModel
import com.zavscom.adonidirectory.ui.detail.BusinessDetailViewModelFactory
import com.zavscom.adonidirectory.ui.directory.CatalogDefinitions
import com.zavscom.adonidirectory.ui.directory.CategoryListScreen
import com.zavscom.adonidirectory.ui.directory.CategoryListViewModel
import com.zavscom.adonidirectory.ui.directory.CategoryListViewModelFactory
import com.zavscom.adonidirectory.ui.directory.DirectoryScreen
import com.zavscom.adonidirectory.ui.directory.DirectoryViewModel
import com.zavscom.adonidirectory.ui.directory.DirectoryViewModelFactory
import com.zavscom.adonidirectory.ui.directory.SearchListViewModel
import com.zavscom.adonidirectory.ui.directory.SearchListViewModelFactory
import com.zavscom.adonidirectory.ui.theme.TownDirectoryTheme

private val listEnter = fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it }
private val listExit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 2 }
private val listPopEnter = fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 2 }
private val listPopExit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TownDirectoryTheme {
                val navController = rememberNavController()
                val repository = remember { DirectoryAppServices.repository }
                val directoryFactory = remember { DirectoryViewModelFactory(repository) }
                val directoryViewModel = viewModel<DirectoryViewModel>(factory = directoryFactory)

                NavHost(
                    navController = navController,
                    startDestination = "directory",
                ) {
                    composable("directory") {
                        val dirState by directoryViewModel.uiState.collectAsStateWithLifecycle()
                        DirectoryScreen(
                            viewModel = directoryViewModel,
                            onCategoryClick = { categoryId ->
                                val q = dirState.searchQuery.trim()
                                if (q.isEmpty()) {
                                    navController.navigate("directory/${Uri.encode(categoryId)}")
                                } else {
                                    navController.navigate("search/${Uri.encode(q)}")
                                }
                            },
                        )
                    }
                    composable(
                        route = "directory/{category}",
                        arguments = listOf(
                            navArgument("category") { type = NavType.StringType },
                        ),
                        enterTransition = { listEnter },
                        exitTransition = { listExit },
                        popEnterTransition = { listPopEnter },
                        popExitTransition = { listPopExit },
                    ) { entry ->
                        val raw = entry.arguments?.getString("category").orEmpty()
                        val catalogId = Uri.decode(raw)
                        val factory = remember(catalogId) {
                            CategoryListViewModelFactory(repository, catalogId)
                        }
                        val vm = viewModel<CategoryListViewModel>(
                            viewModelStoreOwner = entry,
                            key = "cat_$catalogId",
                            factory = factory,
                        )
                        val listState by vm.uiState.collectAsStateWithLifecycle()
                        val title =
                            "${CatalogDefinitions.displayTitleForCatalogId(catalogId)} (${listState.businesses.size})"
                        CategoryListScreen(
                            title = title,
                            businesses = listState.businesses,
                            isLoading = listState.isLoading,
                            onBack = { navController.popBackStack() },
                            onBusinessClick = { b ->
                                navController.navigate("detail/${Uri.encode(b.id)}")
                            },
                            onToolbarSearchClick = {
                                navController.popBackStack("directory", inclusive = false)
                            },
                        )
                    }
                    composable(
                        route = "search/{query}",
                        arguments = listOf(
                            navArgument("query") { type = NavType.StringType },
                        ),
                        enterTransition = { listEnter },
                        exitTransition = { listExit },
                        popEnterTransition = { listPopEnter },
                        popExitTransition = { listPopExit },
                    ) { entry ->
                        val raw = entry.arguments?.getString("query").orEmpty()
                        val query = Uri.decode(raw)
                        val factory = remember(query) {
                            SearchListViewModelFactory(repository, query)
                        }
                        val vm = viewModel<SearchListViewModel>(
                            viewModelStoreOwner = entry,
                            key = "search_${query.hashCode()}",
                            factory = factory,
                        )
                        val listState by vm.uiState.collectAsStateWithLifecycle()
                        val title = "\"$query\" (${listState.businesses.size})"
                        CategoryListScreen(
                            title = title,
                            businesses = listState.businesses,
                            isLoading = listState.isLoading,
                            onBack = { navController.popBackStack() },
                            onBusinessClick = { b ->
                                navController.navigate("detail/${Uri.encode(b.id)}")
                            },
                            onToolbarSearchClick = {
                                navController.popBackStack("directory", inclusive = false)
                            },
                            emptyTitle = "No matches",
                            emptySubtitle = "Try a different search or go back to browse categories.",
                        )
                    }
                    composable(
                        route = "detail/{businessId}",
                        arguments = listOf(
                            navArgument("businessId") { type = NavType.StringType },
                        ),
                        enterTransition = { listEnter },
                        exitTransition = { listExit },
                        popEnterTransition = { listPopEnter },
                        popExitTransition = { listPopExit },
                    ) { entry ->
                        val raw = entry.arguments?.getString("businessId").orEmpty()
                        val businessId = Uri.decode(raw)
                        val detailFactory = remember(businessId) {
                            BusinessDetailViewModelFactory(repository, businessId)
                        }
                        val detailVm = viewModel<BusinessDetailViewModel>(
                            viewModelStoreOwner = entry,
                            factory = detailFactory,
                        )
                        val business by detailVm.business.collectAsStateWithLifecycle()
                        BusinessDetailScreen(
                            business = business,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
