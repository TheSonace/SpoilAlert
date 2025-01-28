package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
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

class ProductAdapter(context: Context, data: MutableList<ProductModel>?) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private var mContext: Context = context
    private var items: MutableList<ProductModel>? = data
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var itemAdapter: ItemAdapter? = null
    @SuppressLint("SimpleDateFormat")
    var formatter = SimpleDateFormat("EEE, dd MMM yyyy")

    override
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = inflater.inflate(R.layout.item_news_paper, parent, false)
        return ProductViewHolder(view)
    }

    override
    fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items?.get(position)

        holder.tvName.text = item?.name
        if (item != null) {
            holder.tvDate.text = formatter.format(item.min_spoildate)
        }
        itemAdapter = ItemAdapter(mContext, item?.item_data)
        holder.rvHeadlines.adapter = itemAdapter
        holder.rvHeadlines.layoutManager = LinearLayoutManager(mContext)
        holder.ivArrow.setOnClickListener { onItemClicked(item) }
        if (item?.isExpanded!!) {
            holder.rvHeadlines.visibility = View.VISIBLE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_up)
        } else {
            holder.rvHeadlines.visibility = View.GONE
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_down)
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
        var tvName: TextView = itemView.findViewById(R.id.tvPaperName)
        var tvDate: TextView = itemView.findViewById(R.id.tvspoildate)
        var rvHeadlines: RecyclerView = itemView.findViewById(R.id.rvHeadlines)
        var ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }
}