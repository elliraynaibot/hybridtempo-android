package com.hybridtempo.android.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseHybridTempoRepository(
    private val context: Context,
) : HybridTempoRepository {
    private val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    override suspend fun currentProfile(): AthleteProfile? {
        if (!isConfigured) return null
        val userId = requireUserId()
        val document = firestore()
            .collection("users")
            .document(userId)
            .get()
            .await()

        if (!document.exists()) return null

        return AthleteProfile(
            name = document.getString("name").orEmpty(),
            raceDate = document.getString("raceDate").orEmpty(),
            trainingStyle = document.getString("trainingStyle") ?: "Hybrid",
            weeklyTrainingFrequency = document.getLong("weeklyTrainingFrequency")?.toInt() ?: 5,
            goals = document.get("goals").asStringList().ifEmpty { listOf("recovery", "race prep") },
            preferredSessionLength = document.getLong("preferredSessionLength")?.toInt() ?: 5,
        )
    }

    override suspend fun upsertProfile(profile: AthleteProfile): SaveResult = withUserDocument { userId ->
        firestore()
            .collection("users")
            .document(userId)
            .set(profile.toFirestoreMap())
            .await()
        SaveResult(persisted = true, message = "Profile saved to Firestore.")
    }

    override suspend fun saveCheckIn(
        checkIn: DailyCheckIn,
        recommendation: BreathworkRecommendation,
    ): SaveResult = withUserDocument { userId ->
        val payload = checkIn.toFirestoreMap() + mapOf(
            "recommendation" to recommendation.toFirestoreMap(),
        )

        firestore()
            .collection("users")
            .document(userId)
            .collection("checkins")
            .document(checkIn.date)
            .set(payload)
            .await()
        SaveResult(persisted = true, message = "Check-in saved to Firestore.")
    }

    override suspend fun completeSession(session: BreathworkSession): SaveResult = withUserDocument { userId ->
        val document = firestore()
            .collection("users")
            .document(userId)
            .collection("sessions")
            .document()

        document
            .set(session.copy(id = document.id).toFirestoreMap())
            .await()
        SaveResult(persisted = true, message = "Session saved to Firestore.")
    }

    override suspend fun recentSessions(limit: Long): List<BreathworkSession> {
        if (!isConfigured) return emptyList()
        val userId = currentUserId() ?: return emptyList()

        return firestore()
            .collection("users")
            .document(userId)
            .collection("sessions")
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val protocol = document.getString("protocol") ?: return@mapNotNull null
                BreathworkSession(
                    id = document.getString("id").orEmpty(),
                    protocol = protocol,
                    durationMinutes = document.getLong("durationMinutes")?.toInt() ?: 0,
                    cadence = document.getString("cadence").orEmpty(),
                    completed = document.getBoolean("completed") ?: false,
                    completedAt = document.getString("completedAt").orEmpty(),
                )
            }
    }

    private suspend fun <T> withUserDocument(block: suspend (String) -> T): T {
        if (!isConfigured) {
            throw FirebaseUnavailableException("Add app/google-services.json to enable Firebase persistence.")
        }

        return block(requireUserId())
    }

    private suspend fun requireUserId(): String {
        currentUserId()?.let { return it }
        return FirebaseAuth.getInstance().signInAnonymously().await().user?.uid
            ?: throw FirebaseUnavailableException("Firebase Auth did not return a user.")
    }

    private fun currentUserId(): String? {
        if (!isConfigured) return null
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

class FirebaseUnavailableException(message: String) : IllegalStateException(message)

private fun AthleteProfile.toFirestoreMap(): Map<String, Any> = mapOf(
    "name" to name,
    "raceDate" to raceDate,
    "trainingStyle" to trainingStyle,
    "weeklyTrainingFrequency" to weeklyTrainingFrequency,
    "goals" to goals,
    "preferredSessionLength" to preferredSessionLength,
)

private fun DailyCheckIn.toFirestoreMap(): Map<String, Any> = mapOf(
    "date" to date,
    "energy" to energy,
    "soreness" to soreness,
    "stress" to stress,
    "mood" to mood,
    "timeAvailable" to timeAvailable,
    "workoutType" to workoutType,
    "workoutDurationMinutes" to workoutDurationMinutes,
    "workoutIntensity" to workoutIntensity,
    "createdAt" to createdAt,
)

private fun BreathworkRecommendation.toFirestoreMap(): Map<String, Any> = mapOf(
    "protocol" to protocol,
    "durationMinutes" to durationMinutes,
    "rationale" to rationale,
    "cadence" to cadence,
)

private fun BreathworkSession.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "protocol" to protocol,
    "durationMinutes" to durationMinutes,
    "cadence" to cadence,
    "completed" to completed,
    "completedAt" to completedAt,
)

private fun Any?.asStringList(): List<String> = (this as? List<*>)
    ?.filterIsInstance<String>()
    .orEmpty()
