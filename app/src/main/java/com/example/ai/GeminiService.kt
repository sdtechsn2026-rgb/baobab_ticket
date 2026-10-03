package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val MODEL_PRO = "gemini-3.1-pro-preview"
        const val MODEL_FLASH = "gemini-3.5-flash"
        const val MODEL_LITE = "gemini-3.1-flash-lite-preview"

        private const val SYSTEM_PROMPT = """
Tu es Baobab AI, l'expert officiel de la plateforme SaaS BaobabTicket en Afrique (Sénégal, Côte d'Ivoire, etc.).
Tu conseilles les organisateurs d'événements, promoteurs, directeurs artistiques et régisseurs.
Tes expertises :
- Fixation des prix en Francs CFA (XOF) pour billets Standard, VIP, VVIP
- Stratégie de vente et préventes en ligne
- Intégration des paiements mobiles (Wave Sénégal, Orange Money)
- Contrôle d'accès, fluidification du scan QR aux portes VIP et grand public
- Rédaction d'annonces marketing percutantes pour WhatsApp, Instagram, LinkedIn
- Convivialité, élégance, prestige et professionnalisme.
Réponds toujours en français, avec un ton chaleureux, sophistiqué et orienté résultats.
"""
    }

    /**
     * Multi-turn chat generation with history
     */
    suspend fun sendChatMessage(
        conversationHistory: List<ChatMessage>,
        userMessage: String,
        modelName: String = MODEL_FLASH
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getSmartFallbackChatResponse(userMessage)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Include past turns (up to last 10 messages for context)
            val recentHistory = conversationHistory.takeLast(10)
            for (msg in recentHistory) {
                val turnObj = JSONObject()
                turnObj.put("role", if (msg.role == "user") "user" else "model")
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.content)
                partsArray.put(partObj)
                turnObj.put("parts", partsArray)
                contentsArray.put(turnObj)
            }

            // Current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            val currentPart = JSONObject()
            currentPart.put("text", userMessage)
            currentParts.put(currentPart)
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            // System Instruction
            val systemInstructionObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", SYSTEM_PROMPT))
            systemInstructionObj.put("parts", sysParts)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)
            rootJson.put("systemInstruction", systemInstructionObj)
            rootJson.put("generationConfig", genConfig)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext getSmartFallbackChatResponse(userMessage)
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                getSmartFallbackChatResponse(userMessage)
            }
        } catch (e: Exception) {
            getSmartFallbackChatResponse(userMessage)
        }
    }

    /**
     * "Créer avec Baobab AI" - Generate complete event concept & ticketing tiers
     */
    suspend fun generateEventConcept(prompt: String): EventConceptAiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackConcept(prompt)
        }

        try {
            val systemPrompt = """
Tu es l'assistant de conception événementielle de BaobabTicket.
À partir de la demande de l'utilisateur, conçois un événement complet au format JSON STRICT :
{
  "title": "Nom accrocheur et élégant de l'événement",
  "subtitle": "Slogan captivant",
  "description": "Description commerciale engageante et immersive (3-4 paragraphes)",
  "category": "Gala & Soirée / Conférence & Tech / Festival / Sport",
  "suggestedVenue": "Lieu prestigieux suggéré à Dakar ou région",
  "palette": "Élégance Dorée & Noir / Afro Moderne Émeraude / Sahel Vibe & Sunset / Dakar Tech Minimal",
  "standardPrice": 25000,
  "vipPrice": 60000,
  "vvipPrice": 150000,
  "standardPerks": ["Accès Cocktail", "Salle Principale"],
  "vipPerks": ["Dîner Gastronomique", "Table Réservée", "Coupe Champagne", "Entrée Fast Track VIP"],
  "vvipPerks": ["Table d'honneur privée", "Lounge VIP Exclusif", "Bar Prestige", "Service Majordome"]
}
Réponds UNIQUEMENT par le JSON sans backticks markdown.
"""
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_PRO:generateContent?key=$apiKey"

            val rootJson = JSONObject()
            val contents = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            turn.put("parts", JSONArray().put(JSONObject().put("text", "Brief : $prompt")))
            contents.put(turn)

            val sys = JSONObject()
            sys.put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            rootJson.put("contents", contents)
            rootJson.put("systemInstruction", sys)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(responseString)
                val rawText = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleanJson)

                return@withContext EventConceptAiResponse(
                    title = parsed.optString("title", "Gala Baobab Prestige"),
                    subtitle = parsed.optString("subtitle", "Soirée d'Exception"),
                    description = parsed.optString("description", "Une expérience inoubliable."),
                    category = parsed.optString("category", "Gala & Soirée"),
                    suggestedVenue = parsed.optString("suggestedVenue", "Hôtel Terrou-Bi, Dakar"),
                    palette = parsed.optString("palette", "Élégance Dorée & Noir"),
                    standardPrice = parsed.optDouble("standardPrice", 25000.0),
                    vipPrice = parsed.optDouble("vipPrice", 60000.0),
                    vvipPrice = parsed.optDouble("vvipPrice", 150000.0),
                    standardPerks = jsonArrayToList(parsed.optJSONArray("standardPerks")),
                    vipPerks = jsonArrayToList(parsed.optJSONArray("vipPerks")),
                    vvipPerks = jsonArrayToList(parsed.optJSONArray("vvipPerks"))
                )
            }
        } catch (_: Exception) {}

        return@withContext generateFallbackConcept(prompt)
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.optString(i))
        }
        return list
    }

    private fun generateFallbackConcept(prompt: String): EventConceptAiResponse {
        val lower = prompt.lowercase()
        return when {
            lower.contains("tech") || lower.contains("ia") || lower.contains("summit") -> {
                EventConceptAiResponse(
                    title = "Dakar AI & Innovation Forum",
                    subtitle = "Bâtir l'écosystème technologique africain",
                    description = "Le rendez-vous incontournable des décideurs tech, investisseurs et développeurs. Deux jours de conférences, hackathons et démonstrations des meilleures solutions d'IA développées sur le continent.",
                    category = "Conférence & Tech",
                    suggestedVenue = "CICAD, Diamniadio, Dakar",
                    palette = "Dakar Tech Minimal",
                    standardPrice = 15000.0,
                    vipPrice = 45000.0,
                    vvipPrice = 120000.0,
                    standardPerks = listOf("Accès Keynotes", "Zone Startup Expo", "Badge digital"),
                    vipPerks = listOf("Déjeuners Networking", "Ateliers Experts", "Accès Lounge VIP"),
                    vvipPerks = listOf("Dîner Exclusif avec les Speakers", "Cocktail Privé Ambassadeurs", "Siège Réservé Premier Rang")
                )
            }
            lower.contains("concert") || lower.contains("festival") || lower.contains("musique") -> {
                EventConceptAiResponse(
                    title = "Teranga Beats Festival",
                    subtitle = "Le son d'Afrique au rythme de Dakar",
                    description = "Une célébration vibrante de musiques urbaines, mbalax moderne et afro-électro sur le littoral de Dakar. Performances live d'artistes internationaux, scénographie lumineuse immersive et stands gastronomiques.",
                    category = "Festival & Concert",
                    suggestedVenue = "Esplanade Monument Renaissance, Ouakam",
                    palette = "Sahel Vibe & Sunset",
                    standardPrice = 10000.0,
                    vipPrice = 30000.0,
                    vvipPrice = 75000.0,
                    standardPerks = listOf("Accès Fosse & Concerts", "Zone Food Court"),
                    vipPerks = listOf("Tribune Surélevée VIP", "Bar Dédié", "Entrée Coupe-file"),
                    vvipPerks = listOf("Espace Canapé Carré d'Or", "Bouteille Offerte", "Accès Backstage & Photo Call")
                )
            }
            else -> {
                EventConceptAiResponse(
                    title = "Soirée Prestige d'Émeraude & Or",
                    subtitle = "Célébration d'excellence & Réseau d'élite",
                    description = "Une soirée de gala grandiose réunissant les personnalités les plus influentes. Accueil sur tapis rouge, dîner gastronomique en cinq temps, ventes aux enchères caritatives et grand concert symphonique en bord d'océan.",
                    category = "Gala & Soirée",
                    suggestedVenue = "Radisson Blu Dakar Sea Plaza / Terrou-Bi",
                    palette = "Élégance Dorée & Noir",
                    standardPrice = 30000.0,
                    vipPrice = 75000.0,
                    vvipPrice = 175000.0,
                    standardPerks = listOf("Cocktail de bienvenue", "Placement Salle de Réception", "Cadeau de bienvenue"),
                    vipPerks = listOf("Dîner Gastronomique 5 Services", "Champagne à discrétion", "Table Prestige Zone A"),
                    vvipPerks = listOf("Table d'Honneur Majordome", "Lounge Privatif avec Cave Sélectionnée", "Accès Salon Privé et Escorte Dédiée")
                )
            }
        }
    }

    private fun getSmartFallbackChatResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("prix") || lower.contains("billet") || lower.contains("tarif") -> {
                """
Pour maximiser votre chiffre d'affaires sur le marché sénégalais et ouest-africain, nous préconisons une stratégie en 3 paliers :

1. **Billet Standard (20 000 - 30 000 FCFA)** : Représente 60% de votre volume. Donne accès à la soirée et au cocktail.
2. **Billet VIP (50 000 - 80 000 FCFA)** : Représente 30% des ventes. Inclut dîner assis, table réservée et coupe-file à l'entrée.
3. **Billet VVIP / Table Carré d'Or (120 000 - 200 000 FCFA)** : Représente 10% mais génère une forte marge. Comprend service majordome, bar prestige et accès salon d'honneur.

💡 *Astuce BaobabTicket* : Activez le paiement Wave et Orange Money avec le QR manuel pour capter les clients qui préfèrent payer directement depuis leur mobile.
"""
            }
            lower.contains("wave") || lower.contains("orange") || lower.contains("paiement") -> {
                """
BaobabTicket intègre une architecture de paiement double, parfaitement adaptée au Sénégal :

- **Mode API Direct** : Idéal si vous possédez un compte marchand Wave Business ou Orange Money Partenaire. Le paiement est validé en temps réel et le billet est émis instantanément.
- **Mode QR Manuel (Mode B)** : Sans frais d'API ! Vous téléversez votre QR personnel Wave ou OM. Le client le scanne, envoie le montant et saisit son ID de transaction. Vous validez en un clic dans votre Dashboard et le ticket est généré automatiquement.

Aucun billet frauduleux ne peut être généré sans validation certifiée.
"""
            }
            lower.contains("scan") || lower.contains("porte") || lower.contains("entrée") || lower.contains("contrôle") -> {
                """
Pour fluidifier le contrôle d'accès le jour J :

1. **Séparez vos portes d'accès** : Définissez au moins une "Porte Principale" (Standard) et un "Accès VIP" dédié.
2. **Mode Hors-Ligne BaobabTicket** : Vos agents de sécurité peuvent continuer à scanner même si le réseau 4G de la Corniche ou du Terrou-Bi est saturé. Les billets scannés sont enregistrés localement dans Room et synchronisés dès le retour de la connexion.
3. **Sécurité Anti-Replay** : Chaque token QR est à usage unique. Tout ticket déjà validé affiche instantanément une alerte sonore et visuelle rouge avec l'heure exacte du premier passage.
"""
            }
            lower.contains("marketing") || lower.contains("communication") || lower.contains("instagram") || lower.contains("whatsapp") -> {
                """
Voici une trame percutante pour votre diffusion WhatsApp & Instagram :

🌟 **[ANNONCE OFFICIELLE] BAOBAB LUXURY GALA 2026** 🌟
Rejoignez les personnalités d'élite pour la soirée la plus attendue de l'année au Terrou-Bi Dakar.

✨ Dîner gastronomique, défilé haute couture & prestige live.
🎟️ Réservez vos billets certifiés dès maintenant sur BaobabTicket :
💳 Paiement direct par Wave & Orange Money
👉 Lien officiel : baobabticket.sn/events/gala-2026

*Places limitées en zone VIP et VVIP.*
"""
            }
            else -> {
                """
Bonjour ! Je suis **Baobab AI**, votre conseiller stratégique pour réussir vos événements avec BaobabTicket.

Je peux vous accompagner sur :
- **La création d'événements & génération de thèmes** avec notre studio visuel
- **La configuration des paiements Wave et Orange Money** (API & QR Manuel)
- **L'optimisation des tarifs en FCFA** pour maximiser vos ventes
- **L'organisation des scanners aux portes d'accès** avec synchronisation hors-ligne

Quelle facette de votre billetterie souhaitez-vous optimiser aujourd'hui ?
"""
            }
        }
    }
}

data class EventConceptAiResponse(
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val suggestedVenue: String,
    val palette: String,
    val standardPrice: Double,
    val vipPrice: Double,
    val vvipPrice: Double,
    val standardPerks: List<String>,
    val vipPerks: List<String>,
    val vvipPerks: List<String>
)
