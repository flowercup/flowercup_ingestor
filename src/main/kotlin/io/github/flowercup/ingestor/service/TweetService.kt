package io.github.flowercup.ingestor.service

import io.github.flowercup.ingestor.db.tables.references.TWEET_DATA
import io.github.flowercup.ingestor.types.TweetData
import org.jooq.DSLContext
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.OffsetDateTime

@Service
class TweetService(val dslContext: DSLContext) {
    fun getTweets(coin: String?, textLike: String?, lastMs: Long?): Flux<TweetData> {
        var condition = noCondition()
        if (coin != null) {
            condition = condition.and(TWEET_DATA.COIN.eq(coin))
        }
        if (textLike != null) {
            condition = condition.and(TWEET_DATA.TEXT.likeIgnoreCase("%$textLike%"))
        }
        if (lastMs != null) {
            condition = condition.and(
                TWEET_DATA.TIMESTAMP.between(
                    OffsetDateTime.now().minusSeconds(lastMs / 1000),
                    OffsetDateTime.now()
                )
            )
        }
        return Flux.from(dslContext.selectFrom(TWEET_DATA).where(condition))
            .map { record -> TweetData(record.timestamp.toString(), record.coin, record.text, record.sentiment) }
    }
}