package io.github.flowercup.ingestor.service

import io.github.flowercup.ingestor.db.tables.references.REDDIT_DATA
import io.github.flowercup.ingestor.db.tables.references.TWEET_DATA
import io.github.flowercup.ingestor.types.TweetData
import io.github.flowercup.ingestor.util.toLong
import org.jooq.DSLContext
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.OffsetDateTime

@Service
class RedditService(val dslContext: DSLContext) {
    fun getPosts(textLike: String?, lastMs: Long?): Flux<io.github.flowercup.ingestor.types.RedditData> {
        var condition = noCondition()
        if (textLike != null) {
            condition = condition.and(REDDIT_DATA.TEXT.likeIgnoreCase("%$textLike%"))
        }
        if (lastMs != null) {
            condition = condition.and(
                REDDIT_DATA.TIMESTAMP.between(
                    OffsetDateTime.now().minusSeconds(lastMs / 1000),
                    OffsetDateTime.now()
                )
            )
        }
        return Flux.from(dslContext.selectFrom(REDDIT_DATA).where(condition))
            .map { record ->
                io.github.flowercup.ingestor.types.RedditData(
                    record.timestamp?.toLong(),
                    record.text,
                    record.sentiment
                )
            }
    }
}