# Complete Authentication System Overview

## 📁 Project Structure

```
app/src/main/java/com/example/duralapapp/
├── data/
│   ├── api/
│   │   ├── AuthApi.kt                    # API interface
│   │   ├── AuthInterceptor.kt            # Adds token to requests
│   │   ├── TokenAuthenticator.kt         # Auto-refresh on 401
│   │   └── RetrofitClient.kt             # Retrofit setup
│   ├── local/
│   │   ├── TokenManager.kt               # DataStore token storage
│   │   └── TokenValidator.kt             # Token validation logic
│   ├── model/
│   │   ├── AuthResponse.kt               # Auth data models
│   │   ├── MessageModels.kt              # Message data models
│   │   ├── ConversationModels.kt         # Conversation models
│   │   └── CallModels.kt                 # Call models
│   └── repository/
│       ├── AuthRepository.kt             # Auth API operations
│       └── TokenRepository.kt            # Token management
├── utils/
│   └── AppStartupManager.kt              # Startup validation
├── SplashActivity.kt                     # Entry point
├── LoginActivity.kt                      # Login screen
└── MainActivity.kt                       # Main screen
```

## 🔄 Complete Authentication Flow

### 1. App Startup Flow

```
App Launch
    ↓
SplashActivity.onCreate()
    ↓
AppStartupManager.validateAppStartup()
    ↓
Check if tokens exist in DataStore
    ↓
┌─────────────────┬──────────────────┐
│                 │                  │
No tokens      Tokens valid      Tokens expired
│                 │                  │
│                 │                  ↓
│                 │             Refresh API call
│                 │                  ↓
│                 │            ┌─────┴─────┐
│                 │            │           │
│                 │         Success      Fail
│                 │            │           │
↓                 ↓            ↓           ↓
Navigate      Navigate     Save       Clear
to Login      to Home      tokens     tokens
                             │           │
                             ↓           ↓
                         Navigate    Navigate
                         to Home     to Login
```

### 2. Login Flow

```
User enters credentials
    ↓
LoginActivity.performLogin()
    ↓
AuthRepository.login()
    ↓
POST /api/auth/login
    ↓
Backend validates & returns tokens
    ↓
TokenManager.saveTokens() → DataStore
    ↓
Navigate to MainActivity
```

### 3. Automatic Token Refresh (During API Calls)

```
API Request
    ↓
AuthInterceptor adds access token
    ↓
Request sent to server
    ↓
Server responds 401 Unauthorized
    ↓
TokenAuthenticator intercepts
    ↓
Get refresh token from DataStore
    ↓
POST /api/auth/refresh
    ↓
┌─────────┬──────────┐
│                    │
Success            Fail
│                    │
↓                    ↓
Save new          Clear tokens
tokens
│                    │
↓                    ↓
Retry original    Return null
request           (user logged out)
    ↓
Return response
```

### 4. Logout Flow

```
User clicks logout
    ↓
AuthRepository.logout()
    ↓
POST /api/auth/logout (optional)
    ↓
TokenManager.clearTokens() → DataStore
    ↓
Navigate to LoginActivity
```

## 🛠️ Key Components

### TokenManager
- **Purpose**: Store and retrieve tokens using DataStore
- **Storage**: Access token, refresh token, token type, expires in, user ID
- **Methods**:
  - `saveTokens()` - Save all token data
  - `updateAccessToken()` - Update only access token
  - `clearTokens()` - Remove all tokens
  - `hasTokens()` - Check if tokens exist
  - Flows: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `userId`

### AuthInterceptor
- **Purpose**: Automatically add authorization header
- **When**: Before every API request
- **Skips**: Login and register endpoints
- **Gets token from**: DataStore via TokenManager

### TokenAuthenticator
- **Purpose**: Automatic token refresh on 401
- **When**: Server returns 401 Unauthorized
- **Thread-safe**: Uses `synchronized` and `AtomicBoolean`
- **Actions**:
  - Calls refresh API
  - Saves new tokens
  - Retries failed request
  - Clears tokens if refresh fails

### AppStartupManager
- **Purpose**: Handle app startup token validation
- **Returns**: `StartupResult` sealed class
  - `NavigateToHome` - Token valid
  - `TokenRefreshed` - Token was refreshed
  - `NavigateToLogin` - No token or refresh failed
  - `Error` - Exception occurred

### TokenValidator
- **Purpose**: Validate token status
- **Methods**:
  - `isTokenValid()` - Check if token is valid
  - `hasRefreshToken()` - Check if refresh token exists
  - `isLoggedIn()` - Check if user has tokens
  - `isTokenExpired()` - Check if token expired

## 📝 Usage Examples

### 1. Initialize (Application class)
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}
```

### 2. Login
```kotlin
lifecycleScope.launch {
    authRepository.login(LoginRequest("user@email.com", "password"))
        .onSuccess { /* Navigate to home */ }
        .onFailure { /* Show error */ }
}
```

### 3. Check Auth Status
```kotlin
lifecycleScope.launch {
    if (tokenRepository.isAuthenticated()) {
        // User is authenticated
    }
}
```

### 4. Manual Refresh
```kotlin
lifecycleScope.launch {
    tokenRepository.refreshToken()
        .onSuccess { /* Token refreshed */ }
        .onFailure { /* Go to login */ }
}
```

## ✨ Features

✅ **Automatic token management**
- Tokens saved on login
- Tokens added to requests automatically
- Tokens refreshed on expiry
- Tokens cleared on logout

✅ **Secure storage**
- Uses Android DataStore
- Persistent across app restarts
- Type-safe preferences

✅ **Thread-safe**
- Synchronized token refresh
- Prevents multiple refresh requests
- Coroutines for async operations

✅ **Error handling**
- Result<T> for all operations
- Graceful degradation
- User-friendly error messages

✅ **Reactive**
- Flow-based token observation
- Real-time token updates
- Lifecycle-aware

## 🔒 Security Notes

1. DataStore is encrypted on modern Android versions
2. Tokens are stored in app-private storage
3. HTTPS should be used for all API calls
4. Consider adding certificate pinning
5. Token refresh prevents token theft reuse

## 🚀 Next Steps

1. Add JWT decoding for proper expiration checking
2. Implement biometric authentication
3. Add token encryption
4. Implement refresh token rotation
5. Add network connectivity checks
6. Implement retry logic with exponential backoff
