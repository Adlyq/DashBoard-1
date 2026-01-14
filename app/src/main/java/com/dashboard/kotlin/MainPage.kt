package com.dashboard.kotlin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dashboard.kotlin.MApplication.Companion.KV
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.clashhelper.ClashStatus
import com.dashboard.kotlin.clashhelper.CommandHelper
import com.dashboard.kotlin.clashhelper.WebUI
import com.dashboard.kotlin.databinding.FragmentMainPageBinding
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import org.json.JSONObject


@DelicateCoroutinesApi
class MainPage : Fragment(), androidx.appcompat.widget.Toolbar.OnMenuItemClickListener,
    View.OnLongClickListener {

    private var _binding: FragmentMainPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ViewCreated", "MainPageViewCreated")

        binding.mToolbar.setOnMenuItemClickListener(this)
        //TODO 添加 app 图标
        binding.mToolbar.title = getString(R.string.app_name) +
                "-V" +
                BuildConfig.VERSION_NAME.replace(Regex(".r.+$"), "")

        if (!Shell.cmd("su -c 'exit'").exec().isSuccess) {
            binding.layoutButtons.run {
                clashStatus.setCardBackgroundColor(
                    ResourcesCompat.getColor(resources, R.color.error, context?.theme)
                )
                clashStatusIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_service_not_running,
                        context?.theme
                    )
                )
                clashStatusText.text = getString(R.string.sui_disable)
                resourcesStatusText.visibility = View.GONE
            }

            lifecycleScope.launch {
                while (true) {
                    if (Shell.cmd("su -c 'exit'").exec().isSuccess) {
                        restartApp()
                        break
                    }
                    delay(2 * 1000)
                }
            }

        }

        binding.layoutButtons.run {
            clashStatus.setOnClickListener {
                ClashStatus.switch()
            }

            menuIpCheck.setOnClickListener {
                runCatching {
                    it.findNavController().navigate(R.id.action_mainPage_to_ipCheckPage)
                }
            }

            menuWebDashboard.setOnLongClickListener(this@MainPage)
            menuWebDashboard.setOnClickListener {
                runCatching {
                    it.findNavController()
                        .navigate(R.id.action_mainPage_to_webViewPage, WebViewPage.getWebViewBundle(requireContext()))
                }
            }

            menuSpeedTest.setOnClickListener {
                runCatching {
                    it.findNavController().navigate(R.id.action_mainPage_to_speedTestPage)
                }
            }
        }

        binding.layoutPages.viewPager.run {
            adapter = object : FragmentStateAdapter(this@MainPage) {
                val pages = listOf(
                    Fragment::class.java,
                    CmdLogPage::class.java,
                    KernelLogPage::class.java
                )

                override fun getItemCount() = pages.size

                override fun createFragment(position: Int) =
                    pages[position].declaredConstructors.first().newInstance() as Fragment
            }

            setCurrentItem(KV.getInt("ViewPagerIndex", 0), false)
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        KV.putInt("ViewPagerIndex", position)
                    }
                })
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(200)
            WebView(requireContext())
        }
    }

    private fun stopStatusScope() {
        ClashStatus.stopGetStatus()
    }

    private fun startStatusScope() {
        ClashStatus.startGetStatus { statusText ->
            runCatching {
                val jsonObject = JSONObject(statusText)
                val upText: String = CommandHelper.autoUnitForSpeed(jsonObject.optString("up"))
                val downText: String =
                    CommandHelper.autoUnitForSpeed(jsonObject.optString("down"))
                val res = CommandHelper.autoUnitForSize(jsonObject.optString("RES"))
                val cpu = jsonObject.optString("CPU")
                binding.layoutButtons.resourcesStatusText.text =
                    getString(R.string.netspeed_status_text).format(
                        upText,
                        downText,
                        res,
                        cpu
                    )
            }
        }
    }

    var runningStatusScope: Job? = null

    override fun onPause() {
        super.onPause()
        Log.d("onPause", "MainPagePause")
        stopStatusScope()
        runningStatusScope?.cancel()
        runningStatusScope = null
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume", "MainPageResume")

        runningStatusScope?.cancel()
        runningStatusScope = lifecycleScope.launch {
            var lastStatus: ClashStatus.Status? = null
            var lastViewPage: Int? = null
            while (true) {
                val status = ClashStatus.getRunStatus()
                if (lastStatus == status) continue else lastStatus = status

                binding.layoutButtons.run {
                    clashStatus.isClickable = status != ClashStatus.Status.CmdRunning
                    if (status == ClashStatus.Status.Running) {
                        resourcesStatusText.visibility = View.VISIBLE
                        startStatusScope()
                    } else {
                        resourcesStatusText.visibility = View.INVISIBLE
                        stopStatusScope()
                    }
                    clashStatus.setCardBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            if (status == ClashStatus.Status.Running)
                                R.color.colorPrimary
                            else
                                R.color.gray, context?.theme
                        )
                    )
                    clashStatusIcon.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            when (status) {
                                ClashStatus.Status.CmdRunning -> R.drawable.ic_refresh
                                ClashStatus.Status.Running -> R.drawable.ic_activited
                                ClashStatus.Status.Stop -> R.drawable.ic_service_not_running
                            }, context?.theme
                        )
                    )
                    clashStatusText.text = when (status) {
                        ClashStatus.Status.CmdRunning -> getString(R.string.clash_charging)
                        ClashStatus.Status.Running -> getString(R.string.clash_enable)
                        ClashStatus.Status.Stop -> getString(R.string.clash_disable)
                    }
                }
                if (status == ClashStatus.Status.CmdRunning) {
                    lastViewPage = binding.layoutPages.viewPager.currentItem
                    binding.layoutPages.viewPager.setCurrentItem(1, true)
                } else launch {
                    lastViewPage?.let {
                        delay(3000)
                        binding.layoutPages.viewPager.setCurrentItem(it, true)
                    }
                }
                delay(500)
            }
        }
    }

    private fun restartApp() {
        val intent: Intent? = activity?.baseContext?.packageManager
            ?.getLaunchIntentForPackage(activity?.baseContext!!.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.putExtra("REBOOT", "reboot")
        startActivity(intent!!)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_update_kernel -> {
                when {
                    ClashStatus.isCmdRunning ->
                        Toast.makeText(context, "现在不可以哦", Toast.LENGTH_SHORT).show()

                    else -> ClashStatus.updateKernel()
                }
                true
            }

            R.id.menu_update_config -> {
                lifecycleScope.launch {
                    val status = ClashStatus.getRunStatus()
                    if (status == ClashStatus.Status.Running)
                        ClashConfig.updateConfig {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    else
                        Toast.makeText(context, "Clash没启动呢", Toast.LENGTH_SHORT).show()
                }
                true
            }

            else -> false
        }

    override fun onLongClick(p0: View?): Boolean {
        AlertDialog.Builder(context).apply {
            setTitle("选择Web面板")
            setView(LinearLayout(context).also { ll ->
                val edit = EditText(context).also {
                    it.visibility = View.GONE
                    it.setText(WebUI.Other.url)
                    it.setSingleLine()
                    it.addTextChangedListener { text ->
                        WebUI.Other.url = text.toString()
                    }
                }
                ll.orientation = LinearLayout.VERTICAL
                ll.addView(
                    Spinner(context).also {
                        it.adapter = ArrayAdapter(
                            context, android.R.layout.simple_list_item_1,
                            WebUI.entries.toTypedArray()
                        )
                        runCatching {
                            it.setSelection(
                                WebUI.entries.indexOf(
                                    WebUI.valueOf(
                                        KV.getString("DB_NAME", "LOCAL")!!
                                    )
                                )
                            )
                        }
                        it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                adapter: AdapterView<*>, v: View,
                                index: Int, id: Long
                            ) {
                                KV.putString("DB_NAME", (v as TextView).text.toString())
                                if (v.text == WebUI.Other.name)
                                    edit.visibility = View.VISIBLE
                                else
                                    edit.visibility = View.GONE
                            }

                            override fun onNothingSelected(p0: AdapterView<*>) {
                                KV.putString("DB_NAME", WebUI.Local.name)
                            }
                        }
                    }
                )
                ll.addView(edit)
            })
        }.show()
        return true
    }

}
