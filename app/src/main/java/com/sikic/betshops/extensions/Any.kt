package com.sikic.betshops.extensions

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers.io

fun Any?.isNotNull() = this != null

fun Observable<Any>.applySchedulers(): Observable<Any> = subscribeOn(io()).observeOn(mainThread())