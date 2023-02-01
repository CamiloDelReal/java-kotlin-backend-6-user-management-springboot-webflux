package org.xapps.service.usersservice.integrations

import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testng.annotations.Test
import org.xapps.service.usersservice.entities.Authentication
import org.xapps.service.usersservice.entities.Login
import org.xapps.service.usersservice.entities.Role
import org.xapps.service.usersservice.entities.User


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserManagementControllerClientSideTests : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

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
    fun loginRoot_success() {
        val loginRequest = Login("root@gmail.com", "123456")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<Login>(loginRequest, headers)
        val response = restTemplate.exchange("/users/login", HttpMethod.POST, request, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertNotNull(response.body)
        val authentication = response.body
        Assertions.assertNotNull(authentication?.token)
        Assertions.assertNotNull(authentication?.expiration)
        adminToken = authentication!!.token
    }

    @Test
    fun loginRoot_failByInvalidPassword() {
        val loginRequest = Login("root@gmail.com", "invalid")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<Login>(loginRequest, headers)
        val response = restTemplate.exchange("/users/login", HttpMethod.POST, request, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun login_failByInvalidCredentials() {
        val loginRequest = Login("invalid@gmail.com", "12345")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<Login>(loginRequest, headers)
        val response = restTemplate.exchange("/users/login", HttpMethod.POST, request, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun createUserWithDefaultRole_success() {
        val testPassword = "qwerty"
        val userRequest = User("johndoe@gmail.com", testPassword, "John Doe")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<User>(userRequest, headers)
        val response = restTemplate.exchange("/users", HttpMethod.POST, request, User::class.java)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertNotNull(response.body)
        val userResponse = response.body
        Assertions.assertEquals(userRequest.email, userResponse?.email)
        Assertions.assertEquals(userRequest.name, userResponse?.name)
        Assertions.assertNotNull(userResponse?.roles)
        Assertions.assertEquals(1, userResponse?.roles?.size)
        Assertions.assertTrue(userResponse?.roles?.any { it.value == Role.GUEST } == true)
        userCreatedWithDefaultRole = userResponse
        userCreatedWithDefaultRolePassword = testPassword
    }

    @Test
    fun createUserWithAdminRole_failByNoAdminCredentials() {
        val userRequest = User("janedoe@gmail.com", "qwerty", "Jane Doe", listOf(Role(Role.ADMINISTRATOR)))
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<User>(userRequest, headers)
        val response = restTemplate.exchange("/users", HttpMethod.POST, request, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test(dependsOnMethods = ["loginRoot_success"])
    fun createUserWithAdminRole_success() {
        val userRequest = User("kathdoe@gmail.com", "qwerty", "Kath Doe", listOf(Role(Role.ADMINISTRATOR)))
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
        val request = HttpEntity<User>(userRequest, headers)
        val response = restTemplate.exchange("/users", HttpMethod.POST, request, User::class.java)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertNotNull(response.body)
        val userResponse = response.body
        Assertions.assertEquals(userRequest.email, userResponse?.email)
        Assertions.assertEquals(userRequest.name, userResponse?.name)
        Assertions.assertNotNull(userResponse?.roles)
        Assertions.assertEquals(1, userResponse?.roles?.size)
        Assertions.assertTrue(userResponse?.roles?.any { it.value == Role.ADMINISTRATOR } == true)
    }

    @Test
    fun createUser_failByEmailDuplicity() {
        val userRequest = User("root@gmail.com", "qwerty", "Root2")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val request = HttpEntity<User>(userRequest, headers)
        val response = restTemplate.exchange("/users", HttpMethod.POST, request, User::class.java)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    fun editUserWithUserCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val testPassword = "12345"
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val userEditRequest = User("annadoe@gmail.com", testPassword, "Anna Doe")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val loginRequest = HttpEntity<Login>(loginEditRequest, headers)
        val loginEditResponse = restTemplate.exchange("/users/login", HttpMethod.POST, loginRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginEditResponse.statusCode)
        val token: String = loginEditResponse.body!!.token
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val editRequest = HttpEntity<User>(userEditRequest, headers)
        val editResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.PUT, editRequest, User::class.java)
        Assertions.assertEquals(HttpStatus.OK, editResponse.statusCode)
        Assertions.assertNotNull(editResponse.body)
        val userResponse = editResponse.body
        Assertions.assertEquals(userEditRequest.email, userResponse?.email)
        Assertions.assertEquals(userEditRequest.name, userResponse?.name)
        Assertions.assertNotNull(userResponse?.roles)
        Assertions.assertEquals(1, userResponse?.roles?.size)
        Assertions.assertTrue(userResponse?.roles?.any { it.value == userCreatedWithDefaultRole!!.roles[0].value } == true)
        userCreatedWithDefaultRole = userResponse
        userCreatedWithDefaultRolePassword = testPassword
        val loginCheckRequest = Login(userEditRequest.email, userEditRequest.password)
        headers.remove(HttpHeaders.AUTHORIZATION)
        val checkRequest = HttpEntity<Login>(loginCheckRequest, headers)
        val loginCheckResponse = restTemplate.exchange("/users/login", HttpMethod.POST, checkRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginCheckResponse.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    fun editUserWithUserCredentials_failByWrongId() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val userEditRequest = User("miadoe@gmail.com", "asdfg", "Mia Doe")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val loginRequest = HttpEntity<Login>(loginEditRequest, headers)
        val loginEditResponse = restTemplate.exchange("/users/login", HttpMethod.POST, loginRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginEditResponse.statusCode)
        val token: String = loginEditResponse.body!!.token
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val editRequest = HttpEntity<User>(userEditRequest, headers)
        val editResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole}fail", HttpMethod.PUT, editRequest, User::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, editResponse.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    fun editUserToAdminWithUserCredentials_failByNoAdminCredentials() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginEditRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val userEditRequest = User("bethdoe@gmail.com", "poiuy", "Beth Doe", listOf(Role(Role.ADMINISTRATOR)))
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val loginRequest = HttpEntity<Login>(loginEditRequest, headers)
        val loginEditResponse = restTemplate.exchange("/users/login", HttpMethod.POST, loginRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginEditResponse.statusCode)
        val token: String = loginEditResponse.body!!.token
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val editRequest= HttpEntity<User>(userEditRequest, headers)
        val editResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.PUT, editRequest, User::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, editResponse.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    fun editUser_failByNoUserCredentials() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val userEditRequest = User("annadoe@gmail.com", "12345", "Anna Doe")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val editRequest = HttpEntity<User>(userEditRequest, headers)
        val editResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.PUT, editRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, editResponse.statusCode)
    }

    @Test(dependsOnMethods = ["loginRoot_success", "createUserWithDefaultRole_success"])
    fun editUserWithAdminCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val testPassword = "zxcvb"
        val userEditRequest = User("sarahdoe@gmail.com", testPassword, "Sarah Doe")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
        val editRequest = HttpEntity<User>(userEditRequest, headers)
        val editResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.PUT, editRequest, User::class.java)
        Assertions.assertEquals(HttpStatus.OK, editResponse.statusCode)
        Assertions.assertNotNull(editResponse.body)
        val userResponse = editResponse.body!!
        Assertions.assertEquals(userEditRequest.email, userResponse.email)
        Assertions.assertEquals(userEditRequest.name, userResponse.name)
        Assertions.assertNotNull(userResponse.roles)
        Assertions.assertEquals(1, userResponse.roles.size)
        Assertions.assertTrue(userResponse.roles.any { it.value == userCreatedWithDefaultRole!!.roles[0].value })
        userCreatedWithDefaultRole = userResponse
        userCreatedWithDefaultRolePassword = testPassword
        val loginCheckRequest = Login(userEditRequest.email, userEditRequest.password)
        headers.remove(HttpHeaders.AUTHORIZATION)
        val checkRequest = HttpEntity<Login>(loginCheckRequest, headers)
        val loginCheckResponse = restTemplate.exchange("/users/login", HttpMethod.POST, checkRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginCheckResponse.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success"])
    fun deleteUser_failByNoCredentials() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val deleteRequest = HttpEntity<User>(headers)
        val deleteResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.DELETE, deleteRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, deleteResponse.statusCode)
    }

    @Test(dependsOnMethods = ["createUserWithDefaultRole_success", "editUserToAdminWithUserCredentials_failByNoAdminCredentials", "editUserWithUserCredentials_failByWrongId", "editUserWithUserCredentials_success", "editUser_failByNoUserCredentials", "editUserWithAdminCredentials_success", "deleteUser_failByNoCredentials"])
    fun deleteUserWithUserCredentials_success() {
        Assertions.assertNotNull(userCreatedWithDefaultRole)
        Assertions.assertNotNull(userCreatedWithDefaultRolePassword)
        val loginDeleteAndCheckRequest = Login(userCreatedWithDefaultRole!!.email, userCreatedWithDefaultRolePassword!!)
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        val loginRequest = HttpEntity<Login>(loginDeleteAndCheckRequest, headers)
        val loginEditResponse = restTemplate.exchange("/users/login", HttpMethod.POST, loginRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.OK, loginEditResponse.statusCode)
        val token: String = loginEditResponse.body!!.token
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val deleteRequest= HttpEntity<User>(headers)
        val deleteResponse = restTemplate.exchange("/users/${userCreatedWithDefaultRole!!.email}", HttpMethod.DELETE, deleteRequest, User::class.java)
        Assertions.assertEquals(HttpStatus.OK, deleteResponse.statusCode)
        Assertions.assertNull(deleteResponse.body)
        headers.remove(HttpHeaders.AUTHORIZATION)
        val checkRequest = HttpEntity<Login>(loginDeleteAndCheckRequest, headers)
        val loginCheckResponse = restTemplate.exchange("/users/login", HttpMethod.POST, checkRequest, Authentication::class.java)
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, loginCheckResponse.statusCode)
        userCreatedWithDefaultRole = null
        userCreatedWithDefaultRolePassword = null
    }
}