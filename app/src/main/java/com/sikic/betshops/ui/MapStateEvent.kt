package com.sikic.betshops.ui

import com.sikic.betshops.models.BetShopData

sealed class MapStateEvent {

    object Loading : MapStateEvent()

    data class GetBetShopsEvent(val betShopData: BetShopData) : MapStateEvent()

    object Error : MapStateEvent()
}