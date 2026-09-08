package dev.hametzaits.gamal

import android.app.Application
import android.content.Context
import dev.hametzaits.gamal.data.AppDatabase

class GamalApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }

    companion object {
        fun prefs(context: Context) =
            context.getSharedPreferences("gamal_prefs", Context.MODE_PRIVATE)

        const val PREF_LEARN_NOTIFICATIONS = "learn_notifications"
    }
}
