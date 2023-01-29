package org.xapps.service.usersservice.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.xapps.service.usersservice.entities.Role.Companion.TABLE_NAME
import java.util.UUID

@RedisHash(TABLE_NAME)
data class Role(
    @Id
    @Indexed
    @JsonProperty(value = "id")
    var id: String = UUID.randomUUID().toString(),

    @Indexed
    @JsonProperty(value = "name")
    var name: String = ""
) {
    companion object {
        const val TABLE_NAME = "roles"
        const val ADMINISTRATOR = "Administrator"
        const val GUEST = "Guest"
    }
}