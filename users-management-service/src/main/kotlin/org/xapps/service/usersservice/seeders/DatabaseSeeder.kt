package org.xapps.service.usersservice.seeders

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.repositories.RoleRepository
import org.xapps.service.usersservice.repositories.UserRepository
import org.xapps.service.usersservice.utils.lazyLogger

@Component
class DatabaseSeeder @Autowired constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @EventListener
    fun seed(event: ContextRefreshedEvent) = runBlocking {
        val count = roleRepository.count().awaitFirstOrNull()
        if (count == null || count == 0L) {
            val administratorRole = Role(value = Role.ADMINISTRATOR)
            roleRepository.save(administratorRole).subscribe {
                log.info("Administrator role created and saved")
            }
            val guestRole = Role(value = Role.GUEST)
            roleRepository.save(guestRole).subscribe {
                log.info("Guest role created and saved")
            }
        }

        val userCount = userRepository.count().awaitFirstOrNull()
        if (userCount == null || userCount == 0L) {
            roleRepository.findByName(Role.ADMINISTRATOR).subscribe {
                val administrator = User(
                    email = "root@gmail.com",
                    password = passwordEncoder.encode("123456"),
                    name = "Administrator",
                    roles = listOf(it)
                )
                userRepository.save(administrator).subscribe {
                    log.info("Administrator user created and saved")
                }
            }

        }
    }

    companion object {
        private val log by lazyLogger()
    }
}