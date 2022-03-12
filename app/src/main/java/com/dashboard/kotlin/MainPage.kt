package com.dashboard.kotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.clashhelper.ClashStatus
import com.dashboard.kotlin.clashhelper.CommandHelper
import com.dashboard.kotlin.suihelper.SuiHelper
import kotlinx.android.synthetic.main.fragment_main_page.*
import kotlinx.android.synthetic.main.fragment_main_page_buttons.*
import kotlinx.android.synthetic.main.fragment_main_pages.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.*


@DelicateCoroutinesApi
class MainPage : Fragment(), androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timer: Timer
    private lateinit var statusScope: Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("onCreateView", "MainPage onCreateView !")
        return inflater.inflate(R.layout.fragment_main_page, container, false)
    }

    private val clashStatusClass = ClashStatus()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ViewCreated", "MainPageViewCreated")

        mToolbar.setOnMenuItemClickListener(this)

        //TODO 添加 app 图标
        mToolbar.title = getString(R.string.app_name) +
                "-V" +
                BuildConfig.VERSION_NAME.replace(Regex(".r.+$"),"-Alpha")

        if (!SuiHelper.checkPermission()) {
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
                    if (SuiHelper.checkPermission(request = false)) {
                        restartApp()
                        break
                    }
                    delay(1 * 1000)
                }
            }

        } else {
            setRunStatusTimer()
        }

        clash_status.setOnClickListener {
            //setStatusCmdRunning()

            GlobalScope.launch {
                doAssestsShellFile(
                    if (clashStatusClass.runStatus()) {
                        "CFM_Stop.sh"
                    } else {
                        "CFM_Start.sh"
                    })
            }
        }

        menu_ip_check.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainPage_to_ipCheckPage)
        }

        menu_web_dashboard.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("URL", "${ClashConfig.baseURL}/ui/" +
                    if ((context?.resources?.configuration?.uiMode
                            ?.and(Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES) {
                        "?theme=dark"
                    }else{
                        "?theme=light"
                    })
            it.findNavController().navigate(R.id.action_mainPage_to_webViewPage, bundle)
        }

        menu_speed_test.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("URL", "https://fast.com/zh/cn/")
            it.findNavController().navigate(R.id.action_mainPage_to_webViewPage, bundle)
        }

        viewPager.adapter = object: FragmentStateAdapter(this){
            val pages = listOf(
                Fragment::class.java,
                LogPage::class.java
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
    }

    private fun setRunStatusTimer(){
        runCatching { timer.cancel() }
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                handler.post{
                    runCatching {
                        when {
                            clashStatusClass.runStatus() ->
                                setStatusRunning()
                            CommandHelper.isCmdRunning() ->
                                setStatusCmdRunning()
                            else ->
                                setStatusStopped()
                        }
                    }
                }
            }
        },0, 300)
    }

    private fun setStatusScope() {
        runCatching { statusScope.cancel() }
        statusScope = lifecycleScope.launch(Dispatchers.IO) {
            while (clashStatusClass.statusThreadFlag) {
                runCatching {
                    val jsonObject = JSONObject(clashStatusClass.statusRawText)
                    val upText: String = CommandHelper.autoUnitForSpeed(jsonObject.optString("up"))
                    val downText: String =
                        CommandHelper.autoUnitForSpeed(jsonObject.optString("down"))
                    val res = CommandHelper.autoUnitForSize(jsonObject.optString("RES"))
                    val cpu = jsonObject.optString("CPU")
                    withContext(Dispatchers.Main) {
                        resources_status_text.text =
                            getString(R.string.netspeed_status_text).format(
                                upText,
                                downText,
                                res,
                                cpu
                            )
                    }
                }
                delay(600)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("onPause", "MainPagePause")
        clashStatusClass.stopGetStatus()
        timer.cancel()
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume", "MainPageResume")
        clashStatusClass.getStatus()
        setRunStatusTimer()
        if (clashStatusClass.runStatus())
            setStatusScope()
    }

    private fun restartApp() {
        val intent: Intent? = activity?.baseContext?.packageManager
            ?.getLaunchIntentForPackage(activity?.baseContext!!.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.putExtra("REBOOT", "reboot")
        startActivity(intent)
    }

    private fun doAssestsShellFile(fileName: String): String {
        context?.assets?.open(fileName)?.let { op ->
            File(context?.externalCacheDir, fileName).let { fo ->
                fo.outputStream().let { ip ->
                    op.copyTo(ip)
                }
                SuiHelper.suCmd("mkdir -p ${ClashConfig.dataPath}/run &&" +
                        "mv -f ${context?.externalCacheDir}/${fileName} ${ClashConfig.dataPath}/run/ass.sh")
                val res = SuiHelper.suCmd("sh '${ClashConfig.dataPath}/run/ass.sh' ${ClashConfig.scriptsPath} 2>&1")
                fo.delete()
                return res
            }
        }
        return ""
    }

    private fun setStatusRunning(){
        if (clash_status_text.text == getString(R.string.clash_enable))
            return
        clash_status.isClickable = true
        clash_status.setCardBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.colorPrimary, context?.theme)
        )
        clash_status_icon.setImageDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.ic_activited, context?.theme)
        )
        clash_status_text.text = getString(R.string.clash_enable)

        resources_status_text.visibility = View.VISIBLE

        clashStatusClass.getStatus()

        setStatusScope()
    }

    private fun setStatusCmdRunning(){
        if (clash_status_text.text == getString(R.string.clash_charging))
            return
        clash_status.isClickable = false
        clash_status.setCardBackgroundColor(
            ResourcesCompat.getColor(
                resources,
                R.color.gray,
                context?.theme
            )
        )
        clash_status_icon.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_refresh,
                context?.theme
            )
        )
        clash_status_text.text = getString(R.string.clash_charging)
        resources_status_text.visibility = View.INVISIBLE
        clashStatusClass.stopGetStatus()
    }

    private fun setStatusStopped(){
        if (clash_status_text.text == getString(R.string.clash_disable))
            return
        clash_status.isClickable = true
        clash_status.setCardBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.gray, context?.theme)
        )
        clash_status_icon.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_service_not_running,
                context?.theme
            )
        )
        clash_status_text.text = getString(R.string.clash_disable)
        resources_status_text.visibility = View.INVISIBLE
        clashStatusClass.stopGetStatus()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when(item.itemId){
            R.id.menu_update_geox -> {
                when{
                    SuiHelper.checkPermission().not() ->
                        Toast.makeText(context, "莫得权限呢", Toast.LENGTH_SHORT).show()
                    CommandHelper.isCmdRunning() ->
                        Toast.makeText(context, "现在不可以哦", Toast.LENGTH_SHORT).show()
                    else -> GlobalScope.launch {
                            doAssestsShellFile("CFM_Update_GeoX.sh")
                        }
                }
                true
            }
            R.id.menu_update_config -> {
                if (clashStatusClass.runStatus())
                    ClashConfig.updateConfig{
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                else
                    Toast.makeText(context, "Clash没启动呢", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
}