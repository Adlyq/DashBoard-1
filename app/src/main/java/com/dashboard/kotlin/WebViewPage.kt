package com.dashboard.kotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import com.dashboard.kotlin.databinding.FragmentWebviewPageBinding
import androidx.core.net.toUri
import com.dashboard.kotlin.MApplication.Companion.KV
import com.dashboard.kotlin.clashhelper.ClashConfig
import com.dashboard.kotlin.clashhelper.WebUI


class WebViewPage : Fragment() {
    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (uploadMessage == null) return@registerForActivityResult

        var results: Array<Uri>? = null

        // 解析选择结果
        if (result.resultCode == Activity.RESULT_OK) {
            val dataString = result.data?.dataString
            val clipData = result.data?.clipData

            if (clipData != null) {
                // 多选处理
                results = Array(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }
            } else if (dataString != null) {
                // 单选处理
                results = arrayOf(dataString.toUri())
            }
        }

        // 2. 将结果回传给 WebView (关键)
        uploadMessage?.onReceiveValue(results)
        uploadMessage = null
    }

    private var _binding: FragmentWebviewPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWebviewPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("ViewCreated", "WebViewPageViewCreated: ${arguments?.getString("URL")}")
        arguments?.getString("URL")?.let {
            binding.webView.loadUrl(it)
        }
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                domStorageEnabled = true
                databaseEnabled = true
            }
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                // Android 5.0+ (API 21+) 调用此方法
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: WebChromeClient.FileChooserParams?
                ): Boolean {
                    // 如果已有未处理的回调，先取消它（防止重复点击卡死）
                    if (uploadMessage != null) {
                        uploadMessage?.onReceiveValue(null)
                        uploadMessage = null
                    }

                    uploadMessage = filePathCallback

                    // 创建文件选择 Intent
                    val intent = fileChooserParams?.createIntent()
                    // 或者自定义 Intent，例如限制类型：
                    // val intent = Intent(Intent.ACTION_GET_CONTENT)
                    // intent.addCategory(Intent.CATEGORY_OPENABLE)
                    // intent.type = "*/*"

                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        uploadMessage?.onReceiveValue(null)
                        uploadMessage = null
                        return false
                    }

                    return true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if ((context?.resources?.configuration?.uiMode
                    ?.and(Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES) {
                binding.webView.settings.forceDark = WebSettings.FORCE_DARK_ON

            }else{
                binding.webView.settings.forceDark = WebSettings.FORCE_DARK_OFF
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("Destroy", "WebViewPageDestroyView")

    }


    companion object {
        fun getWebViewBundle(context: Context): Bundle {
            val bundle = Bundle()

            val db = runCatching {
                WebUI.valueOf(KV.getString("DB_NAME", "LOCAL")!!).url
            }.getOrDefault("${ClashConfig.baseURL}/ui/")
            bundle.putString(
                "URL", db +
                        if ((context.resources?.configuration?.uiMode
                                ?.and(Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES
                        ) {
                            "?theme=dark"
                        } else {
                            "?theme=light"
                        }
            )
            return bundle
        }
    }
}