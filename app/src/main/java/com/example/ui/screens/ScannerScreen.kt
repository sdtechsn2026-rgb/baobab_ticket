package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResultType
import com.example.data.model.TicketScanResult
import com.example.data.repository.ScanLogItem
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

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

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
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1))
                }
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Gate selector & Online toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gate chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        gates.forEach { gate ->
                            val isSelected = (gate == selectedGate)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedGate = gate },
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
                            Text(if (isOffline) "HORS-LIGNE" else "EN LIGNE", color = if (isOffline) UsedAmber else ValidGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Camera Viewfinder Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF090D0A))
                        .border(1.dp, Color(0xFF283A31), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(2.dp, ChampagneGold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .offset(y = (laserOffset * 157).dp)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, EmeraldBright, Color.Transparent)))
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = ChampagneGold.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = selectedGate.uppercase(),
                            color = ChampagneGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Result Display (Clean, High Visibility)
            item {
                AnimatedVisibility(visible = lastResult != null) {
                    if (lastResult != null) {
                        val res = lastResult!!
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
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (res.type == ScanResultType.VALID) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = tintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(res.message, color = tintColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    if (res.ticket != null) {
                                        Text("${res.ticket.attendeeName} • ${res.ticket.ticketTypeName} • ${res.ticket.ticketNumber}", color = IvoryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Scan Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                delay(200)
                                val res = onValidateScan("BAOBAB-SECURE-TOKEN-8F92K7X4-VVIP-2026", selectedGate)
                                lastResult = res
                                triggerHaptic(res.type == ScanResultType.VALID)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = ChampagneGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("scan_test_vip")
                    ) {
                        Text("Scan VIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                delay(200)
                                val res = onValidateScan("BAOBAB-SECURE-TOKEN-4M71N9Q2-STD-2026", selectedGate)
                                lastResult = res
                                triggerHaptic(res.type == ScanResultType.VALID)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = ChampagneGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("scan_test_std")
                    ) {
                        Text("Standard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                delay(200)
                                val res = onValidateScan("BAOBAB-SECURE-TOKEN-9P33T5L1-VVIP-2026", selectedGate)
                                lastResult = res
                                triggerHaptic(false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = UsedAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("scan_test_used")
                    ) {
                        Text("Déjà Utilisé", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Manual Input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("Code manuel (ex: BT-2026-...)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("manual_token_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                coroutineScope.launch {
                                    val res = onValidateScan(manualInput.trim(), selectedGate)
                                    lastResult = res
                                    triggerHaptic(res.type == ScanResultType.VALID)
                                    manualInput = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Valider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Compact Recent Logs
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
