package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.Database
import com.example.spoilalert.R
import com.example.spoilalert.models.ItemModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil


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
        val view = inflater.inflate(R.layout.item_items, parent, false)
        return ItemViewHolder(view)
    }

    override
    fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items?.get(position)

        val expiryDate = item?.spoildate
        val diff: Int = ceil((expiryDate!!.time - Calendar.getInstance().timeInMillis).toFloat() / 86400000).toInt()
        val key = item.RecordKey
        val pos = items?.indexOfFirst {it.RecordKey == key}
        holder.tvspoildate.text = expiryDate.let { formatter.format(it) }
        holder.tvDaysLeft.text = diff.toString()
        holder.deleteButton.setOnClickListener {delete(pos, key)}

        if (diff <= 3){
            holder.tvspoildate.setTextColor(Color.parseColor("RED"))
            holder.tvDaysLeft.setTextColor(Color.parseColor("RED"))
        }
        else {
            holder.tvspoildate.setTextColor(Color.parseColor("#80000000"))
            holder.tvDaysLeft.setTextColor(Color.parseColor("#80000000"))
        }

    }

    override
    fun getItemCount(): Int {
        return items?.size?:0
    }


    fun delete(pos: Int?, key: Int?) { //removes the row
        if (pos != null && key != null) {
            items?.removeAt(pos)
            itemQueries.removedfromstockRecordKey(sdf.format(Calendar.getInstance().time).toString(),
                key.toLong())
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, itemCount)
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvspoildate: TextView = itemView.findViewById(R.id.tvspoildate)
        var tvDaysLeft: TextView = itemView.findViewById(R.id.tvDaysLeft)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}