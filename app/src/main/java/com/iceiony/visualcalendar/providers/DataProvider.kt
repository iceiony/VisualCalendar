package com.iceiony.visualcalendar.providers

import biweekly.component.VEvent
import io.reactivex.rxjava3.core.Observable

interface DataProvider {
    fun today(): Observable<List<VEvent>>
}