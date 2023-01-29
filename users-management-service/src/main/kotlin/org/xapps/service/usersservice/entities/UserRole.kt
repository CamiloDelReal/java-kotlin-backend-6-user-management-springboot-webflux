package org.xapps.service.usersservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

//@RedisHash("users_roles")
data class UserRole(
    @Id
    @Indexed
    var id: UserRoleId
) {
    data class UserRoleId(
        @Indexed
        var userId: Long,

        @Indexed
        var roleId: Long
    )
}