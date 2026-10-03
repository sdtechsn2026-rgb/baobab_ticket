package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Event
import com.example.data.model.EventThemeTemplate
import com.example.ui.components.QrCodeImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStudioScreen(
    event: Event,
    onSaveTheme: (EventThemeTemplate) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(event.themeTemplate) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EVENT STUDIO", color = ChampagneGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                        Text(event.title, color = IvoryWhite, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = IvoryWhite)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveTheme(selectedTheme)
                            Toast.makeText(context, "Thème enregistré !", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepEmerald)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Prévisualisation du Ticket en Temps Réel :",
                    color = Color(0xFFA5B8AD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real-time Preview Card adapting to theme
            item {
                val primaryColor = Color(android.graphics.Color.parseColor(selectedTheme.primaryHex))
                val accentColor = Color(android.graphics.Color.parseColor(selectedTheme.accentHex))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_studio_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = accentColor.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                                ) {
                                    Text(
                                        text = "VIP PREVIEW • ${selectedTheme.displayName.uppercase()}",
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(event.title, color = IvoryWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${event.venueName}, ${event.city}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QrCodeImage(
                                content = "BAOBAB-STUDIO-PREVIEW-${selectedTheme.name}",
                                size = 130.dp,
                                qrColor = GraphiteBlack,
                                backgroundColor = IvoryWhite
                            )
                            Text(
                                text = "Numéro : BT-2026-STUDIO-DEMO",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sélectionnez un Thème Template :",
                    color = IvoryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(EventThemeTemplate.values().size) { idx ->
                val template = EventThemeTemplate.values()[idx]
                val isSelected = (template == selectedTheme)
                val primaryColor = Color(android.graphics.Color.parseColor(template.primaryHex))
                val accentColor = Color(android.graphics.Color.parseColor(template.accentHex))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTheme = template }
                        .testTag("theme_option_${template.name}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) DeepEmerald else DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF283A31))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(primaryColor))
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor))
                            }
                            Column {
                                Text(template.displayName, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Code : ${template.name}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ChampagneGold)
                        }
                    }
                }
            }
        }
    }
}
