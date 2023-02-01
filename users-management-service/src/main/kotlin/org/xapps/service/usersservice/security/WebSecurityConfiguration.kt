package org.xapps.service.usersservice.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.xapps.service.usersservice.services.UserService
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebSecurityConfiguration {

    @Bean
    fun provideSecurityWebFilterChain(
        http: ServerHttpSecurity,
        securityContextRepository: SecurityContextRepository,
        authenticationManager: ReactiveAuthenticationManager,
        securityParams: SecurityParams,
        objectMapper: ObjectMapper
    ): SecurityWebFilterChain {
        http
            .exceptionHandling()
            .authenticationEntryPoint { exchange, ex ->
                Mono.fromRunnable {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                }
            }
            .accessDeniedHandler { exchange, denied ->
                Mono.fromRunnable {
                    exchange.response.statusCode = HttpStatus.FORBIDDEN
                }
            }
            .and()
            .csrf().disable()
            .authenticationManager(authenticationManager)
            .authorizeExchange()
            .pathMatchers(HttpMethod.POST, "/users/login").permitAll()
            .pathMatchers(HttpMethod.POST, "/users").permitAll()
            .anyExchange().authenticated()
            .and()
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .addFilterBefore(provideAuthorizationFilter(securityParams, objectMapper), SecurityWebFiltersOrder.AUTHENTICATION)
        return http.build()
    }

    @Bean
    fun authenticationManager(@Lazy userDetailsService: UserService): ReactiveAuthenticationManager? {
        val authenticationManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
        authenticationManager.setPasswordEncoder(providePasswordEncoder())
        return authenticationManager
    }

    @Bean
    fun providePasswordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun provideAuthorizationFilter(
        securityParams: SecurityParams,
        objectMapper: ObjectMapper
    ): AuthorizationFilter {
        return AuthorizationFilter(securityParams, objectMapper)
    }

}