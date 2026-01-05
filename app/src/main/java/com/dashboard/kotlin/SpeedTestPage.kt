package com.dashboard.kotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dashboard.kotlin.MApplication.Companion.KV
import com.dashboard.kotlin.databinding.FragmentSpeedTestPageBinding

class SpeedTestPage : Fragment() {

    private var _binding: FragmentSpeedTestPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSpeedTestPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewPager.adapter = object: FragmentStateAdapter(this){

            val urls = arrayListOf(
                "https://fast.com/zh/cn/",
                "https://speed.cloudflare.com/",
                "https://www.speedtest.net/",
                "http://ovo.speedtestcustom.com/"
            )

            override fun getItemCount() = urls.size

            override fun createFragment(position: Int) = WebViewPage().also {
                it.arguments = Bundle().apply {
                    putString("URL", urls[position])
                }
            }
        }

        binding.viewPager.setCurrentItem(KV.getInt("SpeedTestIndex", 0), false)
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    KV.putInt("SpeedTestIndex", position)
                }
            }
        )
        runCatching {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(binding.viewPager) as RecyclerView
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * 6) //6 is empirical value

        }
    }
}