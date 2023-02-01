package org.xapps.service.usersservice.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.xapps.service.usersservice.entities.User
import org.xapps.service.usersservice.utils.lazyLogger
import reactor.core.publisher.Mono

@Component
class AuthorizationFilter @Autowired constructor(
    private val securityParams: SecurityParams,
    private val objectMapper: ObjectMapper
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val authzHeaders: List<String>? = request.headers[HttpHeaders.AUTHORIZATION]
        logger.info("Authz headr $authzHeaders")
        var authentication: Authentication? = null
        if(!authzHeaders.isNullOrEmpty()) {
            authentication = authzHeaders.firstNotNullOfOrNull { authzHeader ->
                getAuthentication(authzHeader)
            }
            if (authentication == null) {
                logger.info("Request unauthorized")
                response.statusCode = HttpStatus.UNAUTHORIZED
            }
        }
        return if (authentication != null) {
            chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } else {
            chain.filter(exchange)
        }
    }

    private fun getAuthentication(authzHeader: String): UsernamePasswordAuthenticationToken? {
        var auth: UsernamePasswordAuthenticationToken? = null
        val token: String = authzHeader.replace("Bearer", "")
        try {
            val claims: Claims = Jwts.parser()
                    .setSigningKey(securityParams.key)
                    .parseClaimsJws(token)
                    .body
            val subject = claims.subject
            val user: User = objectMapper.readValue(subject, User::class.java)
            logger.info("User in token $user")
            val authorities: List<GrantedAuthority> = user.roles.map { role -> SimpleGrantedAuthority(role.value) }
            auth = UsernamePasswordAuthenticationToken(user, null, authorities)
        } catch (ex: Exception) {
            logger.info("Exception captured", ex)
        }
        return auth
    }

    companion object {
        private val logger by lazyLogger()
    }
}