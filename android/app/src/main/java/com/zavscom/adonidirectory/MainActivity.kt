package com.zavscom.adonidirectory

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.zavscom.adonidirectory.ui.directory.DirectoryScreen
import com.zavscom.adonidirectory.ui.directory.DirectoryViewModel
import com.zavscom.adonidirectory.ui.directory.DirectoryViewModelFactory
import com.zavscom.adonidirectory.ui.theme.TownDirectoryTheme

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
                        DirectoryScreen(
                            viewModel = directoryViewModel,
                            onBusinessClick = { b ->
                                navController.navigate("detail/${Uri.encode(b.id)}")
                            },
                        )
                    }
                    composable(
                        route = "detail/{businessId}",
                        arguments = listOf(
                            navArgument("businessId") { type = NavType.StringType },
                        ),
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
