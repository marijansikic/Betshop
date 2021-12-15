package com.sikic.betshops.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class BetShopData(
    val count: Int,
    @SerializedName("betshops") val betShopList: List<BetShop>
) :Parcelable
