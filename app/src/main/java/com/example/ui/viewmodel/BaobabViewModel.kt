package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EventEntity
import com.example.data.local.PurchasedTicketEntity
import com.example.data.model.*
import com.example.data.payment.PaymentGatewayResult
import com.example.data.payment.PaymentProvider
import com.example.data.payment.PaymentRequest
import com.example.data.payment.PaymentService
import com.example.data.repository.BaobabRepository
import com.example.data.repository.ScanLogItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BaobabViewModel(
    private val repository: BaobabRepository
) : ViewModel() {

    val paymentService = PaymentService()

    // Room Database Reactive Flows (Flow from EventDao & PurchasedTicketDao)
    val roomEvents: StateFlow<List<EventEntity>> = repository.roomEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val roomPurchasedTickets: StateFlow<List<PurchasedTicketEntity>> = repository.roomPurchasedTickets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Domain StateFlows
    val events: StateFlow<List<Event>> = repository.events
    val ticketTypes: StateFlow<List<TicketType>> = repository.ticketTypes
    val purchasedTickets: StateFlow<List<Ticket>> = repository.tickets
    val orders: StateFlow<List<Order>> = repository.orders
    val pendingManualPayments: StateFlow<List<ManualPaymentProof>> = repository.pendingManualPayments
    val scanLogs: StateFlow<List<ScanLogItem>> = repository.scanLogs
    val isOfflineMode: StateFlow<Boolean> = repository.isOfflineMode
    val unsyncedCount: StateFlow<Int> = repository.unsyncedScansCount
    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val isUserAuthenticated: StateFlow<Boolean> = repository.isUserAuthenticated
    val organizations: StateFlow<List<Organization>> = repository.organizations
    val selectedOrgId: StateFlow<String> = repository.selectedOrgId

    fun signInUser(email: String, name: String = "", phone: String = "", role: UserRole = UserRole.ORGANIZATION_OWNER) {
        repository.signInUser(email, name, phone, role)
    }

    fun signOutUser() {
        repository.signOutUser()
    }

    // Actions
    suspend fun purchaseTicket(
        event: Event,
        ticketType: TicketType,
        quantity: Int,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        paymentMethod: PaymentMethod,
        transactionRef: String = "",
        proofNote: String = ""
    ): Pair<Boolean, String> {
        return repository.purchaseTicket(
            event,
            ticketType,
            quantity,
            customerName,
            customerEmail,
            customerPhone,
            paymentMethod,
            transactionRef,
            proofNote
        )
    }

    suspend fun approveManualPayment(proofId: String) {
        repository.approveManualPayment(proofId)
    }

    fun rejectManualPayment(proofId: String, reason: String) {
        repository.rejectManualPayment(proofId, reason)
    }

    fun transferTicket(
        ticketId: String,
        newName: String,
        newPhone: String,
        newEmail: String
    ): Pair<Boolean, String> {
        return repository.transferTicket(ticketId, newName, newPhone, newEmail)
    }

    suspend fun validateTicketScan(token: String, gate: String): TicketScanResult {
        return repository.validateTicketScan(token, gate)
    }

    suspend fun syncOfflineScans(): Int {
        return repository.syncOfflineScansToCloud()
    }

    fun toggleOfflineMode(enabled: Boolean) {
        repository.setOfflineMode(enabled)
    }

    fun switchOrganization(orgId: String) {
        repository.switchOrganization(orgId)
    }

    fun switchRole(role: UserRole) {
        repository.switchUserRole(role)
    }

    fun signIn(email: String, name: String = "", phone: String = "") {
        repository.signInUser(email, name, phone)
    }

    fun signOut() {
        repository.signOutUser()
    }

    fun createEvent(
        title: String,
        subtitle: String,
        description: String,
        category: String,
        venueName: String,
        venueAddress: String,
        city: String,
        startDate: String,
        endDate: String,
        themeTemplate: EventThemeTemplate,
        ticketTiers: List<TicketType>
    ): Event {
        return repository.createEvent(
            title,
            subtitle,
            description,
            category,
            venueName,
            venueAddress,
            city,
            startDate,
            endDate,
            themeTemplate,
            ticketTiers
        )
    }

    fun updateEventTheme(eventId: String, theme: EventThemeTemplate) {
        repository.updateEventTheme(eventId, theme)
    }

    suspend fun processTicketPayment(
        event: Event,
        ticketType: TicketType,
        quantity: Int,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        provider: PaymentProvider,
        isManualQr: Boolean,
        transactionRef: String
    ): PaymentGatewayResult {
        val total = ticketType.priceXof * quantity
        val request = PaymentRequest(
            eventId = event.id,
            eventTitle = event.title,
            amountXof = total,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            provider = provider,
            isManualQrMode = isManualQr,
            transactionRef = transactionRef
        )

        val result = paymentService.processPayment(request)

        when (result) {
            is PaymentGatewayResult.Success -> {
                val method = when (provider) {
                    PaymentProvider.WAVE -> PaymentMethod.WAVE_API
                    PaymentProvider.ORANGE_MONEY -> PaymentMethod.ORANGE_MONEY_API
                    PaymentProvider.FREE -> PaymentMethod.FREE
                }
                repository.purchaseTicket(
                    event = event,
                    ticketType = ticketType,
                    quantity = quantity,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    paymentMethod = method,
                    transactionRef = result.transactionId
                )
            }
            is PaymentGatewayResult.PendingVerification -> {
                val method = when (provider) {
                    PaymentProvider.WAVE -> PaymentMethod.WAVE_QR_MANUAL
                    PaymentProvider.ORANGE_MONEY -> PaymentMethod.ORANGE_MONEY_QR_MANUAL
                    PaymentProvider.FREE -> PaymentMethod.FREE
                }
                repository.purchaseTicket(
                    event = event,
                    ticketType = ticketType,
                    quantity = quantity,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    paymentMethod = method,
                    transactionRef = result.transactionRef,
                    proofNote = result.instructions
                )
            }
            is PaymentGatewayResult.Failure -> {
                // Return failure without modifying repository state
            }
        }

        return result
    }
}
