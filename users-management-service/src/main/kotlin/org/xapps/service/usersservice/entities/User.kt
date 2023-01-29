package org.xapps.service.usersservice.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Reference
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.lang.Nullable
import org.xapps.service.usersservice.entities.User.Companion.TABLE_NAME
import java.util.UUID

@RedisHash(TABLE_NAME)
data class User(
        @Id
        @Indexed
        @NotNull
        @JsonProperty(value = "id")
        var id: String = UUID.randomUUID().toString(),

        @Indexed
        @NotNull
        @JsonProperty(value = "email")
        var email: String = "",

        @NotNull
        @JsonProperty(value = "password", access = JsonProperty.Access.READ_WRITE)
        var password: String = "",

        @NotNull
        @JsonProperty(value = "name")
        var name: String = "",

//        @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.DETACH])
//        @JoinTable(name = "users_roles",
//                joinColumns = [JoinColumn(name = "userId", referencedColumnName = "id")],
//                inverseJoinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")])
        @Reference
        @JsonProperty(value = "roles")
        var roles: List<Role> = emptyList()

) {
        companion object {
                const val TABLE_NAME = "users"
        }
}