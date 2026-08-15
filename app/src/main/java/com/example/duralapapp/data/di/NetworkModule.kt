package com.example.duralapapp.data.di

import com.example.duralapapp.data.api.AuthApi
import android.content.Context
import com.example.duralapapp.data.network.AuthInterceptor
import com.example.duralapapp.data.network.RetryInterceptor
import com.example.duralapapp.data.network.StaleWhileRevalidateInterceptor
import com.example.duralapapp.data.network.TokenAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


   
//private const val BASE_URL = "http://10.0.2.2:8080/"    //private const val BASE_URL = "http://10.0.2.2:8080/"
    private const val BASE_URL = "http://192.168.0.179:8080/" // আপনার Port সহ
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(com.example.duralapapp.data.model.InstantAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideHttpCache(
        @ApplicationContext context: Context
    ): Cache {
        return Cache(context.cacheDir.resolve("http_cache"), 10L * 1024L * 1024L)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    @Provides
    @Singleton
    @Named("refreshClient")
    fun provideRefreshClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("refreshRetrofit")
    fun provideRefreshRetrofit(
        moshi: Moshi,
        @Named("refreshClient") refreshClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(refreshClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    @Named("refreshAuthApi")
    fun provideRefreshAuthApi(
        @Named("refreshRetrofit") retrofit: Retrofit
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        staleWhileRevalidateInterceptor: StaleWhileRevalidateInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .pingInterval(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(retryInterceptor)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(staleWhileRevalidateInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): com.example.duralapapp.data.api.SearchApi {
        return retrofit.create(com.example.duralapapp.data.api.SearchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideConversationRequestApi(retrofit: Retrofit): com.example.duralapapp.data.api.ConversationRequestApi {
        return retrofit.create(com.example.duralapapp.data.api.ConversationRequestApi::class.java)
    }

    @Provides
    @Singleton
    fun provideConversationApi(retrofit: Retrofit): com.example.duralapapp.data.api.ConversationApi {
        return retrofit.create(com.example.duralapapp.data.api.ConversationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): com.example.duralapapp.data.api.MessageApi {
        return retrofit.create(com.example.duralapapp.data.api.MessageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): com.example.duralapapp.data.api.UserApi {
        return retrofit.create(com.example.duralapapp.data.api.UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCallApi(retrofit: Retrofit): com.example.duralapapp.data.api.CallApi {
        return retrofit.create(com.example.duralapapp.data.api.CallApi::class.java)
    }

    @Provides
    @Singleton
    fun providePresenceApi(retrofit: Retrofit): com.example.duralapapp.data.api.PresenceApi {
        return retrofit.create(com.example.duralapapp.data.api.PresenceApi::class.java)
    }
}
