package org.xapps.service.usersservice.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.annotations.NotNull

data class Login(
    @NotNull
    @JsonProperty(value = "email")
    val email: String,

    @NotNull
    @JsonProperty(value = "password")
    val password: String
)