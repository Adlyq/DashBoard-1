package com.dashboard.kotlin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.dashboard.kotlin.adapters.config.ConfigAdapterCallback
import com.dashboard.kotlin.adapters.config.ConfigRecyclerAdapter
import com.dashboard.kotlin.adapters.config.ConfigYaml
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.suihelper.SuiHelper
import kotlinx.android.synthetic.main.fragment_config_page_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.yamlkt.Yaml
import java.io.File

class ConfigPage : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config_page_list, container, false)
    }

    lateinit var data: ConfigYaml

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch(Dispatchers.IO){
            val oData = Regex("(?<= )https?://[^ '\"\n]*(?=[\n ])").replace(SuiHelper.suCmd(
                "sed -n -E '/^proxies:.*\$/,\$p' ${ClashConfig.configPath}"
            ), " '\$0'")
            //Log.d("TEST", oData)
            data = Yaml.decodeFromString(
                ConfigYaml.serializer(),
                oData
            )
            config_toolbar.setOnMenuItemClickListener {
                when (it.itemId){
                    R.id.menu_subscript_add -> {
                        dialogShow()
                        true
                    }
                    R.id.menu_config_save ->{
                        saveFile()
                        true
                    }
                    else ->
                        false
                }
            }
            withContext(Dispatchers.Main){
                list_view.apply {
                    layoutManager = LinearLayoutManager(context)

                    adapter = ConfigRecyclerAdapter(data){ lst, cb ->
                        dialogShow(lst){ cb(it) }
                    }
                    ItemTouchHelper(
                        ConfigAdapterCallback(
                            adapter as ConfigAdapterCallback.TouchListener
                        )
                    ).attachToRecyclerView(this)
                }
            }
        }
    }

    private fun dialogShow(item: MutableList<String>? = null, onOk: ((List<String>)->Unit)? = null){
        val plane = EditText(context).apply {
            hint = "机场名"
            if (item != null) {
                setText(item[0])
            }
        }
        val url = EditText(context).apply {
            hint = "订阅链接"
            if (item != null) {
                setText(item[1])
            }
        }
        AlertDialog.Builder(context).apply {
            setTitle("Edit")
            setView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(plane)
                addView(url)
            })
            setNegativeButton("Cancel", null)
            setPositiveButton("Ok"){ _, _ ->
                if (item != null) {
                    item[0] = plane.text.toString()
                    item[1] = url.text.toString()
                    onOk?.invoke(item)
                }else{
                    data.addSubscripts(plane.text.toString(), url.text.toString())
                }
            }
        }.show()
    }

    fun saveFile(){
        runCatching {
            File(GExternalCacheDir, "out_config.yaml").outputStream().use { op ->
                op.write(Yaml.encodeToString(ConfigYaml.serializer(), data).toByteArray())
            }
            SuiHelper.suCmd("mv -f ${ClashConfig.configPath} ${ClashConfig.configPath}.o")
            SuiHelper.suCmd("cp -f $GExternalCacheDir/out_config.yaml ${ClashConfig.configPath}")
        }.onSuccess {
            Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show()
            AlertDialog.Builder(context).apply {
                setView(TextView(context).apply {
                    text = it.message
                })
            }.show()
        }
    }

}