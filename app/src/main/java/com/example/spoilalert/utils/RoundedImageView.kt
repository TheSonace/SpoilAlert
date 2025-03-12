package com.example.spoilalert.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

class RoundedImageView : AppCompatImageView {
    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    override fun setImageDrawable(drawable: Drawable?) {
        val radius = 0.02f
        val bitmap = (drawable as BitmapDrawable?)!!.bitmap
        val rid = RoundedBitmapDrawableFactory.create(resources, bitmap)
        if (bitmap != null) {
            rid.cornerRadius = bitmap.width * radius
            super.setImageDrawable(rid)}
    }
}