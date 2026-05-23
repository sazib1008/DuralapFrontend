# Authentication System Setup Guide

## 1. Add Required Dependencies

Add these to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

## 2. Initialize in Application Class (Recommended)

Create `MyApplication.kt`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}
```

Update `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

## 3. Setup SplashActivity for Token Validation

### Create SplashActivity

The [SplashActivity](file:///Users/sazibhossain/AndroidStudioProjects/DuralapApp/app/src/main/java/com/example/duralapapp/SplashActivity.kt) is already created and handles:

```
App Start
   ↓
Check access token
   ↓
IF valid → Home
IF expired → Try refresh
   ↓
    IF refresh success → Save new token → Home
    IF refresh fail → Logout → Login screen
```

### Register in AndroidManifest.xml

```xml
<activity
    android:name=".SplashActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity android:name=".MainActivity" />
<activity android:name=".LoginActivity" />
```

## 4. Initialize Repository

```kotlin
val tokenManager = TokenManager(context)
val tokenValidator = TokenValidator(context)
val authRepository = AuthRepository(
    authApi = RetrofitClient.authApi,
    tokenManager = tokenManager
)
val tokenRepository = TokenRepository(
    authApi = RetrofitClient.authApi,
    tokenManager = tokenManager,
    tokenValidator = tokenValidator
)
```

## 5. Usage Examples

### Register User

```kotlin
lifecycleScope.launch {
    val result = authRepository.register(
        UserCreateRequest(
            username = "john_doe",
            email = "john@example.com",
            password = "password123",
            fullName = "John Doe"
        )
    )
    
    result.onSuccess { userResponse ->
        println("Registration successful: ${userResponse.username}")
    }.onFailure { error ->
        println("Registration failed: ${error.message}")
    }
}
```

### Login User

```kotlin
lifecycleScope.launch {
    val result = authRepository.login(
        LoginRequest(
            usernameOrEmail = "john@example.com",
            password = "password123"
        )
    )
    
    result.onSuccess { authResponse ->
        println("Login successful!")
        println("Access Token: ${authResponse.accessToken}")
        println("User: ${authResponse.user.username}")
        // Tokens are automatically saved to DataStore
    }.onFailure { error ->
        println("Login failed: ${error.message}")
    }
}
```

### Get Current User Profile

```kotlin
lifecycleScope.launch {
    val result = authRepository.getCurrentUser()
    
    result.onSuccess { userResponse ->
        println("User: ${userResponse.username}")
        println("Email: ${userResponse.email}")
    }.onFailure { error ->
        println("Failed to get profile: ${error.message}")
    }
}
```

### Logout

```kotlin
lifecycleScope.launch {
    val accessToken = tokenManager.accessToken.first()
    
    if (accessToken != null) {
        val result = authRepository.logout(accessToken)
        
        result.onSuccess {
            println("Logout successful")
            // Tokens are automatically cleared from DataStore
        }.onFailure { error ->
            println("Logout failed: ${error.message}")
        }
    }
}
```

### Check if User is Logged In

```kotlin
lifecycleScope.launch {
    val isLoggedIn = tokenManager.hasTokens()
    if (isLoggedIn) {
        // Navigate to home screen
    } else {
        // Navigate to login screen
    }
}
```

### Observe Token Changes

```kotlin
lifecycleScope.launch {
    tokenManager.accessToken.collect { token ->
        if (token != null) {
            println("Access token updated: $token")
        } else {
            println("Token cleared")
        }
    }
}
```

### Manual Token Refresh

```kotlin
lifecycleScope.launch {
    val result = tokenRepository.refreshToken()
    
    result.onSuccess { authResponse ->
        println("Token refreshed successfully")
        // Continue with authenticated operations
    }.onFailure { error ->
        println("Token refresh failed, redirect to login")
        // Navigate to login screen
    }
}
```

### Check Authentication Status

```kotlin
lifecycleScope.launch {
    val isAuthenticated = tokenRepository.isAuthenticated()
    if (isAuthenticated) {
        // User is authenticated
    } else {
        // User needs to login
    }
}
```

## 6. Automatic Token Refresh

The system automatically handles token refresh in two ways:

### 1. AuthInterceptor (Before Request)
- Adds access token to every request automatically
- Skips login and register endpoints

### 2. TokenAuthenticator (On 401 Response)
- Detects 401 Unauthorized responses
- Automatically calls refresh token API
- Saves new tokens to DataStore
- Retries the failed request with new token
- If refresh fails, clears tokens and stops retry

### 3. AppStartupManager (On App Start)
- Validates token when app starts
- Refreshes if expired
- Navigates to appropriate screen

## 7. How It Works

### AuthInterceptor
- Automatically adds `Authorization: Bearer <token>` header to all requests
- Skips login and register endpoints
- Gets token from DataStore

### TokenAuthenticator
- Automatically refreshes token when receiving 401 Unauthorized
- Uses refresh token to get new access token
- Retries failed request with new token
- Clears tokens if refresh fails

### TokenManager
- Stores tokens securely using DataStore
- Provides Flow for reactive token observation
- Handles save/update/clear operations

## 8. Architecture Flow

```
App Launch (SplashActivity)
    ↓
AppStartupManager.validateAppStartup()
    ↓
Check tokens in DataStore
    ↓
IF valid → Navigate to Home
IF expired → Call refresh API
    ↓
    IF success → Save tokens → Navigate to Home
    IF fail → Clear tokens → Navigate to Login
    
User Login
    ↓
AuthRepository.login()
    ↓
API Call → Backend
    ↓
Save tokens to DataStore (TokenManager)
    ↓
Subsequent requests:
    - AuthInterceptor adds token automatically
    - TokenAuthenticator refreshes if 401
    ↓
Logout
    ↓
Clear tokens from DataStore
```

## 9. Important Notes

1. **Thread Safety**: TokenAuthenticator uses `synchronized` and `AtomicBoolean` to prevent multiple simultaneous refresh requests

2. **Coroutines**: All repository methods are suspend functions - call from coroutine scope

3. **Error Handling**: All methods return `Result<T>` for proper error handling

4. **Token Storage**: DataStore is encrypted and secure for token storage

5. **Automatic Refresh**: TokenAuthenticator handles 401 responses automatically
