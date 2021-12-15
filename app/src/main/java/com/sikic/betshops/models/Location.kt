package com.sikic.betshops.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    @SerializedName("lng") val longitude: Double,
    @SerializedName("lat") val latitude: Double
) : Parcelable
