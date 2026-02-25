package com.calvintech.churchfinance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChurchFinanceApplication

fun main(args: Array<String>) {
	runApplication<ChurchFinanceApplication>(*args)
}
