package com.productivityos.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.productivityos.app.domain.model.AppUsage
import com.productivityos.app.domain.model.Insight
import com.productivityos.app.domain.model.InsightImpact
import com.productivityos.app.domain.model.ProductivityScore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ── Collection paths ──────────────────────────────────────
    // users/{userId}/scores/{date}
    // users/{userId}/usage/{date}
    // users/{userId}/insights/{date}

    private fun userDoc(userId: String) = firestore.collection("users").document(userId)

    // ── Score sync ────────────────────────────────────────────

    suspend fun syncScoreToFirestore(userId: String, score: ProductivityScore) {
        val date = score.date.toString()
        val data = mapOf(
            "value"           to score.value,
            "focusMinutes"    to score.focusMinutes,
            "distractMinutes" to score.distractMinutes,
            "deepSessions"    to score.deepSessions,
            "date"            to date,
            "syncedAt"        to System.currentTimeMillis()
        )
        userDoc(userId)
            .collection("scores")
            .document(date)
            .set(data, SetOptions.merge())
            .await()
    }

    // ── Usage sync ────────────────────────────────────────────

    suspend fun syncUsageToFirestore(userId: String, usage: List<AppUsage>) {
        val date = LocalDate.now().toString()
        val usageData = usage.map { app ->
            mapOf(
                "packageName"  to app.packageName,
                "appName"      to app.appName,
                "emoji"        to app.emoji,
                "totalMinutes" to app.totalMinutes,
                "scoreImpact"  to app.scoreImpact,
                "category"     to app.category.name
            )
        }
        val data = mapOf(
            "apps"     to usageData,
            "date"     to date,
            "syncedAt" to System.currentTimeMillis()
        )
        userDoc(userId)
            .collection("usage")
            .document(date)
            .set(data, SetOptions.merge())
            .await()
    }

    // ── Insights fetch ────────────────────────────────────────

    suspend fun fetchInsightsFromFirestore(userId: String): List<Insight> {
        val date = LocalDate.now().toString()
        val snapshot = userDoc(userId)
            .collection("insights")
            .document(date)
            .get()
            .await()

        if (!snapshot.exists()) return emptyList()

        @Suppress("UNCHECKED_CAST")
        val insightsList = snapshot.get("insights") as? List<Map<String, Any>> ?: return emptyList()

        return insightsList.mapNotNull { map ->
            runCatching {
                Insight(
                    id          = map["id"] as? String ?: return@mapNotNull null,
                    title       = map["title"] as? String ?: return@mapNotNull null,
                    description = map["description"] as? String ?: return@mapNotNull null,
                    impact      = InsightImpact.valueOf(map["impact"] as? String ?: "MEDIUM"),
                    emoji       = map["emoji"] as? String ?: "💡"
                )
            }.getOrNull()
        }
    }

    // ── Push insights to Firestore (called after InsightEngine runs) ──

    suspend fun pushInsightsToFirestore(userId: String, insights: List<Insight>) {
        val date = LocalDate.now().toString()
        val insightData = insights.map { insight ->
            mapOf(
                "id"          to insight.id,
                "title"       to insight.title,
                "description" to insight.description,
                "impact"      to insight.impact.name,
                "emoji"       to insight.emoji
            )
        }
        val data = mapOf(
            "insights" to insightData,
            "date"     to date,
            "syncedAt" to System.currentTimeMillis()
        )
        userDoc(userId)
            .collection("insights")
            .document(date)
            .set(data, SetOptions.merge())
            .await()
    }
}
