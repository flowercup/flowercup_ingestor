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
data class ExchangeData(val timestamp: Long, val currentValue: Long, val coin: String, val exchangeName: String, val volume: Double) {
    fun convertTimestampToMillis() {
        this.timestamp.toMillisFromPicos()
    }
}

class ExchangeMessageListener(val emitter: FluxSink<ExchangeData>, val objectMapper: ObjectMapper, val exchangeDataRelay: ExchangeDataRelay) : ChannelAwareMessageListener {
    override fun onMessage(message: Message, channel: Channel?) {
        val payload = String(message.body)
        val exchangeData = objectMapper.readValue(payload, ExchangeData::class.java)
        exchangeData.convertTimestampToMillis()
        println(payload)
        emitter.next(exchangeData)
        exchangeDataRelay.publishMessage(exchangeData)
        channel?.basicAck(message.messageProperties.deliveryTag, false)
    }
}

@Component
class ExchangeListener(messageListenerContainerFactory: MessageListenerContainerFactory, val objectMapper: ObjectMapper, val exchangeDataRelay: ExchangeDataRelay) {
    private val exchangeEventListener = messageListenerContainerFactory.createMessageListenerContainer("exchange_queue")

    fun exchangeEvents(): Flux<ExchangeData> {
        return Flux.create { emitter: FluxSink<ExchangeData> ->
            exchangeEventListener.setupMessageListener(ExchangeMessageListener(emitter, objectMapper, exchangeDataRelay))
            emitter.onRequest {
                exchangeEventListener.start()
            }
            emitter.onDispose {
                exchangeEventListener.stop()
            }
        }
    }

}