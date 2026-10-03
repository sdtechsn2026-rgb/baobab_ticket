package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Event
import com.example.data.model.TicketType
import com.example.data.payment.PaymentProvider
import com.example.data.payment.PaymentService
import com.example.ui.components.SimplePaymentSelector
import com.example.ui.components.formatXof
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    events: List<Event>,
    ticketTypes: List<TicketType>,
    onPurchaseRequested: suspend (Event, TicketType, Int, String, String, String, PaymentProvider, Boolean, String) -> Pair<Boolean, String>,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var activePurchasingEvent by remember { mutableStateOf<Event?>(null) }
    var activePurchasingTicketType by remember { mutableStateOf<TicketType?>(null) }
    var showPurchaseModal by remember { mutableStateOf(false) }

    val categories = listOf("Tous", "Gala & Soirée", "Conférence", "Festival")

    val filteredEvents = events.filter { event ->
        val matchesCategory = (selectedCategory == "Tous" || event.category.contains(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.city.contains(searchQuery, ignoreCase = true) ||
                event.venueName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Sleek Minimal Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("search_events_input"),
                    placeholder = { Text("Rechercher un événement...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = Color(0xFF283A31),
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface
                    ),
                    singleLine = true
                )
            }

            // Clean Category Pills
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = (cat == selectedCategory)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("cat_filter_$cat"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) ChampagneGold else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ChampagneGold else Color(0xFF283A31)
                            )
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) GraphiteBlack else IvoryWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Events List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Événements Disponibles (${filteredEvents.size})",
                        color = IvoryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sénégal • XOF",
                        color = Color(0xFFA5B8AD),
                        fontSize = 11.sp
                    )
                }
            }

            // Clean Compact Event Cards
            items(filteredEvents) { event ->
                val eventTypes = ticketTypes.filter { it.eventId == event.id }
                val lowestPrice = eventTypes.minOfOrNull { it.priceXof } ?: 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("event_card_${event.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DeepEmerald,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = event.category.uppercase(),
                                        color = ChampagneGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = event.title,
                                    color = IvoryWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${event.venueName}, ${event.city} • ${event.startDate}",
                                    color = Color(0xFFA5B8AD),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Price Tag
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (lowestPrice == 0.0) "GRATUIT" else "Dès ${formatXof(lowestPrice)}",
                                    color = ChampagneGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${event.soldCount} inscrits",
                                    color = Color(0xFFA5B8AD),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                eventTypes.take(3).forEach { t ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkSurfaceCard,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF384E42))
                                    ) {
                                        Text(
                                            text = t.name,
                                            color = Color(0xFFA5B8AD),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    activePurchasingEvent = event
                                    activePurchasingTicketType = eventTypes.firstOrNull()
                                    showPurchaseModal = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChampagneGold,
                                    contentColor = GraphiteBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("book_event_${event.id}")
                            ) {
                                Text("Réserver", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Paiement & Réservation Simple (Wave & Orange Money)
    if (showPurchaseModal && activePurchasingEvent != null) {
        val event = activePurchasingEvent!!
        val eventTypes = ticketTypes.filter { it.eventId == event.id }

        StreamlinedPurchaseModal(
            event = event,
            ticketTypes = eventTypes,
            selectedType = activePurchasingTicketType ?: eventTypes.first(),
            onSelectType = { activePurchasingTicketType = it },
            onDismiss = { showPurchaseModal = false },
            onConfirmPurchase = { qty, name, phone, email, provider, isManualQr, ref, onDone ->
                coroutineScope.launch {
                    val result = onPurchaseRequested(
                        event,
                        activePurchasingTicketType ?: eventTypes.first(),
                        qty,
                        name,
                        email,
                        phone,
                        provider,
                        isManualQr,
                        ref
                    )
                    onDone(result.first, result.second)
                }
            },
            onNavigateToWallet = {
                showPurchaseModal = false
                onNavigateToWallet()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamlinedPurchaseModal(
    event: Event,
    ticketTypes: List<TicketType>,
    selectedType: TicketType,
    onSelectType: (TicketType) -> Unit,
    onDismiss: () -> Unit,
    onConfirmPurchase: (Int, String, String, String, PaymentProvider, Boolean, String, (Boolean, String) -> Unit) -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val paymentService = remember { PaymentService() }

    var quantity by remember { mutableIntStateOf(1) }
    var customerName by remember { mutableStateOf("Sega Diallo") }
    var customerPhone by remember { mutableStateOf("77 845 12 34") }
    var customerEmail by remember { mutableStateOf("segacod05@gmail.com") }

    // Mode de paiement sélectionné (Wave ou Orange Money par défaut)
    var selectedProvider by remember {
        mutableStateOf(if (selectedType.priceXof == 0.0) PaymentProvider.FREE else PaymentProvider.WAVE)
    }
    var isManualQr by remember { mutableStateOf(false) }
    var transactionRefInput by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }
    var purchaseErrorMessage by remember { mutableStateOf<String?>(null) }

    val rawTotal = selectedType.priceXof * quantity

    val qrContent = remember(selectedProvider, rawTotal, event) {
        val phone = if (selectedProvider == PaymentProvider.WAVE) event.waveMerchantPhone else event.omMerchantPhone
        paymentService.generatePaymentQrContent(selectedProvider, rawTotal, phone)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (purchaseSuccessMessage != null) {
                // Confirmation screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ValidGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ValidGreen, modifier = Modifier.size(32.dp))
                    }

                    Text(
                        text = "Paiement Validé !",
                        color = IvoryWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = purchaseSuccessMessage!!,
                        color = Color(0xFFA5B8AD),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = onNavigateToWallet,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                        modifier = Modifier.fillMaxWidth().testTag("view_my_tickets_button")
                    ) {
                        Text("Voir mon Billet avec QR Sécurisé", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(event.title, color = IvoryWhite, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${event.venueName} • ${event.startDate}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = IvoryWhite)
                    }
                }

                // Tier selection pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ticketTypes.forEach { type ->
                        val isSelected = (type.id == selectedType.id)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectType(type)
                                    if (type.priceXof == 0.0) {
                                        selectedProvider = PaymentProvider.FREE
                                    } else if (selectedProvider == PaymentProvider.FREE) {
                                        selectedProvider = PaymentProvider.WAVE
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DeepEmerald else DarkSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF384E42))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(type.name, color = if (isSelected) ChampagneGold else IvoryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(if (type.priceXof == 0.0) "Gratuit" else formatXof(type.priceXof), color = IvoryWhite, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Attendee Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom complet du participant", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Simple Payment Mode Selector (Wave / Orange Money)
                SimplePaymentSelector(
                    selectedProvider = selectedProvider,
                    onSelectProvider = { selectedProvider = it },
                    isManualQr = isManualQr,
                    onToggleManualQr = { isManualQr = it },
                    amountXof = rawTotal,
                    customerPhone = customerPhone,
                    onPhoneChange = { customerPhone = it },
                    transactionRef = transactionRefInput,
                    onTransactionRefChange = { transactionRefInput = it },
                    qrContent = qrContent
                )

                if (purchaseErrorMessage != null) {
                    Text(
                        text = purchaseErrorMessage!!,
                        color = ExpiredRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Footer Total & Confirm Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MONTANT TOTAL", color = Color(0xFFA5B8AD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(formatXof(rawTotal), color = ChampagneGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    val providerBrandColor = Color(android.graphics.Color.parseColor(selectedProvider.brandColorHex))

                    Button(
                        onClick = {
                            isSubmitting = true
                            purchaseErrorMessage = null
                            onConfirmPurchase(
                                quantity,
                                customerName,
                                customerPhone,
                                customerEmail,
                                selectedProvider,
                                isManualQr,
                                transactionRefInput
                            ) { success, msg ->
                                isSubmitting = false
                                if (success) {
                                    purchaseSuccessMessage = msg
                                } else {
                                    purchaseErrorMessage = msg
                                }
                            }
                        },
                        enabled = !isSubmitting && customerName.isNotBlank() && (selectedProvider == PaymentProvider.FREE || customerPhone.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProvider == PaymentProvider.FREE) ChampagneGold else providerBrandColor,
                            contentColor = if (selectedProvider == PaymentProvider.ORANGE_MONEY) Color.White else GraphiteBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_purchase_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            val buttonText = when (selectedProvider) {
                                PaymentProvider.WAVE -> "Payer avec Wave"
                                PaymentProvider.ORANGE_MONEY -> "Payer avec Orange Money"
                                PaymentProvider.FREE -> "Confirmer l'invitation"
                            }
                            Text(buttonText, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
