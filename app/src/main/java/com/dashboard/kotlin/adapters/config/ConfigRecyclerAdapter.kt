package com.dashboard.kotlin.adapters.config

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.dashboard.kotlin.databinding.FragmentConfigPageItemBinding

class ConfigRecyclerAdapter(
    val values: ConfigYaml,
    private val onClick: (MutableList<String>, (List<String>)->Unit)-> Unit
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
        val key = mKeysP[position]
        holder.idView.text = position.toString()
        holder.contentView.text = key

        holder.root.setOnClickListener {
            val data = mutableListOf(key, mData[key]?.url.toString())
            onClick(data){
                if (key != data[0]) {
                    mData.remove(key)
                    values.addSubscripts(data[0], data[1])
                } else {
                    mData[data[0]]?.url = data[1]
                }
                notifyDataSetChanged()
            }
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