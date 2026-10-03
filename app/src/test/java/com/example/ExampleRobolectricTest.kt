package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BaobabRoomDatabase
import com.example.data.local.EventEntity
import com.example.data.local.PurchasedTicketEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ScanResultType
import com.example.data.repository.BaobabRepository
import com.example.ui.viewmodel.BaobabViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BaobabTicket", appName)
    }

    @Test
    fun `test Room database entities for events and purchased tickets`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = BaobabRoomDatabase.getDatabase(context)

        // Insert event into Room database with name, date, and price
        val testEvent = EventEntity(
            id = "test-evt-001",
            name = "Gala de Thiès Prestige",
            date = "15 Décembre 2026",
            priceXof = 30000.0,
            city = "Thiès",
            venueName = "Hôtel du Rail"
        )
        db.eventDao().insertEvent(testEvent)

        val retrievedEvent = db.eventDao().getEventById("test-evt-001")
        assertNotNull(retrievedEvent)
        assertEquals("Gala de Thiès Prestige", retrievedEvent?.name)
        assertEquals("15 Décembre 2026", retrievedEvent?.date)
        assertEquals(30000.0, retrievedEvent?.priceXof ?: 0.0, 0.01)

        // Insert purchased ticket into Room database with event name, date, and price
        val testTicket = PurchasedTicketEntity(
            ticketNumber = "BT-2026-TEST1234",
            eventId = "test-evt-001",
            eventName = "Gala de Thiès Prestige",
            eventDate = "15 Décembre 2026",
            priceXof = 30000.0,
            ticketTypeName = "VIP",
            attendeeName = "Fatou Dieng",
            attendeePhone = "+221 77 999 88 77",
            qrSecurityToken = "BAOBAB-SECURE-TEST-TOKEN"
        )
        db.ticketDao().insertTicket(testTicket)

        val retrievedTicket = db.ticketDao().getTicketByNumber("BT-2026-TEST1234")
        assertNotNull(retrievedTicket)
        assertEquals("Gala de Thiès Prestige", retrievedTicket?.eventName)
        assertEquals("15 Décembre 2026", retrievedTicket?.eventDate)
        assertEquals(30000.0, retrievedTicket?.priceXof ?: 0.0, 0.01)
        assertEquals("Fatou Dieng", retrievedTicket?.attendeeName)
    }

    @Test
    fun `test ViewModel ticket purchase and scan validation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = BaobabRepository(context)
        val viewModel = BaobabViewModel(repository)

        val event = viewModel.events.value.first()
        val ticketType = viewModel.ticketTypes.value.first { it.eventId == event.id }

        // Test purchase via ViewModel
        val purchaseResult = viewModel.purchaseTicket(
            event = event,
            ticketType = ticketType,
            quantity = 1,
            customerName = "Amadou Sall",
            customerEmail = "amadou@example.sn",
            customerPhone = "+221 77 111 22 33",
            paymentMethod = PaymentMethod.WAVE_API
        )
        assertTrue(purchaseResult.first)

        val newTicket = viewModel.purchasedTickets.value.first { it.attendeeName == "Amadou Sall" }
        assertNotNull(newTicket)

        // First scan: should be VALID
        val scan1 = viewModel.validateTicketScan(newTicket.qrSecurityToken, "Porte Principale")
        assertEquals(ScanResultType.VALID, scan1.type)

        // Second scan: anti-fraud should detect USED
        val scan2 = viewModel.validateTicketScan(newTicket.qrSecurityToken, "Porte Principale")
        assertEquals(ScanResultType.USED, scan2.type)
    }

    @Test
    fun `test PaymentService with Wave and Orange Money`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = BaobabRepository(context)
        val viewModel = BaobabViewModel(repository)

        val event = viewModel.events.value.first()
        val ticketType = viewModel.ticketTypes.value.first { it.eventId == event.id }

        // 1. Test Wave direct payment
        val waveResult = viewModel.processTicketPayment(
            event = event,
            ticketType = ticketType,
            quantity = 1,
            customerName = "Cheikh Ndiaye",
            customerEmail = "cheikh@wave.sn",
            customerPhone = "+221 77 456 78 90",
            provider = com.example.data.payment.PaymentProvider.WAVE,
            isManualQr = false,
            transactionRef = ""
        )
        assertTrue(waveResult is com.example.data.payment.PaymentGatewayResult.Success)

        // 2. Test Orange Money direct payment
        val omResult = viewModel.processTicketPayment(
            event = event,
            ticketType = ticketType,
            quantity = 1,
            customerName = "Aissatou Ba",
            customerEmail = "aissatou@om.sn",
            customerPhone = "+221 78 123 45 67",
            provider = com.example.data.payment.PaymentProvider.ORANGE_MONEY,
            isManualQr = false,
            transactionRef = ""
        )
        assertTrue(omResult is com.example.data.payment.PaymentGatewayResult.Success)

        // 3. Test Wave QR manual proof mode
        val qrResult = viewModel.processTicketPayment(
            event = event,
            ticketType = ticketType,
            quantity = 1,
            customerName = "Ousmane Kane",
            customerEmail = "ousmane@gmail.com",
            customerPhone = "+221 76 999 00 11",
            provider = com.example.data.payment.PaymentProvider.WAVE,
            isManualQr = true,
            transactionRef = "WV-SMS-12345"
        )
        assertTrue(qrResult is com.example.data.payment.PaymentGatewayResult.PendingVerification)
    }
}
