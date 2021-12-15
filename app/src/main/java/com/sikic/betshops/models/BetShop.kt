package com.sikic.betshops.models


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BetShop(
    val name: String,
    val location: Location,
    val id: Int,
    val county: String,
    val city_id: Int,
    val city: String,
    val address: String
) : Parcelable
