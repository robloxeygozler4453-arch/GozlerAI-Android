package com.turkeyproje.gozlerai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChatLine(val text: String, val fromUser: Boolean)

class ChatStore(context: Context) {
    private val prefs = context.getSharedPreferences("gozlerai_chat", Context.MODE_PRIVATE)

    fun load(): List<ChatLine> {
        return try {
            val array = JSONArray(prefs.getString("messages", "[]"))
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ChatLine(item.getString("text"), item.getBoolean("user"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(messages: List<ChatLine>) {
        val array = JSONArray()
        messages.takeLast(100).forEach {
            array.put(JSONObject().put("text", it.text).put("user", it.fromUser))
        }
        prefs.edit().putString("messages", array.toString()).apply()
    }

    fun clear() { prefs.edit().remove("messages").apply() }
}
