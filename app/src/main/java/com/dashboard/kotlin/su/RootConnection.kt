package com.dashboard.kotlin.su

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService


class RootConnection(
    val getContext: () -> Context
) : ServiceConnection {
    var binder: MRootService.RootBinder? = null


    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        binder = service as? MRootService.RootBinder
    }

    override fun onServiceDisconnected(name: ComponentName) {
        binder = null

        RootService.bind(Intent(getContext(), MRootService::class.java), this)
    }
}