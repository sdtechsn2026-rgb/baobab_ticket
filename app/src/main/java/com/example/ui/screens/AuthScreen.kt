package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onSignInSuccess: (email: String, name: String, phone: String, role: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("segacod05@gmail.com") }
    var password by remember { mutableStateOf("••••••••") }
    var fullName by remember { mutableStateOf("Sega Diallo") }
    var phone by remember { mutableStateOf("+221 77 845 12 34") }
    var selectedRole by remember { mutableStateOf(UserRole.ORGANIZATION_OWNER) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Luxury Brand Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(ChampagneGold, DarkGold)))
                    .border(2.dp, ChampagneGold.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = "BaobabTicket Logo",
                    tint = GraphiteBlack,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "BAOBAB TICKET",
                color = ChampagneGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Text(
                text = "BILLETTERIE & CONTRÔLE D'ACCÈS",
                color = Color(0xFFA5B8AD),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector: Connexion vs Inscription
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUp) ChampagneGold else Color.Transparent)
                                .clickable { isSignUp = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Connexion",
                                color = if (!isSignUp) GraphiteBlack else IvoryWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUp) ChampagneGold else Color.Transparent)
                                .clickable { isSignUp = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Inscription",
                                color = if (isSignUp) GraphiteBlack else IvoryWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSignUp) {
                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nom complet") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ChampagneGold)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampagneGold,
                                focusedLabelColor = ChampagneGold
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Téléphone mobile (Wave / OM)") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = ChampagneGold)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampagneGold,
                                focusedLabelColor = ChampagneGold
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Selector
                        Text(
                            text = "Rôle dans la plateforme :",
                            color = Color(0xFFA5B8AD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RoleChoiceChip(
                                label = "Organisateur",
                                isSelected = selectedRole == UserRole.ORGANIZATION_OWNER,
                                onClick = { selectedRole = UserRole.ORGANIZATION_OWNER },
                                modifier = Modifier.weight(1f)
                            )
                            RoleChoiceChip(
                                label = "Contrôleur",
                                isSelected = selectedRole == UserRole.SCANNER,
                                onClick = { selectedRole = UserRole.SCANNER },
                                modifier = Modifier.weight(1f)
                            )
                            RoleChoiceChip(
                                label = "Acheteur",
                                isSelected = selectedRole == UserRole.CUSTOMER,
                                onClick = { selectedRole = UserRole.CUSTOMER },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Adresse Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = ChampagneGold)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            focusedLabelColor = ChampagneGold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ChampagneGold)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            focusedLabelColor = ChampagneGold
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                onSignInSuccess(email, fullName, phone, selectedRole)
                            } else {
                                Toast.makeText(context, "Veuillez entrer une adresse email valide", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = GraphiteBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSignUp) Icons.Default.HowToReg else Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUp) "Créer mon Compte" else "Se Connecter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Demo 1-Click Fast Login
                    OutlinedButton(
                        onClick = {
                            onSignInSuccess("segacod05@gmail.com", "Sega Diallo", "+221 77 845 12 34", UserRole.ORGANIZATION_OWNER)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("auth_demo_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B8AD)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283A31))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accès Rapide Démo (Organisateur)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Guarantee Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F1713))
                    .border(1.dp, Color(0xFF283A31), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Connexion chiffrée • Wave & Orange Money certifiés",
                    color = Color(0xFFA5B8AD),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RoleChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DeepEmerald else DarkSurfaceCard)
            .border(
                1.dp,
                if (isSelected) ChampagneGold else Color(0xFF283A31),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ChampagneGold else Color(0xFFA5B8AD),
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
