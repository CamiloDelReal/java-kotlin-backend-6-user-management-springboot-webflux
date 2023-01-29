package org.xapps.service.usersservice.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.services.UserService
import org.xapps.service.usersservice.services.exceptions.NotFoundException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
    fun getRoles(): ResponseEntity<Flux<Role>> =
        ResponseEntity.ok(userService.getAllRoles())

    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getUsers(): ResponseEntity<Flux<User>> {
        return ResponseEntity.ok(userService.getAll())
    }

    @GetMapping(
        path = ["/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getUser(@PathVariable("id") id: String): ResponseEntity<Mono<User>> {
        return ResponseEntity.ok(userService.getById(id))
    }

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createUser(@Valid @RequestBody newUser: User): ResponseEntity<Mono<User>> {
        return ResponseEntity.ok(userService.create(newUser))
    }

    @PutMapping(
        path = ["/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateUser(@PathVariable("id") id: String, @Valid @RequestBody modifiedUser: User): ResponseEntity<Mono<User>> {
        TODO()
    }

    @DeleteMapping(
        path = ["/{id}"]
    )
    fun deleteUser(@PathVariable("id") id: String): ResponseEntity<Mono<Any>> {
        return ResponseEntity.ok(userService.delete(id))
    }
}

@ControllerAdvice
class UserControllerExceptionHandler {
    @ExceptionHandler
    fun globalCatcher(ex: Exception): ResponseEntity<Any> {
        return when(ex) {
            is NotFoundException -> ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
            else -> ResponseEntity(ex.message, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}