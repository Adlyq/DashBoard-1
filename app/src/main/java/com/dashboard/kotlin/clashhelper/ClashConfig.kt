package com.dashboard.kotlin.clashhelper

import android.util.Log
import com.dashboard.kotlin.GExternalCacheDir
import com.dashboard.kotlin.suihelper.SuiHelper
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ClashConfig {

    var paths: List<String>

    init {
        System.loadLibrary("yaml-reader")
        setTemplate()
        SuiHelper.suCmd(
            "mkdir -p $dataPath/run &&" +
                    "cp -f $dataPath/clash.config $dataPath/run/c.cfg &&" +
                    " echo '\necho \"\${Clash_bin_path};\${Clash_scripts_dir};\"'" +
                    " >> $dataPath/run/c.cfg")
        paths = SuiHelper.suCmd("$dataPath/run/c.cfg").split(';')
        SuiHelper.suCmd("rm -f $dataPath/run/c.cfg")
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

    val mergedConfigPath
        get() = "${dataPath}/run/config.yaml"

    val logPath
        get() = "${dataPath}/run/run.logs"

    val pidPath
        get() = "${dataPath}/run/clash.pid"

    val configPath
        get() = "${dataPath}/config.yaml"

    val baseURL by lazy {
        "http://${getExternalController()}"
    }

    var dashBoard: String
        get() = getFromFile("$GExternalCacheDir/template", arrayOf("external-ui"))
        set(value) {
            setFileNR(
                dataPath, "template"
            ) { modifyFile("$GExternalCacheDir/template", "external-ui", value) }
            return
        }

    val secret by lazy {
        getFromFile("$GExternalCacheDir/template", arrayOf("secret"))
    }

    @DelicateCoroutinesApi
    fun updateConfig(callBack: (r: String) -> Unit) {
        runCatching {
            mergeConfig("config_output.yaml")

            if (SuiHelper.suCmd(
                    "diff '$GExternalCacheDir/config_output.yaml' '$mergedConfigPath' > /dev/null" +
                            "&& echo true") == "true") {
                callBack("配置莫得变化")
                return
            } else
                SuiHelper.suCmd(
                    "mv '$GExternalCacheDir/config_output.yaml' '$mergedConfigPath'")
        }.onFailure {
            callBack("合并失败啦")
            return
        }
        if (SuiHelper.suCmd(
                "$corePath -d $dataPath -f $mergedConfigPath -t > /dev/null " +
                        "&& echo true") == "true")
            updateConfigNet(mergedConfigPath, callBack)
        else
            callBack("配置文件有误唉")
    }

    @DelicateCoroutinesApi
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

    private fun mergeConfig(outputFileName: String) {
        //copyFile(clashDataPath, "config.yaml")
        copyFile(dataPath, "template")
        SuiHelper.suCmd(
            "sed -n -E '/^proxies:.*\$/,\$p' $configPath" +
                    " > $GExternalCacheDir/config.yaml")
        mergeFile(
            "$GExternalCacheDir/template",
            "$GExternalCacheDir/config.yaml",
            "$GExternalCacheDir/$outputFileName"
        )
        deleteFile(GExternalCacheDir, "config.yaml")
    }

    private fun getExternalController(): String {

        val temp = getFromFile("$GExternalCacheDir/template", arrayOf("external-controller"))

        return if (temp.startsWith(":"))
            "127.0.0.1$temp"
        else
            temp
    }



    private fun setFileNR(dirPath: String, fileName: String, func: (file: String) -> Unit) {
        copyFile(dirPath, fileName)
        func("$GExternalCacheDir/${fileName}")
        SuiHelper.suCmd("cp '$GExternalCacheDir/${fileName}' '${dirPath}/${fileName}'")
        deleteFile(GExternalCacheDir, fileName)
    }

    private fun copyFile(dirPath: String, fileName: String) {
        SuiHelper.suCmd("cp '${dirPath}/${fileName}' '$GExternalCacheDir/${fileName}'")
        SuiHelper.suCmd("chmod +rw '$GExternalCacheDir/${fileName}'")
        return
    }

    private fun deleteFile(dirPath: String, fileName: String) {
        runCatching {
            File(dirPath, fileName).delete()
        }
    }

    private external fun getFromFile(path: String, nodes: Array<String>): String
    private external fun modifyFile(path: String, node: String, value: String)
    private external fun mergeFile(
        mainFilePath: String,
        templatePath: String,
        outputFilePath: String
    )

    private fun setTemplate() = copyFile(dataPath, "template")
}