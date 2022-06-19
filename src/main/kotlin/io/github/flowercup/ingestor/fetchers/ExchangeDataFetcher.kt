package io.github.flowercup.ingestor.fetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsSubscription
import com.netflix.graphql.dgs.InputArgument
import io.github.flowercup.ingestor.service.ExchangeDataCandlestickDataService
import io.github.flowercup.ingestor.service.ExchangeDataRelay
import io.github.flowercup.ingestor.service.ExchangeDataService
import io.github.flowercup.ingestor.service.PaginationInfo
import io.github.flowercup.ingestor.types.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@DgsComponent
class ExchangeDataFetcher(
    val exchangeDataService: ExchangeDataService,
    val exchangeDataRelay: ExchangeDataRelay,
    val exchangeDataCandlestickDataService: ExchangeDataCandlestickDataService
) {
    @DgsQuery
    fun exchangeData(
        @InputArgument first: Int,
        @InputArgument after: String?,
        @InputArgument filter: ExchangeDataFilterInput?,
        @InputArgument orderBy: ExchangeDataOrderByInput?
    ): Mono<ExchangeDataConnection> {
        return exchangeDataService.paginate(PaginationInfo(after, first), filter, orderBy, null)
            .map { cursorRecord ->
                ExchangeDataEdge(cursorRecord.dto, cursorRecord.cursor)
            }.collectList()
            .flatMap { edges ->
                Mono.just(ExchangeDataConnection(edges))
            }
    }

    @DgsQuery
    fun exchangeDataCandlestick(
        @InputArgument from: Long,
        @InputArgument to: Long?,
        @InputArgument exchange: ExchangeEnum,
        @InputArgument coin: String
    ): Flux<ExchangeDataCandlestick> {
        return if (to != null) {
            exchangeDataCandlestickDataService.paginate(from, to, exchange, coin)

        } else {
            val now = Instant.now().toEpochMilli()
            exchangeDataCandlestickDataService.paginate(from, now, exchange, coin)
        }
    }

    @DgsQuery
    fun exchangePriceDelta(
        @InputArgument exchange: ExchangeEnum,
        @InputArgument coin: String,
        @InputArgument from: Long?,
        @InputArgument to: Long?,
        @InputArgument(name = "lastMs") lastMs: Long?
    ): Mono<ExchangeDataDelta> {
        if (lastMs != null) {
            val nowTime: Long = Instant.now().toEpochMilli()
            val fromTime: Long = nowTime - lastMs
            return exchangeDataService.getPriceDelta(exchange, coin, fromTime, nowTime)
        }
        return if (to != null && from != null) {
            exchangeDataService.getPriceDelta(exchange, coin, from, to)

        } else if (from != null) {
            val now = Instant.now().toEpochMilli()
            exchangeDataService.getPriceDelta(exchange, coin, from, now)
        } else {
            Mono.error(Throwable("Invalid parameters"))
        }
    }

    @DgsQuery
    fun exchangePriceIncrease(
        @InputArgument exchange: ExchangeEnum,
        @InputArgument coin: String,
        @InputArgument from: Long?,
        @InputArgument to: Long?,
        @InputArgument(name = "lastMs") lastMs: Long?
    ): Mono<ExchangeDataIncrease> {
        if (lastMs != null) {
            val nowTime: Long = Instant.now().toEpochMilli()
            val fromTime: Long = nowTime - lastMs
            return exchangeDataService.getPriceIncrease(exchange, coin, fromTime, nowTime)
        }
        return if (to != null && from != null) {
            exchangeDataService.getPriceIncrease(exchange, coin, from, to)

        } else if (from != null) {
            val now = Instant.now().toEpochMilli()
            exchangeDataService.getPriceIncrease(exchange, coin, from, now)
        } else {
            Mono.error(Throwable("Invalid parameters"))
        }
    }

    @DgsSubscription
    fun exchangeData(
        @InputArgument exchange: ExchangeEnum,
        @InputArgument coin: String
    ): Flux<ExchangeData> {
        return exchangeDataRelay.getStream()
            .filter { exchangeData ->
                exchangeData.exchangeName.lowercase() == exchange.name.lowercase() && exchangeData.coin.lowercase() == coin.lowercase()
            }
            .map {
                ExchangeData(it.exchangeName, it.currentValue.toInt(), it.coin, it.timestamp.toInt())
            }
    }


}