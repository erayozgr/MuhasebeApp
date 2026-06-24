package com.eray.muhasebeapp.shared.data.model

data class Urun(
    val id: Long = 0,
    val barkod: String = "",
    val ad: String,
    val alisFiyati: Double = 0.0,
    val satisFiyati: Double = 0.0,
    val stokAdedi: Int = 0,
    val birim: String = "Adet",
    val kdvOrani: Int = 20 // %0, %1, %10, %20
)