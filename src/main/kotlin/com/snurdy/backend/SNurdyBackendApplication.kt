package com.snurdy.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SNurdyBackendApplication

fun main(args: Array<String>) {
    runApplication<SNurdyBackendApplication>(*args)
}