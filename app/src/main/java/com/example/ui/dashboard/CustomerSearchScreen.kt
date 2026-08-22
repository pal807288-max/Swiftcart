package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ActiveSession
import com.example.data.Item
import com.example.data.Store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSearchScreen(
    viewModel: CustomerViewModel,
    session: ActiveSession,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val searchResultsStores by viewModel.searchResultsStores.collectAsState()
    val searchResultsItems by viewModel.searchResultsItems.collectAsState()

    var selectedProductDetails by remember { mutableStateOf<Item?>(null) }

    // Search Filter States
    var rating4PlusOnly by remember { mutableStateOf(false) }
    var selectedPriceFilter by remember { mutableStateOf("All") } // "All", "Under ₹150", "₹150 - ₹300", "₹300+"
    var selectedTimeFilter by remember { mutableStateOf("All") } // "All", "Under 20m", "Under 30m"
    var selectedCuisineFilter by remember { mutableStateOf("All") } // "All", "Burgers", "Pizza", "Asian", "Bakery", "Grocery"

    val hasActiveFilters = rating4PlusOnly || selectedPriceFilter != "All" || selectedTimeFilter != "All" || selectedCuisineFilter != "All"

    // Computed Filtered Stores
    val filteredStores = remember(searchResultsStores, stores, searchQuery, rating4PlusOnly, selectedPriceFilter, selectedTimeFilter, selectedCuisineFilter) {
        val baseStores = if (searchQuery.isBlank() && hasActiveFilters) stores else searchResultsStores
        baseStores.filter { store ->
            val matchesRating = if (rating4PlusOnly) store.rating >= 4.0 else true

            val etaMins = store.eta.filter { it.isDigit() }.toIntOrNull() ?: 25
            val matchesTime = when (selectedTimeFilter) {
                "Under 20m" -> etaMins <= 20
                "Under 30m" -> etaMins <= 30
                else -> true
            }

            val matchesCuisine = when (selectedCuisineFilter) {
                "All" -> true
                "Grocery" -> store.type.equals("GROCERY", ignoreCase = true)
                else -> store.type.contains(selectedCuisineFilter, ignoreCase = true) ||
                        store.description.contains(selectedCuisineFilter, ignoreCase = true) ||
                        store.name.contains(selectedCuisineFilter, ignoreCase = true)
            }

            matchesRating && matchesTime && matchesCuisine
        }
    }

    // Computed Filtered Items
    val filteredItems = remember(searchResultsItems, searchQuery, rating4PlusOnly, selectedPriceFilter, selectedCuisineFilter) {
        if (searchQuery.isBlank() && !hasActiveFilters) {
            emptyList()
        } else {
            searchResultsItems.filter { item ->
                val matchesRating = if (rating4PlusOnly) item.rating >= 4.0 else true

                val matchesPrice = when (selectedPriceFilter) {
                    "Under ₹150" -> item.price <= 150.0
                    "₹150 - ₹300" -> item.price in 150.0..300.0
                    "₹300+" -> item.price >= 300.0
                    else -> true
                }

                val matchesCuisine = when (selectedCuisineFilter) {
                    "All" -> true
                    else -> item.category.contains(selectedCuisineFilter, ignoreCase = true) ||
                            item.name.contains(selectedCuisineFilter, ignoreCase = true)
                }

                matchesRating && matchesPrice && matchesCuisine
            }
        }
    }

    // Product Details Dialog overlay
    selectedProductDetails?.let { item ->
        ProductDetailsDialog(
            item = item,
            onDismiss = { selectedProductDetails = null },
            onAddToCart = {
                viewModel.addToCart(session.userId, item)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search TextField Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search stores, categories, foods, groceries...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear query"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Done automatically on state change */ }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_text_input")
            )

            // SEARCH FILTER CHIPS ROW (Rating 4.0+, Price, Delivery Time, Cuisine)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rating Filter Chip
                item {
                    FilterChip(
                        selected = rating4PlusOnly,
                        onClick = { rating4PlusOnly = !rating4PlusOnly },
                        label = { Text("⭐ 4.0+ Rating") },
                        leadingIcon = if (rating4PlusOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("search_filter_rating_4plus")
                    )
                }

                // Cuisine Filter Chips
                val cuisines = listOf("All Cuisines", "Burgers", "Pizza", "Asian", "Bakery", "Grocery")
                items(cuisines) { cuisine ->
                    val isSelected = (cuisine == "All Cuisines" && selectedCuisineFilter == "All") || (selectedCuisineFilter == cuisine)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCuisineFilter = if (cuisine == "All Cuisines") "All" else cuisine
                        },
                        label = { Text(if (cuisine == "All Cuisines") "🍽️ Cuisines" else cuisine) },
                        modifier = Modifier.testTag("search_filter_cuisine_${cuisine.take(5)}")
                    )
                }

                // Price Filter Chips
                val prices = listOf("All Prices", "Under ₹150", "₹150 - ₹300", "₹300+")
                items(prices) { price ->
                    val isSelected = (price == "All Prices" && selectedPriceFilter == "All") || (selectedPriceFilter == price)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPriceFilter = if (price == "All Prices") "All" else price
                        },
                        label = { Text(price) },
                        modifier = Modifier.testTag("search_filter_price_${price.take(5)}")
                    )
                }

                // Delivery Time Filter Chips
                val times = listOf("Any Time", "Under 20m", "Under 30m")
                items(times) { time ->
                    val isSelected = (time == "Any Time" && selectedTimeFilter == "All") || (selectedTimeFilter == time)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTimeFilter = if (time == "Any Time") "All" else time
                        },
                        label = { Text(if (time == "Any Time") "⏱️ Time" else "⏱️ $time") },
                        modifier = Modifier.testTag("search_filter_time_${time.take(5)}")
                    )
                }
            }
        }

        if (searchQuery.isBlank() && !hasActiveFilters) {
            // Suggest trending dishes, popular restaurants, and groceries before typing
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trending Dishes Section
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥 Trending Dishes & Groceries", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val trendingDishes = listOf("Butter Chicken 🍲", "Paneer Tikka 🧀", "Smash Burger 🍔", "Farm Fresh Milk 🥛", "Pepperoni Pizza 🍕", "Cold Coffee ☕")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(trendingDishes) { dish ->
                                SuggestionChip(
                                    onClick = { viewModel.setSearchQuery(dish.split(" ").first()) },
                                    label = { Text(dish, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("trending_dish_chip_${dish.take(5)}")
                                )
                            }
                        }
                    }
                }

                // Popular Restaurants Section
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏪 Popular Restaurants & Stores", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val popularStores = listOf("Spice Symphony", "Pizza Paradise", "Burger House", "SwiftCart DarkStore", "Bakery Bliss")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(popularStores) { storeName ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.setSearchQuery(storeName) },
                                    label = { Text("🏬 $storeName", fontSize = 12.sp) },
                                    modifier = Modifier.testTag("popular_store_chip_${storeName.take(5)}")
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Popular Search Keywords",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val popularTags = listOf("Burgers", "Pizza", "Fruits & Veggies", "Dairy & Eggs", "Biryani", "Bakery", "Desserts")
                items(popularTags) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.setSearchQuery(tag) }
                            .testTag("popular_search_tag_$tag"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                  text = tag,
                                  fontSize = 14.sp,
                                  fontWeight = FontWeight.Medium,
                                  color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            // Display Results
            if (filteredStores.isEmpty() && filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No results found for \"$searchQuery\"" else "No matching stores for active filters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing or relaxing your active search filters (Rating, Price, Time, Cuisine)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Store Results Section
                    if (filteredStores.isNotEmpty()) {
                        item {
                            Text(
                                text = "Stores (${filteredStores.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(filteredStores) { store ->
                            SearchResultStoreCard(store = store) {
                                viewModel.selectStore(store)
                            }
                        }
                    }

                    // Item Results Section
                    if (filteredItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "Products (${filteredItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(filteredItems) { item ->
                            val itemStore = stores.find { it.id == item.storeId }
                            SearchResultItemCard(
                                item = item,
                                storeName = itemStore?.name ?: "Partner Store",
                                onAddToCart = {
                                    viewModel.addToCart(session.userId, item)
                                },
                                onCardClick = {
                                    selectedProductDetails = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultStoreCard(
    store: Store,
    onClick: () -> Unit
) {
    val isActive = store.activeStatus
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("search_store_result_${store.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = if (store.logo.isNotEmpty()) store.logo else store.imageUrl,
                    contentDescription = store.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (!isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CLOSED",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${store.type.uppercase()} • ${store.address}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = "View Store",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SearchResultItemCard(
    item: Item,
    storeName: String,
    onAddToCart: () -> Unit,
    onCardClick: () -> Unit
) {
    val isAvailable = item.availability && !item.stockStatus.equals("Out of Stock", ignoreCase = true)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCardClick() }
            .testTag("search_item_result_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = if (item.image.isNotEmpty()) item.image else item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SOLD OUT",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.stockStatus.equals("Low Stock", ignoreCase = true) && isAvailable) {
                        Surface(
                            color = Color(0xFFFFF3CD),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "LOW STOCK",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "From $storeName • ${item.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "₹${String.format(java.util.Locale.US, "%.2f", item.price)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = {
                    if (isAvailable) {
                        onAddToCart()
                    }
                },
                enabled = isAvailable,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .size(36.dp)
                    .testTag("search_add_to_cart_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to cart",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
