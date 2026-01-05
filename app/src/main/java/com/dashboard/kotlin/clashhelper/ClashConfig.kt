package com.dashboard.kotlin.clashhelper

import android.util.Log
import com.dashboard.kotlin.MApplication.Companion.GExternalCacheDir
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


object ClashConfig {

    val paths: List<String> by lazy {
        Shell.cmd(
            "mkdir -p $dataPath/run",
            "cp -f $dataPath/clash.config $dataPath/run/c.cfg",
            "echo '\necho \"\${Clash_bin_path};\${Clash_scripts_dir};\${Subscript_url}\"' >> $dataPath/run/c.cfg",
            "chmod +x $dataPath/run/c.cfg",
            "$dataPath/run/c.cfg"
        ).exec().out.last().split(';')
    }

    init {
        System.loadLibrary("yaml-reader")
        setConfig()
    }

    val dataPath
        get() = "/data/clash"

    val corePath by lazy {
        runCatching {
            if (paths[0] == "") throw Error() else paths[0]
        }.getOrDefault("/data/adb/modules/Clash_For_Magisk/system/bin/clash")
    }

    val scriptsPath by lazy {
        runCatching {
            if (paths[1] == "") throw Error() else paths[1]
        }.getOrDefault( "/data/clash/scripts")
    }

    //val Subscribe by lazy {
    //    runCatching {
    //        paths[1].trim() != ""
    //    }.getOrDefault( false)
    //}

    private val mergedConfigPath
        get() = "${dataPath}/run/config.yaml"

    val logPath
        get() = "${dataPath}/run/run.logs"

    val pidPath
        get() = "${dataPath}/run/clash.pid"

    val configPath
        get() = "${dataPath}/config.yaml"

    val extController by lazy {
        getExternalController()
    }

    val baseURL by lazy {
        "http://$extController"
    }

    val logLevel by lazy {
        getFromFile("$GExternalCacheDir/config.yaml", arrayOf("log-level"))
    }

    val dashBoard by  lazy {
        getFromFile("$GExternalCacheDir/config.yaml", arrayOf("external-ui"))
    }

    val secret by lazy {
        getFromFile("$GExternalCacheDir/config.yaml", arrayOf("secret"))
    }

    fun updateConfig(callBack: (r: String) -> Unit) {
        if (Shell.cmd("diff '$configPath' '$mergedConfigPath' > /dev/null")
                .exec()
                .isSuccess
        ) {
            callBack("配置莫得变化")
            return
        } else {
            val cmd = Shell.cmd("cp -f '$configPath' '$mergedConfigPath'").exec()
            if (cmd.isSuccess.not()){
                callBack("${cmd.out}")
                return
            }
        }
        if (Shell.cmd("$corePath -d $dataPath -f $mergedConfigPath -t > /dev/null").exec().isSuccess)
            updateConfigNet(mergedConfigPath, callBack)
        else
            callBack("配置文件有误唉")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateConfigNet(configPath: String, callBack: (r: String) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val conn =
                    URL("${baseURL}/configs?force=false").openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Authorization", "Bearer $secret")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(
                        JSONObject(
                            mapOf(
                                "path" to configPath
                            )
                        ).toString().toByteArray()
                    )
                }

                conn.connect()
                Log.i("NET", "HTTP CODE : ${conn.responseCode}")
                conn.inputStream.use {
                    val data = it.bufferedReader().readText()
                    Log.i("NET", data)
                }
                withContext(Dispatchers.Main){
                    when (conn.responseCode){
                        204 ->
                            callBack("配置更新成功啦")
                        else ->
                            callBack("更新失败咯，状态码：${conn.responseCode}")
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main){
                    callBack("IO操作出错，你是不是没给俺网络权限")
                }
                Log.w("NET", ex.toString())
            }
        }
    }


    private fun getExternalController(): String {

        val temp = getFromFile("$GExternalCacheDir/config.yaml", arrayOf("external-controller"))

        return when {
            temp.trim() == "" -> "127.0.0.1:9090"
            temp.startsWith(":") -> "127.0.0.1$temp"
            else -> temp
        }
    }

    //private fun setFileNR(dirPath: String, fileName: String, func: (file: String) -> Unit) {
    //    copyFile(dirPath, fileName)
    //    func("$GExternalCacheDir/${fileName}")
    //    Shell.cmd("cp '$GExternalCacheDir/${fileName}' '${dirPath}/${fileName}'").exec()
    //    deleteFile(GExternalCacheDir, fileName)
    //}

    private fun copyFile(dirPath: String, fileName: String): Boolean {
        if (Shell.cmd("ls '${dirPath}/${fileName}'").exec().isSuccess.not())
            return false
        Shell.cmd(
            "cp '${dirPath}/${fileName}' '$GExternalCacheDir/${fileName}'",
            "chmod +rw '$GExternalCacheDir/${fileName}'"
        ).exec()
        return true
    }

    //private fun deleteFile(dirPath: String, fileName: String) {
    //    runCatching {
    //        File(dirPath, fileName).delete()
    //    }
    //}

    private external fun getFromFile(path: String, nodes: Array<String>): String
    private external fun modifyFile(path: String, node: String, value: String)

    private fun setConfig() = copyFile(dataPath, "config.yaml")
}