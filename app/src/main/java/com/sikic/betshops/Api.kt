package com.sikic.betshops

import com.sikic.betshops.models.BetShopData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface Api {
    @GET("/betshops")
    fun getBetShopData(
        @Query("boundingBox") query: String
    ): Call<BetShopData>
}