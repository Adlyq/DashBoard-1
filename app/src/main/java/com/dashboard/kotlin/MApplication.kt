package com.dashboard.kotlin

import android.app.Application
import android.content.Intent
import com.dashboard.kotlin.su.MRootService
import com.dashboard.kotlin.su.RootConnection
import com.tencent.mmkv.MMKV
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService


class MApplication : Application() {
    companion object {
        lateinit var GExternalCacheDir: String
        lateinit var KV: MMKV
    }

    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setInitializers(Shell.Initializer::class.java)
        )
        Shell.enableLegacyStderrRedirection = true
    }

    override fun onCreate() {
        super.onCreate()
        GExternalCacheDir = applicationContext.externalCacheDir.toString()
        MMKV.initialize(applicationContext)
        KV = MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, null)
    }
}