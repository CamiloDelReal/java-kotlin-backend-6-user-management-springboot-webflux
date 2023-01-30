package org.xapps.service.usersservice.repositories

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Repository
import org.xapps.service.usersservice.entities.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class UserRepository @Autowired constructor(
    private val reactiveRedisOperations: ReactiveRedisOperations<String, User>
) {

    fun findAll(): Flux<User> =
        reactiveRedisOperations
            .opsForHash<String, User>()
            .values(User.TABLE_NAME)


    fun findById(id: String): Mono<User> =
        reactiveRedisOperations
            .opsForHash<String, User>()
            .get(User.TABLE_NAME, id)

    fun count(): Mono<Long> =
        reactiveRedisOperations
            .opsForHash<String, User>()
            .keys(User.TABLE_NAME).count()

    fun save(user: User): Mono<User> =
        reactiveRedisOperations
            .opsForHash<String, User>()
            .put(User.TABLE_NAME, user.email, user)
            .map {
                user
            }

    fun delete(id: String): Mono<Long> =
        reactiveRedisOperations
            .opsForHash<String, User>()
            .remove(User.TABLE_NAME, id)
}