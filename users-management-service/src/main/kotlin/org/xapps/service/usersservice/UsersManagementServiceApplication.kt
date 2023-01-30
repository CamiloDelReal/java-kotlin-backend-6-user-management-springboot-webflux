package org.xapps.service.usersservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
@EnableWebFlux
class UsersManagementServiceApplication

fun main(args: Array<String>) {
	runApplication<UsersManagementServiceApplication>(*args)
}
