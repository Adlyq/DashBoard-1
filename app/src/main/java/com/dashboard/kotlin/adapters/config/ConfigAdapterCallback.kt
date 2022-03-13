package com.dashboard.kotlin.adapters.config

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ConfigAdapterCallback(private val cb: TouchListener): ItemTouchHelper.Callback() {

    interface TouchListener{
        fun onMove(from: Int, to: Int)
        fun onDelete(index: Int)
        fun onFinish(index: Int)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int = makeMovementFlags(/*ItemTouchHelper.DOWN or ItemTouchHelper.UP*/0, ItemTouchHelper.LEFT)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        cb.onMove(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        cb.onDelete(viewHolder.absoluteAdapterPosition)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        cb.onFinish(viewHolder.absoluteAdapterPosition)
    }
}