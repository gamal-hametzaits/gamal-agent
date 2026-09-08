package dev.hametzaits.gamal.agent

import dev.hametzaits.gamal.data.AppDatabase
import dev.hametzaits.gamal.data.MessageEntity
import dev.hametzaits.gamal.data.PreferenceEntity

/**
 * Feedback learning loop, v0. Thumbs up/down on an agent reply nudges a local
 * preference profile:
 *  - style:verbosity  -> did the user like long or short answers?
 *  - kw:<token>       -> topics from the user's own message that preceded a
 *                        liked/disliked reply.
 * Scores are clamped and stored on-device only. This is a transparent
 * heuristic - the honest foundation for real RL later (see README roadmap).
 */
class PreferenceLearner(private val db: AppDatabase) {

    suspend fun onFeedback(agentMessage: MessageEntity, rating: Int) {
        if (rating == 0) return
        val delta = if (rating > 0) 1.0 else -1.0

        // Style signal: was the rated reply long or short?
        val longReply = agentMessage.text.length > 160
        bump("style:verbosity", if (longReply) delta * 0.5 else -delta * 0.25, max = 3.0)

        // Topic signals from the user message that preceded this reply.
        val prevUser = db.messageDao().lastUserMessageBefore(agentMessage.timestamp) ?: return
        tokenize(prevUser.text).take(5).forEach { token ->
            bump("kw:$token", delta, max = 5.0)
        }
    }

    private suspend fun bump(key: String, delta: Double, max: Double) {
        val current = db.preferenceDao().score(key) ?: 0.0
        val next = (current + delta).coerceIn(-max, max)
        db.preferenceDao().upsert(PreferenceEntity(key, next))
    }

    private fun tokenize(text: String): List<String> =
        text.split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
}
