package org.xapps.service.usersservice.repositories

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Repository
import org.xapps.service.usersservice.entities.Role
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

    fun findByName(name: String): Mono<Role> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .values(Role.TABLE_NAME)
            .filter { role -> role.name == name }
            .single()


    fun save(role: Role): Mono<Role> =
        reactiveRedisOperations
            .opsForHash<String, Role>()
            .put(Role.TABLE_NAME, role.id, role)
            .map { role }

}