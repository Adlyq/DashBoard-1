package com.dashboard.kotlin

import android.content.Intent
import android.text.Spanned
import android.widget.ScrollView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.su.MRootService
import com.dashboard.kotlin.su.RootConnection
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.*

@DelicateCoroutinesApi
class CmdLogPage : BaseLogPage() {
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

    fun start() {
        val rootConnection = RootConnection()
        RootService.bind(Intent(context, MRootService::class.java), rootConnection);
        readLogJob = lifecycleScope.launch(Dispatchers.IO) {
            val clashV = Shell.cmd("${ClashConfig.corePath} -v").exec().out.firstOrNull() ?: ""
            withContext(Dispatchers.Main) {
                binding.logCat.text = clashV
            }

            // 等待 Binder 就绪
            while (rootConnection.binder == null && isActive) {
                delay(100)
            }
            if (!isActive) return@launch

            val remoteFS = FileSystemManager.getRemote(rootConnection.binder!!)
            val logFile = remoteFS.getFile(ClashConfig.logPath)
            var offset = 0L
            val leftovers = StringBuilder()

            while (isActive) {
                val length = logFile.length()
                if (length < offset) {
                    offset = 0
                    leftovers.clear()
                    withContext(Dispatchers.Main) {
                        binding.logCat.text = formatLog("$clashV\n")
                    }
                }

                if (length > offset) {
                    val ips = logFile.newInputStream()
                    ips.skip(offset)
                    val buffer = ByteArray(8192)
                    var read: Int
                    val sb = StringBuilder()

                    while (true) {
                        read = ips.read(buffer)
                        if (read <= 0) break
                        sb.append(String(buffer, 0, read))
                        offset += read
                    }
                    ips.close()

                    val fullText = leftovers.toString() + sb.toString()
                    leftovers.clear()

                    val lastNewline = fullText.lastIndexOf('\n')
                    if (lastNewline != -1) {
                        val toProcess = fullText.substring(0, lastNewline + 1)
                        if (lastNewline + 1 < fullText.length) {
                            leftovers.append(fullText.substring(lastNewline + 1))
                        }

                        if (toProcess.isNotEmpty()) {
                            val sp = formatLog(toProcess.trimEnd('\n'))
                            withContext(Dispatchers.Main) {
                                binding.logCat.append(sp)
                                binding.scrollView.post {
                                    binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                                }
                            }
                        }
                    } else {
                        leftovers.append(fullText)
                    }
                }
                delay(500)
            }
        }
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
                        "<span style='color:#fb923c'>${it[1]}</span>" + "<span style='color:${levelToColor[it[2]]}'><strong>${it[2]}</strong></span>" + "<span> ${it[3]}</span><br/>"
                    )
                }

            }

            return HtmlCompat.fromHtml(rstr.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}