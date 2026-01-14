package com.dashboard.kotlin

import android.annotation.SuppressLint
import android.text.Spanned
import android.widget.ScrollView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@DelicateCoroutinesApi
class CmdLogPage : BaseLogPage() {
    private val data = MutableStateFlow("")

    private var readLogJob: Job? = null

    override fun onResume() {
        super.onResume()
        readLogJob?.cancel()
        readLogJob = null
        start()
    }

    override fun onPause() {
        super.onPause()
        readLogJob?.cancel()
        readLogJob = null
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    fun start() {
        val clashV = Shell.cmd("${ClashConfig.corePath} -v").exec().out.first()
        data.onEach {
            binding.logCat.text = formatLog("$clashV\n${it}")
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }.catch {
            it.printStackTrace()
        }.launchIn(lifecycleScope + Dispatchers.Main)


        readLogJob = ticker(500L, 0).consumeAsFlow().onEach {

        }.launchIn(lifecycleScope + Dispatchers.IO)
    }

    companion object {
        private val reLog = Regex("(\\[.+])(.{3,4}): (.+)")
        private val levelToColor = mapOf(
            Pair("info", "#58C3F2"),
            Pair("warn", "#CC5ABB"),
            Pair("err", "#C11C1C"),
        )

        private fun formatLog(log: String): Spanned {
            val rstr = StringBuilder()
            log.split("\n").forEach { line ->
                val rl = reLog.find(line)
                if (rl == null) {
                    rstr.append("$line<br/>")
                    return@forEach
                }

                rl.groupValues.let {
                    rstr.append(
                        "<span style='color:#fb923c'>${it[1]}</span>" +
                                "<span style='color:${levelToColor[it[2]]}'><strong>${it[2]}</strong></span>" +
                                "<span> ${it[3]}</span><br/>"
                    )
                }

            }

            return HtmlCompat.fromHtml(rstr.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}