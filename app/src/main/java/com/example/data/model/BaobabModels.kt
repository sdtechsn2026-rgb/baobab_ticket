package com.example.data.model

import java.util.UUID

enum class UserRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ORGANIZATION_OWNER("Propriétaire"),
    ADMIN("Administrateur"),
    MANAGER("Manager"),
    TICKET_MANAGER("Gestionnaire Billetterie"),
    SCANNER("Contrôleur d'accès"),
    ACCOUNTANT("Comptable"),
    VIEWER("Lecteur"),
    CUSTOMER("Participant / Client")
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String = "+221 77 000 00 00",
    val role: UserRole = UserRole.ORGANIZATION_OWNER,
    val organizationId: String = "org-baobab-events"
)

data class Organization(
    val id: String,
    val name: String,
    val description: String,
    val email: String,
    val phone: String,
    val address: String,
    val currency: String = "XOF",
    val logoRes: String = "baobab_ticket_logo",
    val themeColorHex: String = "#123C2A"
)

enum class EventThemeTemplate(val displayName: String, val primaryHex: String, val accentHex: String) {
    LUXURY("Élégance Dorée & Noir", "#123C2A", "#C9A96E"),
    AFRO_MODERN("Afro Moderne Émeraude", "#0F4C3A", "#E0B050"),
    FESTIVAL("Sahel Vibe & Sunset", "#78281F", "#F59E0B"),
    TECH("Dakar Tech Minimal", "#0F172A", "#38BDF8"),
    CORPORATE("Sénégal Business", "#1E3A8A", "#60A5FA"),
    NIGHT("Almadies Nightlife", "#3B0764", "#C084FC")
}

data class Event(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val organizationId: String,
    val category: String,
    val coverImageUrl: String = "",
    val venueName: String,
    val venueAddress: String,
    val city: String = "Dakar",
    val startDate: String,
    val endDate: String,
    val startTime: String = "20:00",
    val endTime: String = "03:00",
    val status: String = "PUBLISHED", // DRAFT, PUBLISHED, ARCHIVED
    val isSeated: Boolean = false,
    val themeTemplate: EventThemeTemplate = EventThemeTemplate.LUXURY,
    val totalCapacity: Int = 1000,
    val soldCount: Int = 573,
    val checkInCount: Int = 342,
    val waveApiConfigured: Boolean = true,
    val orangeMoneyApiConfigured: Boolean = true,
    val waveManualQrEnabled: Boolean = true,
    val orangeMoneyManualQrEnabled: Boolean = true,
    val waveMerchantPhone: String = "+221 77 456 78 90",
    val waveMerchantName: String = "BAOBAB EVENTS SARL",
    val omMerchantPhone: String = "+221 78 123 45 67",
    val omMerchantName: String = "BAOBAB TICKETING",
    val shareSlug: String = "gala-2026"
)

data class TicketType(
    val id: String,
    val eventId: String,
    val name: String, // STANDARD, VIP, VVIP, EARLY BIRD, INVITATION
    val description: String,
    val priceXof: Double,
    val quantityTotal: Int,
    val quantitySold: Int,
    val perks: List<String>,
    val colorHex: String = "#C9A96E",
    val accessGates: List<String> = listOf("Porte Principale")
)

enum class PaymentMethod(val displayName: String, val badgeColorHex: String) {
    WAVE_API("Wave API Direct", "#1BA4E8"),
    ORANGE_MONEY_API("Orange Money API", "#FF6600"),
    WAVE_QR_MANUAL("Wave QR Manuel", "#1BA4E8"),
    ORANGE_MONEY_QR_MANUAL("Orange Money QR Manuel", "#FF6600"),
    FREE("Gratuit / Invitation", "#10B981")
}

enum class PaymentStatus(val label: String) {
    CONFIRMED("Payé & Confirmé"),
    PENDING_VERIFICATION("Vérification en attente"),
    FAILED("Échec"),
    REFUNDED("Remboursé")
}

data class Order(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val ticketTypeId: String,
    val ticketTypeName: String,
    val quantity: Int,
    val unitPriceXof: Double,
    val totalPriceXof: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val transactionReference: String = "",
    val proofNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class TicketStatus(val label: String) {
    ACTIVE("Valide"),
    USED("Déjà Utilisé"),
    CANCELLED("Annulé"),
    EXPIRED("Expiré"),
    REFUNDED("Remboursé"),
    TRANSFERRED("Transféré")
}

data class Ticket(
    val id: String,
    val ticketNumber: String, // e.g., BT-2026-8F92K7X4
    val orderId: String,
    val eventId: String,
    val eventTitle: String,
    val ticketTypeId: String,
    val ticketTypeName: String,
    val attendeeName: String,
    val attendeePhone: String,
    val attendeeEmail: String,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val qrSecurityToken: String,
    val priceXof: Double,
    val seatZone: String = "Accès Libre",
    val gate: String = "Porte Principale",
    val perks: List<String> = emptyList(),
    val usedAt: Long? = null,
    val usedGate: String? = null,
    val usedByScannerId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ManualPaymentProof(
    val id: String,
    val orderId: String,
    val eventTitle: String,
    val customerName: String,
    val customerPhone: String,
    val method: PaymentMethod,
    val amountXof: Double,
    val transactionRef: String,
    val proofNote: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

data class ScannerGate(
    val id: String,
    val name: String,
    val allowedTicketTypes: List<String>
)

enum class ScanResultType {
    VALID,
    USED,
    INVALID,
    EXPIRED,
    WRONG_GATE
}

data class TicketScanResult(
    val type: ScanResultType,
    val message: String,
    val ticket: Ticket? = null,
    val scannedAt: Long = System.currentTimeMillis(),
    val gate: String = "Porte Principale",
    val scannerId: String = "SCANNER-DEVICE-01"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGenerating: Boolean = false
)
