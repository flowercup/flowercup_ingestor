package io.github.flowercup.ingestor.fetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsSubscription
import com.netflix.graphql.dgs.InputArgument
import io.github.flowercup.ingestor.db.routines.references.exchangeDataDelta
import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA_DELTA
import io.github.flowercup.ingestor.service.*
import io.github.flowercup.ingestor.types.*
import io.github.flowercup.ingestor.types.ExchangeData
import io.github.flowercup.ingestor.types.RedditData
import io.github.flowercup.ingestor.types.TweetData
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@DgsComponent
class ExchangeDataFetcher(
    val exchangeDataService: ExchangeDataService,
    val exchangeDataRelay: ExchangeDataRelay,
    val exchangeDataCandlestickDataService: ExchangeDataCandlestickDataService,
    val tweetService: TweetService,
    val redditService: RedditService,
    val dslContext: DSLContext
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
    fun tweetData(
        @InputArgument coin: String?,
        @InputArgument textLike: String?,
        @InputArgument lastMs: Long?
    ): Flux<TweetData> {
        return tweetService.getTweets(coin, textLike, lastMs)
    }

    @DgsQuery
    fun redditData(
        @InputArgument textLike: String?,
        @InputArgument lastMs: Long?
    ): Flux<RedditData> {
        return redditService.getPosts(textLike, lastMs)
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

    @DgsSubscription
    fun priceDeltaTracker(
        @InputArgument exchange: ExchangeEnum,
        @InputArgument coin: String,
        @InputArgument delta: Long,
        @InputArgument changeType: ChangeType,
        @InputArgument lastMs: Long
    ): Flux<PriceDeltaTrigger> {
        return Flux.interval(Duration.ofSeconds(1)).flatMap {
            Mono.from(
                dslContext.select(EXCHANGE_DATA_DELTA.DELTA).from(
                    exchangeDataDelta(
                        coin, exchange.name.uppercase(),
                        OffsetDateTime.now().minusSeconds(lastMs / 1000),
                        OffsetDateTime.now()
                    )
                )
            )
        }.filter { record ->
            when (changeType) {
                ChangeType.Increase -> record.component1()!! > 0 && record.component1()!! > delta
                ChangeType.Decrease -> record.component1()!! < 0 && record.component1()!! * -1 > delta
            }
        }.map { record -> PriceDeltaTrigger(coin, exchange, Instant.now().toEpochMilli(), record.component1()) }
    }

}