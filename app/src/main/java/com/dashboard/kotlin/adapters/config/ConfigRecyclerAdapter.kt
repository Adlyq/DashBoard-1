package com.dashboard.kotlin.adapters.config

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.dashboard.kotlin.ConfigPage

import com.dashboard.kotlin.placeholder.PlaceholderContent.PlaceholderItem
import com.dashboard.kotlin.databinding.FragmentConfigPageItemBinding
import java.util.*

class ConfigRecyclerAdapter(
    private val values: MutableList<PlaceholderItem>,
    private val onClick: (Int)->Unit
) : RecyclerView.Adapter<ConfigRecyclerAdapter.ViewHolder>(), ConfigAdapterCallback.TouchListener {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentConfigPageItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.idView.text = item.id
        holder.contentView.text = item.content
        holder.root.setOnClickListener {
            onClick(position)
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentConfigPageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.itemText
        val root = binding.itemContainer

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

    override fun onMove(from: Int, to: Int) {
        Collections.swap(values, from, to)
        notifyItemMoved(from, to)
    }

    override fun onDelete(index: Int) {
        values.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onFinish(index: Int) {

    }

}