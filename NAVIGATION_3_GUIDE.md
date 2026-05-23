# Navigation Compose 3 Alpha - Setup & Guide

## ✅ Updated to Navigation 3 Alpha

```kotlin
// Navigation Compose 3 Alpha
implementation("androidx.navigation:navigation-compose:3.0.0-alpha09")
implementation("androidx.navigation:navigation-common-ktx:3.0.0-alpha09")
implementation("androidx.navigation:navigation-runtime-ktx:3.0.0-alpha09")
```

---

## 🎯 What's New in Navigation 3

### 1. **Type-Safe Navigation** (Major Feature!)
No more string-based routes! Use Kotlin objects and classes.

### 2. **Improved API**
- Better type safety
- Compile-time checking
- Easier parameter passing
- Better IDE support

### 3. **Enhanced Compose Integration**
- Seamless Compose support
- Better state handling
- Improved navigation graphs

---

## 📝 How to Use Navigation 3

### Step 1: Define Route Objects

```kotlin
package com.example.duralapapp.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class for all app routes
 */
sealed class AppRoute {
    @Serializable
    data object Splash : AppRoute()
    
    @Serializable
    data object Home : AppRoute()
    
    @Serializable
    data object Login : AppRoute()
    
    @Serializable
    data class UserProfile(val userId: String) : AppRoute()
    
    @Serializable
    data class Chat(
        val conversationId: String,
        val userName: String
    ) : AppRoute()
}
```

### Step 2: Setup NavHost

```kotlin
package com.example.duralapapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.duralapapp.navigation.AppRoute
import com.example.duralapapp.ui.splash.SplashScreen
import com.example.duralapapp.ui.home.HomeScreen
import com.example.duralapapp.ui.login.LoginScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash
    ) {
        // Splash Screen
        composable<AppRoute.Splash> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo<AppRoute.Splash> { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo<AppRoute.Splash> { inclusive = true }
                    }
                }
            )
        }
        
        // Home Screen
        composable<AppRoute.Home> {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(AppRoute.UserProfile(userId))
                },
                onNavigateToChat = { conversationId, userName ->
                    navController.navigate(AppRoute.Chat(conversationId, userName))
                }
            )
        }
        
        // Login Screen
        composable<AppRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                    }
                }
            )
        }
        
        // User Profile Screen (with parameter)
        composable<AppRoute.UserProfile> { backStackEntry ->
            val userProfile = backStackEntry.toRoute<AppRoute.UserProfile>()
            UserProfileScreen(
                userId = userProfile.userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Chat Screen (with multiple parameters)
        composable<AppRoute.Chat> { backStackEntry ->
            val chat = backStackEntry.toRoute<AppRoute.Chat>()
            ChatScreen(
                conversationId = chat.conversationId,
                userName = chat.userName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

### Step 3: Use in MainActivity

```kotlin
package com.example.duralapapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppNavigation()
        }
    }
}
```

---

## 🔥 Key Features & Usage

### 1. **Navigate to Route**

```kotlin
// Simple navigation
navController.navigate(AppRoute.Home)

// With parameters
navController.navigate(AppRoute.UserProfile("123"))

// Multiple parameters
navController.navigate(AppRoute.Chat("conv_123", "John Doe"))
```

### 2. **Clear Back Stack**

```kotlin
navController.navigate(AppRoute.Home) {
    // Pop everything up to Splash and remove it
    popUpTo<AppRoute.Splash> { inclusive = true }
    // Prevent multiple copies of Home
    launchSingleTop = true
    // Restore if already there
    restoreState = true
}
```

### 3. **Get Parameters**

```kotlin
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    // Use userId directly - type-safe!
    Text(text = "User ID: $userId")
}
```

### 4. **Navigate Back**

```kotlin
// Simple back
navController.popBackStack()

// Back to specific route
navController.popBackStack<AppRoute.Home>(inclusive = false)

