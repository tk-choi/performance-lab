package com.perflab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PerformanceLabApplication

fun main(args: Array<String>) {
    runApplication<PerformanceLabApplication>(*args)
}
