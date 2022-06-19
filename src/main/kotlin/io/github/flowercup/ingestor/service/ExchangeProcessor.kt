package io.github.flowercup.ingestor.service

import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA
import io.github.flowercup.ingestor.util.toOffsetDatetime
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.annotation.PostConstruct

@Service
class ExchangeProcessor(val exchangeListener: ExchangeListener, val dslContext: DSLContext) {
    @PostConstruct
    fun startExchangeProcessor() {
        exchangeListener.exchangeEvents().subscribe { event ->
            Mono.from(
                dslContext.insertInto(EXCHANGE_DATA)
                    .columns(EXCHANGE_DATA.TIMESTAMP, EXCHANGE_DATA.VALUE, EXCHANGE_DATA.COIN, EXCHANGE_DATA.EXCHANGE, EXCHANGE_DATA.VOLUME)
                    .values(
                        event.timestamp.toOffsetDatetime(),
                        event.currentValue,
                        event.coin,
                        event.exchangeName,
                        event.volume
                    )
            ).subscribe()
        }.run { }
    }
}