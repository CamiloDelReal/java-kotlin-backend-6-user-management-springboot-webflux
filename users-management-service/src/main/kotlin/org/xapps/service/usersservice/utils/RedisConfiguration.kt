package org.xapps.service.usersservice.utils

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User


@Configuration
class RedisConfiguration {

    @Bean
    fun reactiveRedisConnectionFactory(
        @Value("\${spring.redis.host}") host: String,
        @Value("\${spring.redis.port}") port: Int,
        @Value("\${spring.redis.database}") databaseId: Int,
        @Value("\${spring.redis.password}") password: String
    ): ReactiveRedisConnectionFactory? {
        return LettuceConnectionFactory(RedisStandaloneConfiguration().apply {
            hostName = host
            setPort(port)
            database = databaseId
            setPassword(password)
        })
    }

    @Bean
    fun provideReactiveRedisOperationsUser(
        @Qualifier("reactiveRedisConnectionFactory") factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisOperations<String, User> =
        ReactiveRedisTemplate(
            factory,
            RedisSerializationContext.newSerializationContext<String, User>()
                .key(StringRedisSerializer())
                .hashKey(StringRedisSerializer())
                .value(Jackson2JsonRedisSerializer(User::class.java))
                .hashValue(Jackson2JsonRedisSerializer(User::class.java))
                .build()
        )

    @Bean
    fun provideReactiveRedisOperationsRole(
        @Qualifier("reactiveRedisConnectionFactory") factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisOperations<String, Role> =
        ReactiveRedisTemplate(
            factory,
            RedisSerializationContext.newSerializationContext<String, Role>()
                .key(StringRedisSerializer())
                .hashKey(StringRedisSerializer())
                .value(Jackson2JsonRedisSerializer(Role::class.java))
                .hashValue(Jackson2JsonRedisSerializer(Role::class.java))
                .build()
        )

}