package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.Event
import com.example.data.model.ManualPaymentProof
import com.example.data.model.Order
import com.example.data.model.Ticket
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.TicketStatusBadge
import com.example.ui.components.formatXof
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OrganizerDashboardScreen(
    events: List<Event>,
    orders: List<Order>,
    tickets: List<Ticket>,
    pendingProofs: List<ManualPaymentProof>,
    onApproveProof: suspend (String) -> Unit,
    onRejectProof: (String, String) -> Unit,
    onOpenWizard: () -> Unit,
    onOpenStudio: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var participantSearch by remember { mutableStateOf("") }

    val totalRevenue = orders.filter { it.paymentStatus.name == "CONFIRMED" }.sumOf { it.totalPriceXof }
    val totalSold = tickets.size
    val totalCheckedIn = tickets.count { it.status.name == "USED" }
    val pendingCount = pendingProofs.count { it.status == "PENDING" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenWizard,
                containerColor = ChampagneGold,
                contentColor = GraphiteBlack,
                modifier = Modifier.testTag("create_event_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvel Événement")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // SaaS Platform Metrics Grid (Compact & Clear)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Revenue
                    SaaSKpiCard(
                        label = "REVENUS",
                        value = formatXof(totalRevenue),
                        subLabel = "Confirmé",
                        accentColor = ChampagneGold,
                        modifier = Modifier.weight(1f)
                    )
                    // Tickets
                    SaaSKpiCard(
                        label = "BILLETS",
                        value = "$totalSold",
                        subLabel = "Total vendus",
                        accentColor = IvoryWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Check-ins
                    SaaSKpiCard(
                        label = "ENTRÉES",
                        value = "$totalCheckedIn / $totalSold",
                        subLabel = "Scannées",
                        accentColor = ValidGreen,
                        modifier = Modifier.weight(1f)
                    )
                    // Pending
                    SaaSKpiCard(
                        label = "EN ATTENTE",
                        value = "$pendingCount",
                        subLabel = "Paiements QR",
                        accentColor = if (pendingCount > 0) UsedAmber else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clean Platform Tab Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("Validations ($pendingCount)", "Participants ($totalSold)", "Événements (${events.size})")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = (selectedTab == index)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index }
                                .testTag("dashboard_tab_$index"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ChampagneGold else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF283A31))
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) GraphiteBlack else IvoryWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Tab 0: Pending Payments Queue (Concise)
            if (selectedTab == 0) {
                val activePending = pendingProofs.filter { it.status == "PENDING" }
                if (activePending.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ValidGreen, modifier = Modifier.size(28.dp))
                                Text("Aucune validation en attente", color = IvoryWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(activePending) { proof ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UsedAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(proof.customerName, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        PaymentMethodBadge(method = proof.method)
                                    }
                                    Text("Réf : ${proof.transactionRef} • ${proof.customerPhone}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                                    Text(formatXof(proof.amountXof), color = ChampagneGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { onRejectProof(proof.id, "Preuve non reçue") },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Refuser", tint = ExpiredRed, modifier = Modifier.size(18.dp))
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                onApproveProof(proof.id)
                                                Toast.makeText(context, "Billet émis !", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ValidGreen, contentColor = GraphiteBlack),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("approve_payment_${proof.id}")
                                    ) {
                                        Text("Valider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 1: Participants List (Searchable, Compact)
            if (selectedTab == 1) {
                item {
                    OutlinedTextField(
                        value = participantSearch,
                        onValueChange = { participantSearch = it },
                        placeholder = { Text("Filtrer par nom ou numéro...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                val filtered = tickets.filter {
                    participantSearch.isBlank() ||
                            it.attendeeName.contains(participantSearch, ignoreCase = true) ||
                            it.ticketNumber.contains(participantSearch, ignoreCase = true)
                }

                items(filtered) { ticket ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ticket.attendeeName, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${ticket.ticketNumber} • ${ticket.ticketTypeName} • ${ticket.gate}", color = Color(0xFFA5B8AD), fontSize = 10.sp)
                            }
                            TicketStatusBadge(status = ticket.status)
                        }
                    }
                }
            }

            // Tab 2: Events Management
            if (selectedTab == 2) {
                items(events) { ev ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${ev.venueName}, ${ev.city} • ${ev.startDate}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                                Text("${ev.soldCount}/${ev.totalCapacity} vendus", color = ChampagneGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onOpenStudio(ev) },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = ChampagneGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_studio_${ev.id}")
                            ) {
                                Text("Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaaSKpiCard(
    label: String,
    value: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFFA5B8AD), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(subLabel, color = Color(0xFFA5B8AD), fontSize = 9.sp)
        }
    }
}
