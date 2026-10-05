package com.example.data.repository

import android.content.Context
import com.example.data.local.BaobabRoomDatabase
import com.example.data.local.EventEntity
import com.example.data.local.OfflineScanLogEntity
import com.example.data.local.PurchasedTicketEntity
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BaobabRepository(private val context: Context) {

    private val db = BaobabRoomDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Room Database Flows
    val roomEvents: Flow<List<EventEntity>> = db.eventDao().getAllEvents()
    val roomPurchasedTickets: Flow<List<PurchasedTicketEntity>> = db.ticketDao().getAllPurchasedTickets()

    // Current User & Active Organization
    private val _isUserAuthenticated = MutableStateFlow(false)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "usr-sega-01",
            name = "Sega Diallo",
            email = "segacod05@gmail.com",
            phone = "+221 77 845 12 34",
            role = UserRole.ORGANIZATION_OWNER,
            organizationId = "org-baobab-events"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _organizations = MutableStateFlow(
        listOf(
            Organization(
                id = "org-baobab-events",
                name = "Baobab Events",
                description = "Agence leader dans l'organisation de galas de prestige et événements d'envergure à Dakar.",
                email = "contact@baobab-events.sn",
                phone = "+221 33 820 40 50",
                address = "Route des Almadies, Dakar, Sénégal",
                currency = "XOF",
                themeColorHex = "#123C2A"
            ),
            Organization(
                id = "org-dakar-arena",
                name = "Dakar Arena Production",
                description = "Gestionnaire officiel des festivals et concerts majeurs.",
                email = "events@dakar-arena.sn",
                phone = "+221 33 860 11 22",
                address = "Diamniadio, Sénégal",
                currency = "XOF",
                themeColorHex = "#0F4C3A"
            )
        )
    )
    val organizations: StateFlow<List<Organization>> = _organizations.asStateFlow()

    private val _selectedOrgId = MutableStateFlow("org-baobab-events")
    val selectedOrgId: StateFlow<String> = _selectedOrgId.asStateFlow()

    // Events State
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    // Ticket Types State
    private val _ticketTypes = MutableStateFlow<List<TicketType>>(emptyList())
    val ticketTypes: StateFlow<List<TicketType>> = _ticketTypes.asStateFlow()

    // Tickets State (User's tickets & Org tickets)
    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    // Orders State
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Manual Payment Verification Queue
    private val _pendingManualPayments = MutableStateFlow<List<ManualPaymentProof>>(emptyList())
    val pendingManualPayments: StateFlow<List<ManualPaymentProof>> = _pendingManualPayments.asStateFlow()

    // Scan Logs
    private val _scanLogs = MutableStateFlow<List<ScanLogItem>>(emptyList())
    val scanLogs: StateFlow<List<ScanLogItem>> = _scanLogs.asStateFlow()

    // Offline mode status
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _unsyncedScansCount = MutableStateFlow(0)
    val unsyncedScansCount: StateFlow<Int> = _unsyncedScansCount.asStateFlow()

    init {
        seedInitialDemoData()
        syncToLocalDb()
    }

    private fun seedInitialDemoData() {
        val galaEvent = Event(
            id = "evt-gala-2026",
            title = "Baobab Luxury Gala 2026",
            subtitle = "Soirée d'Exception & Networking d'Élite",
            description = "Une célébration prestigieuse rassemblant les leaders économiques, créateurs et innovateurs d'Afrique dans un cadre féerique au bord de l'océan Atlantique. Dîner gastronomique, défilé haute couture, prestations live et remise des Baobab Awards.",
            organizationId = "org-baobab-events",
            category = "Gala & Soirée",
            coverImageUrl = "",
            venueName = "Hôtel Terrou-Bi Resort & Marina",
            venueAddress = "Boulevard Martin Luther King, Corniche Ouest",
            city = "Dakar",
            startDate = "14 Nov 2026",
            endDate = "15 Nov 2026",
            startTime = "20:30",
            endTime = "04:00",
            status = "PUBLISHED",
            isSeated = true,
            themeTemplate = EventThemeTemplate.LUXURY,
            totalCapacity = 800,
            soldCount = 573,
            checkInCount = 342,
            waveApiConfigured = true,
            orangeMoneyApiConfigured = true,
            waveManualQrEnabled = true,
            orangeMoneyManualQrEnabled = true,
            waveMerchantPhone = "+221 77 654 32 10",
            waveMerchantName = "BAOBAB LUXURY EVENTS",
            omMerchantPhone = "+221 78 890 12 34",
            omMerchantName = "BAOBAB TICKETING SARL",
            shareSlug = "baobab-gala-2026"
        )

        val techSummitEvent = Event(
            id = "evt-tech-summit-2026",
            title = "Dakar Tech & AI Summit 2026",
            subtitle = "L'avenir de la Tech et de l'IA en Afrique",
            description = "Deux journées immersives de keynotes, panels d'investisseurs et ateliers pratiques avec les pionniers du numérique africain. Hackathon, pitchs de startups et networking international.",
            organizationId = "org-baobab-events",
            category = "Conférence & Tech",
            coverImageUrl = "",
            venueName = "Centre International de Conférences Abdou Diouf (CICAD)",
            venueAddress = "Pôle Urbain de Diamniadio",
            city = "Diamniadio, Dakar",
            startDate = "05 Déc 2026",
            endDate = "06 Déc 2026",
            startTime = "09:00",
            endTime = "18:00",
            status = "PUBLISHED",
            isSeated = false,
            themeTemplate = EventThemeTemplate.TECH,
            totalCapacity = 1500,
            soldCount = 890,
            checkInCount = 0,
            waveApiConfigured = true,
            orangeMoneyApiConfigured = true,
            waveManualQrEnabled = true,
            orangeMoneyManualQrEnabled = true,
            shareSlug = "dakar-tech-summit-2026"
        )

        val festivalEvent = Event(
            id = "evt-sahel-beats-2026",
            title = "Festival Sahel Beats 2026",
            subtitle = "Musique Afro, Rythmes & Street Art",
            description = "Le plus grand festival de musiques urbaines et traditionnelles au cœur de Dakar. 3 scènes, artistes panafricains, village artisanal et gastronomie locale.",
            organizationId = "org-baobab-events",
            category = "Festival & Concert",
            coverImageUrl = "",
            venueName = "Monument de la Renaissance Africaine",
            venueAddress = "Ouakam, Dakar",
            city = "Dakar",
            startDate = "28 Déc 2026",
            endDate = "30 Déc 2026",
            startTime = "16:00",
            endTime = "02:00",
            status = "PUBLISHED",
            isSeated = false,
            themeTemplate = EventThemeTemplate.AFRO_MODERN,
            totalCapacity = 3000,
            soldCount = 1840,
            checkInCount = 0,
            waveApiConfigured = true,
            orangeMoneyApiConfigured = true,
            shareSlug = "sahel-beats-2026"
        )

        _events.value = listOf(galaEvent, techSummitEvent, festivalEvent)

        // Ticket Types
        val types = listOf(
            TicketType(
                id = "tt-gala-std",
                eventId = "evt-gala-2026",
                name = "STANDARD",
                description = "Accès à la soirée, cocktail de bienvenue, place assise en zone B.",
                priceXof = 25000.0,
                quantityTotal = 400,
                quantitySold = 312,
                perks = listOf("Cocktail de bienvenue", "Accès salle principale", "Programme souvenir"),
                colorHex = "#2EA373",
                accessGates = listOf("Porte Principale")
            ),
            TicketType(
                id = "tt-gala-vip",
                eventId = "evt-gala-2026",
                name = "VIP",
                description = "Dîner gastronomique 3 services, table réservée en zone A, champagne, vestiaire dédié.",
                priceXof = 60000.0,
                quantityTotal = 250,
                quantitySold = 194,
                perks = listOf("Dîner gastronomique", "Coupe de champagne", "Table dédiée Zone A", "Fast Track Entrée VIP"),
                colorHex = "#C9A96E",
                accessGates = listOf("Porte Principale", "Accès VIP")
            ),
            TicketType(
                id = "tt-gala-vvip",
                eventId = "evt-gala-2026",
                name = "VVIP PRESTIGE",
                description = "Table d'honneur privée, service majordome, bar ouvert prestige, accès lounge privé & photo call officiel.",
                priceXof = 150000.0,
                quantityTotal = 150,
                quantitySold = 67,
                perks = listOf("Table d'honneur privée", "Bar prestige illimité", "Service majordome", "Lounge privé VIP", "Photo call officiel"),
                colorHex = "#DFCA9F",
                accessGates = listOf("Porte Principale", "Accès VIP", "Accès VVIP")
            ),
            TicketType(
                id = "tt-tech-free",
                eventId = "evt-tech-summit-2026",
                name = "EARLY BIRD PASS",
                description = "Accès libre aux keynotes publiques et zone d'exposition.",
                priceXof = 0.0,
                quantityTotal = 500,
                quantitySold = 500,
                perks = listOf("Accès Expo", "Keynotes publiques"),
                colorHex = "#38BDF8",
                accessGates = listOf("Porte Principale")
            ),
            TicketType(
                id = "tt-tech-pro",
                eventId = "evt-tech-summit-2026",
                name = "PRO DELEGATE PASS",
                description = "Accès intégral, déjeuners networking, masterclasses IA et cocktail des speakers.",
                priceXof = 45000.0,
                quantityTotal = 600,
                quantitySold = 310,
                perks = listOf("Toutes conférences", "Masterclasses IA", "Déjeuners buffet", "Cocktail Speakers"),
                colorHex = "#60A5FA",
                accessGates = listOf("Porte Principale", "Accès VIP")
            )
        )
        _ticketTypes.value = types

        // Clean initial state: No fake test tickets, no fake proofs.
        // Data is driven exclusively by the real local database and purchases.
        _tickets.value = emptyList()
        _pendingManualPayments.value = emptyList()
    }

    private fun syncToLocalDb() {
        scope.launch {
            // Persist Events in Room Database with name, date, price
            val eventEntities = _events.value.map {
                val basePrice = _ticketTypes.value.filter { tt -> tt.eventId == it.id }.minOfOrNull { tt -> tt.priceXof } ?: 0.0
                EventEntity(
                    id = it.id,
                    name = it.title,
                    date = it.startDate,
                    priceXof = basePrice,
                    category = it.category,
                    venueName = it.venueName,
                    city = it.city,
                    totalCapacity = it.totalCapacity,
                    soldCount = it.soldCount,
                    themeTemplate = it.themeTemplate.name
                )
            }
            db.eventDao().insertEvents(eventEntities)

            // Reactive collection from Room Database: Tickets are 100% sourced from Room
            db.ticketDao().getAllPurchasedTickets().collect { entities ->
                _tickets.value = entities.map { entity ->
                    Ticket(
                        id = entity.ticketNumber,
                        ticketNumber = entity.ticketNumber,
                        orderId = "ord-" + entity.ticketNumber.takeLast(6),
                        eventId = entity.eventId,
                        eventTitle = entity.eventName,
                        ticketTypeId = "tt-" + entity.ticketTypeName.lowercase(),
                        ticketTypeName = entity.ticketTypeName,
                        attendeeName = entity.attendeeName,
                        attendeePhone = entity.attendeePhone,
                        attendeeEmail = entity.attendeeEmail,
                        status = if (entity.status == "USED") TicketStatus.USED else if (entity.status == "TRANSFERRED") TicketStatus.TRANSFERRED else TicketStatus.ACTIVE,
                        qrSecurityToken = entity.qrSecurityToken,
                        priceXof = entity.priceXof,
                        seatZone = entity.seatZone,
                        gate = entity.gate,
                        createdAt = entity.purchasedAt,
                        usedAt = entity.usedAt,
                        usedGate = entity.usedGate
                    )
                }
            }

            checkUnsyncedScans()
        }

        // Reactive collection of active authenticated user session from Room Database
        scope.launch {
            db.userSessionDao().getActiveSession().collect { session ->
                if (session != null && session.isLoggedIn) {
                    val roleEnum = try {
                        UserRole.valueOf(session.role)
                    } catch (_: Exception) {
                        UserRole.ORGANIZATION_OWNER
                    }
                    _currentUser.value = UserProfile(
                        id = session.id,
                        name = session.name,
                        email = session.email,
                        phone = session.phone,
                        role = roleEnum,
                        organizationId = session.organizationId
                    )
                    _selectedOrgId.value = session.organizationId
                    _isUserAuthenticated.value = true
                }
            }
        }
    }

    // --- Authentication & User Session Management ---
    fun signInUser(email: String, name: String = "", phone: String = "", role: UserRole = UserRole.ORGANIZATION_OWNER) {
        val resolvedName = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = UserProfile(
            id = "usr-" + UUID.randomUUID().toString().take(8),
            name = resolvedName,
            email = email,
            phone = phone.ifBlank { "+221 77 845 12 34" },
            role = role,
            organizationId = _selectedOrgId.value
        )
        _currentUser.value = user
        _isUserAuthenticated.value = true
        scope.launch {
            db.userSessionDao().insertSession(
                com.example.data.local.UserSessionEntity(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    phone = user.phone,
                    role = user.role.name,
                    organizationId = user.organizationId,
                    isLoggedIn = true
                )
            )
        }
    }

    fun signOutUser() {
        _isUserAuthenticated.value = false
        scope.launch {
            db.userSessionDao().clearActiveSessions()
        }
        _currentUser.value = UserProfile(
            id = "usr-guest",
            name = "Invité",
            email = "invite@baobabticket.sn",
            phone = "",
            role = UserRole.CUSTOMER,
            organizationId = _selectedOrgId.value
        )
    }

    private suspend fun checkUnsyncedScans() {
        val count = db.scanLogDao().countUnsynced()
        _unsyncedScansCount.value = count
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
    }

    fun switchOrganization(orgId: String) {
        _selectedOrgId.value = orgId
        _currentUser.value = _currentUser.value.copy(organizationId = orgId)
    }

    fun switchUserRole(newRole: UserRole) {
        _currentUser.value = _currentUser.value.copy(role = newRole)
    }

    // Purchase Ticket Flow
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
        val totalPrice = ticketType.priceXof * quantity
        val orderId = "ord-" + UUID.randomUUID().toString().take(8)

        val isAutoApproved = (paymentMethod == PaymentMethod.FREE ||
                paymentMethod == PaymentMethod.WAVE_API ||
                paymentMethod == PaymentMethod.ORANGE_MONEY_API)

        val newOrder = Order(
            id = orderId,
            eventId = event.id,
            eventTitle = event.title,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            ticketTypeId = ticketType.id,
            ticketTypeName = ticketType.name,
            quantity = quantity,
            unitPriceXof = ticketType.priceXof,
            totalPriceXof = totalPrice,
            paymentMethod = paymentMethod,
            paymentStatus = if (isAutoApproved) PaymentStatus.CONFIRMED else PaymentStatus.PENDING_VERIFICATION,
            transactionReference = transactionRef,
            proofNote = proofNote
        )
        _orders.value = listOf(newOrder) + _orders.value

        if (isAutoApproved) {
            val createdTickets = mutableListOf<Ticket>()
            val random = SecureRandom()
            val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

            for (i in 1..quantity) {
                val tokenPart = (1..8).map { characters[random.nextInt(characters.length)] }.joinToString("")
                val ticketNum = "BT-2026-$tokenPart"
                val secureQrToken = "BAOBAB-SECURE-${event.id.takeLast(4)}-$tokenPart-${System.currentTimeMillis()}"

                val ticket = Ticket(
                    id = "tkt-" + UUID.randomUUID().toString().take(8),
                    ticketNumber = ticketNum,
                    orderId = orderId,
                    eventId = event.id,
                    eventTitle = event.title,
                    ticketTypeId = ticketType.id,
                    ticketTypeName = ticketType.name,
                    attendeeName = customerName,
                    attendeePhone = customerPhone,
                    attendeeEmail = customerEmail,
                    status = TicketStatus.ACTIVE,
                    qrSecurityToken = secureQrToken,
                    priceXof = ticketType.priceXof,
                    gate = ticketType.accessGates.firstOrNull() ?: "Porte Principale",
                    perks = ticketType.perks
                )
                createdTickets.add(ticket)

                // Persist purchased ticket directly into Room Database
                db.ticketDao().insertTicket(
                    PurchasedTicketEntity(
                        ticketNumber = ticketNum,
                        eventId = event.id,
                        eventName = event.title,
                        eventDate = event.startDate,
                        priceXof = ticketType.priceXof,
                        ticketTypeName = ticketType.name,
                        attendeeName = customerName,
                        attendeePhone = customerPhone,
                        attendeeEmail = customerEmail,
                        qrSecurityToken = secureQrToken,
                        gate = ticketType.accessGates.firstOrNull() ?: "Porte Principale",
                        seatZone = "Accès Libre",
                        status = "ACTIVE",
                        paymentMethod = paymentMethod.name,
                        purchasedAt = System.currentTimeMillis()
                    )
                )
            }

            _tickets.value = createdTickets + _tickets.value

            _events.value = _events.value.map {
                if (it.id == event.id) it.copy(soldCount = it.soldCount + quantity) else it
            }

            return Pair(true, "Commande confirmée ! Vos billets avec QR sécurisés sont disponibles.")
        } else {
            val proof = ManualPaymentProof(
                id = "proof-" + UUID.randomUUID().toString().take(8),
                orderId = orderId,
                eventTitle = event.title,
                customerName = customerName,
                customerPhone = customerPhone,
                method = paymentMethod,
                amountXof = totalPrice,
                transactionRef = transactionRef,
                proofNote = proofNote,
                status = "PENDING"
            )
            _pendingManualPayments.value = listOf(proof) + _pendingManualPayments.value
            return Pair(true, "Preuve de paiement enregistrée ! Votre billet sera généré après validation.")
        }
    }

    // Organizer Approves Manual Payment
    suspend fun approveManualPayment(proofId: String) {
        val proof = _pendingManualPayments.value.find { it.id == proofId } ?: return
        val order = _orders.value.find { it.id == proof.orderId }

        val eventTitle = proof.eventTitle
        val event = _events.value.find { it.title == eventTitle } ?: _events.value.first()
        val ticketType = _ticketTypes.value.find { it.eventId == event.id } ?: _ticketTypes.value.first()

        val random = SecureRandom()
        val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val tokenPart = (1..8).map { characters[random.nextInt(characters.length)] }.joinToString("")
        val ticketNum = "BT-2026-$tokenPart"
        val secureQrToken = "BAOBAB-SECURE-${event.id.takeLast(4)}-$tokenPart-${System.currentTimeMillis()}"

        val ticket = Ticket(
            id = "tkt-" + UUID.randomUUID().toString().take(8),
            ticketNumber = ticketNum,
            orderId = proof.orderId,
            eventId = event.id,
            eventTitle = event.title,
            ticketTypeId = ticketType.id,
            ticketTypeName = ticketType.name,
            attendeeName = proof.customerName,
            attendeePhone = proof.customerPhone,
            attendeeEmail = order?.customerEmail ?: "client@baobabticket.sn",
            status = TicketStatus.ACTIVE,
            qrSecurityToken = secureQrToken,
            priceXof = proof.amountXof,
            gate = ticketType.accessGates.firstOrNull() ?: "Porte Principale",
            perks = ticketType.perks
        )

        _tickets.value = listOf(ticket) + _tickets.value

        // Persist into Room
        db.ticketDao().insertTicket(
            PurchasedTicketEntity(
                ticketNumber = ticketNum,
                eventId = event.id,
                eventName = event.title,
                eventDate = event.startDate,
                priceXof = proof.amountXof,
                ticketTypeName = ticketType.name,
                attendeeName = proof.customerName,
                attendeePhone = proof.customerPhone,
                attendeeEmail = order?.customerEmail ?: "client@baobabticket.sn",
                qrSecurityToken = secureQrToken,
                gate = ticketType.accessGates.firstOrNull() ?: "Porte Principale",
                seatZone = "Accès Libre",
                status = "ACTIVE",
                paymentMethod = proof.method.name,
                purchasedAt = System.currentTimeMillis()
            )
        )

        _pendingManualPayments.value = _pendingManualPayments.value.map {
            if (it.id == proofId) it.copy(status = "APPROVED", reviewedAt = System.currentTimeMillis()) else it
        }

        if (order != null) {
            _orders.value = _orders.value.map {
                if (it.id == order.id) it.copy(paymentStatus = PaymentStatus.CONFIRMED) else it
            }
        }

        _events.value = _events.value.map {
            if (it.id == event.id) it.copy(soldCount = it.soldCount + 1) else it
        }
    }

    // Organizer Rejects Manual Payment
    fun rejectManualPayment(proofId: String, reason: String) {
        _pendingManualPayments.value = _pendingManualPayments.value.map {
            if (it.id == proofId) it.copy(
                status = "REJECTED",
                rejectionReason = reason,
                reviewedAt = System.currentTimeMillis()
            ) else it
        }
        val proof = _pendingManualPayments.value.find { it.id == proofId }
        if (proof != null) {
            _orders.value = _orders.value.map {
                if (it.id == proof.orderId) it.copy(paymentStatus = PaymentStatus.FAILED) else it
            }
        }
    }

    // Transfer Ticket to another attendee
    fun transferTicket(ticketId: String, newName: String, newPhone: String, newEmail: String): Pair<Boolean, String> {
        val oldTicket = _tickets.value.find { it.id == ticketId }
            ?: return Pair(false, "Billet introuvable")

        if (oldTicket.status != TicketStatus.ACTIVE) {
            return Pair(false, "Ce billet ne peut pas être transféré (${oldTicket.status.label})")
        }

        val random = SecureRandom()
        val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val tokenPart = (1..8).map { characters[random.nextInt(characters.length)] }.joinToString("")
        val newTicketNum = "BT-2026-$tokenPart"
        val newSecureQrToken = "BAOBAB-SECURE-TRANSFERRED-$tokenPart-${System.currentTimeMillis()}"

        val transferredTicket = oldTicket.copy(
            id = "tkt-" + UUID.randomUUID().toString().take(8),
            ticketNumber = newTicketNum,
            attendeeName = newName,
            attendeePhone = newPhone,
            attendeeEmail = newEmail,
            status = TicketStatus.ACTIVE,
            qrSecurityToken = newSecureQrToken,
            createdAt = System.currentTimeMillis()
        )

        // Invalidate old in Room & insert new
        scope.launch {
            db.ticketDao().updateTicketStatus(oldTicket.ticketNumber, "TRANSFERRED", System.currentTimeMillis(), oldTicket.gate)
            db.ticketDao().insertTicket(
                PurchasedTicketEntity(
                    ticketNumber = newTicketNum,
                    eventId = transferredTicket.eventId,
                    eventName = transferredTicket.eventTitle,
                    eventDate = "14 Nov 2026",
                    priceXof = transferredTicket.priceXof,
                    ticketTypeName = transferredTicket.ticketTypeName,
                    attendeeName = newName,
                    attendeePhone = newPhone,
                    attendeeEmail = newEmail,
                    qrSecurityToken = newSecureQrToken,
                    gate = transferredTicket.gate,
                    seatZone = transferredTicket.seatZone,
                    status = "ACTIVE",
                    paymentMethod = "TRANSFER"
                )
            )
        }

        _tickets.value = _tickets.value.map {
            if (it.id == ticketId) it.copy(status = TicketStatus.TRANSFERRED) else it
        } + listOf(transferredTicket)

        return Pair(true, "Billet transféré à $newName. L'ancien QR code a été révoqué.")
    }

    // Scan & Validate Ticket
    suspend fun validateTicketScan(scannedPayload: String, selectedGate: String): TicketScanResult {
        val cleanToken = scannedPayload.trim()

        if (_isOfflineMode.value) {
            val entity = db.ticketDao().getTicketByQrToken(cleanToken)
                ?: db.ticketDao().getTicketByNumber(cleanToken)

            if (entity == null) {
                logScan(cleanToken, "Inconnu", "INCONNU", "INVALIDE", selectedGate, false)
                return TicketScanResult(
                    type = ScanResultType.INVALID,
                    message = "QR non reconnu en base hors-ligne",
                    gate = selectedGate
                )
            }

            if (entity.status == "USED") {
                val timeStr = entity.usedAt?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                } ?: ""
                logScan(entity.ticketNumber, entity.attendeeName, entity.ticketTypeName, "DÉJÀ UTILISÉ", selectedGate, false)
                return TicketScanResult(
                    type = ScanResultType.USED,
                    message = "Ticket déjà scanné à $timeStr !",
                    ticket = null,
                    gate = selectedGate
                )
            }

            if (entity.status != "ACTIVE") {
                logScan(entity.ticketNumber, entity.attendeeName, entity.ticketTypeName, entity.status, selectedGate, false)
                return TicketScanResult(
                    type = ScanResultType.INVALID,
                    message = "Billet non valide (${entity.status})",
                    gate = selectedGate
                )
            }

            val now = System.currentTimeMillis()
            db.ticketDao().updateTicketStatus(entity.ticketNumber, "USED", now, selectedGate)
            logScan(entity.ticketNumber, entity.attendeeName, entity.ticketTypeName, "VALIDE (OFFLINE)", selectedGate, false)
            checkUnsyncedScans()

            return TicketScanResult(
                type = ScanResultType.VALID,
                message = "ENTRÉE VALIDÉE (MODE HORS-LIGNE)",
                ticket = Ticket(
                    id = entity.ticketNumber,
                    ticketNumber = entity.ticketNumber,
                    orderId = "",
                    eventId = entity.eventId,
                    eventTitle = entity.eventName,
                    ticketTypeId = "",
                    ticketTypeName = entity.ticketTypeName,
                    attendeeName = entity.attendeeName,
                    attendeePhone = entity.attendeePhone,
                    attendeeEmail = entity.attendeeEmail,
                    status = TicketStatus.USED,
                    qrSecurityToken = entity.qrSecurityToken,
                    priceXof = entity.priceXof,
                    gate = entity.gate
                ),
                gate = selectedGate
            )
        } else {
            val matchedTicket = _tickets.value.find {
                it.qrSecurityToken == cleanToken || it.ticketNumber.equals(cleanToken, ignoreCase = true)
            }

            if (matchedTicket == null) {
                logScan(cleanToken, "Inconnu", "INCONNU", "INVALIDE", selectedGate, true)
                return TicketScanResult(
                    type = ScanResultType.INVALID,
                    message = "QR CODE NON RECONNU",
                    gate = selectedGate
                )
            }

            if (matchedTicket.status == TicketStatus.USED) {
                val timeStr = matchedTicket.usedAt?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                } ?: ""
                logScan(matchedTicket.ticketNumber, matchedTicket.attendeeName, matchedTicket.ticketTypeName, "DÉJÀ UTILISÉ", selectedGate, true)
                return TicketScanResult(
                    type = ScanResultType.USED,
                    message = "ATTENTION : BILLET DÉJÀ SCANNÉ à $timeStr",
                    ticket = matchedTicket,
                    gate = selectedGate
                )
            }

            if (matchedTicket.status != TicketStatus.ACTIVE) {
                logScan(matchedTicket.ticketNumber, matchedTicket.attendeeName, matchedTicket.ticketTypeName, matchedTicket.status.name, selectedGate, true)
                return TicketScanResult(
                    type = ScanResultType.INVALID,
                    message = "BILLET NON VALIDE (${matchedTicket.status.label})",
                    ticket = matchedTicket,
                    gate = selectedGate
                )
            }

            val now = System.currentTimeMillis()
            _tickets.value = _tickets.value.map {
                if (it.id == matchedTicket.id) it.copy(
                    status = TicketStatus.USED,
                    usedAt = now,
                    usedGate = selectedGate,
                    usedByScannerId = "SCANNER-DEVICE-01"
                ) else it
            }

            // Update in Room
            db.ticketDao().updateTicketStatus(matchedTicket.ticketNumber, "USED", now, selectedGate)

            _events.value = _events.value.map {
                if (it.id == matchedTicket.eventId) it.copy(checkInCount = it.checkInCount + 1) else it
            }

            logScan(matchedTicket.ticketNumber, matchedTicket.attendeeName, matchedTicket.ticketTypeName, "VALIDE", selectedGate, true)

            return TicketScanResult(
                type = ScanResultType.VALID,
                message = "ENTRÉE VALIDÉE AVEC SUCCÈS",
                ticket = matchedTicket.copy(status = TicketStatus.USED, usedAt = now),
                gate = selectedGate
            )
        }
    }

    private suspend fun logScan(ticketNum: String, name: String, tier: String, status: String, gate: String, isSynced: Boolean) {
        val now = System.currentTimeMillis()
        val item = ScanLogItem(
            id = UUID.randomUUID().toString(),
            ticketNumber = ticketNum,
            attendeeName = name,
            ticketTier = tier,
            status = status,
            gate = gate,
            timestamp = now
        )
        _scanLogs.value = listOf(item) + _scanLogs.value

        db.scanLogDao().insert(
            OfflineScanLogEntity(
                ticketNumber = ticketNum,
                eventId = "evt-gala-2026",
                attendeeName = name,
                status = status,
                gate = gate,
                timestamp = now,
                isSynced = isSynced
            )
        )
    }

    suspend fun syncOfflineScansToCloud(): Int {
        val unsynced = db.scanLogDao().getUnsyncedLogs()
        if (unsynced.isEmpty()) return 0

        val updatedTickets = _tickets.value.toMutableList()
        var synchronizedCount = 0

        for (log in unsynced) {
            val idx = updatedTickets.indexOfFirst { it.ticketNumber == log.ticketNumber }
            if (idx != -1 && updatedTickets[idx].status == TicketStatus.ACTIVE) {
                updatedTickets[idx] = updatedTickets[idx].copy(
                    status = TicketStatus.USED,
                    usedAt = log.timestamp,
                    usedGate = log.gate,
                    usedByScannerId = "OFFLINE-DEVICE-SYNC"
                )
                synchronizedCount++
            }
        }

        _tickets.value = updatedTickets
        db.scanLogDao().markAllSynced()
        checkUnsyncedScans()
        return unsynced.size
    }

    // Create New Event Wizard & Persist to Room
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
        val newEventId = "evt-" + UUID.randomUUID().toString().take(8)
        val basePrice = ticketTiers.minOfOrNull { it.priceXof } ?: 0.0

        val newEvent = Event(
            id = newEventId,
            title = title,
            subtitle = subtitle,
            description = description,
            organizationId = _selectedOrgId.value,
            category = category,
            venueName = venueName,
            venueAddress = venueAddress,
            city = city,
            startDate = startDate,
            endDate = endDate,
            themeTemplate = themeTemplate,
            totalCapacity = ticketTiers.sumOf { it.quantityTotal },
            soldCount = 0,
            checkInCount = 0,
            waveApiConfigured = true,
            orangeMoneyApiConfigured = true,
            waveManualQrEnabled = true,
            orangeMoneyManualQrEnabled = true,
            shareSlug = title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "-")
        )

        _events.value = listOf(newEvent) + _events.value
        val tiersWithEventId = ticketTiers.map { it.copy(eventId = newEventId) }
        _ticketTypes.value = _ticketTypes.value + tiersWithEventId

        // Persist Event in Room database with name, date, price
        scope.launch {
            db.eventDao().insertEvent(
                EventEntity(
                    id = newEventId,
                    name = title,
                    date = startDate,
                    priceXof = basePrice,
                    category = category,
                    venueName = venueName,
                    city = city,
                    totalCapacity = newEvent.totalCapacity,
                    soldCount = 0,
                    themeTemplate = themeTemplate.name
                )
            )
        }

        return newEvent
    }

    fun updateEventTheme(eventId: String, theme: EventThemeTemplate) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(themeTemplate = theme) else it
        }
    }
}

data class ScanLogItem(
    val id: String,
    val ticketNumber: String,
    val attendeeName: String,
    val ticketTier: String,
    val status: String,
    val gate: String,
    val timestamp: Long
)
