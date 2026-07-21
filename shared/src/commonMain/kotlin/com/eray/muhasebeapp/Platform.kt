package com.eray.muhasebeapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getEpochMillis(): Long

expect fun getBugununTarihiString(): String