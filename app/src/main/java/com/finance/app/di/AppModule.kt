package com.finance.app.di

import android.content.Context
import android.content.SharedPreferences
import com.finance.app.R
import com.finance.app.data.biometric.BiometricAuthenticatorImpl
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.util.ErrorHandler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindBiometricAuthenticator(
        biometricAuthenticatorImpl: BiometricAuthenticatorImpl
    ): BiometricAuthenticator
    
    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }
        
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }
        
        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
            return context.getSharedPreferences("finance_app_prefs", Context.MODE_PRIVATE)
        }
        
        @Provides
        @Singleton
        fun provideErrorHandler(@ApplicationContext context: Context): ErrorHandler {
            return ErrorHandler(context)
        }
        
        @Provides
        @Singleton
        fun provideNetworkStateObserver(@ApplicationContext context: Context): NetworkStateObserver {
            return NetworkStateObserver(context)
        }
        
        @Provides
        @Singleton
        fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            return GoogleSignIn.getClient(context, gso)
        }
    }
}
