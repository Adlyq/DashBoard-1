package com.dashboard.kotlin

import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.*

class KernelLogPage: BaseLogPage() {
    data class WSLog(val type: String, val payload: String)

    private val gson = Gson()
    private val wsRawLog = Channel<String>(Channel.UNLIMITED)
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    override fun onResume() {
        super.onResume()
        val request = Request.Builder()
            .url("ws://${ClashConfig.extController}/logs?token=${ClashConfig.secret}&level=${ClashConfig.logLevel}")
            .build()
        webSocket?.cancel()
        webSocket = client.newWebSocket(request, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                lifecycleScope.launch { wsRawLog.send(text) }
            }
        })

        lifecycleScope.launch { handleLog() }
    }

    override fun onPause() {
        super.onPause()
        webSocket?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdownNow()
    }

    private val logs = mutableListOf<String>()
    private suspend fun handleLog() {
        showLog()
        for (rawLog in wsRawLog) {
            var wsLog = gson.fromJson(rawLog, WSLog::class.java)
            if (wsLog.type == null)
                wsLog = WSLog("error", rawLog)
            val html = "<span style='color:${levelToColor.getOrDefault(wsLog.type, "")}'><strong>${wsLog.type}</strong></span>" +
                    "&nbsp<span>${wsLog.payload}</span><br/>"
            if (logs.size > MAX_LENGTH - 1) logs.removeLast()
            logs.add(0, html)
            showLog()
        }
    }

    private fun showLog() {
        binding.logCat.text = HtmlCompat.fromHtml(logs.joinToString(""),HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    companion object {
        private const val MAX_LENGTH = 100

        private val levelToColor = mapOf(
            "info" to "#58C3F2",
            "warning" to "#CC5ABB",
            "error" to "#C11C1C",
            "debug" to "#389D3D",
        )
    }
}