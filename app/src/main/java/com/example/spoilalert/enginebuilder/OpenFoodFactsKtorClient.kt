package com.example.spoilalert.enginebuilder

import io.ktor.client.call.*
import io.ktor.client.request.*
import com.example.spoilalert.model.ProductResponse
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.*

class OpenFoodFactsKtorClient(
    endpoint: String = API_URL,
    logLevel: LogLevel = LogLevel.NONE
): OpenFoodFactsClient {

    private val productQueryUrl = with(URLBuilder(endpoint)) {
        appendPathSegments("product")
        build()
    }

    fun createProductUrl(code: String): Url = with(URLBuilder(productQueryUrl)) {
        appendPathSegments("$code.json")
        build()
    }

    private val httpClient by lazy {
        val engine = HttpEngineFactory().create()
        HttpClientFactory(engine).createHttpClient(logLevel)
    }

    override suspend fun fetchProductByCode(code: String): ProductResponse =
        httpClient
            .get(
                url = createProductUrl(code),
                block = { header(HttpHeaders.ContentType, ContentType.Application.Json) }
            )
            .body()

    companion object {
        private const val API_URL = "https://world.openfoodfacts.org/api/v2"
    }
}