package com.sikic.betshops.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sikic.betshops.Api
import com.sikic.betshops.extensions.isNotNull
import com.sikic.betshops.models.BetShopData
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(private val api: Api) : ViewModel() {

    private val _dataState: MutableLiveData<MapStateEvent> = MutableLiveData()

    val dataState: LiveData<MapStateEvent>
        get() = _dataState

    fun getBetShopsData(query: String) {

        _dataState.value = MapStateEvent.Loading

        api.getBetShopData(query).enqueue(object :
            Callback<BetShopData> {
            override fun onFailure(call: Call<BetShopData>, t: Throwable) {
                _dataState.value = MapStateEvent.Error
            }

            override fun onResponse(call: Call<BetShopData>, response: Response<BetShopData>) {
                if (response.isSuccessful && response.body().isNotNull()) {
                    _dataState.value = MapStateEvent.GetBetShopsEvent(response.body()!!)
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
    }
}

sealed class MapStateEvent {

    object Loading : MapStateEvent()

    data class GetBetShopsEvent(val betShopData: BetShopData) : MapStateEvent()

    object Error : MapStateEvent()
}


