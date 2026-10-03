package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.TicketStatus
import com.example.data.model.UserRole
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun formatXof(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)
    return "${formatter.format(amount.toInt())} FCFA"
}

@Composable
fun BaobabHeader(
    currentOrgName: String,
    currentUserRole: UserRole,
    isOffline: Boolean,
    onSwitchOrgClick: () -> Unit,
    onRoleBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DeepEmerald,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(ChampagneGold, DarkGold))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = "BaobabTicket Logo",
                            tint = GraphiteBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BAOBAB",
                                color = ChampagneGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "TICKET",
                                color = IvoryWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = "EVENTS • TICKETS • ACCESS",
                            color = Color(0xFFA5B8AD),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Org Switcher & Role Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Offline badge indicator if active
                    if (isOffline) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = UsedAmber.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UsedAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(UsedAmber)
                                )
                                Text(
                                    text = "OFFLINE",
                                    color = UsedAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Organization Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSwitchOrgClick() }
                            .testTag("org_selector_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF384E42))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentOrgName,
                                color = IvoryWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Changer d'organisation",
                                tint = IvoryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Role Badge
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onRoleBadgeClick() }
                            .testTag("role_badge_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = ChampagneGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                    ) {
                        Text(
                            text = currentUserRole.label,
                            color = ChampagneGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LuxuryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    testTag: String = "luxury_button"
) {
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ChampagneGold
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChampagneGold,
                contentColor = GraphiteBlack
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(text = text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun TicketStatusBadge(status: TicketStatus) {
    val (bgColor, textColor) = when (status) {
        TicketStatus.ACTIVE -> Pair(ValidGreen.copy(alpha = 0.2f), ValidGreen)
        TicketStatus.USED -> Pair(UsedAmber.copy(alpha = 0.2f), UsedAmber)
        TicketStatus.CANCELLED -> Pair(ExpiredRed.copy(alpha = 0.2f), ExpiredRed)
        TicketStatus.EXPIRED -> Pair(ExpiredRed.copy(alpha = 0.2f), ExpiredRed)
        TicketStatus.REFUNDED -> Pair(Color.Gray.copy(alpha = 0.2f), Color.LightGray)
        TicketStatus.TRANSFERRED -> Pair(PendingBlue.copy(alpha = 0.2f), PendingBlue)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = status.label.uppercase(Locale.ROOT),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PaymentMethodBadge(method: PaymentMethod) {
    val color = when (method) {
        PaymentMethod.WAVE_API, PaymentMethod.WAVE_QR_MANUAL -> WaveBlue
        PaymentMethod.ORANGE_MONEY_API, PaymentMethod.ORANGE_MONEY_QR_MANUAL -> OrangeMoney
        PaymentMethod.FREE -> ValidGreen
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = method.displayName,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
