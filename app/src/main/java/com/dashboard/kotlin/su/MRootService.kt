package com.dashboard.kotlin.su

import android.content.Intent
import android.os.BaseBundle
import android.os.Binder
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

class MRootService : RootService() {
    inner class RootBinder : Binder() {
        val service = this@MRootService

        val fsService = FileSystemManager.getService()
    }
    override fun onBind(p0: Intent) = RootBinder()
}