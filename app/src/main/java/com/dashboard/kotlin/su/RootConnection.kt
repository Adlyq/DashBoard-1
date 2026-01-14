package com.dashboard.kotlin.su

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService


class RootConnection : ServiceConnection {
    var binder: IBinder? = null


    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        binder = service
    }

    override fun onServiceDisconnected(name: ComponentName) {
        binder = null
    }
}