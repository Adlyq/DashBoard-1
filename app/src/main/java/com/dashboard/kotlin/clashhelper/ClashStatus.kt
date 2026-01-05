package com.dashboard.kotlin.clashhelper

import android.util.Log
import com.dashboard.kotlin.clashhelper.ClashStatus.Status.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL

@DelicateCoroutinesApi
object ClashStatus {

    private var getStatusScope: Job? = null

    enum class Status{
        CmdRunning, Running, Stop
    }

    suspend fun getRunStatus() =
        withContext(Dispatchers.IO) {
            when {
                isCmdRunning -> CmdRunning
                isClashRunning -> Running
                else -> Stop
            }
        }


    private val isClashRunning by LazyWithTimeOut(500) {
        runCatching {
            val conn = URL(ClashConfig.baseURL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 500
            conn.setRequestProperty("Authorization", "Bearer ${ClashConfig.secret}")
    
            val response = conn.inputStream.bufferedReader().readText()

            response.contains("{\"hello\":\"mihomo") || response.contains("{\"hello\":\"clash")
        }.getOrDefault(
            Shell.cmd("kill -0 `cat ${ClashConfig.pidPath}`")
                .exec().isSuccess
        )
    }


    private val isGetStatusRunning
        get() = getStatusScope?.isActive?:false

    fun startGetStatus(cb: (String)->Unit) {
        if (isGetStatusRunning) return
        getStatusScope = GlobalScope.launch(Dispatchers.IO) {
            val secret = ClashConfig.secret
            val baseURL = ClashConfig.baseURL
            runCatching {
                val conn =
                    URL("${baseURL}/traffic").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $secret")

                conn.inputStream.use {
                    var lastCpuTotal = 0L
                    var lastClashCpuTotal = 0L

                    while (true) {
                        var cpuTotal = 0L
                        var clashCpuTotal = 0L
                        Shell.cmd("cat /proc/stat | grep \"cpu \"").exec().out.first()
                            .replace("\n","")
                            .replace("cpu ","")
                            .split(Regex(" +")).forEach{ str ->
                                runCatching {
                                    cpuTotal += str.toLong()
                                }
                            }
                        Shell.cmd("cat /proc/`cat ${ClashConfig.pidPath}`/stat").exec().out.first()
                            .split(Regex(" +"))
                            .filterIndexed { index, _ -> index in 13..16 }
                            .forEach{ str ->
                                runCatching {
                                    clashCpuTotal += str.toLong()
                                }
                            }
                        val cpuAVG = BigDecimal(
                            runCatching {
                                ((clashCpuTotal - lastClashCpuTotal) /
                                        (cpuTotal - lastCpuTotal).toDouble() *100)
                            }.getOrDefault(0) as Double
                        ).setScale(2, RoundingMode.HALF_UP)

                        lastClashCpuTotal = clashCpuTotal
                        lastCpuTotal = cpuTotal

                        val res = Shell.cmd(
                            "cat /proc/`cat ${ClashConfig.pidPath}`/status | grep VmRSS | awk '{print \$2}'"
                        ).exec().out.first()
                        val result = it.bufferedReader().readLine()
                            .replace("}", ",\"RES\":\"$res\",\"CPU\":\"$cpuAVG%\"}")
                        withContext(Dispatchers.Main){
                            cb(result)
                        }

                        delay(600)
                    }
                }
            }.onFailure {
                Log.d("TRAFFIC-W", it.toString())
            }
        }
    }

    fun stopGetStatus() {
        getStatusScope?.cancel()
        getStatusScope = null
    }

    var isCmdRunning = false
        private set

    fun start(){
        if (isCmdRunning) return
        isCmdRunning = true
        Shell.cmd(
            "sh ${ClashConfig.scriptsPath}/clash.service -s && sh ${ClashConfig.scriptsPath}/clash.iptables -s"
        ).submit{
            isCmdRunning = false
        }
    }

    fun stop(){
        if (isCmdRunning) return
        isCmdRunning = true
        Shell.cmd(
            "sh ${ClashConfig.scriptsPath}/clash.service -k",
            "${ClashConfig.scriptsPath}/clash.iptables -k"
        ).submit{
            isCmdRunning = false
        }
    }

    fun switch() =
        GlobalScope.launch {
            when (getRunStatus()) {
                CmdRunning -> Unit
                Running -> stop()
                Stop -> start()
            }
        }


    fun updateKernel(){
        if (isCmdRunning) return
        isCmdRunning = true
        Shell.cmd("sh ${ClashConfig.scriptsPath}/update.sh").submit{
            Log.w("TAG", "updateKernel: ${it.out.joinToString("\n")}", )
            isCmdRunning = false
        }
    }
}