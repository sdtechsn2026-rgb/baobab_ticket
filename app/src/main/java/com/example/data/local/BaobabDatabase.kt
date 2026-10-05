package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entity: Events ---
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String, // Nom de l'événement
    val date: String, // Date de l'événement
    val priceXof: Double, // Prix de base ou référence
    val category: String = "Gala & Soirée",
    val venueName: String = "Dakar, Sénégal",
    val city: String = "Dakar",
    val totalCapacity: Int = 1000,
    val soldCount: Int = 0,
    val themeTemplate: String = "LUXURY"
)

// --- Room Entity: Purchased Tickets ---
@Entity(tableName = "purchased_tickets")
data class PurchasedTicketEntity(
    @PrimaryKey val ticketNumber: String,
    val eventId: String,
    val eventName: String, // Nom de l'événement
    val eventDate: String, // Date de l'événement
    val priceXof: Double, // Prix payé en FCFA
    val ticketTypeName: String, // STANDARD, VIP, VVIP
    val attendeeName: String,
    val attendeePhone: String,
    val attendeeEmail: String = "",
    val qrSecurityToken: String,
    val gate: String = "Porte Principale",
    val seatZone: String = "Accès Libre",
    val status: String = "ACTIVE", // ACTIVE, USED, TRANSFERRED
    val paymentMethod: String = "WAVE_API",
    val purchasedAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null,
    val usedGate: String? = null
)

// --- DAO for Events ---
@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: String)
}

// --- DAO for Purchased Tickets ---
@Dao
interface PurchasedTicketDao {
    @Query("SELECT * FROM purchased_tickets ORDER BY purchasedAt DESC")
    fun getAllPurchasedTickets(): Flow<List<PurchasedTicketEntity>>

    @Query("SELECT * FROM purchased_tickets WHERE eventId = :eventId ORDER BY purchasedAt DESC")
    fun getTicketsByEvent(eventId: String): Flow<List<PurchasedTicketEntity>>

    @Query("SELECT * FROM purchased_tickets WHERE ticketNumber = :ticketNumber LIMIT 1")
    suspend fun getTicketByNumber(ticketNumber: String): PurchasedTicketEntity?

    @Query("SELECT * FROM purchased_tickets WHERE qrSecurityToken = :token LIMIT 1")
    suspend fun getTicketByQrToken(token: String): PurchasedTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: PurchasedTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<PurchasedTicketEntity>)

    @Update
    suspend fun updateTicket(ticket: PurchasedTicketEntity)

    @Query("UPDATE purchased_tickets SET status = :status, usedAt = :usedAt, usedGate = :gate WHERE ticketNumber = :ticketNumber")
    suspend fun updateTicketStatus(ticketNumber: String, status: String, usedAt: Long, gate: String)
}

// --- Offline Scan Log Entity & DAO (for access control & offline sync) ---
@Entity(tableName = "offline_scan_logs")
data class OfflineScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketNumber: String,
    val eventId: String,
    val attendeeName: String,
    val status: String,
    val gate: String,
    val timestamp: Long,
    val isSynced: Boolean = false
)

@Dao
interface OfflineScanLogDao {
    @Insert
    suspend fun insert(log: OfflineScanLogEntity)

    @Query("SELECT * FROM offline_scan_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<OfflineScanLogEntity>

    @Query("SELECT * FROM offline_scan_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<OfflineScanLogEntity>

    @Query("UPDATE offline_scan_logs SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllSynced()

    @Query("SELECT COUNT(*) FROM offline_scan_logs WHERE isSynced = 0")
    suspend fun countUnsynced(): Int
}

// --- Room Entity: User Session & Authentication ---
@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey val id: String, // User UID
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "ORGANIZATION_OWNER",
    val organizationId: String = "org-baobab-events",
    val token: String = "",
    val isLoggedIn: Boolean = true,
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveSession(): Flow<UserSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity)

    @Query("UPDATE user_sessions SET isLoggedIn = 0")
    suspend fun clearActiveSessions()

    @Query("DELETE FROM user_sessions")
    suspend fun deleteAll()
}

// --- Database Configuration ---
@Database(
    entities = [
        EventEntity::class,
        PurchasedTicketEntity::class,
        OfflineScanLogEntity::class,
        UserSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BaobabRoomDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun ticketDao(): PurchasedTicketDao
    abstract fun scanLogDao(): OfflineScanLogDao
    abstract fun userSessionDao(): UserSessionDao

    companion object {
        @Volatile
        private var INSTANCE: BaobabRoomDatabase? = null

        fun getDatabase(context: Context): BaobabRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BaobabRoomDatabase::class.java,
                    "baobab_platform.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
