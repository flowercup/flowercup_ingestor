package io.github.flowercup.ingestor.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.rabbitmq.client.Channel
import io.github.flowercup.ingestor.util.toMillisFromPicos
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

@JsonSerialize
data class RedditData(val text: String, val time: Long, val sentiment: Double, val entitiesList: List<String>?)

class RedditMessageListener(val emitter: FluxSink<RedditData>, val objectMapper: ObjectMapper) :
    ChannelAwareMessageListener {
    override fun onMessage(message: Message, channel: Channel?) {
        val payload = String(message.body)
        val tweetData = objectMapper.readValue(payload, RedditData::class.java)
        println(payload)
        emitter.next(tweetData)
        channel?.basicAck(message.messageProperties.deliveryTag, false)
    }
}

@Component
class RedditListener(messageListenerContainerFactory: MessageListenerContainerFactory, val objectMapper: ObjectMapper) {
    private val exchangeEventListener = messageListenerContainerFactory.createMessageListenerContainer("postsProcessed")

    fun exchangeEvents(): Flux<RedditData> {
        return Flux.create { emitter: FluxSink<RedditData> ->
            exchangeEventListener.setupMessageListener(RedditMessageListener(emitter, objectMapper))
            emitter.onRequest {
                exchangeEventListener.start()
            }
            emitter.onDispose {
                exchangeEventListener.stop()
            }
        }
    }

}