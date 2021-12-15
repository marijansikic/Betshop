package com.sikic.betshops.models

import com.google.android.gms.maps.model.LatLng

import com.google.maps.android.clustering.ClusterItem

class CustomMarker(val betShop: BetShop) : ClusterItem {
    override fun getPosition(): LatLng = with(betShop.location) {
        LatLng(latitude, longitude)
    }
    override fun getTitle(): String = betShop.name
    override fun getSnippet(): String = betShop.name
}

