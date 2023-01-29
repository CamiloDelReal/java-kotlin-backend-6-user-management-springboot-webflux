package org.xapps.service.usersservice.services

import org.springframework.stereotype.Service
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.repositories.RoleRepository
import org.xapps.service.usersservice.repositories.UserRepository
import org.xapps.service.usersservice.services.exceptions.NotFoundException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService (
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) {

    fun getAllRoles(): Flux<Role> =
        roleRepository
            .findAll()

    fun getAll(): Flux<User> =
        userRepository
            .findAll()
            .map { user -> user.apply { password = "<<Protected>>" } }

    fun getById(id: String): Mono<User> =
        userRepository
            .findById(id)
            .map { user -> user.apply { password = "<<Protected>>" } }

    fun create(newUser: User): Mono<User> {
        return userRepository
            .save(newUser)
    }

    fun delete(id: String): Mono<Any> {
        return userRepository
            .delete(id)
            .flatMap {  count ->
                if(count == 1L)
                    Mono.empty()
                else {
                    Mono.error(NotFoundException("User with ID $id not found"))
                }
            }
    }
}