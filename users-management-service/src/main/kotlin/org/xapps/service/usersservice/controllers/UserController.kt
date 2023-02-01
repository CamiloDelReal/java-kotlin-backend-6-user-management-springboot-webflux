package org.xapps.service.usersservice.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.xapps.service.usersservice.entities.Authentication
import org.xapps.service.usersservice.entities.Login
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.services.UserService
import org.xapps.service.usersservice.services.exceptions.CredentialsNotProvided
import org.xapps.service.usersservice.services.exceptions.EmailNotAvailableException
import org.xapps.service.usersservice.services.exceptions.InvalidCredentialsException
import org.xapps.service.usersservice.services.exceptions.NotFoundException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import javax.validation.Valid

@RestController
@RequestMapping(path = ["/users"])
class UserController(
    private val userService: UserService
) {

    @GetMapping(
        path = ["/roles"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator')")
    fun getRoles(): Flux<Role> =
        userService.getAllRoles()

    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator')")
    fun getUsers(): Flux<User> =
            userService.getAll()

    @PostMapping(
        path = ["/login"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun login(@Valid @RequestBody login: Login): Mono<Authentication> =
            userService.login(login)

    @GetMapping(
        path = ["/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.email == #id")
    fun getUser(@PathVariable("id") id: String): Mono<User> =
        userService.getById(id)

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createUser(@Valid @RequestBody newUser: User): Mono<User> =
        userService.hasAdminRole(newUser)
            .flatMap { hasAdminRole ->
                if(!hasAdminRole) {
                    userService.create(newUser)
                } else {
                    ReactiveSecurityContextHolder.getContext()
                        .flatMap { context ->
                            val principal = context.authentication?.principal
                            if (principal != null && principal is User) {
                                userService.hasAdminRole(principal)
                                    .flatMap { credentialsHasAdminRole ->
                                        if (credentialsHasAdminRole) {
                                            userService.create(newUser)
                                        } else {
                                            Mono.error(CredentialsNotProvided("Request requires Administrator credentials to proceed"))
                                        }
                                    }
                            } else {
                                Mono.error(CredentialsNotProvided("Invalid credentials"))
                            }
                        }
                        .switchIfEmpty {
                            Mono.error(CredentialsNotProvided("Request requires Administrator credentials to proceed"))
                        }
                }
            }

    @PutMapping(
        path = ["/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.email == #id")
    fun updateUser(@Valid @RequestBody updatedUser: User, @PathVariable("id") id: String): Mono<User> =
        userService.hasAdminRole(updatedUser)
            .flatMap { hasAdminRole ->
                if(!hasAdminRole) {
                    userService.update(id, updatedUser)
                } else {
                    ReactiveSecurityContextHolder.getContext()
                        .flatMap { context ->
                            val principal = context.authentication?.principal
                            if (principal != null && principal is User) {
                                userService.hasAdminRole(principal)
                                    .flatMap { credentialsHasAdminRole ->
                                        if (credentialsHasAdminRole) {
                                            userService.update(id, updatedUser)
                                        } else {
                                            Mono.error(CredentialsNotProvided("Request requires Administrator credentials to proceed"))
                                        }
                                    }
                            } else {
                                Mono.error(CredentialsNotProvided("Invalid credentials"))
                            }
                        }
                        .switchIfEmpty {
                            Mono.error(CredentialsNotProvided("Request requires Administrator credentials to proceed"))
                        }
                }
            }

    @DeleteMapping(
        path = ["/{id}"]
    )
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.email == #id")
    fun deleteUser(@PathVariable("id") id: String): Mono<Any> =
        userService.delete(id)
}

@ControllerAdvice
class UserControllerExceptionHandler {
    @ExceptionHandler
    fun globalCatcher(ex: Exception): ResponseEntity<Any> {
        return when(ex) {
            is NotFoundException -> ResponseEntity(HttpStatus.NOT_FOUND)
            is InvalidCredentialsException -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            is EmailNotAvailableException -> ResponseEntity(HttpStatus.BAD_REQUEST)
            is AccessDeniedException -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            is BadCredentialsException -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            is CredentialsNotProvided -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            else -> ResponseEntity(ex.message, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}