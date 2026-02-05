package com.snurdy.backend.global.extension

fun String.extractBearerToken(): String =
    trim().let { token ->
        if (token.startsWith("Bearer ", ignoreCase = true))
            token.substringAfter(' ').trim()
        else
            token
    }