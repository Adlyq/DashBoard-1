package com.dashboard.kotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.dashboard.kotlin.adapters.config.ConfigAdapterCallback
import com.dashboard.kotlin.adapters.config.ConfigData
import com.dashboard.kotlin.adapters.config.ConfigRecyclerAdapter
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.placeholder.PlaceholderContent
import com.dashboard.kotlin.suihelper.SuiHelper
import kotlinx.android.synthetic.main.fragment_config_page_list.*
import kotlinx.coroutines.launch
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlElement

class ConfigPage : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config_page_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list_view.apply {
            layoutManager = LinearLayoutManager(context)
            ConfigData(Yaml.decodeYamlFromString(
                SuiHelper.suCmd(
                    "sed -n -E '/^proxies:.*\$/,\$p' ${ClashConfig.configPath}"
                )
            ))
            adapter = ConfigRecyclerAdapter(PlaceholderContent.ITEMS){
                Toast.makeText(context, "$it", Toast.LENGTH_SHORT).show()
            }
            ItemTouchHelper(
                ConfigAdapterCallback(
                    adapter as ConfigAdapterCallback.TouchListener
                )
            ).attachToRecyclerView(this)
        }
    }

}