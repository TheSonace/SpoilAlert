package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.Database
import com.example.spoilalert.R
import com.example.spoilalert.models.ItemModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class ItemAdapter(context: Context, data: MutableList<ItemModel>?) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private val database = Database(AndroidSqliteDriver(Database.Schema, context, "launch.db"))
    private val itemQueries = database.itemQueries

    private val myFormat = "yyyyMMdd"
    private val sdf = SimpleDateFormat(myFormat, Locale.US)

    private var items: MutableList<ItemModel>? = data
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
        val key = item?.RecordKey
        val pos = items?.indexOfFirst {it.RecordKey == key}
        holder.tvDescription.text = item?.spoildate?.let { formatter.format(it) }
        holder.deleteButton.setOnClickListener {delete(pos, key)}

    }

    override
    fun getItemCount(): Int {
        return items?.size?:0
    }


    fun delete(pos: Int?, key: Int?) { //removes the row
        if (pos != null && key != null) {
            items?.removeAt(pos)
            Log.d("datetime", sdf.format(Calendar.getInstance().time).toString())
            itemQueries.removedfromstockRecordKey(sdf.format(Calendar.getInstance().time).toString(),
                key.toLong())
            Log.d("RecordKey", key.toLong().toString())
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, itemCount)

        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        var tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        var deleteButton: Button = itemView.findViewById(R.id.deleteButton)

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}