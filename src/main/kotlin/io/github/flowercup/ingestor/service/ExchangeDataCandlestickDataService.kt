package io.github.flowercup.ingestor.service

import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA_CANDLESTICK
import io.github.flowercup.ingestor.types.ExchangeDataCandlestick
import io.github.flowercup.ingestor.types.ExchangeEnum
import io.github.flowercup.ingestor.util.toLong
import io.github.flowercup.ingestor.util.toOffsetDatetime
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ExchangeDataCandlestickDataService(val dslContext: DSLContext) {
    fun paginate(from: Long, to: Long, exchange: ExchangeEnum, coin: String): Flux<ExchangeDataCandlestick> {
        return Flux.from(
            dslContext.selectFrom(EXCHANGE_DATA_CANDLESTICK)
                .where(EXCHANGE_DATA_CANDLESTICK.EXCHANGE.equalIgnoreCase(exchange.name))
                .and(EXCHANGE_DATA_CANDLESTICK.COIN.equalIgnoreCase(coin))
                .and(EXCHANGE_DATA_CANDLESTICK.BUCKET.between(from.toOffsetDatetime(), to.toOffsetDatetime()))
                .orderBy(EXCHANGE_DATA_CANDLESTICK.BUCKET.desc())
        )
            .map {
                ExchangeDataCandlestick(
                    it.bucket?.toLong(),
                    it.exchange,
                    it.coin,
                    it.open,
                    it.high,
                    it.low,
                    it.close
                )
            }
    }
}