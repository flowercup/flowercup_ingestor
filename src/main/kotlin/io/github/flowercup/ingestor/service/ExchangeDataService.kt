package io.github.flowercup.ingestor.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.github.flowercup.ingestor.db.routines.references.exchangeDataDelta
import io.github.flowercup.ingestor.db.routines.references.exchangeDataIncrease
import io.github.flowercup.ingestor.db.routines.references.first
import io.github.flowercup.ingestor.db.routines.references.last
import io.github.flowercup.ingestor.db.tables.records.ExchangeDataRecord
import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA
import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA_CANDLESTICK
import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA_DELTA
import io.github.flowercup.ingestor.db.tables.references.EXCHANGE_DATA_INCREASE
import io.github.flowercup.ingestor.types.*
import io.github.flowercup.ingestor.types.ExchangeData
import io.github.flowercup.ingestor.util.toLong
import io.github.flowercup.ingestor.util.toOffsetDatetime
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Month

@JsonSerialize
data class ExchangeDataCursorParameters(
    val timestamp: Long?,
)

@Service
class ExchangeDataService(
    val dslContext: DSLContext, override val objectMapper: ObjectMapper
) : BaseService<ExchangeDataFilterInput, ExchangeDataOrderByInput, ExchangeDataCursorParameters, Any, ExchangeDataRecord, ExchangeData, Any>(
    dslContext,
    ExchangeDataCursorParameters::class.java
) {

    override fun table(): Table<*> {
        return EXCHANGE_DATA
    }

    override fun select(): List<SelectField<*>> {
        return listOf(EXCHANGE_DATA.EXCHANGE, EXCHANGE_DATA.COIN, EXCHANGE_DATA.VALUE, EXCHANGE_DATA.TIMESTAMP)
    }

    override fun groupBy(): List<GroupField> {
        return listOf()
    }

    override fun recordToDto(record: Record): ExchangeData {
        val typed = recordToTypedRecord(record)
        return ExchangeData(
            typed.exchange,
            typed.value?.toInt(),
            typed.coin,
            typed.timestamp?.toInstant()?.toEpochMilli()?.toInt(),
            typed.volume
        )
    }

    override fun recordToTypedRecord(record: Record): ExchangeDataRecord {
        return record.into(ExchangeDataRecord::class.java)
    }

    override fun getOne(id: Any): Mono<ExchangeData> {
        TODO("Not yet implemented")
    }

    override fun createCursor(
        orderBy: ExchangeDataOrderByInput?,
        record: ExchangeDataRecord?
    ): ExchangeDataCursorParameters {
        return ExchangeDataCursorParameters(
            timestamp = record?.timestamp?.toLong()
        )
    }

    override fun createSeek(cursor: ExchangeDataCursorParameters): MutableList<Field<*>> {
        val seek: MutableList<Field<*>> = mutableListOf()
        if (cursor.timestamp != null) {
            seek.add(DSL.`val`(cursor.timestamp.toOffsetDatetime()))
        }
        return seek
    }

    override fun initialPaginationQuery(extraData: Any?): Table<*> {
        return table()
    }

    override fun filter(filter: ExchangeDataFilterInput?): Condition {
        var condition: Condition = noCondition()

        if (filter?.coin != null) {
            condition = condition.and(
                when (filter.coin.option) {
                    FilterOption.greaterThan -> EXCHANGE_DATA.COIN.greaterOrEqual(filter.coin.value)
                    FilterOption.lessThan -> EXCHANGE_DATA.COIN.lessOrEqual(filter.coin.value)
                    FilterOption.equal -> EXCHANGE_DATA.COIN.eq(filter.coin.value)
                    FilterOption.like -> EXCHANGE_DATA.COIN.likeIgnoreCase("%" + filter.coin.value + "%")
                    else -> noCondition()
                }
            )
        }

        if (filter?.timestamp != null) {
            condition = condition.and(
                if (filter.timestamp.valueTo == null) {
                    EXCHANGE_DATA.TIMESTAMP.greaterOrEqual(filter.timestamp.valueFrom.toOffsetDatetime())
                } else {
                    EXCHANGE_DATA.TIMESTAMP.between(
                        filter.timestamp.valueFrom.toOffsetDatetime(),
                        filter.timestamp.valueTo.toOffsetDatetime()
                    )
                }
            )
        }

        if (filter?.exchange != null) {
            condition = condition.and(EXCHANGE_DATA.EXCHANGE.equalIgnoreCase(filter.exchange.name))
        }

        if (filter?.value != null) {
            condition = condition.and(
                when (filter.value.option) {
                    NumericFilterOption.greaterThan -> EXCHANGE_DATA.VALUE.greaterOrEqual(filter.value.value)
                    NumericFilterOption.lessThan -> EXCHANGE_DATA.VALUE.lessOrEqual(filter.value.value)
                    NumericFilterOption.equal -> EXCHANGE_DATA.VALUE.eq(filter.value.value)
                    else -> noCondition()
                }
            )
        }

        return condition
    }

    override fun orderBy(orderBy: ExchangeDataOrderByInput?): List<SortField<*>> {
        val order: MutableList<SortField<*>> = mutableListOf()
        when (orderBy?.timestamp) {
            Sort.asc -> order.add(EXCHANGE_DATA.TIMESTAMP.asc())
            Sort.desc -> order.add(EXCHANGE_DATA.TIMESTAMP.desc())
            else -> {}
        }
        if (orderBy == null) {
            order.add(EXCHANGE_DATA.TIMESTAMP.desc())
        }
        return order;
    }

    fun getPriceDelta(exchange: ExchangeEnum, coin: String, from: Long, to: Long): Mono<ExchangeDataDelta> {
        return Mono.from(
            dslContext.select(
                EXCHANGE_DATA_DELTA.FIRST_VALUE,
                EXCHANGE_DATA_DELTA.LAST_VALUE,
                EXCHANGE_DATA_DELTA.DELTA
            )
                .from(
                    exchangeDataDelta(
                        coin.uppercase(),
                        exchange.name.uppercase(),
                        from.toOffsetDatetime(),
                        to.toOffsetDatetime()
                    )
                )
        ).map { record ->
            ExchangeDataDelta(
                from,
                to,
                record.component3(),
                record.component1(),
                record.component2()
            )
        }
    }

    fun getPriceIncrease(exchange: ExchangeEnum, coin: String, from: Long, to: Long): Mono<ExchangeDataIncrease> {
        return Mono.from(
            dslContext.select(
                EXCHANGE_DATA_INCREASE.FIRST_VALUE,
                EXCHANGE_DATA_INCREASE.LAST_VALUE,
                EXCHANGE_DATA_INCREASE.CHANGE,
                EXCHANGE_DATA_INCREASE.CHANGE_TYPE
            ).from(
                exchangeDataIncrease(
                    coin.uppercase(),
                    exchange.name.uppercase(),
                    from.toOffsetDatetime(),
                    to.toOffsetDatetime()
                )
            )
        ).map { record ->
            ExchangeDataIncrease(
                from,
                to,
                record.component3(),
                when {
                    record.component4() != null && record.component4().equals("INCREASE", true) -> {
                        ChangeType.Increase
                    }
                    record.component4() != null && record.component4().equals("DECREASE", true) -> {
                        ChangeType.Decrease
                    }
                    else -> {
                        null
                    }
                },
                record.component1(),
                record.component2()
            )
        }
    }
}