package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResultType
import com.example.data.model.TicketScanResult
import com.example.data.repository.ScanLogItem
import com.example.ui.camera.CameraScannerView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannerScreen(
    isOffline: Boolean,
    unsyncedCount: Int,
    scanLogs: List<ScanLogItem>,
    onToggleOffline: (Boolean) -> Unit,
    onValidateScan: suspend (String, String) -> TicketScanResult,
    onSyncNow: suspend () -> Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedGate by remember { mutableStateOf("Porte Principale") }
    val gates = listOf("Porte Principale", "Accès VIP", "Accès VVIP")

    var manualInput by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<TicketScanResult?>(null) }
    var isScanningActive by remember { mutableStateOf(true) }

    fun triggerHaptic(isValid: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isValid) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
                }
            }
        } catch (_: Exception) {}
    }

    fun handleScanPayload(payload: String) {
        coroutineScope.launch {
            val res = onValidateScan(payload, selectedGate)
            lastResult = res
            triggerHaptic(res.type == ScanResultType.VALID)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(GraphiteBlack)) {
        val isDesktop = maxWidth >= 720.dp

        if (isDesktop) {
            // --- COMPUTER / DESKTOP LAYOUT (2 PANES) ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Camera Viewfinder with controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Bar
                    ScannerHeaderBar(
                        selectedGate = selectedGate,
                        gates = gates,
                        onSelectGate = { selectedGate = it },
                        isOffline = isOffline,
                        onToggleOffline = onToggleOffline
                    )

                    // Real CameraX Scanner View
                    CameraScannerView(
                        onQrDetected = { qrPayload ->
                            if (isScanningActive) {
                                handleScanPayload(qrPayload)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Quick simulator buttons
                    QuickTestRow(
                        onScanTest = { token -> handleScanPayload(token) }
                    )
                }

                // Right Pane: Validation Monitor & Logs Console
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Manual Token Input Bar
                    ManualInputBar(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        onSubmit = {
                            if (manualInput.isNotBlank()) {
                                handleScanPayload(manualInput.trim())
                                manualInput = ""
                            }
                        }
                    )

                    // Big Result Card
                    ScanResultDisplay(lastResult = lastResult)

                    // Live Scan Audit Logs
                    Text(
                        text = "Journal des Scans en Direct (${scanLogs.size})",
                        color = IvoryWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (scanLogs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("En attente des premiers scans aux portes...", color = Color(0xFFA5B8AD), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                items(scanLogs) { log ->
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                    val isSuccess = log.status.contains("VALIDE")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceCard)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${log.attendeeName} • ${log.ticketTier}",
                                                color = IvoryWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                            Text(
                                                text = "${log.ticketNumber} • ${log.gate} • $timeStr",
                                                color = Color(0xFFA5B8AD),
                                                fontSize = 9.5.sp
                                            )
                                        }
                                        Text(
                                            text = log.status,
                                            color = if (isSuccess) ValidGreen else ExpiredRed,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- MOBILE LAYOUT (VERTICAL STACK) ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ScannerHeaderBar(
                        selectedGate = selectedGate,
                        gates = gates,
                        onSelectGate = { selectedGate = it },
                        isOffline = isOffline,
                        onToggleOffline = onToggleOffline
                    )
                }

                // CameraX Preview
                item {
                    CameraScannerView(
                        onQrDetected = { qrPayload ->
                            if (isScanningActive) {
                                handleScanPayload(qrPayload)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )
                }

                // Validation Result
                item {
                    ScanResultDisplay(lastResult = lastResult)
                }

                // Quick Simulators
                item {
                    QuickTestRow(
                        onScanTest = { token -> handleScanPayload(token) }
                    )
                }

                // Manual Input
                item {
                    ManualInputBar(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        onSubmit = {
                            if (manualInput.isNotBlank()) {
                                handleScanPayload(manualInput.trim())
                                manualInput = ""
                            }
                        }
                    )
                }

                // Recent Logs
                items(scanLogs.take(5)) { log ->
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    val isSuccess = log.status.contains("VALIDE")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${log.attendeeName} • ${log.ticketTier} ($timeStr)", color = IvoryWhite, fontSize = 11.sp)
                            Text(log.status, color = if (isSuccess) ValidGreen else ExpiredRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerHeaderBar(
    selectedGate: String,
    gates: List<String>,
    onSelectGate: (String) -> Unit,
    isOffline: Boolean,
    onToggleOffline: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gate selection
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            gates.forEach { gate ->
                val isSelected = (gate == selectedGate)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectGate(gate) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DeepEmerald else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF283A31))
                ) {
                    Text(
                        text = gate,
                        color = if (isSelected) ChampagneGold else Color(0xFFA5B8AD),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Online/Offline Pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { onToggleOffline(!isOffline) }
                .testTag("toggle_offline_button"),
            shape = RoundedCornerShape(14.dp),
            color = if (isOffline) UsedAmber.copy(alpha = 0.2f) else ValidGreen.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isOffline) UsedAmber else ValidGreen)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isOffline) UsedAmber else ValidGreen))
                Text(
                    text = if (isOffline) "HORS-LIGNE" else "EN LIGNE",
                    color = if (isOffline) UsedAmber else ValidGreen,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScanResultDisplay(lastResult: TicketScanResult?) {
    AnimatedVisibility(visible = lastResult != null) {
        if (lastResult != null) {
            val res = lastResult
            val (bgColor, tintColor) = when (res.type) {
                ScanResultType.VALID -> Pair(ValidGreen.copy(alpha = 0.15f), ValidGreen)
                ScanResultType.USED -> Pair(UsedAmber.copy(alpha = 0.15f), UsedAmber)
                ScanResultType.INVALID, ScanResultType.EXPIRED, ScanResultType.WRONG_GATE -> Pair(ExpiredRed.copy(alpha = 0.15f), ExpiredRed)
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("scan_result_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, tintColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (res.type == ScanResultType.VALID) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(res.message, color = tintColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        if (res.ticket != null) {
                            Text(
                                text = "${res.ticket.attendeeName} • ${res.ticket.ticketTypeName} • ${res.ticket.ticketNumber}",
                                color = IvoryWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTestRow(onScanTest: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = { onScanTest("BAOBAB-SECURE-TOKEN-8F92K7X4-VVIP-2026") },
            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = ChampagneGold),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("scan_test_vip")
        ) {
            Text("Scan VIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onScanTest("BAOBAB-SECURE-TOKEN-4M71N9Q2-STD-2026") },
            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = ChampagneGold),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("scan_test_std")
        ) {
            Text("Standard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onScanTest("BAOBAB-SECURE-TOKEN-9P33T5L1-VVIP-2026") },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = UsedAmber),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("scan_test_used")
        ) {
            Text("Déjà Utilisé", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ManualInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Code manuel (ex: BT-2026-...)", fontSize = 11.sp) },
            modifier = Modifier.weight(1f).testTag("manual_token_input"),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Valider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
