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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.EventConceptAiResponse
import com.example.data.model.Event
import com.example.data.model.EventThemeTemplate
import com.example.data.model.TicketType
import com.example.ui.components.formatXof
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventWizardScreen(
    initialConcept: EventConceptAiResponse? = null,
    onEventCreated: (Event) -> Unit,
    onCancel: () -> Unit,
    createEventInRepo: (String, String, String, String, String, String, String, String, String, EventThemeTemplate, List<TicketType>) -> Event,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Info
    var title by remember { mutableStateOf(initialConcept?.title ?: "Gala d'Excellence Dakar") }
    var subtitle by remember { mutableStateOf(initialConcept?.subtitle ?: "Prestige, Networking & Culture") }
    var category by remember { mutableStateOf(initialConcept?.category ?: "Gala & Soirée") }
    var description by remember {
        mutableStateOf(
            initialConcept?.description
                ?: "Une soirée mémorable rassemblant personnalités, créateurs et décideurs pour célébrer l'innovation et l'excellence africaine."
        )
    }

    // Step 2: Date & Lieu
    var startDate by remember { mutableStateOf("20 Déc 2026") }
    var startTime by remember { mutableStateOf("20:00") }
    var venueName by remember { mutableStateOf(initialConcept?.suggestedVenue ?: "Radisson Blu Dakar Sea Plaza") }
    var venueAddress by remember { mutableStateOf("Route de la Corniche Ouest") }
    var city by remember { mutableStateOf("Dakar") }

    // Step 3: Billetterie (Tiers)
    var stdPrice by remember { mutableDoubleStateOf(initialConcept?.standardPrice ?: 25000.0) }
    var stdQty by remember { mutableIntStateOf(400) }
    var vipPrice by remember { mutableDoubleStateOf(initialConcept?.vipPrice ?: 60000.0) }
    var vipQty by remember { mutableIntStateOf(200) }
    var vvipPrice by remember { mutableDoubleStateOf(initialConcept?.vvipPrice ?: 150000.0) }
    var vvipQty by remember { mutableIntStateOf(50) }

    // Step 4: Theme Template
    var selectedTheme by remember { mutableStateOf(EventThemeTemplate.LUXURY) }

    // Step 5: Payments
    var enableWaveApi by remember { mutableStateOf(true) }
    var enableOmApi by remember { mutableStateOf(true) }
    var enableWaveQrManual by remember { mutableStateOf(true) }
    var enableOmQrManual by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Création d'Événement", color = IvoryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Étape $currentStep sur 5", color = ChampagneGold, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = IvoryWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepEmerald)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurfaceElevated,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IvoryWhite)
                        ) {
                            Text("Précédent")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < 5) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                            modifier = Modifier.testTag("wizard_next_button")
                        ) {
                            Text("Suivant", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val tiers = listOf(
                                    TicketType(
                                        id = "tt-new-std",
                                        eventId = "",
                                        name = "STANDARD",
                                        description = "Accès salle principale",
                                        priceXof = stdPrice,
                                        quantityTotal = stdQty,
                                        quantitySold = 0,
                                        perks = listOf("Accès soirée", "Cocktail de bienvenue"),
                                        accessGates = listOf("Porte Principale")
                                    ),
                                    TicketType(
                                        id = "tt-new-vip",
                                        eventId = "",
                                        name = "VIP",
                                        description = "Dîner gastronomique et table dédiée",
                                        priceXof = vipPrice,
                                        quantityTotal = vipQty,
                                        quantitySold = 0,
                                        perks = listOf("Dîner gastronomique", "Coupe de champagne", "Fast Track Entrée VIP"),
                                        accessGates = listOf("Porte Principale", "Accès VIP")
                                    ),
                                    TicketType(
                                        id = "tt-new-vvip",
                                        eventId = "",
                                        name = "VVIP PRESTIGE",
                                        description = "Table prestige et salon d'honneur",
                                        priceXof = vvipPrice,
                                        quantityTotal = vvipQty,
                                        quantitySold = 0,
                                        perks = listOf("Table privée majordome", "Lounge privé VIP", "Bar prestige"),
                                        accessGates = listOf("Porte Principale", "Accès VIP", "Accès VVIP")
                                    )
                                )

                                val created = createEventInRepo(
                                    title,
                                    subtitle,
                                    description,
                                    category,
                                    venueName,
                                    venueAddress,
                                    city,
                                    startDate,
                                    startDate,
                                    selectedTheme,
                                    tiers
                                )
                                Toast.makeText(context, "Événement publié avec succès !", Toast.LENGTH_LONG).show()
                                onEventCreated(created)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ValidGreen, contentColor = GraphiteBlack),
                            modifier = Modifier.testTag("wizard_publish_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publier l'Événement", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
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
            when (currentStep) {
                1 -> {
                    item {
                        Text("Étape 1 : Informations Générales", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Titre de l'événement") },
                            modifier = Modifier.fillMaxWidth().testTag("wizard_title_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("Slogan / Sous-titre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Catégorie (Gala, Festival, Conférence...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description détaillée") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    }
                }
                2 -> {
                    item {
                        Text("Étape 2 : Date & Lieu", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Heure") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = venueName,
                            onValueChange = { venueName = it },
                            label = { Text("Nom du lieu (Hôtel, Salle, Plage...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = venueAddress,
                            onValueChange = { venueAddress = it },
                            label = { Text("Adresse") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ville (Dakar, Thiès, Saly...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                3 -> {
                    item {
                        Text("Étape 3 : Billetterie & Prix (FCFA / XOF)", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Configurez les 3 catégories standards :", color = Color(0xFFA5B8AD), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard
                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Billet STANDARD", color = ChampagneGold, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = "${stdPrice.toInt()}",
                                        onValueChange = { stdPrice = it.toDoubleOrNull() ?: 0.0 },
                                        label = { Text("Prix XOF") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = "$stdQty",
                                        onValueChange = { stdQty = it.toIntOrNull() ?: 100 },
                                        label = { Text("Quantité") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // VIP
                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Billet VIP", color = ChampagneGold, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = "${vipPrice.toInt()}",
                                        onValueChange = { vipPrice = it.toDoubleOrNull() ?: 0.0 },
                                        label = { Text("Prix XOF") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = "$vipQty",
                                        onValueChange = { vipQty = it.toIntOrNull() ?: 50 },
                                        label = { Text("Quantité") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // VVIP
                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Billet VVIP PRESTIGE", color = ChampagneGold, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = "${vvipPrice.toInt()}",
                                        onValueChange = { vvipPrice = it.toDoubleOrNull() ?: 0.0 },
                                        label = { Text("Prix XOF") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = "$vvipQty",
                                        onValueChange = { vvipQty = it.toIntOrNull() ?: 20 },
                                        label = { Text("Quantité") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
                4 -> {
                    item {
                        Text("Étape 4 : Thème Event Studio", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Sélectionnez l'identité visuelle de votre événement :", color = Color(0xFFA5B8AD), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        EventThemeTemplate.values().forEach { template ->
                            val isSelected = (template == selectedTheme)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedTheme = template },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) DeepEmerald else DarkSurfaceElevated
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ChampagneGold else Color(0xFF283A31)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(template.displayName, color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Couleurs : ${template.primaryHex} / ${template.accentHex}", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ChampagneGold)
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    item {
                        Text("Étape 5 : Modes de Paiement", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Cochez les moyens que vous souhaitez proposer aux participants :", color = Color(0xFFA5B8AD), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = enableWaveApi, onCheckedChange = { enableWaveApi = it })
                                    Text("Wave API Direct (Validation instantanée)", color = IvoryWhite, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = enableWaveQrManual, onCheckedChange = { enableWaveQrManual = it })
                                    Text("Wave QR Manuel (Mode B - Sans contrat marchand)", color = IvoryWhite, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = enableOmApi, onCheckedChange = { enableOmApi = it })
                                    Text("Orange Money API Direct", color = IvoryWhite, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = enableOmQrManual, onCheckedChange = { enableOmQrManual = it })
                                    Text("Orange Money QR Manuel (Mode B)", color = IvoryWhite, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Final Recap Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepEmerald.copy(alpha = 0.4f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("RÉCAPITULATIF :", color = ChampagneGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Événement : $title", color = IvoryWhite, fontWeight = FontWeight.Bold)
                                Text("Lieu : $venueName, $city", color = Color(0xFFA5B8AD), fontSize = 12.sp)
                                Text("Capacité totale : ${stdQty + vipQty + vvipQty} places", color = IvoryWhite, fontSize = 12.sp)
                                Text("Paliers : Standard (${formatXof(stdPrice)}) • VIP (${formatXof(vipPrice)}) • VVIP (${formatXof(vvipPrice)})", color = ChampagneGold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
