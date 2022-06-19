package io.github.flowercup.ingestor.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration

@Service
class ExchangeDataRelay(val exchangeProducer: Sinks.Many<ExchangeData>) {
    fun publishMessage(exchangeData: ExchangeData) {
        exchangeProducer.tryEmitNext(exchangeData)
    }

    fun getStream(): Flux<ExchangeData> {
        return exchangeProducer.asFlux()
    }
}

@Configuration
class ExchangeDataRelayConfig {
    @Bean
    fun exchangeProducer(): Sinks.Many<ExchangeData> = Sinks.many().replay().limit(Duration.ofSeconds(30))
}