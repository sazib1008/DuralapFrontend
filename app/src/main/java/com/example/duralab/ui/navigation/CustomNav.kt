package com.example.duralab.ui.navigation

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.duralab.ui.auth.AuthViewModel
import com.example.duralab.ui.auth.LoginScreen
import com.example.duralab.ui.auth.RegisterScreen
import com.example.duralab.ui.call.CallHistoryScreen
import com.example.duralab.ui.call.CallScreen
import com.example.duralab.ui.call.CallViewModel
import com.example.duralab.ui.chat.ChatScreen
import com.example.duralab.ui.chat.ChatViewModel
import com.example.duralab.ui.dashboard.DashboardScreen
import com.example.duralab.ui.dashboard.DashboardViewModel
import com.example.duralab.ui.profile.ProfileScreen
import com.example.duralab.ui.splash.SplashScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.reflect.KClass

// 1. Routes
sealed class AppRoute : Parcelable {
    sealed class Main : AppRoute() {
        @Parcelize object Home : Main()
        @Parcelize object Calls : Main()
        @Parcelize object Profile : Main()
    }

    @Parcelize object Splash : AppRoute()
    @Parcelize object Login : AppRoute()
    @Parcelize object Register : AppRoute()

    @Parcelize data class Chat(val chatId: String) : AppRoute()
    @Parcelize data class Call(val userId: String) : AppRoute()
}

interface RequiresLogin

// 2. AppBackStack
class AppBackStack<T : Any>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String,
    private val startRoute: T,
    private val loginRoute: T
) {
    private var _backStackList = savedStateHandle.get<List<T>>(key)?.toMutableStateList() 
        ?: mutableStateListOf(startRoute)

    val backStack: SnapshotStateList<T> = _backStackList

    var isLoggedIn by mutableStateOf(false)
    var onLoginSuccessRoute: T? = null

    private fun saveState() {
        savedStateHandle[key] = ArrayList(backStack)
    }

    fun add(route: T) {
        if (route is RequiresLogin && !isLoggedIn) {
            onLoginSuccessRoute = route
            if (backStack.lastOrNull() != loginRoute) {
                backStack.add(loginRoute)
            }
        } else {
            backStack.add(route)
        }
        saveState()
    }

    fun remove(): Boolean {
        if (backStack.size > 1) {
            val removed = backStack.removeLastOrNull() != null
            saveState()
            return removed
        }
        return false
    }

    fun login() {
        isLoggedIn = true
        onLoginSuccessRoute?.let {
            backStack.remove(loginRoute)
            backStack.add(it)
            onLoginSuccessRoute = null
        }
        saveState()
    }
    
    fun resetToLogin() {
        isLoggedIn = false
        backStack.clear()
        backStack.add(loginRoute)
        saveState()
    }
}

// 3. TopLevelBackStack
class TopLevelBackStack<T : AppRoute>(
    private val startKey: AppRoute.Main
) {
    private val topLevelStacks = mutableMapOf<AppRoute.Main, MutableList<T>>()

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf<T>()

    init {
        addTopLevel(startKey)
    }

    fun addTopLevel(key: AppRoute.Main) {
        if (!topLevelStacks.containsKey(key)) {
            topLevelStacks[key] = mutableListOf(key as T)
        }
        topLevelKey = key
        syncBackStack()
    }

    fun add(route: T) {
        topLevelStacks[topLevelKey]?.add(route)
        syncBackStack()
    }

    fun removeLast(): Boolean {
        val currentStack = topLevelStacks[topLevelKey]
        return if (currentStack != null && currentStack.size > 1) {
            currentStack.removeAt(currentStack.size - 1)
            syncBackStack()
            true
        } else false
    }

    private fun syncBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStacks[topLevelKey] ?: emptyList())
    }
    
    fun resetAll() {
        topLevelStacks.clear()
        addTopLevel(AppRoute.Main.Home)
    }
}

// 4. Entry Provider & NavDisplay
class EntryProviderScope<T : Any> {
    val entries = mutableMapOf<KClass<out T>, @Composable (T) -> Unit>()
    
    inline fun <reified R : T> entry(noinline content: @Composable (R) -> Unit) {
        entries[R::class] = { route -> content(route as R) }
    }
}

fun <T : Any> entryProvider(builder: EntryProviderScope<T>.() -> Unit): EntryProviderScope<T> {
    val scope = EntryProviderScope<T>()
    scope.builder()
    return scope
}

