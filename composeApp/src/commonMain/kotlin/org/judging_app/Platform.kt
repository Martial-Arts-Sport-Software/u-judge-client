package org.judging_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform