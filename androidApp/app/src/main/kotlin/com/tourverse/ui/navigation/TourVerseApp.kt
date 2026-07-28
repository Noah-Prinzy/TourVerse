package com.tourverse.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.tourverse.AppContainer
import com.tourverse.ui.screens.*
import kotlinx.coroutines.launch
import java.util.UUID

private object Routes {
    const val Destinations = "destinations"
    const val Destination = "destination/{destinationId}"
    const val Login = "login"
    const val Register = "register"
    const val Profile = "profile"
    const val Favorites = "favorites"
    const val Trips = "trips"
    const val Trip = "trip/{tripId}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourVerseApp(container: AppContainer) {
    val nav = rememberNavController()
    val session by container.sessionManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { container.sessionManager.restore() }
    if (session.initializing) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TourVerse") },
                actions = {
                    TextButton({ nav.navigate(Routes.Destinations) }) { Text("Explore") }
                    if (session.authenticated) {
                        TextButton({ nav.navigate(Routes.Trips) }) { Text("Trips") }
                        TextButton({ nav.navigate(Routes.Favorites) }) { Text("Favorites") }
                        TextButton({ nav.navigate(Routes.Profile) }) { Text("Profile") }
                        TextButton({
                            scope.launch {
                                container.sessionManager.logout()
                                nav.navigate(Routes.Login) { popUpTo(0) }
                            }
                        }) { Text("Logout") }
                    } else {
                        TextButton({ nav.navigate(Routes.Login) }) { Text("Login") }
                    }
                }
            )
        }
    ) { padding ->
        NavHost(nav, Routes.Destinations, Modifier.padding(padding)) {
            composable(Routes.Destinations) {
                HomeScreen(onDestinationClick = { nav.navigate("destination/$it") })
            }
            composable(Routes.Destination) { entry ->
                val id = entry.arguments?.getString("destinationId")
                if (!id.isUuid()) PlaceholderScreen("Invalid destination", "The destination ID is invalid.")
                else {
                    val model: DestinationDetailViewModel = viewModel(factory = factory {
                        DestinationDetailViewModel(id!!, session.user?.id, container.communityRepository)
                    })
                    DestinationDetailScreen(model, session.authenticated, session.user?.id) { nav.navigate(Routes.Login) }
                }
            }
            composable(Routes.Login) {
                val model: AuthViewModel = viewModel(factory = factory {
                    AuthViewModel(container.authRepository, container.sessionManager)
                })
                LoginScreen(model, { nav.navigate(Routes.Destinations) { popUpTo(Routes.Login) { inclusive = true } } }, { nav.navigate(Routes.Register) })
            }
            composable(Routes.Register) {
                val model: AuthViewModel = viewModel(factory = factory {
                    AuthViewModel(container.authRepository, container.sessionManager)
                })
                RegisterScreen(model, { nav.navigate(Routes.Profile) { popUpTo(Routes.Register) { inclusive = true } } }, { nav.navigate(Routes.Login) })
            }
            composable(Routes.Profile) {
                Protected(session.authenticated, nav) {
                    val model: ProfileViewModel = viewModel(factory = factory {
                        ProfileViewModel(container.profileRepository, container.sessionManager)
                    })
                    ProfileScreen(model) { nav.navigate(Routes.Register) { popUpTo(0) } }
                }
            }
            composable(Routes.Trips) {
                Protected(session.authenticated, nav) {
                    val model: TripsViewModel = viewModel(factory = factory { TripsViewModel(container.tripRepository) })
                    TripsScreen(model) { nav.navigate("trip/$it") }
                }
            }
            composable(Routes.Favorites) {
                Protected(session.authenticated, nav) {
                    val model: FavoritesViewModel = viewModel(factory = factory { FavoritesViewModel(container.communityRepository) })
                    FavoritesScreen(model) { nav.navigate("destination/$it") }
                }
            }
            composable(Routes.Trip) { entry ->
                Protected(session.authenticated, nav) {
                    val id = entry.arguments?.getString("tripId")
                    if (!id.isUuid()) PlaceholderScreen("Invalid trip", "The trip ID is invalid.")
                    else {
                        val model: TripsViewModel = viewModel(factory = factory { TripsViewModel(container.tripRepository, id) })
                        TripDetailScreen(
                            model,
                            onDeleted = { nav.navigate(Routes.Trips) { popUpTo(Routes.Trips) { inclusive = true } } },
                            openDestination = { nav.navigate("destination/$it") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Protected(authenticated: Boolean, nav: NavHostController, content: @Composable () -> Unit) {
    if (!authenticated) {
        LaunchedEffect(Unit) { nav.navigate(Routes.Login) }
    } else content()
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(message)
    }
}

private fun String?.isUuid() = this != null && runCatching { UUID.fromString(this) }.isSuccess

private fun <T : ViewModel> factory(create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
