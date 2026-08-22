package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TransparentPricingCard(
    subtotal: Double,
    deliveryFee: Double = 30.0,
    platformFee: Double = 5.0,
    taxRate: Double = 0.05,
    discount: Double = 0.0,
    loyaltyDiscount: Double = 0.0,
    couponDiscount: Double = 0.0,
    couponCode: String = "",
    ecoPackaging: Boolean = false,
    isPlusSubscriber: Boolean = false,
    showHeading: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    val calculatedSubtotal = subtotal.coerceAtLeast(0.0)
    val ecoFee = if (ecoPackaging) 5.0 else 0.0
    val actualDeliveryFee = if (isPlusSubscriber) 0.0 else (if (calculatedSubtotal > 0) deliveryFee else 0.0)
    val actualPlatformFee = if (calculatedSubtotal > 0) platformFee else 0.0
    val tax = calculatedSubtotal * taxRate
    val grandTotal = (calculatedSubtotal + actualDeliveryFee + actualPlatformFee + tax + ecoFee - discount - loyaltyDiscount - couponDiscount).coerceAtLeast(0.0)

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "What is Platform Fee?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "A nominal ₹5.00 flat fee helps us continuously improve the SwiftCart app, maintain 24/7 dedicated customer support, and fund safety insurance for our delivery partners.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.testTag("platform_fee_dialog_dismiss")
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transparent_pricing_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showHeading) {
                Text(
                    text = "Transparent Bill Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", calculatedSubtotal)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Delivery Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Delivery Fee", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    if (isPlusSubscriber) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FREE (Plus)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (isPlusSubscriber) "₹0.00" else "₹${String.format(Locale.US, "%.2f", actualDeliveryFee)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPlusSubscriber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Eco Packaging Fee if selected
            if (ecoPackaging) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Eco Packaging 🌱", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "₹5.00",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Platform Fee with Info Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showInfoDialog = true }
                        .testTag("platform_fee_info_trigger")
                ) {
                    Text("Platform Fee", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Platform Fee Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", actualPlatformFee)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Taxes (GST 5%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Taxes & Govt Charges (GST 5%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", tax)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Discount if applied
            if (discount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Coupon / Plus Discount", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "-₹${String.format(Locale.US, "%.2f", discount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Loyalty Points Discount
            if (loyaltyDiscount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Loyalty Rewards Discount (100 pts)", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "-₹${String.format(Locale.US, "%.2f", loyaltyDiscount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Coupon Discount
            if (couponDiscount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (couponCode.isNotBlank()) "Coupon Discount ($couponCode)" else "Coupon Discount",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "-₹${String.format(Locale.US, "%.2f", couponDiscount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // To Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("To Pay Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", grandTotal)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("pricing_card_grand_total")
                )
            }
        }
    }
}
