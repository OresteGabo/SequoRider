package dev.orestegabo.sequorider

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform