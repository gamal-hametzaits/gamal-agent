package dev.hametzaits.gamal.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.hametzaits.gamal.R
import dev.hametzaits.gamal.data.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onFeedback: (Message, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
    }

    fun submit(messages: List<Message>) {
        items.clear()
        items.addAll(messages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].role == "user") TYPE_USER else TYPE_AGENT

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserHolder(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            AgentHolder(inflater.inflate(R.layout.item_message_agent, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        if (holder is UserHolder) {
            holder.txt.text = message.text
            holder.time.text = timeFmt.format(Date(message.timestamp))
        } else if (holder is AgentHolder) {
            holder.txt.text = message.text
            holder.time.text = timeFmt.format(Date(message.timestamp))
            val ctx = holder.itemView.context
            val dim = ContextCompat.getColor(ctx, R.color.gamal_text_dim)
            val red = ContextCompat.getColor(ctx, R.color.gamal_red)
            holder.up.imageTintList = ColorStateList.valueOf(if (message.rating == 1) red else dim)
            holder.down.imageTintList = ColorStateList.valueOf(if (message.rating == -1) red else dim)
            holder.up.setOnClickListener {
                val next = if (message.rating == 1) 0 else 1
                onFeedback(message, next)
            }
            holder.down.setOnClickListener {
                val next = if (message.rating == -1) 0 else -1
                onFeedback(message, next)
            }
        }
    }

    class UserHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txt: TextView = view.findViewById(R.id.txtMessage)
        val time: TextView = view.findViewById(R.id.txtTime)
    }

    class AgentHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txt: TextView = view.findViewById(R.id.txtMessage)
        val time: TextView = view.findViewById(R.id.txtTime)
        val up: ImageButton = view.findViewById(R.id.btnThumbUp)
        val down: ImageButton = view.findViewById(R.id.btnThumbDown)
    }
}
