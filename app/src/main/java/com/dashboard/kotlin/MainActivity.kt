package com.dashboard.kotlin

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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


        lifecycleScope.launch {
            while (isActive) {
                when(Shell.isAppGrantedRoot()) {
                    false -> {
                        val navHostFragment =
                            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                        val navController = navHostFragment.navController
                        navController.navigate(R.id.action_mainPage_to_noRootFragment)
                        return@launch
                    }
                    true -> {
                        break
                    }
                    null -> {
                        delay(500)
                    }
                }
            }
        }

        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES) {
            val componentName = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)

            if (componentName?.className == TileButtonService::class.java.name) {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                val navController = navHostFragment.navController
                val bundle = WebViewPage.getWebViewBundle(this)
                navController.navigate(R.id.action_mainPage_to_webViewPage_withoutBackStack, bundle)
            }
        }
    }
}