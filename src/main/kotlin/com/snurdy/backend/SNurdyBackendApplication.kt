package com.snurdy.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SNurdyBackendApplication // object가 아닌 class로 변경

fun main(args: Array<String>) {
    runApplication<SNurdyBackendApplication>(*args)
}