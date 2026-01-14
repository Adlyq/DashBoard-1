package com.dashboard.kotlin

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.dashboard.kotlin.MApplication.Companion.KV

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        KV.putBoolean("TailLongClick", false)
        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES) {
            val componentName = intent.extras?.get(Intent.EXTRA_COMPONENT_NAME) as ComponentName?
            componentName ?: return
            Class.forName(componentName.className).declaredConstructors.first().newInstance().apply {
                if (this is TileButtonService) {
                    KV.putBoolean("TailLongClick", true)
                }
            }
        }
    }
}