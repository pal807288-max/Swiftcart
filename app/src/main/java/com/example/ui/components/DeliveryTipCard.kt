package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DeliveryTipCard(
    orderId: String,
    currentTip: Double = 0.0,
    onTipUpdated: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTipAmount by remember(currentTip) { mutableStateOf(currentTip) }
    var customTipInput by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showThankYou by remember(currentTip) { mutableStateOf(currentTip > 0) }

    val presetAmounts = listOf(10.0, 20.0, 30.0)

    fun applyTip(amount: Double) {
        if (orderId.isBlank()) return
        isSubmitting = true
        val db = FirebaseFirestore.getInstance()
        db.collection("orders").document(orderId)
            .update("tipAmount", amount)
            .addOnSuccessListener {
                isSubmitting = false
                selectedTipAmount = amount
                showThankYou = amount > 0
                onTipUpdated?.invoke(amount)
            }
            .addOnFailureListener {
                isSubmitting = false
            }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("delivery_tip_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "Tip",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add a Tip for your Delivery Partner 🛵",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "100% of your tip goes directly to the delivery hero.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Select Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                presetAmounts.forEach { amt ->
                    val isSelected = !isCustomSelected && selectedTipAmount == amt
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            isCustomSelected = false
                            val newTip = if (isSelected) 0.0 else amt
                            applyTip(newTip)
                        },
                        label = {
                            Text(
                                text = "₹${amt.toInt()}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                FilterChip(
                    selected = isCustomSelected,
                    onClick = {
                        isCustomSelected = !isCustomSelected
                    },
                    label = {
                        Text(
                            text = "Custom",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom Tip Input Row
            if (isCustomSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customTipInput,
                        onValueChange = { customTipInput = it },
                        label = { Text("Enter Tip Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val customVal = customTipInput.toDoubleOrNull() ?: 0.0
                            if (customVal >= 0) {
                                applyTip(customVal)
                            }
                        },
                        enabled = !isSubmitting && customTipInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }

            // Thank-you animation/banner when tip is set
            AnimatedVisibility(
                visible = showThankYou && selectedTipAmount > 0,
                enter = fadeIn() + scaleIn()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Thank you",
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thank you! You added a ₹${String.format("%.2f", selectedTipAmount)} tip for your delivery partner! ❤️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}
