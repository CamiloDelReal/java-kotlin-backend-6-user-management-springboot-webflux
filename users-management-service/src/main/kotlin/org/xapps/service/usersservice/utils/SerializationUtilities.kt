package org.xapps.service.usersservice.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class SerializationUtilities {

    @Bean
    fun provideObjectMapper(): ObjectMapper =
        ObjectMapper()

}