@Composable
fun <T : Any> NavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    entryProvider: EntryProviderScope<T>
) {
    val currentRoute = backStack.lastOrNull() ?: return
    Box(modifier = modifier) {
        val entry = entryProvider.entries[currentRoute::class]
        if (entry != null) {
            entry(currentRoute)
        }
    }
}

// 5. ViewModel
@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val authBackStack = AppBackStack<AppRoute>(
        savedStateHandle = savedStateHandle,
        key = "auth_stack",
        startRoute = AppRoute.Splash, // start route
        loginRoute = AppRoute.Login
    )

    val mainBackStack = TopLevelBackStack<AppRoute>(
        startKey = AppRoute.Main.Home
    )
}

// 6. Navigation App
@Composable
fun AppNavigation(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val authBackStack = viewModel.authBackStack
    val mainBackStack = viewModel.mainBackStack

    BackHandler {
        if (authBackStack.isLoggedIn) {
            if (!mainBackStack.removeLast()) {
                // Exit app or minimize logic goes here.
            }
        } else {
            authBackStack.remove()
        }
    }

    Scaffold(
        bottomBar = {
            if (authBackStack.isLoggedIn) {
                BottomNavigationBar(
                    currentTab = mainBackStack.topLevelKey,
                    onTabSelected = { mainBackStack.addTopLevel(it) }
                )
            }
        }
    ) { padding ->
        NavDisplay(
            backStack = if (authBackStack.isLoggedIn) mainBackStack.backStack else authBackStack.backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {
                // Auth & Pre-login
                entry<AppRoute.Splash> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    SplashScreen(
                        viewModel = authViewModel,
                        onNavigateToDashboard = {
                            authBackStack.login()
                        },
                        onNavigateToLogin = {
                            authBackStack.add(AppRoute.Login)
                        }
                    )
                }
                
                entry<AppRoute.Login> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    LoginScreen(
                        viewModel = authViewModel,
                        biometricHelper = authViewModel.biometricHelper,
                        onLoginSuccess = {
                            authBackStack.login()
                        },
                        onNavigateToRegister = {
                            authBackStack.add(AppRoute.Register)
                        }
                    )
                }
                
                entry<AppRoute.Register> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            authBackStack.login()
                        },
                        onNavigateToLogin = {
                            authBackStack.remove()
                        }
                    )
                }

                // Main Dashboard
                entry<AppRoute.Main.Home> {
                    val dashViewModel: DashboardViewModel = hiltViewModel()
                    DashboardScreen(
                        viewModel = dashViewModel,
                        onNavigateToChat = { chatId ->
                            mainBackStack.add(AppRoute.Chat(chatId))
                        },
                        onNavigateToProfile = {
                            mainBackStack.addTopLevel(AppRoute.Main.Profile)
                        }
                    )
                }

                // Call History
                entry<AppRoute.Main.Calls> {
                    CallHistoryScreen(
                        onNavigateBack = {
                            mainBackStack.removeLast()
                        },
                        onNavigateToCall = { userId ->
                            mainBackStack.add(AppRoute.Call(userId))
                        }
                    )
                }

                // Profile
                entry<AppRoute.Main.Profile> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    ProfileScreen(
                        onNavigateBack = {
                            mainBackStack.removeLast()
                        },
                        onLogout = {
                            authViewModel.logout()
                            mainBackStack.resetAll()
                            authBackStack.resetToLogin()
                        }
                    )
                }

                // Chat
                entry<AppRoute.Chat> { route ->
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    ChatScreen(
                        chatId = route.chatId,
                        viewModel = chatViewModel,
                        onNavigateBack = {
                            mainBackStack.removeLast()
                        },
                        onNavigateToCall = { userId ->
                            mainBackStack.add(AppRoute.Call(userId))
                        }
                    )
                }

                // Call
                entry<AppRoute.Call> { route ->
                    val callViewModel: CallViewModel = hiltViewModel()
                    CallScreen(
                        userId = route.userId,
                        viewModel = callViewModel,
                        onNavigateBack = {
                            mainBackStack.removeLast()
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: AppRoute.Main,
    onTabSelected: (AppRoute.Main) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = currentTab == AppRoute.Main.Home,
            onClick = { onTabSelected(AppRoute.Main.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Call, contentDescription = "Calls") },
            label = { Text("Calls") },
            selected = currentTab == AppRoute.Main.Calls,
            onClick = { onTabSelected(AppRoute.Main.Calls) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Profile") },
            label = { Text("Settings") },
            selected = currentTab == AppRoute.Main.Profile,
            onClick = { onTabSelected(AppRoute.Main.Profile) }
        )
    }
}
