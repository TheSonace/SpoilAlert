package com.example.spoilalert.enginebuilder

import com.example.spoilalert.model.ProductResponse

interface OpenFoodFactsClient {
    /**
     * Returns product by code.
     *
     * @param code product code
     * @return product
     */
    suspend fun fetchProductByCode(code: String): ProductResponse
}

//        3274080005003