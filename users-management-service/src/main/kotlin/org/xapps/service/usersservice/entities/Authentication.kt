package org.xapps.service.usersservice.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class Authentication(
    @JsonProperty(value = "token")
    val token: String,

    @JsonProperty(value = "expiration")
    val expiration: Long
)