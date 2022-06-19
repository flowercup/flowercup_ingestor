package io.github.flowercup.ingestor.configuration

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfig(val connectionFactory: ConnectionFactory) {
    @Bean
    fun jooqDSLContext(): DSLContext {
        return DSL.using(connectionFactory, SQLDialect.POSTGRES).dsl()
    }
}