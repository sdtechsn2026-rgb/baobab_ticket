package com.example.data.payment

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

enum class PaymentProvider(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val brandColorHex: String,
    val merchantPhone: String,
    val merchantName: String,
    val ussdInstruction: String?
) {
    WAVE(
        id = "wave",
        displayName = "Wave",
        subtitle = "Paiement direct à 1% ou 0% de frais",
        brandColorHex = "#1BA4E8",
        merchantPhone = "+221 77 654 32 10",
        merchantName = "BAOBAB LUXURY EVENTS",
        ussdInstruction = null
    ),
    ORANGE_MONEY(
        id = "orange_money",
        displayName = "Orange Money",
        subtitle = "Validation mobile via code secret ou QR",
        brandColorHex = "#FF6600",
        merchantPhone = "+221 78 890 12 34",
        merchantName = "BAOBAB TICKETING SARL",
        ussdInstruction = "Tapez #144# pour valider"
    ),
    FREE(
        id = "free",
        displayName = "Accès Gratuit",
        subtitle = "Billet invitation ou inscription libre",
        brandColorHex = "#10B981",
        merchantPhone = "",
        merchantName = "",
        ussdInstruction = null
    )
}

sealed interface PaymentGatewayResult {
    data class Success(
        val transactionId: String,
        val provider: PaymentProvider,
        val amountXof: Double,
        val receiptCode: String,
        val message: String
    ) : PaymentGatewayResult

    data class PendingVerification(
        val orderId: String,
        val provider: PaymentProvider,
        val amountXof: Double,
        val transactionRef: String,
        val instructions: String
    ) : PaymentGatewayResult

    data class Failure(
        val errorMessage: String,
        val provider: PaymentProvider
    ) : PaymentGatewayResult
}

data class PaymentRequest(
    val orderId: String = "ord-" + UUID.randomUUID().toString().take(8),
    val eventId: String,
    val eventTitle: String,
    val amountXof: Double,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String = "",
    val provider: PaymentProvider,
    val isManualQrMode: Boolean = false,
    val transactionRef: String = "",
    val proofNote: String = ""
)

class PaymentService {

    /**
     * Traitement du paiement (Wave direct, Orange Money ou mode QR manuel)
     */
    suspend fun processPayment(request: PaymentRequest): PaymentGatewayResult = withContext(Dispatchers.IO) {
        // Validation du numéro de téléphone sénégalais si payant
        if (request.provider != PaymentProvider.FREE) {
            val cleanPhone = request.customerPhone.replace(" ", "").replace("-", "")
            if (cleanPhone.length < 9) {
                return@withContext PaymentGatewayResult.Failure(
                    errorMessage = "Veuillez renseigner un numéro de téléphone valide pour le paiement.",
                    provider = request.provider
                )
            }
        }

        // Gratuit / Invitation
        if (request.provider == PaymentProvider.FREE || request.amountXof == 0.0) {
            return@withContext PaymentGatewayResult.Success(
                transactionId = "FREE-" + UUID.randomUUID().toString().take(8).uppercase(),
                provider = PaymentProvider.FREE,
                amountXof = 0.0,
                receiptCode = "REC-FREE-${System.currentTimeMillis()}",
                message = "Invitation confirmée avec succès."
            )
        }

        // Mode B : Paiement par QR manuel (Preuve SMS fournie par le client)
        if (request.isManualQrMode) {
            delay(500)
            val ref = if (request.transactionRef.isNotBlank()) {
                request.transactionRef.trim()
            } else {
                "${if (request.provider == PaymentProvider.WAVE) "WV" else "OM"}-SN-${(100000..999999).random()}"
            }

            return@withContext PaymentGatewayResult.PendingVerification(
                orderId = request.orderId,
                provider = request.provider,
                amountXof = request.amountXof,
                transactionRef = ref,
                instructions = "Preuve transmise à l'organisateur (${request.provider.displayName}). Le billet sera activé dès rapprochement bancaire."
            )
        }

        // Mode A : API Directe (Simulation temps réel avec Wave / Orange Money)
        delay(900) // Simulation temps réseau bancaire

        val random = SecureRandom()
        val suffix = (1..6).map { ('0'..'9').random() }.joinToString("")

        return@withContext when (request.provider) {
            PaymentProvider.WAVE -> {
                val waveTxId = "WV-DKR-$suffix"
                PaymentGatewayResult.Success(
                    transactionId = waveTxId,
                    provider = PaymentProvider.WAVE,
                    amountXof = request.amountXof,
                    receiptCode = "WAVE-RECEIPT-$suffix",
                    message = "Paiement Wave instantané validé avec succès."
                )
            }
            PaymentProvider.ORANGE_MONEY -> {
                val omTxId = "OM-SN-$suffix"
                PaymentGatewayResult.Success(
                    transactionId = omTxId,
                    provider = PaymentProvider.ORANGE_MONEY,
                    amountXof = request.amountXof,
                    receiptCode = "OM-RECEIPT-$suffix",
                    message = "Débit Orange Money confirmé."
                )
            }
            PaymentProvider.FREE -> {
                PaymentGatewayResult.Success(
                    transactionId = "FREE-$suffix",
                    provider = PaymentProvider.FREE,
                    amountXof = 0.0,
                    receiptCode = "FREE-$suffix",
                    message = "Accès gratuit validé."
                )
            }
        }
    }

    /**
     * Génère la chaîne de données pour le QR Code marchand Wave ou OM
     */
    fun generatePaymentQrContent(provider: PaymentProvider, amountXof: Double, merchantPhone: String): String {
        return when (provider) {
            PaymentProvider.WAVE -> {
                val phone = merchantPhone.ifBlank { "+221776543210" }
                "wave://pay?recipient=$phone&amount=${amountXof.toInt()}&currency=XOF"
            }
            PaymentProvider.ORANGE_MONEY -> {
                val phone = merchantPhone.ifBlank { "+221788901234" }
                "om://transfer?to=$phone&amount=${amountXof.toInt()}&ref=BAOBABTICKET"
            }
            PaymentProvider.FREE -> "baobab://free-access"
        }
    }
}
