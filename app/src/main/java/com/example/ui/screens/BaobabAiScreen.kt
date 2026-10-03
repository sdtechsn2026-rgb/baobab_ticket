package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.EventConceptAiResponse
import com.example.ai.GeminiService
import com.example.data.model.ChatMessage
import com.example.ui.components.formatXof
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BaobabAiScreen(
    geminiService: GeminiService,
    onApplyConceptToWizard: (EventConceptAiResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "model",
                content = "Bonjour ! Je suis **Baobab AI**. Comment puis-je vous aider aujourd'hui ? (Tarifs FCFA, Wave / OM, stratégie de vente ou création d'événement)"
            )
        )
    }
    var currentInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var conceptPrompt by remember { mutableStateOf("Gala de fin d'année à Thiès, 800 invités, thème noir et or") }
    var generatedConcept by remember { mutableStateOf<EventConceptAiResponse?>(null) }
    var isGeneratingConcept by remember { mutableStateOf(false) }

    val quickQuestions = listOf(
        "Tarifs recommandés en FCFA ?",
        "Comment marche le paiement Wave / OM ?",
        "Rédige une annonce Instagram",
        "Fluidifier le scan aux portes"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GraphiteBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Sleek Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Assistant Chatbot", "Concepteur d'Événement").forEachIndexed { idx, title ->
                    val isSelected = (selectedTab == idx)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = idx }
                            .testTag("ai_tab_$idx"),
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

            if (selectedTab == 0) {
                // Quick chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickQuestions) { q ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isGenerating) currentInput = q
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
                        ) {
                            Text(q, color = Color(0xFFA5B8AD), fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isUser = (msg.role == "user")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 290.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isUser) ChampagneGold else DarkSurfaceElevated)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    color = if (isUser) GraphiteBlack else IvoryWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Text("Baobab AI réfléchit...", color = ChampagneGold, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                // Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        placeholder = { Text("Posez votre question...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (currentInput.isNotBlank() && !isGenerating) {
                                val text = currentInput.trim()
                                currentInput = ""
                                chatMessages.add(ChatMessage(role = "user", content = text))
                                coroutineScope.launch {
                                    isGenerating = true
                                    val reply = geminiService.sendChatMessage(chatMessages, text)
                                    chatMessages.add(ChatMessage(role = "model", content = reply))
                                    isGenerating = false
                                    listState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(ChampagneGold).testTag("send_ai_chat_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = GraphiteBlack, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Event Generator
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Idée d'événement :", color = IvoryWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = conceptPrompt,
                                    onValueChange = { conceptPrompt = it },
                                    modifier = Modifier.fillMaxWidth().testTag("ai_concept_prompt_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    maxLines = 3
                                )
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isGeneratingConcept = true
                                            val c = geminiService.generateEventConcept(conceptPrompt)
                                            generatedConcept = c
                                            isGeneratingConcept = false
                                        }
                                    },
                                    enabled = !isGeneratingConcept && conceptPrompt.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("generate_concept_button")
                                ) {
                                    if (isGeneratingConcept) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GraphiteBlack, strokeWidth = 2.dp)
                                    } else {
                                        Text("Générer avec Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (generatedConcept != null) {
                        val c = generatedConcept!!
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeepEmerald.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(c.title, color = IvoryWhite, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("${c.suggestedVenue} • ${c.palette}", color = ChampagneGold, fontSize = 11.sp)
                                    Text("Standard: ${formatXof(c.standardPrice)} • VIP: ${formatXof(c.vipPrice)} • VVIP: ${formatXof(c.vvipPrice)}", color = IvoryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    Button(
                                        onClick = { onApplyConceptToWizard(c) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = GraphiteBlack),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("apply_concept_button")
                                    ) {
                                        Text("Transférer dans le Wizard", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
