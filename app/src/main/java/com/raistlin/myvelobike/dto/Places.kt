package com.raistlin.myvelobike.dto

import com.google.android.gms.maps.model.LatLng

data class Place(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val fillStatus: Int,
    val electric: Boolean,
    val visits: Int
)

fun Place.toLatLng() = LatLng(latitude, longitude)