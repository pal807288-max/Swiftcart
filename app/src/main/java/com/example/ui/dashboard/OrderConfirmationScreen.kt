package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.Order
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun OrderConfirmationScreen(
    orderId: String,
    onViewOrdersClick: () -> Unit,
    onContinueShoppingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fetchedOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(orderId) {
        if (orderId.isNotBlank()) {
            FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        fetchedOrder = doc.toObject(Order::class.java)
                    }
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("order_confirmation_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Order Placed Successfully!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val displayId = if (orderId.length > 8) orderId.take(8) else orderId
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Order ID: #$displayId",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Status Banner
            fetchedOrder?.let { order ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (order.paymentStatus.equals("cod", ignoreCase = true)) Color(0xFFFFF8E1)
                        else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("payment_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = if (order.paymentStatus.equals("cod", ignoreCase = true)) Color(0xFFF57F17) else Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (order.paymentStatus.equals("cod", ignoreCase = true)) "Pay via Cash on Delivery (COD)"
                                else "Paid via ${order.paymentMethod}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (order.paymentStatus.equals("cod", ignoreCase = true)) Color(0xFFF57F17) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (order.paymentStatus.equals("cod", ignoreCase = true)) "Payment Status: COD • Collect cash at doorstep"
                                else "Payment Status: Verified & Successful • Prepaid",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (order.ecoPackaging) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("order_confirmation_eco_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌱", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eco-Friendly Packaging Chosen • Plastic Saved!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val orderSubtotal = if (order.items.isNotEmpty()) order.items.sumOf { it.price * it.quantity } else ((order.totalAmount - 35.0) / 1.05).coerceAtLeast(0.0)

                com.example.ui.components.TransparentPricingCard(
                    subtotal = orderSubtotal,
                    deliveryFee = 30.0,
                    platformFee = 5.0,
                    taxRate = 0.05,
                    discount = 0.0,
                    ecoPackaging = order.ecoPackaging,
                    showHeading = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your order has been received and is being sent to the restaurant for preparation.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onViewOrdersClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("view_my_orders_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track Order in 'My Orders'", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinueShoppingClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("continue_shopping_button")
            ) {
                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue Exploring")
            }
        }
    }
}
