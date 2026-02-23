package com.calvin_tech.church_finance

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<ChurchFinanceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
