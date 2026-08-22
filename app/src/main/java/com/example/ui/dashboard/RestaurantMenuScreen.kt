package com.example.ui.dashboard

import com.example.data.firestore.isDarkStore
import com.example.data.firestore.waitColor
import com.example.data.firestore.waitLabel
import com.example.data.firestore.waitShortBadge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Restaurant
import com.example.data.firestore.Review
import com.example.data.firestore.isDarkStore
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Timer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantMenuScreen(
    restaurant: Restaurant,
    viewModel: CustomerViewModel,
    userEmail: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems by viewModel.menuItems.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val restaurantReviews by viewModel.selectedRestaurantReviews.collectAsState()
    val followedRestaurants by viewModel.followedRestaurants.collectAsState()

    val isFollowed = remember(followedRestaurants, restaurant.restaurantId) {
        followedRestaurants.contains(restaurant.restaurantId)
    }

    var showHygieneTooltip by remember { mutableStateOf(false) }
    var onlyWithPhotosFilter by remember { mutableStateOf(false) }
    var photoPreviewUrl by remember { mutableStateOf<String?>(null) }

    // Table Booking States
    var showTableBookingDialog by remember { mutableStateOf(false) }
    var tableBookingSuccessMessage by remember { mutableStateOf<String?>(null) }
    var bookingGuests by remember { mutableIntStateOf(2) }
    var bookingTimeSlot by remember { mutableStateOf("07:00 PM") }
    var bookingDate by remember { mutableStateOf("Today") }

    val totalItemCount = viewModel.totalItemCount
    val totalAmount = viewModel.totalAmount

    var activePriceFilter by remember { mutableStateOf("All Prices") }
    val priceFilters = listOf("All Prices", "Under ₹100", "₹100 - ₹250", "₹250+")

    val filteredMenuItems = remember(menuItems, activePriceFilter) {
        when (activePriceFilter) {
            "Under ₹100" -> menuItems.filter { it.price < 100.0 }
            "₹100 - ₹250" -> menuItems.filter { it.price in 100.0..250.0 }
            "₹250+" -> menuItems.filter { it.price > 250.0 }
            else -> menuItems
        }
    }

    val (avgRating, reviewCount) = remember(restaurantReviews) {
        if (restaurantReviews.isEmpty()) Pair(0.0, 0)
        else Pair(restaurantReviews.map { it.rating }.average(), restaurantReviews.size)
    }

    val displayedReviews = remember(restaurantReviews, onlyWithPhotosFilter) {
        if (onlyWithPhotosFilter) {
            restaurantReviews.filter { it.photos.isNotEmpty() }
        } else {
            restaurantReviews
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = restaurant.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("menu_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Floating View Cart Bar
            AnimatedVisibility(
                visible = totalItemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$totalItemCount item${if (totalItemCount > 1) "s" else ""} in cart",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Total: ₹${String.format("%.2f", totalAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Button(
                            onClick = onNavigateToCart,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("view_cart_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Cart", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("restaurant_menu_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Restaurant Banner Header
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = restaurant.photoUrl.ifBlank { "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600" },
                            contentDescription = restaurant.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = restaurant.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "OPEN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (restaurant.category.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = restaurant.category,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (restaurant.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = restaurant.address,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Star Rating Badge
                                Surface(
                                    color = Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (reviewCount > 0) String.format(Locale.US, "%.1f", avgRating) else "New",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (reviewCount > 0) "($reviewCount review${if (reviewCount > 1) "s" else ""})" else "(No reviews)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Hygiene Verified Badge
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { showHygieneTooltip = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Hygiene Verified",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", restaurant.hygieneRating)}/5 Hygiene 🛡️",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Delivery ETA & Live Wait Time Badges
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isInstant = restaurant.isDarkStore
                                Surface(
                                    color = if (isInstant) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isInstant) Icons.Default.Bolt else Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = if (isInstant) Color(0xFF0284C7) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isInstant) "Instant: 10-15 min ⚡" else "Delivery: ${restaurant.deliveryEta.ifBlank { "20-30 min" }}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isInstant) Color(0xFF0369A1) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = restaurant.waitColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = restaurant.waitColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Entity Details Row: Operating Hours, Delivery Radius, Prep Time
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏰ ${restaurant.operatingHours.ifBlank { "08:00 AM - 11:00 PM" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "📍 Radius: ${restaurant.deliveryRadiusKm} km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "🍳 Prep: ${restaurant.prepTimeMinutes}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Follow / Following Button
                                FilterChip(
                                    selected = isFollowed,
                                    onClick = {
                                        viewModel.toggleFollowRestaurant(userEmail, restaurant.restaurantId)
                                    },
                                    label = {
                                        Text(
                                            text = if (isFollowed) "Following ❤️" else "+ Follow",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Follow",
                                            tint = if (isFollowed) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("restaurant_follow_button")
                                )

                                // Zomato-style Book a Table Button
                                Button(
                                    onClick = { showTableBookingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("book_table_button")
                                ) {
                                    Text("🍽️ Book a Table", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Menu Section Title
            item {
                Text(
                    text = "Menu Items (${filteredMenuItems.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Price Range Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priceFilters.forEach { priceLabel ->
                        FilterChip(
                            selected = activePriceFilter == priceLabel,
                            onClick = { activePriceFilter = priceLabel },
                            label = { Text(priceLabel, fontSize = 12.sp) },
                            modifier = Modifier.testTag("price_chip_${priceLabel.replace(" ", "_")}")
                        )
                    }
                }
            }

            // Menu Items List
            if (filteredMenuItems.isEmpty()) {
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
                            Text(
                                text = if (menuItems.isEmpty())
                                    "No menu items available for this restaurant right now."
                                else "No menu items match price filter '$activePriceFilter'.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredMenuItems, key = { it.itemId }) { item ->
                    val quantity = viewModel.getItemQuantity(item.itemId)

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_item_card_${item.itemId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.photoUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600" },
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Veg/Non-Veg Icon Indicator
                                    Surface(
                                        color = if (item.isVeg) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = if (item.isVeg) "VEG 🌱" else "NON-VEG 🍗",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isVeg) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Product Freshness / Expiry Tag (Blinkit / Quick Commerce)
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = if (item.freshnessTag.isNotBlank()) item.freshnessTag else "Farm Fresh 🌿",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = item.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "₹${String.format("%.2f", item.price)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Add / Quantity Stepper
                                if (quantity == 0) {
                                    Button(
                                        onClick = { viewModel.addToCart(item, restaurant) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("add_item_button_${item.itemId}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ADD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updateQuantity(item.itemId, -1) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("decrement_item_${item.itemId}")
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = "$quantity",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.updateQuantity(item.itemId, 1) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("increment_item_${item.itemId}")
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Customer Reviews Section Header & Filter
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Reviews (${displayedReviews.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val photoCount = restaurantReviews.count { it.photos.isNotEmpty() }
                    if (photoCount > 0) {
                        FilterChip(
                            selected = onlyWithPhotosFilter,
                            onClick = { onlyWithPhotosFilter = !onlyWithPhotosFilter },
                            label = { Text("With Photos 📷 ($photoCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("filter_photo_reviews_chip")
                        )
                    }
                }
            }

            if (displayedReviews.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (onlyWithPhotosFilter) "No reviews with photos yet." else "No reviews yet. Be the first to order and review!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedReviews, key = { it.reviewId }) { review ->
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val formattedDate = remember(review.createdAt) { dateFormat.format(Date(review.createdAt)) }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_card_${review.reviewId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (star <= review.rating) Color(0xFFFFB300) else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${review.rating}/5",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (review.reviewText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${review.reviewText}\"",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }

                            if (review.photos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(review.photos) { photoUrl ->
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "Customer Review Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .clickable { photoPreviewUrl = photoUrl }
                                                .testTag("review_photo_thumbnail")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Hygiene Tooltip Dialog
    if (showHygieneTooltip) {
        AlertDialog(
            onDismissRequest = { showHygieneTooltip = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hygiene & Food Safety Rating", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Based on kitchen cleanliness and food safety standards verified by SwiftCart.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHygieneTooltip = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Photo Preview Dialog
    photoPreviewUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { photoPreviewUrl = null },
            title = { Text("Customer Review Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                AsyncImage(
                    model = url,
                    contentDescription = "Full Review Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { photoPreviewUrl = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Zomato-style Table Booking Dialog
    if (showTableBookingDialog) {
        AlertDialog(
            onDismissRequest = { showTableBookingDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍽️ Book a Table at ${restaurant.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Date & Time for dining:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Date Selection Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Today", "Tomorrow", "In 2 Days").forEach { dateOpt ->
                            FilterChip(
                                selected = bookingDate == dateOpt,
                                onClick = { bookingDate = dateOpt },
                                label = { Text(dateOpt, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Time Slots
                    Text("Select Time Slot:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("01:00 PM", "07:00 PM", "08:30 PM", "09:30 PM").forEach { timeOpt ->
                            FilterChip(
                                selected = bookingTimeSlot == timeOpt,
                                onClick = { bookingTimeSlot = timeOpt },
                                label = { Text(timeOpt, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Guests Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Number of Guests:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (bookingGuests > 1) bookingGuests-- },
                                enabled = bookingGuests > 1
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("$bookingGuests Guests", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { bookingGuests++ }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    tableBookingSuccessMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(msg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tableBookingSuccessMessage = "Table booked successfully for $bookingGuests Guests on $bookingDate at $bookingTimeSlot! 🎉"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Booking 🍽️", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTableBookingDialog = false; tableBookingSuccessMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

