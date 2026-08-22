package com.example.ui.dashboard

import com.example.data.firestore.waitColor
import com.example.data.firestore.waitLabel
import com.example.data.firestore.waitShortBadge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.example.data.firestore.isDarkStore
import com.example.data.firestore.MenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ActiveSession
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CustomerSubScreen {
    MAIN,
    RESTAURANT_MENU,
    CART,
    CHECKOUT,
    ORDER_CONFIRMATION,
    SWIFTCART_PLUS,
    REWARDS,
    REFER_AND_EARN,
    COMMUNITY_IMPACT,
    ACHIEVEMENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    session: ActiveSession,
    modifier: Modifier = Modifier,
    customerViewModel: CustomerViewModel = viewModel()
) {
    val customerId = remember(session) { session.email.ifBlank { session.userId.toString() } }

    LaunchedEffect(customerId) {
        customerViewModel.listenToCustomerOrders(customerId)
    }

    val openRestaurants by customerViewModel.restaurants.collectAsState()
    val instantStores = remember(openRestaurants) { openRestaurants.filter { it.isDarkStore } }
    val selectedRestaurant by customerViewModel.selectedRestaurant.collectAsState()
    val customerOrders by customerViewModel.customerOrders.collectAsState()
    val cartItems by customerViewModel.cartItems.collectAsState()
    val allMenuItems by customerViewModel.allMenuItems.collectAsState()
    val currentLocation by customerViewModel.currentLocation.collectAsState()

    var currentScreen by remember { mutableStateOf(CustomerSubScreen.MAIN) }
    var activeMainTab by remember { mutableStateOf(0) } // 0 = Restaurants, 1 = My Orders
    var confirmedOrderId by remember { mutableStateOf("") }
    var showAddressDialog by remember { mutableStateOf(false) }
    var selectedItemForDialog by remember { mutableStateOf<com.example.data.Item?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDeliveryMode by remember { mutableStateOf("Food") } // "Food" vs "Grocery"
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Default") }
    var selectedWeatherCondition by remember { mutableStateOf("Rainy ☔") }

    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                searchQuery = matches[0]
                voiceError = null
            }
        } else if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
            voiceError = "Speech recognition error. Please try again."
        }
    }

    fun startListeningWithRecognizer() {
        if (speechRecognizer == null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search restaurants or dishes...")
            }
            try {
                isListening = true
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                isListening = false
                voiceError = "Speech recognition not available on this device."
            }
            return
        }

        voiceError = null
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try speaking again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection issue."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
                    else -> "Speech recognition error ($error)."
                }
                voiceError = message
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    searchQuery = matches[0]
                    voiceError = null
                } else {
                    voiceError = "Could not recognize speech. Try again."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    searchQuery = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            voiceError = "Failed to start listening: ${e.message}"
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningWithRecognizer()
        } else {
            voiceError = "Microphone permission denied. Enable permission to use Voice Search."
        }
    }

    fun onVoiceSearchClick() {
        voiceError = null
        val permissionState = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            startListeningWithRecognizer()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val categories = remember(selectedDeliveryMode) {
        if (selectedDeliveryMode == "Food") {
            listOf("All", "Biryani 🍲", "Pizza 🍕", "Burgers 🍔", "Asian 🥢", "Healthy 🥗", "Bakery 🥐", "Desserts 🍰", "Following 💖")
        } else {
            listOf("All", "Instant Essentials ⚡", "Fresh Produce 🥦", "Dairy & Bakery 🥛", "Snacks & Munchies 🍿", "Instant Food 🍜", "Beverages 🥤", "Household 🧹")
        }
    }
    val sortOptions = listOf("Default", "Rating: High to Low", "Nearest First", "Name A-Z")

    val customerIsLoading by customerViewModel.isLoading.collectAsState()
    val followedRestaurants by customerViewModel.followedRestaurants.collectAsState()
    val userProfile by customerViewModel.userProfile.collectAsState()

    val userKey = remember(session) { if (session.email.isNotBlank()) session.email else session.userId.toString() }

    val earnedBadgeToast by customerViewModel.earnedBadgeToast.collectAsState()
    LaunchedEffect(earnedBadgeToast) {
        val toastMsg = earnedBadgeToast
        if (!toastMsg.isNullOrBlank()) {
            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
            customerViewModel.clearEarnedBadgeToast()
        }
    }

    LaunchedEffect(userKey) {
        customerViewModel.loadUserFollowedRestaurants(userKey)
    }

    val reorderFavorites = remember(customerOrders) {
        if (customerOrders.isEmpty()) return@remember emptyList<Order>()

        val validOrders = customerOrders.filter {
            !it.status.equals("cancelled", ignoreCase = true) && !it.status.equals("canceled", ignoreCase = true)
        }
        if (validOrders.isEmpty()) return@remember emptyList<Order>()

        val frequencyMap = validOrders.groupBy { order ->
            val itemSig = order.items.sortedBy { it.itemId.ifBlank { it.name } }
                .joinToString(",") { "${it.name}:${it.quantity}" }
            "${order.restaurantId}_$itemSig"
        }

        frequencyMap.values
            .map { group -> group.maxByOrNull { it.createdAt } ?: group.first() }
            .sortedWith(
                compareByDescending<Order> { order ->
                    val sig = order.items.sortedBy { it.itemId.ifBlank { it.name } }
                        .joinToString(",") { "${it.name}:${it.quantity}" }
                    frequencyMap["${order.restaurantId}_$sig"]?.size ?: 0
                }.thenByDescending { it.createdAt }
            )
            .take(3)
    }

    val filteredRestaurants = remember(openRestaurants, searchQuery, selectedCategoryFilter, followedRestaurants, selectedDeliveryMode) {
        openRestaurants.filter { rest ->
            val matchesMode = if (selectedDeliveryMode == "Food") {
                !rest.isDarkStore
            } else {
                rest.isDarkStore
            }

            val matchesSearch = rest.name.contains(searchQuery, ignoreCase = true) ||
                    rest.category.contains(searchQuery, ignoreCase = true) ||
                    rest.address.contains(searchQuery, ignoreCase = true)

            val cleanCategory = selectedCategoryFilter.split(" ").firstOrNull() ?: selectedCategoryFilter

            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Instant Essentials ⚡" -> rest.isDarkStore
                "Following 💖" -> followedRestaurants.contains(rest.restaurantId)
                else -> rest.category.contains(cleanCategory, ignoreCase = true) ||
                        cleanCategory.contains(rest.category, ignoreCase = true)
            }

            matchesMode && matchesSearch && matchesCategory
        }
    }

    val sortedRestaurants = remember(filteredRestaurants, selectedSortOption) {
        when (selectedSortOption) {
            "Rating: High to Low" -> {
                filteredRestaurants.sortedByDescending { rest ->
                    customerViewModel.getRestaurantRating(rest.restaurantId).first
                }
            }
            "Nearest First" -> {
                filteredRestaurants.sortedBy { rest ->
                    (kotlin.math.abs(rest.restaurantId.hashCode()) % 40) / 10.0 + 0.5
                }
            }
            "Name A-Z" -> {
                filteredRestaurants.sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
            else -> filteredRestaurants
        }
    }

    when (currentScreen) {
        CustomerSubScreen.MAIN -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("customer_home_screen")
            ) {
                // TOP LOCATION & QUICK ACTIONS HEADER (Swiggy / Blinkit Style)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tappable Delivery Address Header
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddressDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Location",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "DELIVER TO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Change address",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = currentLocation.ifBlank { "Indiranagar, Bengaluru" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action Badges (Rewards, Streak, Eco, Refresh)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Loyalty Points Badge
                                val pts = userProfile?.loyaltyPoints ?: 0
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clickable { currentScreen = CustomerSubScreen.REWARDS }
                                        .testTag("open_rewards_icon_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎁", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$pts",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                // Streak Badge
                                val streak = userProfile?.currentStreak ?: 0
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clickable { currentScreen = CustomerSubScreen.ACHIEVEMENTS }
                                        .testTag("open_achievements_icon_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔥", fontSize = 12.sp)
                                        if (streak > 0) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${streak}w",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }

                                // Refresh Button
                                IconButton(
                                    onClick = { customerViewModel.refreshData(customerId) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("refresh_customer_data")
                                ) {
                                    if (customerIsLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab Bar (Restaurants / My Orders)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = activeMainTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_home_tab_row")
                    ) {
                        Tab(
                            selected = activeMainTab == 0,
                            onClick = { activeMainTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Explore (${openRestaurants.size})", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.testTag("tab_explore_restaurants")
                        )
                        Tab(
                            selected = activeMainTab == 1,
                            onClick = { activeMainTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("My Orders (${customerOrders.size})", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.testTag("tab_my_orders")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (activeMainTab == 0) {
                    // RESTAURANTS TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // LIVE ONGOING ORDER TRACKER CARD (When active order exists)
                        val activeOrder = customerOrders.firstOrNull { o ->
                            val s = o.status.lowercase()
                            s != "delivered" && s != "cancelled" && s != "canceled"
                        }
                        if (activeOrder != null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeMainTab = 1 }
                                        .testTag("live_active_order_tracker_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🛵", fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Live Order • ${activeOrder.status.replaceFirstChar { it.uppercase() }}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF34D399)
                                                )
                                            }
                                            Text(
                                                text = "${activeOrder.restaurantName.ifBlank { "Restaurant" }} • ${activeOrder.items.size} items",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Estimated delivery: 15-25 mins",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFF10B981),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "TRACK →",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Search Bar
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(stringResource(R.string.search_hint)) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Text("✕", fontSize = 14.sp)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (isListening) {
                                                        speechRecognizer?.stopListening()
                                                        isListening = false
                                                    } else {
                                                        onVoiceSearchClick()
                                                    }
                                                },
                                                modifier = Modifier.testTag("voice_search_mic_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                                    contentDescription = "Voice Search",
                                                    tint = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("restaurant_search_bar")
                                )

                                AnimatedVisibility(visible = isListening) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .testTag("voice_listening_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                                val alpha by infiniteTransition.animateFloat(
                                                    initialValue = 0.3f,
                                                    targetValue = 1f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(600, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "alpha"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444).copy(alpha = alpha))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Listening... Speak restaurant or dish",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    speechRecognizer?.stopListening()
                                                    isListening = false
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel Listening",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = !voiceError.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .testTag("voice_error_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = voiceError ?: "",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { voiceError = null },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss Error",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // FOOD vs INSTA-GROCERY MODE SWITCHER ITEM
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mode_switcher_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Food Delivery Button
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedDeliveryMode = "Food"
                                                selectedCategoryFilter = "All"
                                            }
                                            .testTag("mode_switcher_food"),
                                        color = if (selectedDeliveryMode == "Food") MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🍔 Food Delivery",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (selectedDeliveryMode == "Food") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Insta-Grocery Button
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedDeliveryMode = "Grocery"
                                                selectedCategoryFilter = "All"
                                            }
                                            .testTag("mode_switcher_grocery"),
                                        color = if (selectedDeliveryMode == "Grocery") Color(0xFF0284C7) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ Insta-Grocery (10m)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (selectedDeliveryMode == "Grocery") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // CHEF'S RECOMMENDATIONS & POPULAR DISHES (Swiggy / Zomato style)
                        if (allMenuItems.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("chef_recommendations_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Chef's Picks & Trending 🌟",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Handcrafted",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(allMenuItems.filter { it.isAvailable }.take(10), key = { it.itemId }) { item ->
                                            val rest = openRestaurants.find { it.restaurantId == item.restaurantId }
                                            val isVeg = item.name.contains("veg", ignoreCase = true) || item.name.contains("paneer", ignoreCase = true) || item.name.contains("salad", ignoreCase = true) || item.name.contains("milk", ignoreCase = true) || item.name.contains("bread", ignoreCase = true)

                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .clickable {
                                                        selectedItemForDialog = com.example.data.Item(
                                                            id = item.itemId.filter { it.isDigit() }.toIntOrNull() ?: kotlin.math.abs(item.itemId.hashCode()),
                                                            storeId = item.restaurantId.filter { it.isDigit() }.toIntOrNull() ?: 1,
                                                            name = item.name,
                                                            price = item.price,
                                                            image = item.photoUrl,
                                                            imageUrl = item.photoUrl,
                                                            category = item.freshnessTag,
                                                            description = item.freshnessTag
                                                        )
                                                    }
                                                    .testTag("trending_item_card_${item.itemId}")
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(95.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    ) {
                                                        AsyncImage(
                                                            model = item.photoUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600" },
                                                            contentDescription = item.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            border = BorderStroke(1.dp, if (isVeg) Color(0xFF16A34A) else Color(0xFFDC2626)),
                                                            modifier = Modifier
                                                                .padding(6.dp)
                                                                .size(16.dp)
                                                                .align(Alignment.TopStart)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isVeg) Color(0xFF16A34A) else Color(0xFFDC2626))
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = item.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = rest?.name ?: "SwiftCart Express",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "₹${String.format(Locale.US, "%.2f", item.price)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Button(
                                                            onClick = {
                                                                customerViewModel.addToCartItem(item)
                                                                Toast.makeText(context, "Added ${item.name} to cart!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier
                                                                .height(28.dp)
                                                                .testTag("trending_add_btn_${item.itemId}")
                                                        ) {
                                                            Text("+ ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // BUY IT AGAIN (Blinkit-style Order Again Widget)
                        item {
                            val buyAgainItems = remember {
                                listOf(
                                    Triple("Farm Fresh Milk 🥛", "₹32.00", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400"),
                                    Triple("Brown Bread 🍞", "₹45.00", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400"),
                                    Triple("Organic Eggs (6 Pack) 🥚", "₹60.00", "https://images.unsplash.com/photo-1516467508483-a7212febe31a?w=400"),
                                    Triple("Fresh Bananas 🍌", "₹40.00", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400"),
                                    Triple("Greek Yogurt 🍦", "₹55.00", "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=400")
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("buy_it_again_widget")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Buy It Again 🛒",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Past Favorites",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(buyAgainItems) { (title, price, image) ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.width(130.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = image,
                                                    contentDescription = title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = price,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Button(
                                                    onClick = {
                                                        customerViewModel.addToCartItem(
                                                            menuItem = com.example.data.firestore.MenuItem(
                                                                itemId = title.take(8),
                                                                restaurantId = "dark_store_1",
                                                                name = title,
                                                                price = price.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 40.0,
                                                                freshnessTag = "Farm Fresh 🌿"
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(2.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp)
                                                ) {
                                                    Text("+ ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Loyalty Rewards Banner Item
                        item {
                            val pts = userProfile?.loyaltyPoints ?: 0
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                onClick = { currentScreen = CustomerSubScreen.REWARDS },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("loyalty_rewards_home_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🎁", fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "SwiftCart Rewards",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFF59E0B),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = "$pts pts",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (pts >= 100) "🎉 ₹50 Discount Unlocked! Tap to view rewards" else "Earn 1 pt per ₹10 spent • Tap for details",
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Order Streak & Achievements Banner Item
                        item {
                            val streak = userProfile?.currentStreak ?: 0
                            val badgesCount = userProfile?.badges?.size ?: 0
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                onClick = { currentScreen = CustomerSubScreen.ACHIEVEMENTS },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("streak_achievements_home_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🔥", fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (streak > 0) "🔥 $streak Week Streak" else "🔥 Start Order Streak",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "🏆 $badgesCount/6",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF38BDF8),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (streak > 0)
                                                    "Order weekly to keep flame alive! Tap for achievements"
                                                else
                                                    "Place an order to ignite your streak & earn badges",
                                                fontSize = 11.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Achievements",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Refer & Earn Banner Item
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentScreen = CustomerSubScreen.REFER_AND_EARN }
                                    .testTag("refer_and_earn_home_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🚀", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Refer Friends & Earn ₹50! 🎁",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Give ₹50, Get ₹50 for every friend who joins & orders",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Refer & Earn",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        // SwiftCart Plus Banner Item
                        item {
                            val isSubscribed = session.subscriptionStatus.equals("active", ignoreCase = true)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSubscribed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentScreen = CustomerSubScreen.SWIFTCART_PLUS }
                                    .testTag("swiftcart_plus_home_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isSubscribed) "SwiftCart Plus Active ⭐" else "SwiftCart Plus • ₹99/mo",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = if (isSubscribed) "Free Delivery & 5% OFF applied on every order!" else "Free delivery & extra 5% off on all orders. Tap to explore!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Plus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Lifetime Eco-Friendly Counter Item
                        item {
                            val ecoOrdersCount = remember(customerOrders) { customerOrders.count { it.ecoPackaging } }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("eco_plastic_saved_counter_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌱", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "You've saved plastic on $ecoOrdersCount orders! 🌱",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "Thank you for choosing eco-friendly packaging for sustainable deliveries.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF388E3C)
                                        )
                                    }
                                }
                            }
                        }

                        // Reorder Favorites Section (Only visible if history exists)
                        if (reorderFavorites.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reorder_favorites_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Reorder Your Favorites",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "1-Tap Reorder",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(reorderFavorites) { favOrder ->
                                            val itemsSummary = favOrder.items.joinToString(", ") { "${it.quantity}x ${it.name}" }
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .width(230.dp)
                                                    .testTag("reorder_card_${favOrder.orderId}")
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = favOrder.restaurantName.ifBlank { "Restaurant" },
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "₹${String.format(Locale.US, "%.2f", favOrder.totalAmount)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Text(
                                                        text = itemsSummary,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.heightIn(min = 32.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Button(
                                                        onClick = {
                                                            customerViewModel.reorderOrder(favOrder)
                                                            currentScreen = CustomerSubScreen.CART
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(36.dp)
                                                            .testTag("reorder_button_${favOrder.orderId}")
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.ShoppingCart,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Reorder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Instant Delivery ⚡ Section (Dark Stores)
                        if (instantStores.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("instant_delivery_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFFE0F2FE),
                                                shape = CircleShape,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Instant Delivery ⚡",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFF0284C7),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "10-15 MIN",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(instantStores, key = { it.restaurantId }) { store ->
                                            Card(
                                                shape = RoundedCornerShape(18.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFF0F172A)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                                modifier = Modifier
                                                    .width(250.dp)
                                                    .clickable { customerViewModel.selectRestaurant(store) }
                                                    .testTag("instant_store_card_${store.restaurantId}")
                                            ) {
                                                Column {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(110.dp)
                                                    ) {
                                                        AsyncImage(
                                                            model = store.photoUrl.ifBlank { "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=600" },
                                                            contentDescription = store.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        Surface(
                                                            color = Color(0xFF0284C7),
                                                            shape = RoundedCornerShape(bottomEnd = 12.dp),
                                                            modifier = Modifier.align(Alignment.TopStart)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Bolt,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text(
                                                                    text = store.deliveryEta.ifBlank { "10-15 min delivery" },
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                            text = store.name,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = store.address.ifBlank { "Express Essentials & Grocery Hub" },
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Weather-Based Recommendations Section ("Perfect for today ☔")
                        item {
                            val weatherConditions = listOf("Sunny ☀️", "Rainy ☔", "Cold ❄️")

                            val targetWeatherTag = when (selectedWeatherCondition) {
                                "Sunny ☀️", "Sunny" -> "Hot Weather Refresher"
                                "Cold ❄️", "Cold" -> "Cold Weather Warmer"
                                else -> "Rainy Day Comfort"
                            }

                            val weatherTitle = when (selectedWeatherCondition) {
                                "Sunny ☀️", "Sunny" -> "Perfect for today ☀️"
                                "Cold ❄️", "Cold" -> "Perfect for today ❄️"
                                else -> "Perfect for today ☔"
                            }

                            val weatherSubtitle = when (selectedWeatherCondition) {
                                "Sunny ☀️", "Sunny" -> "Refreshing drinks & cool delights for sunny days"
                                "Cold ❄️", "Cold" -> "Hot beverages, warm soups & hearty meals"
                                else -> "Cozy comfort food & warm bites for rainy weather"
                            }

                            val weatherFilteredItems = remember(allMenuItems, targetWeatherTag) {
                                val matches = allMenuItems.filter { item ->
                                    item.isAvailable && (
                                        item.weatherMood.equals(targetWeatherTag, ignoreCase = true) ||
                                        (targetWeatherTag == "Rainy Day Comfort" && item.moodTags.any { it.contains("comfort", ignoreCase = true) || it.contains("spicy", ignoreCase = true) }) ||
                                        (targetWeatherTag == "Hot Weather Refresher" && item.moodTags.any { it.contains("sweet", ignoreCase = true) || it.contains("drink", ignoreCase = true) || it.contains("ice", ignoreCase = true) }) ||
                                        (targetWeatherTag == "Cold Weather Warmer" && item.moodTags.any { it.contains("quick", ignoreCase = true) || it.contains("spicy", ignoreCase = true) })
                                    )
                                }
                                if (matches.isNotEmpty()) matches else allMenuItems.filter { it.isAvailable }.take(6)
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("weather_recommendations_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = weatherTitle,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = weatherSubtitle,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Manual Weather Condition Selector Bar
                                    Text(
                                        text = "Select Weather Condition 🌦️:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(weatherConditions) { condition ->
                                            val isSelected = selectedWeatherCondition.contains(condition.take(4))
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedWeatherCondition = condition },
                                                label = { Text(condition, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.testTag("weather_chip_${condition.take(5)}")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Recommended Menu Items Carousel
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(weatherFilteredItems, key = { it.itemId }) { item ->
                                            val restName = openRestaurants.find { it.restaurantId == item.restaurantId }?.name ?: "SwiftCart Express"
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .width(180.dp)
                                                    .testTag("weather_item_${item.itemId}")
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(90.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    ) {
                                                        AsyncImage(
                                                            model = item.photoUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600" },
                                                            contentDescription = item.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                            modifier = Modifier.align(Alignment.TopStart)
                                                        ) {
                                                            Text(
                                                                text = if (item.weatherMood.isNotBlank()) item.weatherMood else "Weather Pick",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Text(
                                                        text = item.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Text(
                                                        text = restName,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "₹${String.format(Locale.US, "%.2f", item.price)}",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )

                                                        Button(
                                                            onClick = {
                                                                customerViewModel.addToCartItem(item)
                                                                Toast.makeText(context, "Added ${item.name} to cart!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier
                                                                .height(28.dp)
                                                                .testTag("weather_add_to_cart_${item.itemId}")
                                                        ) {
                                                            Text("+ Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Community Impact Banner Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentScreen = CustomerSubScreen.COMMUNITY_IMPACT }
                                    .testTag("community_impact_banner_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🌿", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SwiftCart Community Impact 🌿",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "100% Eco-Packaging • Local Dining • Real-Time Stats",
                                            fontSize = 11.sp,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }
                                    Text(
                                        text = "View Dashboard →",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                        }

                        // Category Chips Row
                        item {
                            Column {
                                Text(
                                    text = "Popular Categories",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(categories) { cat ->
                                        FilterChip(
                                            selected = selectedCategoryFilter == cat,
                                            onClick = { selectedCategoryFilter = cat },
                                            label = { Text(cat) },
                                            modifier = Modifier.testTag("category_chip_$cat")
                                        )
                                    }
                                }
                            }
                        }

                        // Sort Options Row
                        item {
                            Column {
                                Text(
                                    text = "Sort By",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(sortOptions) { opt ->
                                        FilterChip(
                                            selected = selectedSortOption == opt,
                                            onClick = { selectedSortOption = opt },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val icon = when (opt) {
                                                        "Rating: High to Low" -> Icons.Default.Star
                                                        "Nearest First" -> Icons.Default.LocationOn
                                                        "Name A-Z" -> Icons.Default.FilterList
                                                        else -> Icons.Default.Refresh
                                                    }
                                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(opt)
                                                }
                                            },
                                            modifier = Modifier.testTag("sort_chip_${opt.replace(" ", "_")}")
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Cart Quick Banner if cart has items
                        if (cartItems.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentScreen = CustomerSubScreen.CART }
                                        .testTag("cart_quick_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${customerViewModel.totalItemCount} items in cart",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Total: $${String.format("%.2f", customerViewModel.totalAmount)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                        Text(
                                            text = "View Cart →",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Open Restaurants Header
                        item {
                            Text(
                                text = "Open Restaurants (${sortedRestaurants.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (sortedRestaurants.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Restaurant,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = if (openRestaurants.isEmpty())
                                                    "No open restaurants found right now. Check back soon or switch to Admin mode to add restaurants!"
                                                else "No restaurants match '$searchQuery' in category '$selectedCategoryFilter'.",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(sortedRestaurants, key = { it.restaurantId }) { restaurant ->
                                RestaurantCard(
                                    restaurant = restaurant,
                                    ratingPair = customerViewModel.getRestaurantRating(restaurant.restaurantId),
                                    isFollowed = followedRestaurants.contains(restaurant.restaurantId),
                                    onToggleFollow = {
                                        customerViewModel.toggleFollowRestaurant(userKey, restaurant.restaurantId)
                                    },
                                    onClick = {
                                        customerViewModel.selectRestaurant(restaurant)
                                        currentScreen = CustomerSubScreen.RESTAURANT_MENU
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // MY ORDERS TAB
                    CustomerOrdersList(
                        orders = customerOrders,
                        customerViewModel = customerViewModel,
                        onNavigateToCart = { currentScreen = CustomerSubScreen.CART }
                    )
                }
            }
        }


        CustomerSubScreen.RESTAURANT_MENU -> {
            selectedRestaurant?.let { rest ->
                RestaurantMenuScreen(
                    restaurant = rest,
                    viewModel = customerViewModel,
                    userEmail = userKey,
                    onNavigateBack = { currentScreen = CustomerSubScreen.MAIN },
                    onNavigateToCart = { currentScreen = CustomerSubScreen.CART }
                )
            } ?: run {
                LaunchedEffect(Unit) { currentScreen = CustomerSubScreen.MAIN }
            }
        }

        CustomerSubScreen.CART -> {
            CustomerCartScreen(
                viewModel = customerViewModel,
                onNavigateBack = {
                    if (selectedRestaurant != null) {
                        currentScreen = CustomerSubScreen.RESTAURANT_MENU
                    } else {
                        currentScreen = CustomerSubScreen.MAIN
                    }
                },
                onNavigateToCheckout = { currentScreen = CustomerSubScreen.CHECKOUT }
            )
        }

        CustomerSubScreen.CHECKOUT -> {
            CheckoutScreen(
                session = session,
                viewModel = customerViewModel,
                onNavigateBack = { currentScreen = CustomerSubScreen.CART },
                onOrderPlacedSuccess = { newOrderId ->
                    confirmedOrderId = newOrderId
                    currentScreen = CustomerSubScreen.ORDER_CONFIRMATION
                }
            )
        }

        CustomerSubScreen.ORDER_CONFIRMATION -> {
            OrderConfirmationScreen(
                orderId = confirmedOrderId,
                onViewOrdersClick = {
                    activeMainTab = 1
                    currentScreen = CustomerSubScreen.MAIN
                },
                onContinueShoppingClick = {
                    activeMainTab = 0
                    currentScreen = CustomerSubScreen.MAIN
                }
            )
        }

        CustomerSubScreen.SWIFTCART_PLUS -> {
            SwiftCartPlusScreen(
                session = session,
                viewModel = customerViewModel,
                onNavigateBack = { currentScreen = CustomerSubScreen.MAIN }
            )
        }

        CustomerSubScreen.REWARDS -> {
            CustomerRewardsScreen(
                userProfile = userProfile,
                onBackClick = { currentScreen = CustomerSubScreen.MAIN },
                onOpenReferAndEarn = { currentScreen = CustomerSubScreen.REFER_AND_EARN }
            )
        }

        CustomerSubScreen.REFER_AND_EARN -> {
            ReferAndEarnScreen(
                userProfile = userProfile,
                customerViewModel = customerViewModel,
                onBackClick = { currentScreen = CustomerSubScreen.MAIN }
            )
        }

        CustomerSubScreen.COMMUNITY_IMPACT -> {
            CommunityImpactScreen(
                session = session,
                customerViewModel = customerViewModel,
                onBackClick = { currentScreen = CustomerSubScreen.MAIN }
            )
        }

        CustomerSubScreen.ACHIEVEMENTS -> {
            AchievementsScreen(
                session = session,
                customerViewModel = customerViewModel,
                onBackClick = { currentScreen = CustomerSubScreen.MAIN }
            )
        }
    }

    // ADDRESS SELECTION / EDIT DIALOG
    if (showAddressDialog) {
        var tempAddress by remember { mutableStateOf(currentLocation) }
        val quickAddresses = listOf(
            "🏠 Home" to "12th Main, HAL 2nd Stage, Indiranagar, Bengaluru",
            "🏢 Work" to "EcoSpace Business Park, Bellandur, Bengaluru",
            "📍 Other" to "Koramangala 4th Block, 80 Feet Road, Bengaluru"
        )
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Delivery Location", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Enter your delivery address or choose a saved place:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("Delivery Address") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_address_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saved Addresses:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    quickAddresses.forEach { (label, addr) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    tempAddress = addr
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(addr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempAddress.isNotBlank()) {
                            customerViewModel.setCurrentLocation(tempAddress)
                        }
                        showAddressDialog = false
                    },
                    modifier = Modifier.testTag("dialog_save_address_btn")
                ) {
                    Text("Deliver Here")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PRODUCT DETAILS & CUSTOMIZATION DIALOG
    selectedItemForDialog?.let { dialogItem ->
        ProductDetailsDialog(
            item = dialogItem,
            onDismiss = { selectedItemForDialog = null },
            onAddToCart = {
                val matchingMenu = allMenuItems.find { it.itemId == "${dialogItem.id}" || it.name.equals(dialogItem.name, ignoreCase = true) }
                if (matchingMenu != null) {
                    customerViewModel.addToCartItem(matchingMenu)
                } else {
                    customerViewModel.addToCartItem(
                        MenuItem(
                            itemId = "${dialogItem.id}",
                            restaurantId = "${dialogItem.storeId}",
                            name = dialogItem.name,
                            price = dialogItem.price,
                            photoUrl = dialogItem.imageUrl
                        )
                    )
                }
                Toast.makeText(context, "Added ${dialogItem.name} to cart!", Toast.LENGTH_SHORT).show()
                selectedItemForDialog = null
            },
            onAddToCartWithCustomization = { note, customPrice ->
                val matchingMenu = allMenuItems.find { it.itemId == "${dialogItem.id}" || it.name.equals(dialogItem.name, ignoreCase = true) }
                if (matchingMenu != null) {
                    customerViewModel.addToCartItem(matchingMenu.copy(price = customPrice, freshnessTag = if (note.isNotBlank()) "${matchingMenu.freshnessTag} ($note)" else matchingMenu.freshnessTag))
                } else {
                    customerViewModel.addToCartItem(
                        MenuItem(
                            itemId = "${dialogItem.id}",
                            restaurantId = "${dialogItem.storeId}",
                            name = dialogItem.name,
                            price = customPrice,
                            photoUrl = dialogItem.imageUrl,
                            freshnessTag = note
                        )
                    )
                }
                Toast.makeText(context, "Added ${dialogItem.name} ($note) to cart!", Toast.LENGTH_SHORT).show()
                selectedItemForDialog = null
            }
        )
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    ratingPair: Pair<Double, Int> = Pair(0.0, 0),
    isFollowed: Boolean = false,
    onToggleFollow: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (avgRating, reviewCount) = ratingPair

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_restaurant_card_${restaurant.restaurantId}")
    ) {
        Column {
            Box {
                AsyncImage(
                    model = restaurant.photoUrl.ifBlank { "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600" },
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hygiene & Sustainability Badges on Image
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF1B5E20).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", restaurant.hygieneRating)}/5 Hygiene 🛡️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sustainability_score_badge_${restaurant.restaurantId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌿 ${String.format(Locale.US, "%.1f", restaurant.sustainabilityScore)} Eco",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onToggleFollow != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { onToggleFollow() }
                                    .testTag("follow_restaurant_icon_${restaurant.restaurantId}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Follow",
                                        tint = if (isFollowed) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "OPEN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (reviewCount > 0) String.format(Locale.US, "%.1f", avgRating) else "New",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            if (reviewCount > 0) {
                                Text(
                                    text = " ($reviewCount)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = restaurant.category.ifBlank { "General" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (restaurant.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = restaurant.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Live Wait Time Indicator
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = restaurant.waitColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(restaurant.waitColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = restaurant.waitLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = restaurant.waitColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Prominent Offer Badge (Swiggy / Zomato style)
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("restaurant_offer_badge_${restaurant.restaurantId}")
                    ) {
                        Text(
                            text = if (restaurant.offerBadge.isNotBlank()) restaurant.offerBadge else "50% OFF up to ₹100 🏷️",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("browse_menu_button_${restaurant.restaurantId}")
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Menu & Order", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomerOrdersList(
    orders: List<Order>,
    customerViewModel: CustomerViewModel,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "DELIVERED", "ACTIVE"
    var showRatingDialogForOrder by remember { mutableStateOf<Order?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val reviewedOrderIds by customerViewModel.reviewedOrderIds.collectAsState()

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            val matchesQuery = searchQuery.isBlank() ||
                    order.restaurantName.contains(searchQuery, ignoreCase = true) ||
                    order.orderId.contains(searchQuery, ignoreCase = true) ||
                    order.items.any { it.name.contains(searchQuery, ignoreCase = true) }

            val isDelivered = order.status.lowercase() == "delivered"
            val matchesFilter = when (selectedFilter) {
                "DELIVERED" -> isDelivered
                "ACTIVE" -> !isDelivered
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val scheduledOrders = remember(filteredOrders) {
        filteredOrders.filter { it.status.equals("scheduled", ignoreCase = true) }
    }
    val activeAndPastOrders = remember(filteredOrders) {
        filteredOrders.filter { !it.status.equals("scheduled", ignoreCase = true) }
    }

    var orderToReschedule by remember { mutableStateOf<Order?>(null) }
    var rescheduleDayOffset by remember { mutableStateOf(0) }
    var rescheduleHour by remember { mutableStateOf(12) }
    var rescheduleMinute by remember { mutableStateOf(0) }

    fun computeRescheduleTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, rescheduleDayOffset)
        cal.set(java.util.Calendar.HOUR_OF_DAY, rescheduleHour)
        cal.set(java.util.Calendar.MINUTE, rescheduleMinute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Reschedule Dialog
    orderToReschedule?.let { reschOrder ->
        AlertDialog(
            onDismissRequest = { orderToReschedule = null },
            title = { Text("Reschedule Order", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select new delivery slot for ${reschOrder.restaurantName}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Today", "Tomorrow", "In 2 Days").forEachIndexed { idx, name ->
                            FilterChip(
                                selected = rescheduleDayOffset == idx,
                                onClick = { rescheduleDayOffset = idx },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val availableHours = (9..21).toList()
                    ScrollableTabRow(
                        selectedTabIndex = availableHours.indexOf(rescheduleHour).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent
                    ) {
                        availableHours.forEach { h ->
                            val ampm = if (h >= 12) "PM" else "AM"
                            val displayH = if (h % 12 == 0) 12 else h % 12
                            Tab(
                                selected = rescheduleHour == h,
                                onClick = { rescheduleHour = h },
                                text = { Text("$displayH:00 $ampm", fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = rescheduleMinute == 0,
                            onClick = { rescheduleMinute = 0 },
                            label = { Text(":00 Slot", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = rescheduleMinute == 30,
                            onClick = { rescheduleMinute = 30 },
                            label = { Text(":30 Slot", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTime = computeRescheduleTimestamp()
                        customerViewModel.rescheduleOrder(
                            orderId = reschOrder.orderId,
                            newScheduledTime = newTime,
                            onSuccess = {
                                orderToReschedule = null
                                snackbarMessage = "Order rescheduled successfully!"
                            },
                            onError = { err ->
                                snackbarMessage = err
                            }
                        )
                    }
                ) {
                    Text("Save New Slot")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { orderToReschedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search orders by store or item...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("orders_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${orders.size})", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_all_orders")
                )
                FilterChip(
                    selected = selectedFilter == "DELIVERED",
                    onClick = { selectedFilter = "DELIVERED" },
                    label = { Text("Delivered", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_delivered_orders")
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("In Progress", fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_active_orders")
                )
            }
        }

        snackbarMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        snackbarMessage = null
                        onNavigateToCart()
                    }) {
                        Text("View Cart", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (orders.isEmpty()) "No past orders found." else "No orders match filter.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Scheduled Orders Distinct Section
                if (scheduledOrders.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📅", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Scheduled Orders (${scheduledOrders.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "These orders wait in queue and automatically go to the kitchen 30 mins before delivery.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    items(scheduledOrders, key = { "sched_${it.orderId}" }) { schedOrder ->
                        CustomerOrderCard(
                            order = schedOrder,
                            isReviewed = false,
                            onRateOrder = null,
                            onReorder = null,
                            onReschedule = { orderToReschedule = schedOrder },
                            onCancelScheduled = {
                                customerViewModel.cancelScheduledOrder(
                                    orderId = schedOrder.orderId,
                                    onSuccess = { snackbarMessage = "Scheduled order cancelled." },
                                    onError = { err -> snackbarMessage = err }
                                )
                            }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Active and Past Orders
                items(activeAndPastOrders, key = { it.orderId }) { order ->
                    CustomerOrderCard(
                        order = order,
                        isReviewed = reviewedOrderIds.contains(order.orderId),
                        onRateOrder = { showRatingDialogForOrder = order },
                        onReorder = {
                            customerViewModel.reorderOrder(order)
                            snackbarMessage = "Added items from ${order.restaurantName} to cart!"
                        }
                    )
                }
            }
        }

        // Rating Dialog
        showRatingDialogForOrder?.let { order ->
            OrderRatingDialog(
                order = order,
                onDismiss = { showRatingDialogForOrder = null },
                onSubmit = { rating, reviewText, photos, partnerRating, partnerFeedback, compliments ->
                    customerViewModel.submitReview(
                        order = order,
                        rating = rating,
                        reviewText = reviewText,
                        photos = photos,
                        onSuccess = {
                            if (order.deliveryPartnerId.isNotBlank()) {
                                customerViewModel.submitDeliveryPartnerReview(
                                    order = order,
                                    partnerRating = partnerRating,
                                    feedbackText = partnerFeedback,
                                    complimentTags = compliments
                                )
                            }
                            showRatingDialogForOrder = null
                            snackbarMessage = "Thank you! Your feedback for food & delivery partner has been recorded."
                        },
                        onError = { err ->
                            snackbarMessage = err
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun CustomerOrderCard(
    order: Order,
    isReviewed: Boolean = false,
    onRateOrder: (() -> Unit)? = null,
    onReorder: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onCancelScheduled: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(order.createdAt) { dateFormat.format(Date(order.createdAt)) }

    val isScheduled = order.status.lowercase() == "scheduled"
    val isDelivered = order.status.lowercase() == "delivered"

    // Colored badge according to status: placed, preparing, ready, assigned, picked_up, delivered, scheduled
    val (statusText, badgeBg, badgeTextColor) = when (order.status.lowercase()) {
        "scheduled" -> Triple("SCHEDULED", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        "placed" -> Triple("ORDER PLACED", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "preparing" -> Triple("PREPARING", Color(0xFFFFF3E0), Color(0xFFE65100))
        "ready" -> Triple("READY FOR PICKUP", Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        "assigned" -> Triple("COURIER ASSIGNED", Color(0xFFE0F2F1), Color(0xFF00695C))
        "picked_up" -> Triple("OUT FOR DELIVERY", Color(0xFFE8EAF6), Color(0xFF283593))
        "delivered" -> Triple("DELIVERED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple(order.status.uppercase(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_order_card_${order.orderId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (order.restaurantName.isNotBlank()) order.restaurantName else "SwiftCart Restaurant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Order #${order.orderId.takeLast(6).uppercase()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = CircleShape
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (isScheduled && order.scheduledDeliveryTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val schedFormat = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
                val schedStr = schedFormat.format(Date(order.scheduledDeliveryTime))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🗓️ Scheduled Delivery: $schedStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Items List
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.name}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.paymentMethod.isNotBlank()) {
                        Text(
                            text = "Paid via ${order.paymentMethod}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isScheduled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onReschedule != null) {
                        OutlinedButton(
                            onClick = onReschedule,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("reschedule_order_button")
                        ) {
                            Text("Reschedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onCancelScheduled != null) {
                        Button(
                            onClick = onCancelScheduled,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("cancel_scheduled_button")
                        ) {
                            Text("Cancel Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (onReorder != null || onRateOrder != null || isReviewed) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReviewed) {
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reviewed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    } else if (onRateOrder != null && isDelivered) {
                        OutlinedButton(
                            onClick = onRateOrder,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("rate_order_button_${order.orderId}")
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rate Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (onReorder != null) {
                        Button(
                            onClick = onReorder,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reorder_button_${order.orderId}")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reorder 🔄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderRatingDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, reviewText: String, photos: List<String>, partnerRating: Int, partnerFeedback: String, compliments: List<String>) -> Unit
) {
    var selectedRating by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var photoUrlInput by remember { mutableStateOf("") }
    var attachedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }

    var partnerRating by remember { mutableStateOf(5) }
    var partnerFeedback by remember { mutableStateOf("") }
    var selectedCompliments by remember { mutableStateOf<Set<String>>(emptySet()) }

    val presetPhotoSamples = listOf(
        Pair("Burger 🍔", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500"),
        Pair("Pizza 🍕", "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500"),
        Pair("Meal Bowl 🥗", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rate & Review Order", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How was your food from ${if (order.restaurantName.isNotBlank()) order.restaurantName else "the store"}?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star stars",
                                tint = if (star <= selectedRating) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Food Review (optional)") },
                    placeholder = { Text("Food taste, portion, temperature...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_text_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Photo URL Input
                OutlinedTextField(
                    value = photoUrlInput,
                    onValueChange = { photoUrlInput = it },
                    label = { Text("Attach Photo URL (max 3)") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    singleLine = true,
                    trailingIcon = {
                        if (photoUrlInput.isNotBlank() && attachedPhotos.size < 3) {
                            IconButton(onClick = {
                                if (photoUrlInput.isNotBlank() && attachedPhotos.size < 3) {
                                    attachedPhotos = attachedPhotos + photoUrlInput.trim()
                                    photoUrlInput = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Photo")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_photo_url_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick preset photo chips
                Text(
                    text = "Quick Sample Photos:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    presetPhotoSamples.forEach { (label, url) ->
                        SuggestionChip(
                            onClick = {
                                if (attachedPhotos.size < 3 && !attachedPhotos.contains(url)) {
                                    attachedPhotos = attachedPhotos + url
                                }
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            enabled = attachedPhotos.size < 3 && !attachedPhotos.contains(url)
                        )
                    }
                }

                if (attachedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachedPhotos) { photoUrl ->
                            Box(modifier = Modifier.size(56.dp)) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Attached photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .clickable {
                                            attachedPhotos = attachedPhotos - photoUrl
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // DELIVERY PARTNER RATING SECTION
                Text(
                    text = "Rate Delivery Service 🚴",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (order.deliveryPartnerName.isNotBlank()) "How was your delivery with ${order.deliveryPartnerName}?" else "How was your delivery partner?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { partnerRating = star },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= partnerRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star partner stars",
                                tint = if (star <= partnerRating) Color(0xFF0284C7) else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("On Time ⏱️", "Polite 🤝", "Careful 📦").forEach { comp ->
                        val isSelected = selectedCompliments.contains(comp)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCompliments = if (isSelected) selectedCompliments - comp else selectedCompliments + comp
                            },
                            label = { Text(comp, fontSize = 10.sp) },
                            modifier = Modifier.testTag("partner_compliment_${comp.take(4)}")
                        )
                    }
                }

                OutlinedTextField(
                    value = partnerFeedback,
                    onValueChange = { partnerFeedback = it },
                    label = { Text("Delivery feedback (optional)") },
                    placeholder = { Text("Quick delivery, polite rider...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delivery_partner_feedback_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPhotos = (attachedPhotos + listOf(photoUrlInput.trim())).filter { it.isNotBlank() }.take(3)
                    onSubmit(selectedRating, reviewText, finalPhotos, partnerRating, partnerFeedback, selectedCompliments.toList())
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_review_dialog_button")
            ) {
                Text("Submit Feedback", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityImpactScreen(
    session: ActiveSession,
    customerViewModel: CustomerViewModel,
    onBackClick: () -> Unit
) {
    val allAppOrders by customerViewModel.allAppOrders.collectAsState()
    val customerOrders by customerViewModel.customerOrders.collectAsState()
    val restaurants by customerViewModel.restaurants.collectAsState()

    // Aggregate App-Wide Calculations
    val completedAppOrders = remember(allAppOrders) {
        allAppOrders.filter { !it.status.equals("cancelled", ignoreCase = true) && !it.status.equals("canceled", ignoreCase = true) }
    }
    val rawMealsDelivered = remember(completedAppOrders) {
        completedAppOrders.sumOf { order -> order.items.sumOf { it.quantity } }
    }
    val totalMealsDelivered = maxOf(rawMealsDelivered, 1420 + rawMealsDelivered)

    val rawEcoOrders = completedAppOrders.size
    val totalEcoOrders = maxOf(rawEcoOrders, 1180 + rawEcoOrders)

    val rawRestaurantsSupported = remember(completedAppOrders, restaurants) {
        val orderRests = completedAppOrders.map { it.restaurantId }.filter { it.isNotBlank() }.toSet()
        maxOf(orderRests.size, restaurants.size)
    }
    val totalLocalRestaurantsSupported = maxOf(rawRestaurantsSupported, 28)

    val rawEarningsPaidOut = remember(completedAppOrders) {
        completedAppOrders.sumOf { 35.0 + it.tipAmount }
    }
    val totalEarningsPaidOut = maxOf(rawEarningsPaidOut, 18450.0 + rawEarningsPaidOut)

    // User Personal Contributions
    val myValidOrders = remember(customerOrders) {
        customerOrders.filter { !it.status.equals("cancelled", ignoreCase = true) && !it.status.equals("canceled", ignoreCase = true) }
    }
    val myDeliveriesCount = myValidOrders.size
    val myMealsCount = myValidOrders.sumOf { order -> order.items.sumOf { it.quantity } }
    val myRestCount = myValidOrders.map { it.restaurantId }.filter { it.isNotBlank() }.distinct().size
    val myEarningsContribution = myValidOrders.sumOf { 35.0 + it.tipAmount }
    val myPlasticSavedKg = String.format(Locale.US, "%.1f", myDeliveriesCount * 0.35)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Community Impact", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌿", fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("community_impact_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Hero Header Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_impact_hero_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.25f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🌍", fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SwiftCart Collective Impact",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-time app-wide sustainability metrics",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Every order placed on SwiftCart fuels sustainable local dining, plastic-free eco packaging, and fair payouts for local delivery partners.",
                            fontSize = 13.sp,
                            color = Color(0xFFECFDF5),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Personal Highlight Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46).copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personal_impact_highlight_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Green Contribution 💚",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (myDeliveriesCount > 0)
                                "You've contributed to $myDeliveriesCount deliveries ($myMealsCount meals) across $myRestCount local restaurant partners and helped delivery partners earn ₹${String.format(Locale.US, "%.0f", myEarningsContribution)}!"
                            else
                                "You haven't placed an order yet. Place your first order to join our green dining movement!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        if (myDeliveriesCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "🍃 Saved ~$myPlasticSavedKg kg of single-use plastic with 100% compostable packaging!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // App-Wide Aggregate Stats Header
            item {
                Text(
                    text = "App-Wide Live Impact 📊",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // App-Wide Stats 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ImpactStatCard(
                            emoji = "🍲",
                            value = "${String.format(Locale.US, "%,d", totalMealsDelivered)}+",
                            label = "Total Meals Delivered",
                            modifier = Modifier.weight(1f)
                        )
                        ImpactStatCard(
                            emoji = "📦",
                            value = "${String.format(Locale.US, "%,d", totalEcoOrders)}+",
                            label = "Eco-Packaging Orders",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ImpactStatCard(
                            emoji = "🏪",
                            value = "$totalLocalRestaurantsSupported+",
                            label = "Local Restaurants Supported",
                            modifier = Modifier.weight(1f)
                        )
                        ImpactStatCard(
                            emoji = "🚴",
                            value = "₹${String.format(Locale.US, "%,.0f", totalEarningsPaidOut)}",
                            label = "Partner Earnings Paid",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sustainability Pillars Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SwiftCart Eco Commitments 🛡️",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PillarItem(
                            icon = "🍃",
                            title = "100% Biodegradable Packaging",
                            description = "All partner restaurants use plant-starch cutlery, unbleached paper bags, and non-toxic food containers."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        PillarItem(
                            icon = "⚡",
                            title = "Electric Fleet Priority",
                            description = "Route optimization engine prioritizes electric two-wheelers and bicycles for low-carbon delivery."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        PillarItem(
                            icon = "🤝",
                            title = "Direct Partner Support",
                            description = "100% of customer tips and fair delivery payouts go directly to local delivery partners without deductions."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PillarItem(
    icon: String,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    session: ActiveSession,
    customerViewModel: CustomerViewModel,
    onBackClick: () -> Unit
) {
    val userProfile by customerViewModel.userProfile.collectAsState()
    val badges = remember(userProfile, customerViewModel) {
        customerViewModel.getAchievementBadges()
    }
    val unlockedCount = badges.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My Achievements", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🏆", fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("achievements_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Streak Card Header
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_streak_summary_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🔥", fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${userProfile?.currentStreak ?: 0} Week Streak!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Longest Streak: ${userProfile?.longestStreak ?: 0} weeks",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$unlockedCount/${badges.size} Badges",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Order at least once every 7 days to maintain and grow your weekly streak! Unlocking achievements earns you rewards and exclusive SwiftCart perks.",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Badges Section Header
            item {
                Text(
                    text = "Achievement Badges 🎖️",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Badges Grid List
            items(badges, key = { it.id }) { badge ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.isUnlocked)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (badge.isUnlocked)
                        BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    else
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isUnlocked) 2.dp else 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("badge_card_${badge.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Icon Box
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                color = if (badge.isUnlocked)
                                    Color(0xFF38BDF8).copy(alpha = 0.2f)
                                else
                                    Color.Gray.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = badge.iconEmoji,
                                        fontSize = 28.sp,
                                        color = if (badge.isUnlocked) Color.Unspecified else Color.Gray
                                    )
                                }
                            }

                            if (!badge.isUnlocked) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Badge Info Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = badge.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                if (badge.isUnlocked) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Unlocked",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = badge.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Bar for Locked Badges
                            if (!badge.isUnlocked) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { (badge.currentCount.toFloat() / badge.requiredCount.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = Color(0xFF38BDF8),
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    Text(
                                        text = badge.progressText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            } else {
                                Text(
                                    text = badge.progressText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

