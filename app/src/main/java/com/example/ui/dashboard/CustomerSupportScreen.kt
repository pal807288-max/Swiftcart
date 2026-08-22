package com.example.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FaqItem(
    val id: Int,
    val question: String,
    val questionHindi: String,
    val answer: String,
    val category: String
)

data class SupportChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val time: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
)

data class SupportTicket(
    val ticketId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val issueType: String = "",
    val orderId: String = "",
    val description: String = "",
    val status: String = "OPEN",
    val createdAt: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    session: ActiveSession,
    viewModel: CustomerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: FAQs, 1: Live Chat, 2: Raise Ticket, 3: Call Helpline

    val tabs = listOf(
        "FAQs" to Icons.Default.HelpOutline,
        "Live Chat" to Icons.Default.Chat,
        "Raise Ticket" to Icons.Default.ConfirmationNumber,
        "Helpline" to Icons.Default.Call
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "24/7 Customer Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF4CAF50),
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Support Active (Avg response: 1 min)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("support_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Header Row
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("support_tab_$index")
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> FaqSection()
                1 -> LiveChatSection(session = session)
                2 -> RaiseTicketSection(session = session, viewModel = viewModel)
                3 -> HelplineSection()
            }
        }
    }
}

@Composable
fun FaqSection() {
    val faqs = remember {
        listOf(
            FaqItem(
                id = 1,
                question = "Order cancel kaise karu? (How to cancel order)",
                questionHindi = "ऑर्डर कैंसिल कैसे करें?",
                answer = "You can cancel your order within 60 seconds of placing it directly from the 'Track Order' screen. If the restaurant has already started preparing your food, please connect with Live Chat or call Helpline for instant cancellation request.",
                category = "Cancellation"
            ),
            FaqItem(
                id = 2,
                question = "Refund kab milega? (When will I get my refund?)",
                questionHindi = "रिफंड कब मिलेगा?",
                answer = "Refunds for cancelled or returned orders are instantly credited to your SwiftCart Pay Wallet. For UPI/Credit Card payments, bank processing takes 1 to 3 business days depending on your bank.",
                category = "Refunds"
            ),
            FaqItem(
                id = 3,
                question = "Missing ya wrong items ki complaint kaise karu?",
                questionHindi = "गुम या गलत आइटम की शिकायत कैसे करें?",
                answer = "Go to Customer Orders -> Select Order -> Click 'Report Issue' or 'Raise Support Ticket'. Select the missing/damaged item. Our automated system issues an instant refund or replacement coupon within 5 minutes.",
                category = "Order Issue"
            ),
            FaqItem(
                id = 4,
                question = "Payment fail ho gaya, lekin money deduct ho gaya?",
                questionHindi = "पेमेंट फेल हो गया लेकिन पैसे कट गए?",
                answer = "Do not worry! If payment failed but money was deducted from your bank, the bank automatically reverts the amount within 24 to 48 hours. You can also raise a ticket in 'Raise Ticket' tab with your UPI Transaction ID.",
                category = "Payment"
            ),
            FaqItem(
                id = 5,
                question = "Delivery partner delayed hai, kya kare?",
                questionHindi = "डिलीवरी पार्टनर लेट हो रहा है?",
                answer = "Check live GPS tracking on 'Track Order' screen. You can directly call or message your assigned delivery partner. If delay exceeds 15 minutes past ETA, click 'Safety SOS / Support' for order priority escalation.",
                category = "Delivery"
            ),
            FaqItem(
                id = 6,
                question = "SwiftCart Plus membership benefits & cancellation",
                questionHindi = "SwiftCart Plus मेम्बरशिप के फायदे?",
                answer = "SwiftCart Plus gives you Unlimited Free Delivery on orders above ₹99, exclusive extra discounts up to 30%, and priority customer support. You can manage or cancel auto-renewal anytime in Profile -> SwiftCart Plus.",
                category = "Plus"
            )
        )
    }

    var expandedFaqId by remember { mutableStateOf<Int?>(1) } // Default expand first FAQ
    var searchQuery by remember { mutableStateOf("") }

    val filteredFaqs = remember(searchQuery, faqs) {
        if (searchQuery.isBlank()) faqs
        else faqs.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
                    it.answer.contains(searchQuery, ignoreCase = true) ||
                    it.questionHindi.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Search FAQs
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("faq_search_input"),
            placeholder = { Text("Search FAQ (e.g. refund, cancel, payment)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Frequently Asked Questions",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tap any question below to view solution",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        filteredFaqs.forEach { faq ->
            val isExpanded = expandedFaqId == faq.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        expandedFaqId = if (isExpanded) null else faq.id
                    }
                    .testTag("faq_item_${faq.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = faq.question,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = faq.answer,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = faq.questionHindi,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LiveChatSection(session: ActiveSession) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            SupportChatMessage(
                sender = "assistant",
                text = "Namaste ${session.fullName.ifBlank { "User" }}! 👋 Welcome to SwiftCart 24/7 Support. How can I help you today?"
            )
        )
    }

    var inputMessage by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val quickQuestions = listOf(
        "Where is my refund?",
        "Cancel my last order",
        "Report missing items",
        "Speak with human agent"
    )

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = SupportChatMessage(sender = "user", text = text.trim())
        messages.add(userMsg)
        inputMessage = ""

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            isTyping = true
            delay(1200)
            isTyping = false

            val lower = text.lowercase()
            val replyText = when {
                lower.contains("refund") -> "Refund requests for cancelled or disputed orders are credited directly to your SwiftCart Pay Wallet or original payment method. You can track real-time refund status in your Order History or submit a ticket under 'Raise Ticket'."
                lower.contains("cancel") -> "To cancel an active order, visit 'Customer Orders' -> 'Track Order' before food preparation starts. For urgent issues, please submit a ticket in 'Raise Ticket'."
                lower.contains("missing") || lower.contains("wrong") || lower.contains("damage") -> "We apologize for the issue! Please submit a dispute under 'Raise Ticket' with your order details and items, and our support team will resolve it."
                lower.contains("agent") || lower.contains("human") || lower.contains("speak") -> "Our support desk operates 24/7. Please create a ticket in the 'Raise Ticket' tab or call our toll-free helpline directly."
                lower.contains("payment") || lower.contains("upi") -> "If your account was debited for a failed transaction, bank auto-reversal is initiated within 24 hours. Check your transaction details under Payment Configuration."
                else -> "Thank you for reaching out to SwiftCart Support. For account-specific assistance or issues, please raise a ticket under 'Raise Ticket' so our team can investigate with your order ID."
            }

            messages.add(SupportChatMessage(sender = "assistant", text = replyText))
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Quick Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickQuestions.forEach { question ->
                SuggestionChip(
                    onClick = { sendUserMessage(question) },
                    label = { Text(question, fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Bottom)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.time,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            if (isTyping) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SwiftCart Support is typing...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Type your query here...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { sendUserMessage(inputMessage) },
                enabled = inputMessage.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseTicketSection(
    session: ActiveSession,
    viewModel: CustomerViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    val issueTypes = listOf(
        "Order Delayed / Late Delivery",
        "Missing or Wrong Item Delivered",
        "Food / Item Quality Issue",
        "Payment / Refund Query",
        "Delivery Partner Behavior",
        "Account & Plus Subscription",
        "Other Inquiry"
    )

    var selectedIssueType by remember { mutableStateOf(issueTypes[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var orderIdInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submittedTicketId by remember { mutableStateOf<String?>(null) }

    var userTickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }

    // Fetch user's existing tickets
    LaunchedEffect(session.email, session.userId) {
        val userKey = session.email.ifBlank { session.userId.toString() }
        db.collection("support_tickets")
            .whereEqualTo("userId", userKey)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val tickets = snap.documents.mapNotNull { doc ->
                        doc.toObject(SupportTicket::class.java)?.copy(ticketId = doc.id)
                    }.sortedByDescending { it.createdAt }
                    userTickets = tickets
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Raise a Support Ticket",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Fill in issue details below. Our support team will investigate and respond within 15 minutes.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Issue Type Selector
        Text(
            text = "Issue Category *",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedIssueType,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("ticket_issue_dropdown"),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                issueTypes.forEach { issue ->
                    DropdownMenuItem(
                        text = { Text(issue) },
                        onClick = {
                            selectedIssueType = issue
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Order ID Field
        Text(
            text = "Order ID (Optional)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = orderIdInput,
            onValueChange = { orderIdInput = it },
            placeholder = { Text("e.g. #ORD-94820") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ticket_order_id_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Issue Description Field
        Text(
            text = "Describe your issue *",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = descriptionInput,
            onValueChange = { descriptionInput = it },
            placeholder = { Text("Please describe what went wrong (e.g., missing items, bad food quality, payment pending)...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("ticket_description_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = {
                if (descriptionInput.isBlank()) {
                    Toast.makeText(context, "Please enter issue description", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSubmitting = true
                val generatedTicketId = "TKT-${(10000..99999).random()}"
                val userKey = session.email.ifBlank { session.userId.toString() }

                val newTicket = hashMapOf(
                    "ticketId" to generatedTicketId,
                    "userId" to userKey,
                    "userEmail" to session.email,
                    "issueType" to selectedIssueType,
                    "orderId" to orderIdInput,
                    "description" to descriptionInput.trim(),
                    "status" to "OPEN",
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("support_tickets")
                    .document(generatedTicketId)
                    .set(newTicket)
                    .addOnSuccessListener {
                        isSubmitting = false
                        submittedTicketId = generatedTicketId
                        descriptionInput = ""
                        orderIdInput = ""
                    }
                    .addOnFailureListener { e ->
                        isSubmitting = false
                        Toast.makeText(context, "Failed to submit ticket: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            enabled = !isSubmitting && descriptionInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_ticket_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting Ticket...")
            } else {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Existing Tickets List
        if (userTickets.isNotEmpty()) {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Support Tickets (${userTickets.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            userTickets.forEach { ticket ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${ticket.ticketId}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = if (ticket.status == "OPEN") Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = ticket.status,
                                    color = if (ticket.status == "OPEN") Color(0xFFE65100) else Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.issueType,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        if (ticket.orderId.isNotBlank()) {
                            Text(
                                text = "Order: ${ticket.orderId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Ticket Submission Confirmation Dialog
    submittedTicketId?.let { ticketId ->
        AlertDialog(
            onDismissRequest = { submittedTicketId = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Ticket Raised Successfully!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Ticket ID: #$ticketId\n\nOur Customer Support team has received your ticket and is reviewing it. An executive will contact you via app/call within 15 minutes.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { submittedTicketId = null },
                    modifier = Modifier.testTag("ticket_success_ok_btn")
                ) {
                    Text("OK, Got It")
                }
            }
        )
    }
}

@Composable
fun HelplineSection() {
    val context = LocalContext.current
    val helplineNumber = "18001237943" // 1800-123-7943 Toll Free

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SwiftCart Toll-Free Support Helpline",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Available 24 hours a day, 7 days a week",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "1800-123-SWIFT (1800-123-7943)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Free of charge from any Indian network",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$helplineNumber"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dialer opened: 1800-123-7943", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("call_helpline_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Helpline Now", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Priority Helpline Card for Emergency
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Safety & Emergency Escalation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "For safety SOS or urgent delivery incidents on active orders, call our Safety Response Team instantly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
