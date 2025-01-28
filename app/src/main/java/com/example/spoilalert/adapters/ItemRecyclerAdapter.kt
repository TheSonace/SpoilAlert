package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spoilalert.R
import com.example.spoilalert.models.ItemModel
import java.text.SimpleDateFormat

class ItemAdapter(context: Context, data: List<ItemModel>?) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    private var items: List<ItemModel>? = data
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    @SuppressLint("SimpleDateFormat")
    var formatter = SimpleDateFormat("EEE, dd MMM yyyy")

    override
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = inflater.inflate(R.layout.item_headlines, parent, false)
        return ItemViewHolder(view)
    }

    override
    fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items?.get(position)

        holder.tvTitle.text = item?.barCode
        holder.tvDescription.text = item?.spoildate?.let { formatter.format(it) }

    }

    override
    fun getItemCount(): Int {
        return items?.size?:0
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        var tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }
}