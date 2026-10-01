package io.github.ieswar23.greenbasket.data.remote

import android.content.res.AssetManager
import com.google.gson.Gson
import io.github.ieswar23.greenbasket.data.remote.dto.PlaceOrderRequest
import io.github.ieswar23.greenbasket.data.remote.dto.PlaceOrderResponse
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import kotlin.random.Random

/**
 * Serves the GreenBasket API from bundled JSON assets so the app works fully offline while still
 * exercising a real Retrofit/OkHttp stack. Adds a small random latency to mimic a network.
 */
class MockInterceptor(
    private val assets: AssetManager,
    private val gson: Gson,
    private val latencyMs: LongRange = 300L..700L,
    private val clock: () -> Long = System::currentTimeMillis,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!latencyMs.isEmpty()) {
            Thread.sleep(Random.nextLong(latencyMs.first, latencyMs.last + 1))
        }
        val path = request.url.encodedPath.removePrefix("/v1/")
        return when {
            request.method == "GET" && path in ROUTES -> json(request, 200, readAsset(ROUTES.getValue(path)))
            request.method == "POST" && path == "orders" -> json(request, 201, placeOrder(request))
            else -> json(request, 404, """{"error":"Not found: ${request.method} $path"}""")
        }
    }

    private fun placeOrder(request: Request): String {
        val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        val order = gson.fromJson(body, PlaceOrderRequest::class.java)
        require(order.items.isNotEmpty()) { "Cannot place an empty order" }
        val orderId = "GB" + (clock() / 1000 % 100_000_000).toString().padStart(8, '0')
        val response = PlaceOrderResponse(
            orderId = orderId,
            status = "PLACED",
            etaMinutes = if (order.slotId == "express") 15 else 60,
        )
        return gson.toJson(response)
    }

    private fun readAsset(name: String): String =
        assets.open("api/$name").bufferedReader().use { it.readText() }

    private fun json(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody(JSON))
            .build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val ROUTES = mapOf(
            "catalog/categories" to "categories.json",
            "catalog/products" to "products.json",
            "home/banners" to "banners.json",
            "orders/history" to "order_history.json",
        )
    }
}
