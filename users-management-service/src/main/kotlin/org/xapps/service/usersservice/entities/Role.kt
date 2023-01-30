package org.xapps.service.usersservice.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.xapps.service.usersservice.entities.Role.Companion.TABLE_NAME

@RedisHash(TABLE_NAME)
data class Role(
    @Id
    @Indexed
    @JsonProperty(value = "value")
    var value: String = ""
) {
    companion object {
        const val TABLE_NAME = "roles"
        const val ADMINISTRATOR = "Administrator"
        const val GUEST = "Guest"
    }
}