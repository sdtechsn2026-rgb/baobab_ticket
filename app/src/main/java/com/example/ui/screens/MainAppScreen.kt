package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.EventConceptAiResponse
import com.example.ai.GeminiService
import com.example.data.model.Event
import com.example.data.model.UserRole
import com.example.data.repository.BaobabRepository
import com.example.ui.components.BaobabHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import com.example.ui.viewmodel.BaobabViewModel

enum class AppNavTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DISCOVERY("Explorer", Icons.Default.Explore),
    WALLET("Billets", Icons.Default.ConfirmationNumber),
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SCANNER("Scanner", Icons.Default.QrCodeScanner),
    AI_STUDIO("IA Studio", Icons.Default.AutoAwesome)
}

@Composable
fun MainAppScreen(
    viewModel: BaobabViewModel,
    geminiService: GeminiService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(AppNavTab.DISCOVERY) }

    // Sub-screen navigations
    var isWizardOpen by remember { mutableStateOf(false) }
    var wizardConceptToUse by remember { mutableStateOf<EventConceptAiResponse?>(null) }
    var eventInStudio by remember { mutableStateOf<Event?>(null) }

    // Dialogs
    var showOrgSwitcher by remember { mutableStateOf(false) }
    var showRoleSwitcher by remember { mutableStateOf(false) }

    // ViewModel StateFlows backed by Room Database & Repository
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val organizations by viewModel.organizations.collectAsStateWithLifecycle()
    val selectedOrgId by viewModel.selectedOrgId.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val ticketTypes by viewModel.ticketTypes.collectAsStateWithLifecycle()
    val tickets by viewModel.purchasedTickets.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val pendingManualPayments by viewModel.pendingManualPayments.collectAsStateWithLifecycle()
    val scanLogs by viewModel.scanLogs.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()

    val currentOrg = organizations.find { it.id == selectedOrgId } ?: organizations.first()

    // Handle Back Button
    BackHandler(enabled = isWizardOpen || eventInStudio != null || currentTab != AppNavTab.DISCOVERY) {
        when {
            isWizardOpen -> isWizardOpen = false
            eventInStudio != null -> eventInStudio = null
            currentTab != AppNavTab.DISCOVERY -> currentTab = AppNavTab.DISCOVERY
        }
    }

    if (isWizardOpen) {
        EventWizardScreen(
            initialConcept = wizardConceptToUse,
            onEventCreated = {
                isWizardOpen = false
                wizardConceptToUse = null
                currentTab = AppNavTab.DASHBOARD
            },
            onCancel = {
                isWizardOpen = false
                wizardConceptToUse = null
            },
            createEventInRepo = { title, sub, desc, cat, venue, addr, city, sDate, eDate, theme, tiers ->
                viewModel.createEvent(title, sub, desc, cat, venue, addr, city, sDate, eDate, theme, tiers)
            }
        )
        return
    }

    if (eventInStudio != null) {
        EventStudioScreen(
            event = eventInStudio!!,
            onSaveTheme = { newTheme ->
                viewModel.updateEventTheme(eventInStudio!!.id, newTheme)
                eventInStudio = null
            },
            onClose = { eventInStudio = null }
        )
        return
    }

    @Composable
    fun ScreenContent() {
        when (currentTab) {
            AppNavTab.DISCOVERY -> {
                DiscoveryScreen(
                    events = events,
                    ticketTypes = ticketTypes,
                    onPurchaseRequested = { ev, tType, qty, name, email, phone, provider, isManualQr, ref ->
                        val result = viewModel.processTicketPayment(ev, tType, qty, name, email, phone, provider, isManualQr, ref)
                        when (result) {
                            is com.example.data.payment.PaymentGatewayResult.Success -> Pair(true, result.message)
                            is com.example.data.payment.PaymentGatewayResult.PendingVerification -> Pair(true, result.instructions)
                            is com.example.data.payment.PaymentGatewayResult.Failure -> Pair(false, result.errorMessage)
                        }
                    },
                    onNavigateToWallet = {
                        currentTab = AppNavTab.WALLET
                    }
                )
            }
            AppNavTab.WALLET -> {
                WalletScreen(
                    tickets = tickets,
                    onTransferTicket = { tktId, name, phone, email ->
                        viewModel.transferTicket(tktId, name, phone, email)
                    }
                )
            }
            AppNavTab.DASHBOARD -> {
                OrganizerDashboardScreen(
                    events = events,
                    orders = orders,
                    tickets = tickets,
                    pendingProofs = pendingManualPayments,
                    onApproveProof = { proofId ->
                        viewModel.approveManualPayment(proofId)
                    },
                    onRejectProof = { proofId, reason ->
                        viewModel.rejectManualPayment(proofId, reason)
                    },
                    onOpenWizard = {
                        wizardConceptToUse = null
                        isWizardOpen = true
                    },
                    onOpenStudio = { ev ->
                        eventInStudio = ev
                    }
                )
            }
            AppNavTab.SCANNER -> {
                ScannerScreen(
                    isOffline = isOfflineMode,
                    unsyncedCount = unsyncedCount,
                    scanLogs = scanLogs,
                    onToggleOffline = { viewModel.toggleOfflineMode(it) },
                    onValidateScan = { token, gate ->
                        viewModel.validateTicketScan(token, gate)
                    },
                    onSyncNow = {
                        val count = viewModel.syncOfflineScans()
                        Toast.makeText(context, "$count scan(s) synchronisés avec succès !", Toast.LENGTH_SHORT).show()
                        count
                    }
                )
            }
            AppNavTab.AI_STUDIO -> {
                BaobabAiScreen(
                    geminiService = geminiService,
                    onApplyConceptToWizard = { concept ->
                        wizardConceptToUse = concept
                        isWizardOpen = true
                    }
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 720.dp

        if (isDesktop) {
            // --- DESKTOP / ORDINATEUR WORKSTATION LAYOUT ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GraphiteBlack)
            ) {
                NavigationRail(
                    containerColor = DeepEmerald,
                    contentColor = IvoryWhite,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ChampagneGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = GraphiteBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "BAOBAB",
                                color = ChampagneGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AppNavTab.values().forEach { tab ->
                        val isSelected = (currentTab == tab)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = GraphiteBlack,
                                selectedTextColor = ChampagneGold,
                                indicatorColor = ChampagneGold,
                                unselectedIconColor = Color(0xFFA5B8AD),
                                unselectedTextColor = Color(0xFFA5B8AD)
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Role & Org Switcher shortcut on Desktop Rail
                    IconButton(
                        onClick = { showOrgSwitcher = true },
                        modifier = Modifier.testTag("desktop_org_switcher")
                    ) {
                        Icon(Icons.Default.Business, contentDescription = "Changer d'organisation", tint = ChampagneGold)
                    }
                    IconButton(
                        onClick = { showRoleSwitcher = true },
                        modifier = Modifier.testTag("desktop_role_switcher")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Changer de rôle", tint = IvoryWhite)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Desktop Main Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    BaobabHeader(
                        currentOrgName = currentOrg.name,
                        currentUserRole = currentUser.role,
                        isOffline = isOfflineMode,
                        onSwitchOrgClick = { showOrgSwitcher = true },
                        onRoleBadgeClick = { showRoleSwitcher = true }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        ScreenContent()
                    }
                }
            }
        } else {
            // --- MOBILE LAYOUT ---
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = GraphiteBlack,
                topBar = {
                    BaobabHeader(
                        currentOrgName = currentOrg.name,
                        currentUserRole = currentUser.role,
                        isOffline = isOfflineMode,
                        onSwitchOrgClick = { showOrgSwitcher = true },
                        onRoleBadgeClick = { showRoleSwitcher = true }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = DarkSurfaceElevated,
                        contentColor = IvoryWhite,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppNavTab.values().forEach { tab ->
                            val isSelected = (currentTab == tab)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GraphiteBlack,
                                    selectedTextColor = ChampagneGold,
                                    indicatorColor = ChampagneGold,
                                    unselectedIconColor = Color(0xFFA5B8AD),
                                    unselectedTextColor = Color(0xFFA5B8AD)
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenContent()
                }
            }
        }
    }

    // Organization Switcher Modal (Multi-tenant)
    if (showOrgSwitcher) {
        AlertDialog(
            onDismissRequest = { showOrgSwitcher = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Changer d'Organisation", color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "BaobabTicket isole strictement les données et événements de chaque organisation.",
                        color = Color(0xFFA5B8AD),
                        fontSize = 12.sp
                    )

                    organizations.forEach { org ->
                        val isSelected = (org.id == selectedOrgId)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.switchOrganization(org.id)
                                    showOrgSwitcher = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DeepEmerald else DarkSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF384E42))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(org.name, color = IvoryWhite, fontWeight = FontWeight.Bold)
                                    Text(org.address, color = Color(0xFFA5B8AD), fontSize = 11.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ChampagneGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrgSwitcher = false }) {
                    Text("Fermer", color = ChampagneGold)
                }
            }
        )
    }

    // Role Switcher Modal (Role-Based Access Control)
    if (showRoleSwitcher) {
        AlertDialog(
            onDismissRequest = { showRoleSwitcher = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Changer de Rôle Utilisateur", color = IvoryWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Testez l'expérience selon votre rôle (RBAC) :",
                        color = Color(0xFFA5B8AD),
                        fontSize = 12.sp
                    )

                    val demoRoles = listOf(
                        UserRole.ORGANIZATION_OWNER,
                        UserRole.SCANNER,
                        UserRole.ADMIN,
                        UserRole.CUSTOMER
                    )

                    demoRoles.forEach { role ->
                        val isSelected = (role == currentUser.role)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.switchRole(role)
                                    showRoleSwitcher = false
                                    // Auto switch tab to role focus
                                    if (role == UserRole.SCANNER) currentTab = AppNavTab.SCANNER
                                    if (role == UserRole.CUSTOMER) currentTab = AppNavTab.WALLET
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DeepEmerald else DarkSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ChampagneGold else Color(0xFF384E42))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(role.label, color = IvoryWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ChampagneGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitcher = false }) {
                    Text("Fermer", color = ChampagneGold)
                }
            }
        )
    }
}
