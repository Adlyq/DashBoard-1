package com.dashboard.kotlin.adapters.config

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.dashboard.kotlin.databinding.FragmentConfigPageItemBinding

class ConfigRecyclerAdapter(
    val values: ConfigYaml,
    private val onClick: (MutableList<String>)->Unit
) : RecyclerView.Adapter<ConfigRecyclerAdapter.ViewHolder>(), ConfigAdapterCallback.TouchListener {
    private val mKeysP
        get() = mData.keys.toList()
    private val mData = values.`proxy-providers`
    private val holders = mutableListOf<ViewHolder>()
        //get() {
        //    field.sortBy { it.absoluteAdapterPosition }
        //    return field
        //}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            FragmentConfigPageItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.idView.text = position.toString()
        val key = mKeysP[position]
        holder.contentView.text = key
        val data = mutableListOf(mKeysP[position],
            mData[key]?.url.toString())
        holder.root.setOnClickListener {
            onClick(data)
            if (key != data[0]){
                mData.remove(key)
            }else
                mData[data[0]]?.url = data[1]
        }
        holders.add(holder)
    }

    override fun getItemCount(): Int = mKeysP.size

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
        //Collections.swap(mKeysP, from, to)
        //notifyItemMoved(from, to)
    }

    override fun onDelete(index: Int) {
        values.deleteSubscript(mKeysP[index])
        holders.removeAt(index)
        notifyItemRemoved(index)
        for (i in index until holders.size){
            holders[i].idView.text = i.toString()
        }
    }

    override fun onFinish(index: Int) {
        //Log.e("YAML", ConfigData.toYAML(mData[mKeysP[index]]))
    }

}