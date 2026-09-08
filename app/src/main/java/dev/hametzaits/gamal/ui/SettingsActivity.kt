package dev.hametzaits.gamal.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import dev.hametzaits.gamal.GamalApp
import dev.hametzaits.gamal.R
import dev.hametzaits.gamal.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var status: TextView
    private lateinit var stats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        db = (application as GamalApp).db
        status = findViewById(R.id.txtNotifStatus)
        stats = findViewById(R.id.txtStats)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnOpenNotifSettings).setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        val prefs = GamalApp.prefs(this)
        val switch = findViewById<SwitchMaterial>(R.id.switchLearn)
        switch.isChecked = prefs.getBoolean(GamalApp.PREF_LEARN_NOTIFICATIONS, true)
        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(GamalApp.PREF_LEARN_NOTIFICATIONS, isChecked).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        status.text = getString(
            if (enabled) R.string.notif_access_enabled else R.string.notif_access_disabled
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val notifCount = db.notificationDao().totalCount()
            val ratedCount = db.messageDao().ratedCount()
            val profileSize = db.preferenceDao().all().size
            withContext(Dispatchers.Main) {
                stats.text = "התראות שנקלטו: $notifCount\n" +
                    "תשובות שדירגת: $ratedCount\n" +
                    "אותות למידה בפרופיל: $profileSize"
            }
        }
    }
}
