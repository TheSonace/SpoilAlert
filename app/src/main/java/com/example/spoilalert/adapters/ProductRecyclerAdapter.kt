package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.Database
import com.example.spoilalert.MainActivity
import com.example.spoilalert.R
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.models.ProductModel
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.ceil


class ProductAdapter(context: Context, data: MutableList<ProductModel>?,
                     private var binding: ActivityMainBinding
) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private var mContext: Context = context
    private var items: MutableList<ProductModel>? = data?.toMutableList()
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var itemAdapter: ItemAdapter? = null
    @SuppressLint("SimpleDateFormat")
    var formatter = SimpleDateFormat("EEE, dd MMM yyyy")

    private val database = Database(AndroidSqliteDriver(Database.Schema, context, "launch.db"))
    private val productQueries = database.productQueries

    override
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = inflater.inflate(R.layout.activity_main_products, parent, false)
        return ProductViewHolder(view)
    }

    override
    fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val animator: RecyclerView.ItemAnimator? = holder.rvHeadlines.getItemAnimator()
        if (animator != null) {
            animator.removeDuration = 0
        }

        val item = items?.get(position)

        if (item!!.item_data.isEmpty() && items!!.size < 2) {
            binding.introText.visibility = View.VISIBLE
            binding.introButton.visibility = View.VISIBLE
        }
        else {
            binding.introText.visibility = View.GONE
            binding.introButton.visibility = View.GONE
        }

        if (item.item_data.isEmpty()) {
            val key = item.item_data
            val pos = items?.indexOfFirst {it.item_data == key}
            if (pos != null) {
                items?.removeAt(pos)
                holder.rvHeadlines.post(Runnable {
                    notifyDataSetChanged()
                    try { notifyItemRangeChanged(pos, itemCount) }
                    catch (_: IndexOutOfBoundsException) {}

                })
            }
        }

        holder.tvName.text = item.name
        holder.tvDate.text = formatter.format(item.min_spoildate)
        itemAdapter = ItemAdapter(mContext, item.item_data)
        holder.rvHeadlines.adapter = itemAdapter
        holder.rvHeadlines.layoutManager = LinearLayoutManager(mContext)
        holder.tvName.setOnClickListener { onItemClicked(item) }

        holder.prodInfo.setOnClickListener {
            val productpreviewlist = productQueries.getimg(item.barCode).executeAsList()[0]
            MainActivity.openPreview(productpreviewlist, item, binding)
            }

        holder.ivArrow.setOnClickListener { onItemClicked(item) }
        try {
            val expiryDate = item.item_data[0].spoildate

            val diff: Int = ceil((expiryDate.time - Calendar.getInstance().timeInMillis).toFloat() / 86400000).toInt()
            holder.tvDate.text = expiryDate.let { formatter.format(it) }
            holder.tvProductQty.text = item.item_data.size.toString()

            if (diff <= 3){
                holder.tvDate.setTextColor(Color.parseColor("RED"))
            }
            else {
                holder.tvDate.setTextColor(Color.parseColor("#80000000"))
            }}
        catch (_: IndexOutOfBoundsException) {}


        if (item.isExpanded!!) {
            holder.rvHeadlines.visibility = View.VISIBLE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_up)
            holder.tvQtyText.visibility = View.GONE
            holder.tvProductQty.visibility = View.GONE
            holder.tvDateText.visibility = View.GONE
            holder.tvDate.visibility = View.GONE
        } else {
            holder.rvHeadlines.visibility = View.GONE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_down)
            holder.tvQtyText.visibility = View.VISIBLE
            holder.tvProductQty.visibility = View.VISIBLE
            holder.tvDateText.visibility = View.VISIBLE
            holder.tvDate.visibility = View.VISIBLE
        }
    }

    override
    fun getItemCount(): Int {
        return items?.size ?: 0
    }

    private fun onItemClicked(productModel: ProductModel?) {
        productModel?.isExpanded = !productModel?.isExpanded!!
        notifyDataSetChanged()
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById(R.id.tvProductName)
        var tvQtyText: TextView = itemView.findViewById(R.id.tvQtyText)
        var tvProductQty: TextView = itemView.findViewById(R.id.tvProductQty)
        var tvDateText: TextView = itemView.findViewById(R.id.tvDateText)
        var tvDate: TextView = itemView.findViewById(R.id.tvspoildate)
        var rvHeadlines: RecyclerView = itemView.findViewById(R.id.rvHeadlines)
        var prodInfo: ImageView = itemView.findViewById(R.id.prodInfo)
        var ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }
}