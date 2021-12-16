package com.sikic.betshops.utils.network

import com.sikic.betshops.models.BetShopData
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query


interface Api {
    @GET("/betshops")
    fun getBetShopData(
        @Query("boundingBox") query: String
    ): Single<BetShopData>
}