package com.dashboard.kotlin

import android.annotation.SuppressLint
import android.text.Html
import android.text.Spanned
import android.widget.ScrollView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.clashhelper.ClashStatus
import com.topjohnwu.superuser.BusyBoxInstaller
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

@DelicateCoroutinesApi
class CmdLogPage : BaseLogPage() {
    private val job = Shell.Builder.create()
        .setInitializers(Shell.Initializer::class.java)
        .build()
        .newJob().add("cat ${ClashConfig.logPath}")
    var flag = false

    override fun onResume() {
        super.onResume()
        start()
    }

    override fun onPause() {
        super.onPause()
        readLogScope?.cancel()
    }

    var readLogScope: Job? = null

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    fun start(){
        if (readLogScope?.isActive == true) return
        binding.logCat.setOnTouchListener { v, _ ->
            flag = true
            v.performClick()
            false
        }
        readLogScope = lifecycleScope.launch(Dispatchers.IO) {
            val clashV = Shell.cmd("${ClashConfig.corePath} -v").exec().out.first()
            withContext(Dispatchers.Main){
                binding.logCat.text = formatLog("$clashV\n${readLog()}")
            }
            while (true){
                if (ClashStatus.isCmdRunning){
                    flag = false
                    delay(200)
                } else {
                    delay(1000)
                }
                if (flag) continue
                withContext(Dispatchers.Main){
                    runCatching {
                        binding.logCat.text = formatLog("$clashV\n${readLog()}")
                        binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    private suspend fun readLog(): String{
        val lst = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            job.to(lst).exec()
        }
        return lst.joinToString("\n")
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
                    rstr.append("<span style='color:#fb923c'>${it[1]}</span>" +
                            "<span style='color:${levelToColor[it[2]]}'><strong>${it[2]}</strong></span>" +
                            "<span> ${it[3]}</span><br/>")
                }

            }

            return HtmlCompat.fromHtml(rstr.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}