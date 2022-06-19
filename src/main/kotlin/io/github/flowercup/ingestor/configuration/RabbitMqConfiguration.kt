package io.github.flowercup.ingestor.configuration

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@EnableRabbit
class RabbitMqConfiguration() {
    @Bean
    fun exchangeQueue(): Queue {
        return Queue("exchange_queue", true)
    }

    @Bean
    fun exchangeExchange(): TopicExchange {
        return TopicExchange("ingestor_exchange", true, false)
    }

    @Bean
    fun exchangeBinding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with("exchange_queue")
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}

@Configuration
class RabbitMqQueueDeclaration(val rabbitAdmin: RabbitAdmin) {
    @PostConstruct
    fun declareQueues() {
        val exchangeDataExchange: Exchange = ExchangeBuilder.topicExchange("ingestor_exchange").durable(true).build()
        val exchangeQueue: Queue = QueueBuilder.durable("exchange_queue").build()
        rabbitAdmin.declareExchange(exchangeDataExchange)
        rabbitAdmin.declareQueue(exchangeQueue)
        rabbitAdmin.declareBinding(BindingBuilder.bind(exchangeQueue).to(exchangeDataExchange).with("exchange_queue").noargs())
    }
}