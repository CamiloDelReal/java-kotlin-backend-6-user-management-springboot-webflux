package org.xapps.service.usersservice.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class SecurityParams(
    @Value("\${security.token.key}")
    val key: String,

    @Value("\${security.token.validity}")
    val validity: Long
)