package org.xapps.service.usersservice.integrations

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.config.EnableWebFlux
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testng.annotations.Test
import org.xapps.service.usersservice.entities.Authentication
import org.xapps.service.usersservice.entities.Login
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User
import java.util.*


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EnableWebFlux
@AutoConfigureMockMvc
class UserManagementControllerServerSideTests : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private val mapper = ObjectMapper()

    private var adminToken: String? = null
    private var userCreatedWithDefaultRole: User? = null
    private var userCreatedWithDefaultRolePassword: String? = null

    companion object {

        @JvmStatic
        @Container
        val redisContainer: GenericContainer<*> = GenericContainer(
                DockerImageName.parse("redis:5.0.3-alpine")
        )
            .withReuse(true)
            .withExposedPorts(6379)

        init {
            redisContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun setDatasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.redis.host", redisContainer::getHost)
            registry.add("spring.redis.port", { redisContainer.getMappedPort(6379) } )
        }

    }

    @Test
    @Throws(Exception::class)
    fun loginRoot_success() {
        val loginRequest = Login("root@gmail.com", "123456")
        val loginResponse = webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<Authentication>()
            .returnResult()
        Assertions.assertNotNull(loginResponse.responseBody)
        val authentication = loginResponse.responseBody
        Assertions.assertNotNull(authentication?.token)
        Assertions.assertNotNull(authentication?.expiration)
        adminToken = authentication!!.token
    }

    @Test
    @Throws(Exception::class)
    fun loginRoot_failByInvalidPassword() {
        val loginRequest = Login("root@gmail.com", "invalid")
        webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @Throws(Exception::class)
    fun login_failByInvalidCredentials() {
        val loginRequest = Login("invalid@gmail.com", "12345")
        webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @Throws(Exception::class)
    fun createUserWithDefaultRole_success() {
        val testPassword = "qwerty"
        val userRequest = User("johndoe@gmail.com", testPassword, "John Doe")
        val result = webTestClient
            .post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<User>()
            .returnResult()
        Assertions.assertNotNull(result.responseBody)
        val userResponse = result.responseBody
        Assertions.assertEquals(userRequest.email, userResponse?.email)
        Assertions.assertEquals(userRequest.name, userResponse?.name)
        Assertions.assertNotNull(userResponse?.roles)
        Assertions.assertEquals(1, userResponse?.roles?.size)
        Assertions.assertTrue(userResponse?.roles?.any { it.value == Role.GUEST } == true)
        userCreatedWithDefaultRole = userResponse
        userCreatedWithDefaultRolePassword = testPassword
    }

    @Test
    @Throws(Exception::class)
    fun createUserWithAdminRole_failByNoAdminCredentials() {
        val userRequest = User("janedoe@gmail.com", "qwerty", "Jane Doe", listOf(Role(Role.ADMINISTRATOR)))
        webTestClient
            .post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test(dependsOnMethods = ["loginRoot_success"])
    @Throws(Exception::class)
    fun createUserWithAdminRole_success() {
        val userRequest = User("kathdoe@gmail.com", "qwerty", "Kath Doe", listOf(Role(Role.ADMINISTRATOR)))
        val result = webTestClient
            .post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<User>()
            .returnResult()
        Assertions.assertNotNull(result.responseBody)
        val userResponse = result.responseBody
        Assertions.assertEquals(userRequest.email, userResponse?.email)
        Assertions.assertEquals(userRequest.name, userResponse?.name)
        Assertions.assertNotNull(userResponse?.roles)
        Assertions.assertEquals(1, userResponse?.roles?.size)
        Assertions.assertTrue(userResponse?.roles?.any { it.value == Role.ADMINISTRATOR } == true)
    }

    @Test
    @Throws(Exception::class)
    fun createUser_failByEmailDuplicity() {
        val userRequest = User("root@gmail.com", "qwerty", "Root2")
        webTestClient
            .post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun editUserWithUserCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val testPassword = "12345"
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val loginEditResult = webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginEditRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<Authentication>()
            .returnResult()
        val userEditRequest = User("annadoe@gmail.com", testPassword, "Anna Doe")
        val token = loginEditResult.responseBody!!.token
        val userEditResult = webTestClient
            .put()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(userEditRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<User>()
            .returnResult()
        userCreatedWithDefaultRole = userEditResult.responseBody
        userCreatedWithDefaultRolePassword = testPassword
        val loginCheckRequest = Login(userEditRequest.email, userEditRequest.password)
        webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginCheckRequest)
            .exchange()
            .expectStatus().isOk
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun editUserWithUserCredentials_failByWrongId() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val loginResult = webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginEditRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<Authentication>()
            .returnResult()
        val userEditRequest = User("miadoe@gmail.com", "asdfg", "Mia Doe")
        val token = loginResult.responseBody!!.token
        webTestClient
            .put()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email + "wrong")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(userEditRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun editUserToAdminWithUserCredentials_failByNoAdminCredentials() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val loginEditResult = webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginEditRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<Authentication>()
            .returnResult()
        val token = loginEditResult.responseBody!!.token
        val userEditRequest = User("bethdoe@gmail.com", "poiuy", "Beth Doe", listOf(Role(Role.ADMINISTRATOR)))
        webTestClient
            .put()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(userEditRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun editUser_failByNoUserCredentials() {
        val userEditRequest = User("annadoe@gmail.com", "12345", "Anna Doe")
        webTestClient
            .put()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(userEditRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test(dependsOnMethods = ["loginRoot_success", "createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun editUserWithAdminCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val testPassword = "zxcvb"
        val userEditRequest = User("sarahdoe@gmail.com", testPassword, "Sarah Doe")
        val userEditResult = webTestClient
            .put()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .bodyValue(userEditRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<User>()
            .returnResult()
        userCreatedWithDefaultRole = userEditResult.responseBody
        Assertions.assertEquals(userEditRequest.email, userCreatedWithDefaultRole?.email)
        Assertions.assertEquals(userEditRequest.name, userCreatedWithDefaultRole?.name)
        Assertions.assertNotNull(userCreatedWithDefaultRole?.roles)
        Assertions.assertEquals(1, userCreatedWithDefaultRole?.roles?.size)
        Assertions.assertTrue(userCreatedWithDefaultRole?.roles?.any { it.value == Role.GUEST } == true)
        userCreatedWithDefaultRolePassword = testPassword
        val loginCheckRequest = Login(userEditRequest.email, userEditRequest.password)
        webTestClient
            .post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(loginCheckRequest)
            .exchange()
            .expectStatus().isOk
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    @Throws(Exception::class)
    fun deleteUser_failByNoCredentials() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        webTestClient
            .delete()
            .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success", "editUserToAdminWithUserCredentials_failByNoAdminCredentials", "editUserWithUserCredentials_failByWrongId", "editUserWithUserCredentials_success", "editUser_failByNoUserCredentials", "editUserWithAdminCredentials_success", "deleteUser_failByNoCredentials"])
    @Throws(Exception::class)
    fun deleteUserWithUserCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginDeleteAndCheckRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val loginDeleteResult = webTestClient
                .post()
                .uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(loginDeleteAndCheckRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody<Authentication>()
                .returnResult()
        val token = loginDeleteResult.responseBody!!.token
        webTestClient
                .delete()
                .uri("/users/{id}", userCreatedWithDefaultRole!!.email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
        webTestClient
                .post()
                .uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(loginDeleteAndCheckRequest)
                .exchange()
                .expectStatus().isUnauthorized
        userCreatedWithDefaultRole = null
        userCreatedWithDefaultRolePassword = null
    }
}