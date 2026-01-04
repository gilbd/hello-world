package com.babymonitor.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
