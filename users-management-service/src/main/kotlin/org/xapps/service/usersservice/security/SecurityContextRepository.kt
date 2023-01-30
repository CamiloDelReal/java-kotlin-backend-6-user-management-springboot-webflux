package org.xapps.service.usersservice.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class SecurityContextRepository @Autowired constructor(
        @Lazy private val authenticationManager: ReactiveAuthenticationManager
) : ServerSecurityContextRepository {

    override fun save(swe: ServerWebExchange, sc: SecurityContext): Mono<Void> {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun load(swe: ServerWebExchange): Mono<SecurityContext> {
        return Mono.justOrEmpty(swe.request.headers.getFirst(HttpHeaders.AUTHORIZATION))
            .filter { authHeader -> authHeader.startsWith("Bearer ") }
            .flatMap { authHeader ->
                val authToken: String = authHeader.substring(7)
                val auth: Authentication = UsernamePasswordAuthenticationToken(authToken, authToken)
                authenticationManager.authenticate(auth).map { SecurityContextImpl() }
            }
    }
}