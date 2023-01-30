package org.xapps.service.usersservice.repositories

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Repository
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class RoleRepository @Autowired constructor(
    private val reactiveRedisOperations: ReactiveRedisOperations<String, Role>
) {

    fun findAll(): Flux<Role> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .values(Role.TABLE_NAME)

    fun count(): Mono<Long> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .keys(Role.TABLE_NAME)
            .count()

    fun findById(id: String): Mono<User> =
            reactiveRedisOperations
                    .opsForHash<String, User>()
                    .get(User.TABLE_NAME, id)

    fun findByName(name: String): Mono<Role> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .values(Role.TABLE_NAME)
            .filter { role -> role.value == name }
            .single()


    fun save(role: Role): Mono<Role> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .put(Role.TABLE_NAME, role.value, role)
            .map { role }

}