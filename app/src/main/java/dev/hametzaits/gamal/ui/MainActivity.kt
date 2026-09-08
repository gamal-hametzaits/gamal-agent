package dev.hametzaits.gamal.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.hametzaits.gamal.GamalApp
import dev.hametzaits.gamal.R
import dev.hametzaits.gamal.agent.GamalAgent
import dev.hametzaits.gamal.agent.PreferenceLearner
import dev.hametzaits.gamal.data.GamalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var store: GamalStore
    private lateinit var agent: GamalAgent
    private lateinit var learner: PreferenceLearner
    private lateinit var adapter: ChatAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = (application as GamalApp).store
        agent = GamalAgent(store)
        learner = PreferenceLearner(store)

        recycler = findViewById(R.id.recyclerChat)
        emptyView = findViewById(R.id.txtEmpty)
        val input = findViewById<EditText>(R.id.inputMessage)
        val send = findViewById<ImageButton>(R.id.btnSend)

        adapter = ChatAdapter { message, newRating ->
            lifecycleScope.launch(Dispatchers.IO) {
                store.setRating(message.id, newRating)
                learner.onFeedback(message.copy(rating = newRating), newRating)
            }
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        store.onMessagesChanged = { refreshChat() }
        refreshChat()

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val doSend = {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.setText("")
                sendMessage(text)
            }
        }
        send.setOnClickListener { doSend() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { doSend(); true } else false
        }
    }

    override fun onDestroy() {
        store.onMessagesChanged = null
        super.onDestroy()
    }

    private fun refreshChat() {
        val messages = store.allMessages()
        runOnUiThread {
            adapter.submit(messages)
            emptyView.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            if (messages.isNotEmpty()) recycler.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            store.insertMessage("user", text, System.currentTimeMillis())
            delay(350) // small beat so the reply feels considered, not canned
            val reply = agent.respond(text)
            store.insertMessage("agent", reply, System.currentTimeMillis())
        }
    }
}
