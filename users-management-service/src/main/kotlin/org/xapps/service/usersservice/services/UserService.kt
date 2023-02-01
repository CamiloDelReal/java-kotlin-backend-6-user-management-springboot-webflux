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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.xapps.service.usersservice.entities.Authentication
import org.xapps.service.usersservice.entities.Login
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.repositories.RoleRepository
import org.xapps.service.usersservice.repositories.UserRepository
import org.xapps.service.usersservice.security.SecurityParams
import org.xapps.service.usersservice.services.exceptions.EmailNotAvailableException
import org.xapps.service.usersservice.services.exceptions.EmailNotFound
import org.xapps.service.usersservice.services.exceptions.InvalidCredentialsException
import org.xapps.service.usersservice.services.exceptions.NotFoundException
import org.xapps.service.usersservice.utils.lazyLogger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Instant
import java.util.*


@Service
class UserService @Autowired constructor(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    @Lazy private val authenticationManager: ReactiveAuthenticationManager,
    private val securityParams: SecurityParams,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder
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
            .findById(newUser.email)
            .flatMap {
                Mono.error<User>(EmailNotAvailableException("Email ${newUser.email} is not available"))
            }
            .switchIfEmpty {
                roleRepository.findByNames(newUser.roles.map { it.value })
                    .collectList()
                    .flatMap { persistedRoles ->
                        val userRoles: List<Role> = if(persistedRoles.isNullOrEmpty()) {
                            listOf(Role(value = Role.GUEST))
                        } else {
                            persistedRoles
                        }
                        newUser.roles = userRoles
                        newUser.password = passwordEncoder.encode(newUser.password)
                        userRepository.save(newUser)
                    }.single()
            }
    }

    fun update(id: String, updatedUser: User): Mono<User> {
        return userRepository
            .findById(id)
            .flatMap { existentUser ->
                if (id != updatedUser.email) {
                    userRepository.findById(updatedUser.email)
                        .flatMap {
                            Mono.error<User>(EmailNotAvailableException("Email ${updatedUser.email} is not available"))
                        }
                        .switchIfEmpty {
                            roleRepository.findByNames(updatedUser.roles.map { it.value })
                                .collectList()
                                .flatMap { persistedRoles ->
                                    val userRoles: List<Role> = if(persistedRoles.isNullOrEmpty()) {
                                        existentUser.roles
                                    } else {
                                        persistedRoles
                                    }
                                    updatedUser.roles = userRoles
                                    updatedUser.password = passwordEncoder.encode(updatedUser.password)
                                    userRepository.delete(id)
                                        .flatMap {  count ->
                                            if(count == 1L)
                                                userRepository.save(updatedUser)
                                            else {
                                                Mono.error(NotFoundException("Error deleting old user registry with ID $id"))
                                            }
                                        }
                                }.single()
                        }
                } else {
                    roleRepository.findByNames(updatedUser.roles.map { it.value })
                        .collectList()
                        .flatMap { persistedRoles ->
                            val userRoles: List<Role> = if(persistedRoles.isNullOrEmpty()) {
                                existentUser.roles
                            } else {
                                persistedRoles
                            }
                            updatedUser.roles = userRoles
                            updatedUser.password = passwordEncoder.encode(updatedUser.password)
                            userRepository.save(updatedUser)
                        }.single()
                }
            }
            .switchIfEmpty {
                Mono.error(EmailNotFound("Email $id nor found in database"))
            }
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

    fun hasAdminRole(user: User): Mono<Boolean> {
        return Mono.create { sink ->
            logger.info("Checking Administrator role")
            logger.info("Roles ${user.roles}")
            if(user.roles.map { it.value }.contains(Role.ADMINISTRATOR)) {
                sink.success(true)
            } else {
                sink.success(false)
            }
        }
    }

    companion object {
        private val logger by lazyLogger()
    }
}