# Hilt & Compose Splash Screen Setup Guide

## 1. Add Required Dependencies

Update `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    // ... existing config
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Existing dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Hilt Navigation Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
```

## 2. Update AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".DuralapApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DuralapApp">
        
        <!-- Splash Activity - Launcher -->
        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.DuralapApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="false" />
        
        <!-- Login Activity -->
        <activity
            android:name=".LoginActivity"
            android:exported="false" />
    </application>

</manifest>
```

## 3. Project Structure

```
app/src/main/java/com/example/duralapapp/
├── data/
│   ├── api/
│   │   ├── AuthApi.kt
│   │   ├── AuthInterceptor.kt
│   │   ├── TokenAuthenticator.kt
│   │   └── RetrofitClient.kt
│   ├── local/
│   │   ├── TokenManager.kt
│   │   └── TokenValidator.kt
│   ├── model/
│   │   └── ... (model files)
│   └── repository/
│       ├── AuthRepository.kt
│       └── TokenRepository.kt
├── di/
│   └── AppModule.kt                    # Hilt dependency injection
├── ui/
│   └── splash/
│       ├── SplashUiState.kt            # UI State
│       ├── SplashViewModel.kt          # Hilt ViewModel
│       └── SplashScreen.kt             # Compose UI
├── utils/
│   └── AppStartupManager.kt
├── DuralapApplication.kt               # Hilt Application
├── SplashActivity.kt                   # Compose Activity
├── LoginActivity.kt
└── MainActivity.kt
```

## 4. Architecture Pattern

### MVVM with Hilt + Compose

```
SplashActivity (View)
    ↓
@AndroidEntryPoint
    ↓
SplashViewModel (ViewModel)
    ↓
@Inject constructor
    ↓
AppModule (Hilt Module)
    ↓
Provides dependencies
    ↓
Repositories & Managers
```

## 5. UI State Management

### SplashUiState
```kotlin
data class SplashUiState(
    val isLoading: Boolean = true,
    val statusText: String = "Initializing System...",
    val progress: Float = 0f,
    val isNavigationReady: Boolean = false,
    val navigationDestination: NavigationDestination = NavigationDestination.Loading
)
```

### Navigation Destinations
```kotlin
sealed class NavigationDestination {
    object Loading : NavigationDestination()
    object Home : NavigationDestination()
    object Login : NavigationDestination()
    object Error : NavigationDestination()
}
```

## 6. How It Works

### App Startup Flow

1. **Application Starts**
   - `DuralapApplication.onCreate()` initializes RetrofitClient
   
2. **SplashActivity Launches**
   - `@AndroidEntryPoint` enables Hilt injection
   - ViewModel injected automatically
   
3. **ViewModel Initialization**
   - `init` block calls `initializeApp()`
   - Animates progress bar
   - Validates tokens via `AppStartupManager`
   
4. **UI State Updates**
   - Progress animates from 0 to 1
   - Status text changes based on result
   - Navigation flag set when ready
   
5. **Compose Observes State**
   - `collectAsState()` collects UI state
   - UI recomposes automatically
   - Navigation triggered when ready

### Data Flow

```
User opens app
    ↓
SplashActivity created
    ↓
Hilt injects ViewModel
    ↓
ViewModel.init() → initializeApp()
    ↓
Update UI state (progress animation)
    ↓
Validate tokens
    ↓
Update UI state (navigation ready)
    ↓
Compose observes state change
    ↓
LaunchedEffect triggers navigation
    ↓
Navigate to Home or Login
```

## 7. Key Features

### ✅ Hilt Dependency Injection
- Automatic dependency management
- Singleton pattern for repositories
- Clean architecture

### ✅ Dedicated UI State
- `SplashUiState` data class
- Immutable state updates
- Type-safe navigation

### ✅ Compose UI
- Declarative UI
- Animated logo
- Progress bar animation
- Material 3 design

### ✅ StateFlow
- Reactive state management
- Lifecycle-aware
- Auto-completion on navigation

## 8. Benefits

1. **Separation of Concerns**
   - UI State separate from UI
   - ViewModel handles logic
   - Activity only observes

2. **Testability**
   - Easy to unit test ViewModel
   - Mock dependencies with Hilt

3. **Maintainability**
   - Clear data flow
   - Predictable state management
   - Scalable architecture

4. **Modern Android**
   - Jetpack Compose
   - Hilt DI
   - Coroutines & Flow
   - MVVM pattern

## 9. Next Steps

1. Add error handling UI
2. Implement retry mechanism
3. Add loading states for other screens
4. Create dedicated UI states for each screen
5. Add navigation component
6. Implement deep linking
