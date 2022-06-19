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
data class TweetData(val created: String, val tag: String, val sentiment: Double, val text: String) {
}

class TweetMessageListener(val emitter: FluxSink<TweetData>, val objectMapper: ObjectMapper) : ChannelAwareMessageListener {
    override fun onMessage(message: Message, channel: Channel?) {
        val payload = String(message.body)
        val tweetData = objectMapper.readValue(payload, TweetData::class.java)
        println(payload)
        emitter.next(tweetData)
        channel?.basicAck(message.messageProperties.deliveryTag, false)
    }
}

@Component
class TweetListener(messageListenerContainerFactory: MessageListenerContainerFactory, val objectMapper: ObjectMapper) {
    private val exchangeEventListener = messageListenerContainerFactory.createMessageListenerContainer("tweetsProcessed")

    fun exchangeEvents(): Flux<TweetData> {
        return Flux.create { emitter: FluxSink<TweetData> ->
            exchangeEventListener.setupMessageListener(TweetMessageListener(emitter, objectMapper))
            emitter.onRequest {
                exchangeEventListener.start()
            }
            emitter.onDispose {
                exchangeEventListener.stop()
            }
        }
    }

}