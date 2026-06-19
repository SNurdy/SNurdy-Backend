package com.snurdy.backend

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import java.util.TimeZone

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients
class SNurdyBackendApplication {

    @PostConstruct
    fun initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}

fun main(args: Array<String>) {
    runApplication<SNurdyBackendApplication>(*args)
}
