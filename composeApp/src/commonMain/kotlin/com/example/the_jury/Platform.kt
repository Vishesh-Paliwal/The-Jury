package com.example.the_jury

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform