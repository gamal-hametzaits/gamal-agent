package dev.hametzaits.gamal

import android.app.Application
import android.content.Context
import dev.hametzaits.gamal.data.GamalStore

class GamalApp : Application() {
    val store: GamalStore by lazy { GamalStore.get(this) }

    companion object {
        fun prefs(context: Context) =
            context.getSharedPreferences("gamal_prefs", Context.MODE_PRIVATE)

        const val PREF_LEARN_NOTIFICATIONS = "learn_notifications"
    }
}
