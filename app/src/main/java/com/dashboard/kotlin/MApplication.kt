package com.dashboard.kotlin

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV
import com.topjohnwu.superuser.BusyBoxInstaller
import com.topjohnwu.superuser.Shell

class MApplication : Application() {
    companion object {
        lateinit var GExternalCacheDir: String
        lateinit var KV: MMKV
    }

    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setInitializers(Shell.Initializer::class.java)
        )
    }

    override fun onCreate() {
        super.onCreate()
        GExternalCacheDir = applicationContext.externalCacheDir.toString()
        MMKV.initialize(applicationContext)
        KV = MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, null)
    }
}