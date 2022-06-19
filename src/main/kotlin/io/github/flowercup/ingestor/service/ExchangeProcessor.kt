package io.github.flowercup.ingestor.service

import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA
import io.github.flowercup.ingestor.db.tables.references.REDDIT_DATA
import io.github.flowercup.ingestor.db.tables.references.TWEET_DATA
import io.github.flowercup.ingestor.util.toOffsetDatetime
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.annotation.PostConstruct

@Service
class ExchangeProcessor(
    val exchangeListener: ExchangeListener,
    val tweetListener: TweetListener,
    val redditListener: RedditListener,
    val dslContext: DSLContext
) {
    @PostConstruct
    fun startExchangeProcessor() {
        exchangeListener.exchangeEvents().subscribe { event ->
            Mono.from(
                dslContext.insertInto(EXCHANGE_DATA)
                    .columns(
                        EXCHANGE_DATA.TIMESTAMP,
                        EXCHANGE_DATA.VALUE,
                        EXCHANGE_DATA.COIN,
                        EXCHANGE_DATA.EXCHANGE,
                        EXCHANGE_DATA.VOLUME
                    )
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

    @PostConstruct
    fun startTweetProcessor() {
        tweetListener.exchangeEvents().subscribe() { event ->
            Mono.from(
                dslContext.insertInto(TWEET_DATA)
                    .columns(TWEET_DATA.TIMESTAMP, TWEET_DATA.COIN, TWEET_DATA.SENTIMENT, TWEET_DATA.TEXT)
                    .values(
                        OffsetDateTime.parse(event.created, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        event.tag,
                        event.sentiment,
                        event.text
                    )
            ).subscribe()
        }.run { }
    }

    @PostConstruct
    fun startRedditProcessor() {
        redditListener.exchangeEvents().subscribe() { event ->
            Mono.from(
                dslContext.insertInto(REDDIT_DATA)
                    .columns(REDDIT_DATA.TIMESTAMP, REDDIT_DATA.TEXT, REDDIT_DATA.SENTIMENT, REDDIT_DATA.ENTITIES)
                    .values(
                        event.time.toOffsetDatetime(),
                        event.text,
                        event.sentiment,
                        event.entitiesList?.toTypedArray()
                    )
            ).subscribe()
        }.run { }
    }
}