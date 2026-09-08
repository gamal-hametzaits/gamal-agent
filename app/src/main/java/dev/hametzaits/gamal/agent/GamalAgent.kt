package dev.hametzaits.gamal.agent

import dev.hametzaits.gamal.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The on-device brain, v0. Fully local, rule-based responder that reads the
 * local preference profile (built from thumbs feedback) and the local
 * notification archive. No network calls, no external model.
 * See README: this is the honest foundation for a future RL loop.
 */
class GamalAgent(private val db: AppDatabase) {

    private val greetings = listOf("היי", "שלום", "אהלן", "מה נשמע", "מה קורה", "בוקר טוב", "ערב טוב")
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("d בMMMM yyyy", Locale("he"))

    suspend fun respond(rawInput: String): String {
        val input = rawInput.trim()
        val prefs = db.preferenceDao().all().associate { it.key to it.score }
        val verbosity = prefs["style:verbosity"] ?: 0.0

        val likedTopic = prefs.entries
            .filter { it.key.startsWith("kw:") && it.value >= 2.0 }
            .map { it.key.removePrefix("kw:") }
            .firstOrNull { input.contains(it) }

        val body = when {
            input.isEmpty() -> return "לא קיבלתי טקסט. נסה שוב?"
            greetings.any { input.contains(it) } && input.length < 25 ->
                "היי! 🐪 שמח לראות אותך. במה אעזור היום?"
            input.contains("מי אתה") || input.contains("מה אתה") ->
                "אני הגמל - סוכן אישי שרץ כולו במכשיר שלך. בלי ענן, בלי מיקרופון. אני זוכר את השיחות שלנו במאגר מקומי, ולומד מהדירוגים שלך (👍/👎) איך אתה אוהב שאענה."
            input.contains("התראות") || input.contains("מסרים") || input.contains("מי כתב") || input.contains("מה קיבלתי") ->
                notificationSummary()
            input.contains("מה השעה") || input.contains("מה שעה") ->
                "השעה עכשיו ${timeFmt.format(Date())}."
            input.contains("מה התאריך") || input.contains("איזה יום") ->
                "היום ${dateFmt.format(Date())}."
            input.contains("תודה") || input.contains("אחלה") || input.contains("מעולה") ->
                "בשמחה 🐪 אם התשובות טובות - אגודל למעלה עוזר לי לזכור מה עובד."
            input.endsWith("?") || input.contains("איך") || input.contains("למה") || input.contains("מתי") ->
                reflectiveAnswer(input, verbosity, question = true)
            else -> reflectiveAnswer(input, verbosity, question = false)
        }

        return if (likedTopic != null) {
            "שמתי לב שהנושא הזה קרוב אליך. $body"
        } else body
    }

    private suspend fun notificationSummary(): String {
        val dayAgo = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val count = db.notificationDao().countSince(dayAgo)
        if (count == 0) {
            return "אין לי עדיין הרשאה לקרוא התראות, או שלא נכנסו התראות ביממה האחרונה. אפשר להפעיל גישה בהגדרות ← גישה להתראות. אני קורא התראות רק באישור מפורש שלך, והכול נשאר במכשיר."
        }
        val top = db.notificationDao().topAppSince(dayAgo)
        return if (top?.appName != null) {
            "ביממה האחרונה קלטתי $count התראות, בעיקר מ-${top.appName}. הכול שמור במאגר המקומי במכשיר שלך בלבד."
        } else {
            "ביממה האחרונה קלטתי $count התראות. הכול שמור במאגר המקומי במכשיר שלך בלבד."
        }
    }

    private fun reflectiveAnswer(input: String, verbosity: Double, question: Boolean): String {
        val short = when {
            question -> "שאלה טובה. בגרסה 0 אני עדיין סוכן מקומי פשוט - אין לי מודל שפה מלא במכשיר. אני זוכר את השאלה, וכשאדרג תשובות עם 👍/👎 אלמד מה מתאים לך."
            else -> "קלטתי: \"$input\". אני עוד לומד איך אתה אוהב שאענה - דרג אותי עם האגודלים ואשתפר."
        }
        if (verbosity <= -0.5) return short

        val extra = when {
            question -> "מה שכן אני יודע לעשות כבר עכשיו: לזכור כל שיחה במאגר המקומי, לסכם את ההתראות שנכנסו למכשיר (אחרי שתאשר גישה), ולהתאים את הסגנון שלי לפי הדירוגים שלך. תכונות עומק - תשובות אמיתיות ממודל - בתוכנית לגרסאות הבאות (ראה README)."
            else -> "מה שאני יודע לעשות כבר עכשיו: לזכור כל שיחה במאגר המקומי, לסכם את ההתראות שנכנסו למכשיר (אחרי שתאשר גישה), ולהתאים את הסגנון שלי לפי הדירוגים שלך."
        }
        return if (verbosity >= 0.5) "$short\n\n$extra" else short
    }
}
