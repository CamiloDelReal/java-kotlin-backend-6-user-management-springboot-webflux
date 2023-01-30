package org.xapps.service.usersservice.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.xapps.service.usersservice.entities.Authentication
import org.xapps.service.usersservice.entities.Login
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.repositories.RoleRepository
import org.xapps.service.usersservice.repositories.UserRepository
import org.xapps.service.usersservice.security.SecurityParams
import org.xapps.service.usersservice.services.exceptions.InvalidCredentialsException
import org.xapps.service.usersservice.services.exceptions.NotFoundException
import org.xapps.service.usersservice.utils.lazyLogger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*


@Service
class UserService @Autowired constructor(
        private val roleRepository: RoleRepository,
        private val userRepository: UserRepository,
        @Lazy private val authenticationManager: ReactiveAuthenticationManager,
        private val securityParams: SecurityParams,
        private val objectMapper: ObjectMapper
): ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return userRepository.findById(username)
                .flatMap { user ->
                    if (user != null) {
                        val authorities: List<GrantedAuthority> = user.roles.map { role -> SimpleGrantedAuthority(role.value) }
                        Mono.just(org.springframework.security.core.userdetails.User(user.email, user.password, true, true, true, true, authorities))
                    } else {
                        Mono.error(InvalidCredentialsException("Email $username not found"))
                    }
                }
    }

    fun getAllRoles(): Flux<Role> =
        roleRepository
            .findAll()

    fun login(login: Login): Mono<Authentication> {
        return authenticationManager.authenticate(UsernamePasswordAuthenticationToken(login.email, login.password))
            .flatMap { authentication ->
                if (authentication == null) {
                    Mono.error(InvalidCredentialsException("Credentials are invalid"))
                } else {
                    userRepository.findById(login.email)
                        .flatMap { user ->
                            val currentTimestamp: Long = Instant.now().toEpochMilli()
                            val expiration = Date(currentTimestamp + securityParams.validity)
                            val innerUser = user.copy(password = "<<Protected>>")
                            val token: String = Jwts.builder()
                                .setSubject(objectMapper.writeValueAsString(innerUser))
                                .setIssuedAt(Date(currentTimestamp))
                                .setExpiration(expiration)
                                .signWith(SignatureAlgorithm.HS256, securityParams.key)
                                .compact()
                            Mono.just(Authentication(token, expiration.time))
                        }
                }

            }
    }

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

    companion object {
        private val logger by lazyLogger()
    }
}