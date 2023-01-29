package org.xapps.service.usersservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UsersManagementServiceApplication

fun main(args: Array<String>) {
	runApplication<UsersManagementServiceApplication>(*args)
}
