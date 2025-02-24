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
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.fragment_main_page.*
import kotlinx.android.synthetic.main.fragment_main_page_buttons.*
import kotlinx.android.synthetic.main.fragment_main_pages.*
import kotlinx.coroutines.*
import org.json.JSONObject


@DelicateCoroutinesApi
class MainPage : Fragment(), androidx.appcompat.widget.Toolbar.OnMenuItemClickListener,
    View.OnLongClickListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if (KV.getBoolean("TailLongClick", false))
            runCatching {
                findNavController()
                    .navigate(R.id.action_mainPage_to_webViewPage_withoutBackStack, getWebViewBundle())
            }

        Log.d("onCreateView", "MainPage onCreateView !")
        return inflater.inflate(R.layout.fragment_main_page, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ViewCreated", "MainPageViewCreated")

        mToolbar.setOnMenuItemClickListener(this)
        //TODO 添加 app 图标
        mToolbar.title = getString(R.string.app_name) +
                "-V" +
                BuildConfig.VERSION_NAME.replace(Regex(".r.+$"),"")

        if (!Shell.cmd("su -c 'exit'").exec().isSuccess) {
            clash_status.setCardBackgroundColor(
                ResourcesCompat.getColor(resources, R.color.error, context?.theme)
            )
            clash_status_icon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_service_not_running,
                    context?.theme
                )
            )
            clash_status_text.text = getString(R.string.sui_disable)
            resources_status_text.visibility = View.GONE

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

        clash_status.setOnClickListener {
            ClashStatus.switch()
        }

        menu_ip_check.setOnClickListener {
            runCatching {
                it.findNavController().navigate(R.id.action_mainPage_to_ipCheckPage)
            }
        }

        menu_web_dashboard.setOnLongClickListener(this)
        menu_web_dashboard.setOnClickListener {
            runCatching {
                it.findNavController().navigate(R.id.action_mainPage_to_webViewPage, getWebViewBundle())
            }
        }

        menu_speed_test.setOnClickListener {
            runCatching {
                it.findNavController().navigate(R.id.action_mainPage_to_speedTestPage)
            }
        }

        viewPager.adapter = object: FragmentStateAdapter(this){
            val pages = listOf(
                Fragment::class.java,
                CmdLogPage::class.java,
                KernelLogPage::class.java
            )

            override fun getItemCount() = pages.size

            override fun createFragment(position: Int) = pages[position].newInstance()
        }

        viewPager.setCurrentItem(KV.getInt("ViewPagerIndex", 0), false)
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    KV.putInt("ViewPagerIndex", position)
                }
            })

        lifecycleScope.launch(Dispatchers.Main) {
            delay(200)
            WebView(requireContext())
        }
    }

    private fun stopStatusScope(){
        ClashStatus.stopGetStatus()
    }

    private fun startStatusScope() {
        ClashStatus.startGetStatus{ statusText ->
            runCatching {
                val jsonObject = JSONObject(statusText)
                val upText: String = CommandHelper.autoUnitForSpeed(jsonObject.optString("up"))
                val downText: String =
                    CommandHelper.autoUnitForSpeed(jsonObject.optString("down"))
                val res = CommandHelper.autoUnitForSize(jsonObject.optString("RES"))
                val cpu = jsonObject.optString("CPU")
                    resources_status_text.text =
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
            while (true){
                val status = ClashStatus.getRunStatus()
                if (lastStatus == status) continue else lastStatus = status

                clash_status.isClickable = status != ClashStatus.Status.CmdRunning
                if (status != ClashStatus.Status.Running) {
                    resources_status_text.visibility = View.VISIBLE
                    startStatusScope()
                } else {
                    resources_status_text.visibility = View.INVISIBLE
                    stopStatusScope()
                }
                clash_status.setCardBackgroundColor(
                    ResourcesCompat.getColor(resources,
                        if (status == ClashStatus.Status.Running)
                            R.color.colorPrimary
                        else
                            R.color.gray
                        , context?.theme)
                )
                clash_status_icon.setImageDrawable(
                    ResourcesCompat.getDrawable(resources,
                        when(status){
                            ClashStatus.Status.CmdRunning -> R.drawable.ic_refresh
                            ClashStatus.Status.Running -> R.drawable.ic_activited
                            ClashStatus.Status.Stop -> R.drawable.ic_service_not_running
                        }
                        , context?.theme)
                )
                clash_status_text.text = when(status){
                    ClashStatus.Status.CmdRunning -> getString(R.string.clash_charging)
                    ClashStatus.Status.Running -> getString(R.string.clash_enable)
                    ClashStatus.Status.Stop -> getString(R.string.clash_disable)
                }
                if (status == ClashStatus.Status.CmdRunning) {
                    lastViewPage = viewPager.currentItem
                    viewPager.setCurrentItem(1, true)
                } else launch {
                    lastViewPage?.let {
                        delay(3000)
                        viewPager.setCurrentItem(it, true)
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
        startActivity(intent)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when(item.itemId){
            R.id.menu_update_geox -> {
                when{
                    !Shell.cmd("su -c 'exit'").exec().isSuccess ->
                        Toast.makeText(context, "莫得权限呢", Toast.LENGTH_SHORT).show()
                    ClashStatus.isCmdRunning ->
                        Toast.makeText(context, "现在不可以哦", Toast.LENGTH_SHORT).show()
                    else -> ClashStatus.updateGeox()
                }
                true
            }
            R.id.menu_update_config -> {
                lifecycleScope.launch {
                    val status = ClashStatus.getRunStatus()
                    if (status == ClashStatus.Status.Running)
                        ClashConfig.updateConfig{
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
                        it.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1,
                            WebUI.values())
                        runCatching {
                            it.setSelection(
                                WebUI.values().toList().indexOf(
                                    WebUI.valueOf(
                                        KV.getString("DB_NAME", "LOCAL")!!
                                    )
                                )
                            )
                        }
                        it.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(adapter: AdapterView<*>, v: View,
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

    private fun getWebViewBundle(): Bundle {
        val bundle = Bundle()

        val db = runCatching {
            WebUI.valueOf(KV.getString("DB_NAME", "LOCAL")!!).url
        }.getOrDefault("${ClashConfig.baseURL}/ui/")
        bundle.putString("URL", db +
                if ((context?.resources?.configuration?.uiMode
                        ?.and(Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES) {
                    "?theme=dark"
                }else{
                    "?theme=light"
                })
        return bundle
    }
}
