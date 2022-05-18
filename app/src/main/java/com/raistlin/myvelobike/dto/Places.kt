package com.raistlin.myvelobike.dto

data class Place(
    val id: Int,
    val position: Position,
    val fillStatus: Int,
    val electric: Boolean,
    val visits: Int
)