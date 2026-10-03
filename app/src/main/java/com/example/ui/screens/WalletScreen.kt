package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ticket
import com.example.data.model.TicketStatus
import com.example.ui.components.QrCodeImage
import com.example.ui.components.TicketStatusBadge
import com.example.ui.components.formatXof
import com.example.ui.theme.*

@Composable
fun WalletScreen(
    tickets: List<Ticket>,
    onTransferTicket: (String, String, String, String) -> Pair<Boolean, String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTicketForTransfer by remember { mutableStateOf<Ticket?>(null) }
    var transferName by remember { mutableStateOf("") }
    var transferPhone by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mes Billets (${tickets.size})",
                    color = IvoryWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "QR Certifié",
                    color = ChampagneGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFF384E42), modifier = Modifier.size(48.dp))
                        Text("Aucun billet actif", color = IvoryWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tickets) { ticket ->
                        CleanTicketCard(
                            ticket = ticket,
                            onTransferClick = {
                                selectedTicketForTransfer = ticket
                                transferName = ""
                                transferPhone = ""
                            },
                            onCopyToken = {
                                clipboardManager.setText(AnnotatedString(ticket.ticketNumber))
                                Toast.makeText(context, "N° copié : ${ticket.ticketNumber}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Clean Transfer Dialog
    if (selectedTicketForTransfer != null) {
        val ticket = selectedTicketForTransfer!!
        AlertDialog(
            onDismissRequest = { selectedTicketForTransfer = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Transférer le billet", color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nouveau titulaire pour ${ticket.ticketNumber} :", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                    OutlinedTextField(
                        value = transferName,
                        onValueChange = { transferName = it },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = transferPhone,
                        onValueChange = { transferPhone = it },
                        label = { Text("Téléphone (+221...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (transferName.isNotBlank() && transferPhone.isNotBlank()) {
                            val res = onTransferTicket(ticket.id, transferName, transferPhone, "")
                            selectedTicketForTransfer = null
                            Toast.makeText(context, res.second, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Transférer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicketForTransfer = null }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CleanTicketCard(
    ticket: Ticket,
    onTransferClick: () -> Unit,
    onCopyToken: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ticket_card_${ticket.ticketNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepEmerald)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(ticket.eventTitle, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${ticket.ticketTypeName} • ${ticket.gate}", color = ChampagneGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                TicketStatusBadge(status = ticket.status)
            }

            // Body with QR & Essential details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Titulaire : ${ticket.attendeeName}", color = IvoryWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(ticket.seatZone, color = Color(0xFFA5B8AD), fontSize = 11.sp)
                }

                // QR Code
                QrCodeImage(
                    content = ticket.qrSecurityToken,
                    size = 150.dp,
                    qrColor = if (ticket.status == TicketStatus.ACTIVE) GraphiteBlack else Color.Gray,
                    backgroundColor = IvoryWhite
                )

                // Ticket number & copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(ticket.ticketNumber, color = ChampagneGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    IconButton(onClick = onCopyToken, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    }
                }

                if (ticket.status == TicketStatus.ACTIVE) {
                    OutlinedButton(
                        onClick = onTransferClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                    ) {
                        Text("Transférer le billet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
