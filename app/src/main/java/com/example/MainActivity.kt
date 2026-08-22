package com.example

import com.google.firebase.FirebaseApp
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.example.util.LanguageManager
import java.util.Locale
import android.content.res.Configuration
import com.example.data.AppDatabase
import com.example.data.AuthRepositoryImpl
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.ForgotPasswordScreen
import com.example.ui.auth.LanguageSelectionScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.PhoneAuthScreen
import com.example.ui.auth.SignUpScreen
import com.example.ui.auth.SplashScreen
import com.example.ui.auth.VerificationScreen
import com.example.ui.dashboard.CustomerViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DeliveryPartnerApplicationScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var firebaseInitErrorState by mutableStateOf<String?>(null)

    private fun initFirebaseServices(): Boolean {
        return try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
            firebaseInitErrorState = null
            Log.d("FirebaseAudit", "FirebaseApp & Firestore offline persistence successfully initialized in MainActivity.")
            true
        } catch (e: Throwable) {
            Log.e("FirebaseAudit", "FirebaseApp init failure: ${e.message}", e)
            firebaseInitErrorState = e.localizedMessage ?: "Unable to initialize backend services."
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initFirebaseServices()
        
        // Initialize LanguageManager
        LanguageManager.init(applicationContext)

        // Initialize Room Database & Auth Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val authRepository = AuthRepositoryImpl(database.userDao())

        enableEdgeToEdge()
        setContent {
            val currentLanguage by LanguageManager.currentLanguage.collectAsState()
            val context = LocalContext.current
            val localizedContext = remember(currentLanguage) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                MyApplicationTheme {
                    val initError = firebaseInitErrorState
                    if (initError != null) {
                        FirebaseInitErrorFallbackScreen(
                            errorMessage = initError,
                            onRetry = {
                                initFirebaseServices()
                            }
                        )
                    } else {
                        // Initialize ViewModel with our Factory
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModel.Factory(authRepository)
                        )

                        // Initialize CustomerViewModel
                        val customerViewModel: CustomerViewModel = viewModel()

                        val activeSession by authViewModel.activeSession.collectAsStateWithLifecycle()
                        val navController = rememberNavController()

                        // State to prevent navigation side-effects before initialization is fully loaded
                        var isSessionChecked by remember { mutableStateOf(false) }

                        LaunchedEffect(activeSession) {
                            if (isSessionChecked) {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (activeSession != null) {
                                    if (currentRoute != "dashboard") {
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                            popUpTo("signup") { inclusive = true }
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    if (currentRoute != "login" && currentRoute != "splash" && currentRoute != "language" && currentRoute != "forgot_password" && currentRoute != "signup" && currentRoute != "phone_auth") {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            } else {
                                isSessionChecked = true
                            }
                        }

                val startDestination = "splash"

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("splash") {
                        SplashScreen(
                            onSplashFinished = {
                                val target = if (activeSession != null) {
                                    "dashboard"
                                } else {
                                    "login"
                                }
                                navController.navigate(target) {
                                    popUpTo("splash") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToSignUp = {
                                authViewModel.clearMessages()
                                navController.navigate("signup")
                            },
                            onNavigateToForgotPassword = {
                                authViewModel.clearMessages()
                                navController.navigate("forgot_password")
                            },
                            onNavigateToVerification = {
                                authViewModel.clearMessages()
                                navController.navigate("verification")
                            },
                            onNavigateToPhoneAuth = {
                                authViewModel.clearMessages()
                                navController.navigate("phone_auth")
                            },
                            onNavigateToDeliveryPartnerApplication = {
                                authViewModel.clearMessages()
                                navController.navigate("delivery_partner_application")
                            },
                            onLoginSuccess = {
                                authViewModel.clearMessages()
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("delivery_partner_application") {
                        DeliveryPartnerApplicationScreen(
                            session = activeSession,
                            onApplicationSubmitted = {
                                authViewModel.clearMessages()
                                if (activeSession != null) {
                                    navController.navigate("dashboard") {
                                        popUpTo("delivery_partner_application") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate("login") {
                                        popUpTo("delivery_partner_application") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onCancel = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("phone_auth") {
                        PhoneAuthScreen(
                            viewModel = authViewModel,
                            onNavigateBack = {
                                authViewModel.clearMessages()
                                authViewModel.resetPhoneAuthFlow()
                                navController.navigate("login") {
                                    popUpTo("phone_auth") { inclusive = true }
                                }
                            },
                            onAuthSuccess = {
                                authViewModel.clearMessages()
                                navController.navigate("dashboard") {
                                    popUpTo("phone_auth") { inclusive = true }
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("signup") {
                        SignUpScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate("login") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            },
                            onNavigateToVerification = {
                                authViewModel.clearMessages()
                                navController.navigate("verification")
                            },
                            onSignUpSuccess = {
                                authViewModel.clearMessages()
                                navController.navigate("dashboard") {
                                    popUpTo("signup") { inclusive = true }
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("forgot_password") {
                        ForgotPasswordScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate("login") {
                                    popUpTo("forgot_password") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("verification") {
                        VerificationScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate("login") {
                                    popUpTo("verification") { inclusive = true }
                                }
                            },
                            onVerificationSuccess = {
                                navController.navigate("login") {
                                    popUpTo("verification") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        val currentSession = activeSession
                        if (currentSession != null) {
                            // Sync user ID with CustomerViewModel & Save FCM token
                            LaunchedEffect(currentSession.userId) {
                                customerViewModel.setUserId(currentSession.userId)

                                try {
                                    val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: currentSession.userId.toString()
                                    com.example.data.notification.NotificationHelper.syncFcmToken(firebaseUid)
                                } catch (e: Exception) {
                                    Log.w("FCM", "Could not trigger FCM token sync: ${e.message}")
                                }
                            }
                            DashboardScreen(
                                session = currentSession,
                                viewModel = authViewModel,
                                customerViewModel = customerViewModel,
                                onLogoutSuccess = {
                                    customerViewModel.clearSessionState()
                                    authViewModel.clearMessages()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            // Fallback to loading screen if session is briefly null
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun FirebaseInitErrorFallbackScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connection Error",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SwiftCart could not initialize backend services. Please verify your internet connection or backend configuration.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Connection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
