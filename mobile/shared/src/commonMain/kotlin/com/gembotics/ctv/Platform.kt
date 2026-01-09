package com.gembotics.ctv

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform