package com.example.spoilalert.model

import kotlinx.serialization.Serializable

@Serializable
data class SelectedImages(
    val front: SelectedImage? = null,
    val ingredients: SelectedImage? = null,
    val nutrition: SelectedImage? = null,
)