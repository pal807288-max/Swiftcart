package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Item

@Composable
fun ProductDetailsDialog(
    item: Item,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit,
    onAddToCartWithCustomization: (customizationNote: String, customPrice: Double) -> Unit = { _, _ -> onAddToCart() }
) {
    // Customization States
    var selectedPortion by remember { mutableStateOf("Half / Regular") } // "Half / Regular" or "Full / Large"
    var selectedSpiceLevel by remember { mutableStateOf("Medium 🌶️") } // "Mild 🌶️", "Medium 🌶️🌶️", "Spicy 🌶️🌶️🌶️"
    var extraCheeseSelected by remember { mutableStateOf(false) } // +₹30
    var extraDipSelected by remember { mutableStateOf(false) } // +₹20
    var crispyToppingsSelected by remember { mutableStateOf(false) } // +₹35
    var specialInstructions by remember { mutableStateOf("") }

    // Dynamic Price Calculation
    val portionDelta = if (selectedPortion == "Full / Large") 40.0 else 0.0
    val addOnsPrice = (if (extraCheeseSelected) 30.0 else 0.0) +
            (if (extraDipSelected) 20.0 else 0.0) +
            (if (crispyToppingsSelected) 35.0 else 0.0)
    val calculatedPrice = item.price + portionDelta + addOnsPrice

    val isFoodItem = !item.category.lowercase().contains("grocery") &&
            !item.category.lowercase().contains("dairy") &&
            !item.category.lowercase().contains("household")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp)
                .testTag("product_details_dialog_${item.id}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image & Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = if (item.image.isNotEmpty()) item.image else item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Close button overlay
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                            .testTag("product_details_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Category Tag & Stock Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Stock Status Badge
                        val (badgeColor, textColor) = when {
                            !item.availability || item.stockStatus.equals("Out of Stock", ignoreCase = true) -> 
                                Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
                            item.stockStatus.equals("Low Stock", ignoreCase = true) -> 
                                Pair(Color(0xFFFFF3CD), Color(0xFF856404))
                            else -> 
                                Pair(Color(0xFFD4EDDA), Color(0xFF155724))
                        }

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.stockStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Product Name
                    Text(
                        text = item.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Price with dynamic updates
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${String.format(java.util.Locale.US, "%.2f", calculatedPrice)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (calculatedPrice > item.price) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Base: ₹${String.format(java.util.Locale.US, "%.2f", item.price)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description Section
                    Text(
                        text = if (item.description.isNotEmpty()) item.description else "No description available for this product.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(14.dp))

                    // ITEM CUSTOMIZATION SECTION
                    Text(
                        text = "Customize Your Order",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Portion Size Selection
                    Text(
                        text = "Portion Size",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Half / Regular", "Full / Large (+₹40)").forEach { portionOption ->
                            val isSelected = selectedPortion == portionOption || (portionOption.startsWith("Half") && selectedPortion == "Half / Regular") || (portionOption.startsWith("Full") && selectedPortion == "Full / Large")
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPortion = if (portionOption.startsWith("Full")) "Full / Large" else "Half / Regular"
                                },
                                label = { Text(portionOption, fontSize = 12.sp) },
                                modifier = Modifier.testTag("portion_${portionOption.take(4)}")
                            )
                        }
                    }

                    // 2. Spice Level Selection (if food item)
                    if (isFoodItem) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Spice Level",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Mild 🌶️", "Medium 🌶️🌶️", "Spicy 🌶️🌶️🌶️").forEach { spice ->
                                FilterChip(
                                    selected = selectedSpiceLevel == spice,
                                    onClick = { selectedSpiceLevel = spice },
                                    label = { Text(spice, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("spice_${spice.take(4)}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Add-ons & Extras
                    Text(
                        text = "Add-ons & Extras",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = extraCheeseSelected,
                                onCheckedChange = { extraCheeseSelected = it },
                                modifier = Modifier.testTag("checkbox_extra_cheese")
                            )
                            Text("Extra Cheese 🧀 (+₹30.00)", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = extraDipSelected,
                                onCheckedChange = { extraDipSelected = it },
                                modifier = Modifier.testTag("checkbox_extra_dip")
                            )
                            Text("Extra Sauce / Dip 🥣 (+₹20.00)", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = crispyToppingsSelected,
                                onCheckedChange = { crispyToppingsSelected = it },
                                modifier = Modifier.testTag("checkbox_crispy_toppings")
                            )
                            Text("Crispy Toppings ✨ (+₹35.00)", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Special Instructions
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text("Special Requests / Instructions", fontSize = 12.sp) },
                        placeholder = { Text("E.g., Less oil, extra napkins...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("special_instructions_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Availability Info or Add to Cart Button
                    val isAvailable = item.availability && !item.stockStatus.equals("Out of Stock", ignoreCase = true)
                    
                    if (isAvailable) {
                        Button(
                            onClick = {
                                // Build Customization Note Summary
                                val noteParts = mutableListOf<String>()
                                noteParts.add(selectedPortion)
                                if (isFoodItem) noteParts.add(selectedSpiceLevel)
                                if (extraCheeseSelected) noteParts.add("Extra Cheese")
                                if (extraDipSelected) noteParts.add("Extra Dip")
                                if (crispyToppingsSelected) noteParts.add("Crispy Toppings")
                                if (specialInstructions.isNotBlank()) noteParts.add("Note: ${specialInstructions.trim()}")

                                val fullNote = noteParts.joinToString(", ")
                                onAddToCartWithCustomization(fullNote, calculatedPrice)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("product_details_add_to_cart_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add to Cart • ₹${String.format(java.util.Locale.US, "%.2f", calculatedPrice)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This item is currently out of stock.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
