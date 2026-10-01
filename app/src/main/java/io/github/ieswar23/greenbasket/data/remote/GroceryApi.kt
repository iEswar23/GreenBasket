package io.github.ieswar23.greenbasket.data.remote

import io.github.ieswar23.greenbasket.data.remote.dto.BannersResponse
import io.github.ieswar23.greenbasket.data.remote.dto.CategoriesResponse
import io.github.ieswar23.greenbasket.data.remote.dto.OrderHistoryResponse
import io.github.ieswar23.greenbasket.data.remote.dto.PlaceOrderRequest
import io.github.ieswar23.greenbasket.data.remote.dto.PlaceOrderResponse
import io.github.ieswar23.greenbasket.data.remote.dto.ProductsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GroceryApi {

    @GET("catalog/categories")
    suspend fun categories(): CategoriesResponse

    @GET("catalog/products")
    suspend fun products(): ProductsResponse

    @GET("home/banners")
    suspend fun banners(): BannersResponse

    @GET("orders/history")
    suspend fun orderHistory(): OrderHistoryResponse

    @POST("orders")
    suspend fun placeOrder(@Body request: PlaceOrderRequest): PlaceOrderResponse

    companion object {
        const val BASE_URL = "https://api.greenbasket.app/v1/"
    }
}
