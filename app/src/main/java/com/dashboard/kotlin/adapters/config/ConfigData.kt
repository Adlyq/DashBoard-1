package com.dashboard.kotlin.adapters.config

import android.util.Log
import net.mamoe.yamlkt.*

class ConfigData(data: YamlElement) {

    val proxies = (data as YamlMap)["proxies"]
    val proxy_providers = mutableListOf<MutableMap<String, Any>>()
    val proxy_groups = mutableListOf<MutableMap<String, Any>>()
    val rule_providers = mutableListOf<MutableMap<String, Any>>()
    val rules = mutableMapOf<String, Any>()

    init {
        for ((name, value) in (data as YamlMap)["proxy-providers"] as YamlMap){
            proxy_providers.add(mutableMapOf())
        }
    }

}