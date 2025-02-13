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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spoilalert.R
import com.example.spoilalert.models.ProductModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds


class ProductAdapter(context: Context, data: MutableList<ProductModel>?) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private var mContext: Context = context
    private var items: MutableList<ProductModel>? = data?.toMutableList()
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var itemAdapter: ItemAdapter? = null
    @SuppressLint("SimpleDateFormat")
    var formatter = SimpleDateFormat("EEE, dd MMM yyyy")

    override
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = inflater.inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view)
    }

    override
    fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val animator: RecyclerView.ItemAnimator? = holder.rvHeadlines.getItemAnimator()
        if (animator != null) {
            animator.removeDuration = 0
        }
        val item = items?.get(position)

        holder.tvName.text = item?.name
        if (item != null) {
            holder.tvDate.text = formatter.format(item.min_spoildate)
        }
        itemAdapter = ItemAdapter(mContext, item?.item_data)
        holder.rvHeadlines.adapter = itemAdapter
        holder.rvHeadlines.layoutManager = LinearLayoutManager(mContext)
        holder.ivArrow.setOnClickListener { onItemClicked(item) }
        holder.tvName.setOnClickListener { onItemClicked(item) }
        val expiryDate = item?.item_data?.get(0)?.spoildate

        val diff: Int = ceil((expiryDate!!.time - Calendar.getInstance().timeInMillis).toFloat() / 86400000).toInt()
        holder.tvDate.text = expiryDate.let { formatter.format(it) }
//        holder.tvDaysLeft.text = diff.toString()
        holder.tvProductQty.text = item.item_data.size.toString()

        if (diff <= 3){
            holder.tvDate.setTextColor(Color.parseColor("RED"))
//            holder.tvDaysLeft.setTextColor(Color.parseColor("RED"))
        }
        else {
            holder.tvDate.setTextColor(Color.parseColor("#80000000"))
//            holder.tvDaysLeft.setTextColor(Color.parseColor("#80000000"))
        }

        if (item.isExpanded!!) {
            holder.rvHeadlines.visibility = View.VISIBLE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_up)
            holder.tvQtyText.visibility = View.GONE
            holder.tvProductQty.visibility = View.GONE
            holder.tvDateText.visibility = View.GONE
            holder.tvDate.visibility = View.GONE
//            holder.tvDaysLeftText.visibility = View.GONE
//            holder.tvDaysLeft.visibility = View.GONE
        } else {
            holder.rvHeadlines.visibility = View.GONE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_down)
            holder.tvQtyText.visibility = View.VISIBLE
            holder.tvProductQty.visibility = View.VISIBLE
            holder.tvDateText.visibility = View.VISIBLE
            holder.tvDate.visibility = View.VISIBLE
//            holder.tvDaysLeftText.visibility = View.VISIBLE
//            holder.tvDaysLeft.visibility = View.VISIBLE
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
//        var tvDaysLeftText: TextView = itemView.findViewById(R.id.tvDaysLeftText)
//        var tvDaysLeft: TextView = itemView.findViewById(R.id.tvDaysLeft)
        var rvHeadlines: RecyclerView = itemView.findViewById(R.id.rvHeadlines)
        var ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }
}