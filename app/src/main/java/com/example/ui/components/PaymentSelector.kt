package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.payment.PaymentProvider
import com.example.ui.theme.*

@Composable
fun SimplePaymentSelector(
    selectedProvider: PaymentProvider,
    onSelectProvider: (PaymentProvider) -> Unit,
    isManualQr: Boolean,
    onToggleManualQr: (Boolean) -> Unit,
    amountXof: Double,
    customerPhone: String,
    onPhoneChange: (String) -> Unit,
    transactionRef: String,
    onTransactionRefChange: (String) -> Unit,
    qrContent: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Sélectionnez votre mode de paiement :",
            color = IvoryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        if (amountXof == 0.0) {
            // Free ticket mode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectProvider(PaymentProvider.FREE) },
                shape = RoundedCornerShape(12.dp),
                color = DeepEmerald,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ChampagneGold)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ValidGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = ValidGreen, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Accès Gratuit / Invitation", color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Aucun paiement requis", color = Color(0xFFA5B8AD), fontSize = 11.sp)
                    }
                }
            }
        } else {
            // Wave & Orange Money Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wave Card
                PaymentProviderCard(
                    provider = PaymentProvider.WAVE,
                    isSelected = (selectedProvider == PaymentProvider.WAVE),
                    onSelect = { onSelectProvider(PaymentProvider.WAVE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_wave_payment")
                )

                // Orange Money Card
                PaymentProviderCard(
                    provider = PaymentProvider.ORANGE_MONEY,
                    isSelected = (selectedProvider == PaymentProvider.ORANGE_MONEY),
                    onSelect = { onSelectProvider(PaymentProvider.ORANGE_MONEY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_orange_money_payment")
                )
            }

            // Phone Number Input
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onPhoneChange,
                label = { Text("Numéro ${selectedProvider.displayName} (+221...)") },
                placeholder = { Text("77 000 00 00") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(selectedProvider.brandColorHex))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_phone_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Switch: Instant Direct vs QR Scanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isManualQr) "Mode QR Code (Scanner depuis l'application)" else "Mode Direct (Débit automatique 1-clic)",
                    color = Color(0xFFA5B8AD),
                    fontSize = 11.sp
                )
                TextButton(
                    onClick = { onToggleManualQr(!isManualQr) }
                ) {
                    Text(
                        text = if (isManualQr) "Passer au débit 1-clic" else "Payer par QR Code",
                        color = ChampagneGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // QR Code Box (If QR mode is selected)
            AnimatedVisibility(visible = isManualQr) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(android.graphics.Color.parseColor(selectedProvider.brandColorHex)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Scannez avec votre application ${selectedProvider.displayName}",
                            color = IvoryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        QrCodeImage(
                            content = qrContent,
                            size = 120.dp,
                            qrColor = GraphiteBlack,
                            backgroundColor = IvoryWhite
                        )

                        Text(
                            text = "Bénéficiaire : ${selectedProvider.merchantName}\nNuméro : ${selectedProvider.merchantPhone}\nMontant : ${formatXof(amountXof)}",
                            color = Color(0xFFA5B8AD),
                            fontSize = 10.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        OutlinedTextField(
                            value = transactionRef,
                            onValueChange = onTransactionRefChange,
                            label = { Text("Référence de transaction (SMS reçu)") },
                            placeholder = { Text(if (selectedProvider == PaymentProvider.WAVE) "ex: WV-DKR-8932" else "ex: OM-SN-4921") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_transaction_ref_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentProviderCard(
    provider: PaymentProvider,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = Color(android.graphics.Color.parseColor(provider.brandColorHex))

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) brandColor.copy(alpha = 0.15f) else DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) brandColor else Color(0xFF283A31)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(brandColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (provider == PaymentProvider.WAVE) "W" else "OM",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = brandColor)
                )
            }

            Text(
                text = provider.displayName,
                color = IvoryWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )

            Text(
                text = if (provider == PaymentProvider.WAVE) "0% ou 1% frais" else "Orange Money Sénégal",
                color = Color(0xFFA5B8AD),
                fontSize = 10.sp
            )
        }
    }
}
