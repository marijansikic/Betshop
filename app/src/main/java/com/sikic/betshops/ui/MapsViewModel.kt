package com.sikic.betshops.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLngBounds
import com.sikic.betshops.utils.network.Api
import com.sikic.betshops.models.BetShop
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(private val api: Api) : ViewModel() {

    private val _dataState: MutableLiveData<MapStateEvent> = MutableLiveData()
    val dataState: LiveData<MapStateEvent>
        get() = _dataState

    private var compositeDisposable = CompositeDisposable()

    /**
    top-right latitude (lat1)
    top-right longitude (lon1)
    bottom-left latitude (lat2)
    bottom-left longitude (lon2)
     */
    private fun mapVisibleRegionQueryToString(visibleRegion: LatLngBounds): String {
        return StringBuilder(visibleRegion.northeast.latitude.toString())
            .append(",")
            .append(visibleRegion.northeast.longitude.toString())
            .append(",")
            .append(visibleRegion.southwest.latitude.toString())
            .append(",")
            .append(visibleRegion.southwest.longitude.toString())
            .toString()
    }

    fun getBetShopsData(visibleRegion: LatLngBounds) {
        compositeDisposable.clear()
        _dataState.value = MapStateEvent.Loading

        val query = mapVisibleRegionQueryToString(visibleRegion)
        api.getBetShopData(query)
            .doOnSubscribe { compositeDisposable.add(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    _dataState.value = MapStateEvent.GetBetShopsEvent(it)
                },
                {
                    _dataState.value = MapStateEvent.Error
                }
            )
    }

    fun mapResponseToText(betShop: BetShop) =
        StringBuilder(betShop.name.trimStart())
            .append("\n")
            .append(betShop.address)
            .append("\n")
            .append(betShop.city)
            .append(" - ")
            .append(betShop.county).toString()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}


