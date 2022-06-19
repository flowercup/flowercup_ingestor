package io.github.flowercup.ingestor.service

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.MessageListenerContainer
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.stereotype.Component


@Component
class MessageListenerContainerFactory(val connectionFactory: ConnectionFactory) {
    fun createMessageListenerContainer(queueName: String?): MessageListenerContainer {
        val mlc = SimpleMessageListenerContainer(connectionFactory)
        mlc.addQueueNames(queueName)
        mlc.acknowledgeMode = AcknowledgeMode.MANUAL
        return mlc
    }
}