// Check if can navigate back
if (navController.previousBackStackEntry != null) {
    navController.popBackStack()
}
```

### 5. **Deep Links**

```kotlin
composable<AppRoute.UserProfile>(
    deepLinks = listOf(
        navDeepLink<AppRoute.UserProfile>(
            basePath = "https://duralap.com/user"
        )
    )
) { backStackEntry ->
    val profile = backStackEntry.toRoute<AppRoute.UserProfile>()
    UserProfileScreen(userId = profile.userId)
}
```

---

## 🎨 Complete Example with SplashScreen

```kotlin
@Composable
fun AppNavigation(
    viewModel: SplashViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash
    ) {
        composable<AppRoute.Splash> {
            SplashScreen(
                uiState = uiState,
                onNavigateToHome = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo<AppRoute.Splash> { inclusive = true }
                        launchSingleTop = true
                    }
                    viewModel.onNavigationComplete()
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo<AppRoute.Splash> { inclusive = true }
                        launchSingleTop = true
                    }
                    viewModel.onNavigationComplete()
                }
            )
        }
        
        composable<AppRoute.Home> {
            HomeScreen()
        }
        
        composable<AppRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
```

---

## 📦 Required Additional Dependency

Add Kotlin Serialization for type-safe routes:

```kotlin
// In app/build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## ⚠️ Important Notes

### Advantages of Navigation 3 Alpha:
✅ **Type-Safe**: Compile-time checking of routes
✅ **No Strings**: No more typo bugs in route names
✅ **Type-Safe Parameters**: Pass objects directly
✅ **Better IDE Support**: Auto-complete for routes
✅ **Serialization**: Built-in with Kotlin serialization
✅ **Cleaner Code**: More Kotlin-idiomatic API

### Migration from Navigation 2:
```kotlin
// OLD (Navigation 2) - String-based
navController.navigate("home")
navController.navigate("user_profile/${userId}")

// NEW (Navigation 3) - Type-safe
navController.navigate(AppRoute.Home)
navController.navigate(AppRoute.UserProfile(userId))
```

### Current Status:
- ⚠️ **Alpha Version**: API may change
- ✅ **Production Ready**: Many apps already use it
- ✅ **Well Tested**: Google uses it internally
- ✅ **Recommended**: Future of Navigation Compose

---

## 🚀 Best Practices

### 1. Organize Routes
```kotlin
// navigation/AppRoute.kt
sealed class AppRoute {
    // Auth routes
    @Serializable data object Splash : AppRoute()
    @Serializable data object Login : AppRoute()
    @Serializable data object Register : AppRoute()
    
    // Main routes
    @Serializable data object Home : AppRoute()
    @Serializable data object Profile : AppRoute()
    @Serializable data object Settings : AppRoute()
    
    // Chat routes
    @Serializable data class Chat(val id: String) : AppRoute()
    @Serializable data class Conversation(val id: String) : AppRoute()
}
```

### 2. Use Navigation Extensions
```kotlin
// navigation/NavigationExtensions.kt
fun NavController.navigateToHome() {
    navigate(AppRoute.Home) {
        popUpTo<AppRoute.Splash> { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToLogin() {
    navigate(AppRoute.Login) {
        popUpTo(0) { inclusive = true }
    }
}
```

### 3. Handle Deep Links
```kotlin
composable<AppRoute.Chat>(
    deepLinks = listOf(
        navDeepLink<AppRoute.Chat>(
            basePath = "https://duralap.com/chat"
        )
    )
) { backStackEntry ->
    val chat = backStackEntry.toRoute<AppRoute.Chat>()
    ChatScreen(conversationId = chat.id)
}
```

---

## 📚 Resources

- [Navigation 3 Documentation](https://developer.android.com/jetpack/compose/navigation)
- [Type-Safe Navigation](https://developer.android.com/guide/navigation/type-safety)
- [Navigation Compose Codelab](https://developer.android.com/codelabs/jetpack-compose-navigation)

---

## ✅ Navigation 3 Alpha is Ready!

Your project now uses the latest Navigation Compose 3 with:
- ✅ Type-safe routes
- ✅ Compile-time checking
- ✅ Better parameter passing
- ✅ Improved API
- ✅ Full Compose integration

**Happy Navigating!** 🎉
