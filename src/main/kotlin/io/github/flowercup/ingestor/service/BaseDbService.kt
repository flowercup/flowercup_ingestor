package io.github.flowercup.ingestor.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.*
import org.jooq.impl.DSL.noCondition
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

data class CursorRecord<DTO>(
    val dto: DTO,
    val cursor: String
)

data class PaginationInfo(
    val after: String?,
    val first: Int,
)

abstract class BaseService<F, OB, C : Any, M, R : Record, DTO : Any, ID : Any>(
    private val dslContext: DSLContext,
    private val cClazz: Class<C>
) :
    FilterSupport<F>, OrderBySupport<OB>,
    CursorSupport<C> {

    abstract fun table(): Table<*>

    abstract fun select(): List<SelectField<*>>

    abstract fun groupBy(): List<GroupField>

    abstract fun recordToDto(record: Record): DTO

    abstract fun initialPaginationQuery(extraData: M?): Table<*>

    abstract fun createSeek(cursor: C): MutableList<Field<*>>

    abstract fun createCursor(orderBy: OB?, record: R?): C

    abstract fun recordToTypedRecord(record: Record): R

    @Transactional
    open fun paginate(
        paginationInfo: PaginationInfo,
        filter: F?,
        orderBy: OB?,
        extraData: M?,
    ): Flux<CursorRecord<DTO>> {
        initialPaginationQuery(extraData)
        val query = dslContext.select(select()).from(initialPaginationQuery(extraData))
        return if (paginationInfo.after == null) {
            Flux.from(query.where(filter(filter)).orderBy(orderBy(orderBy)).limit(paginationInfo.first))
                .map { record ->
                    CursorRecord(
                        recordToDto(record),
                        encodeCursor(createCursor(orderBy, recordToTypedRecord(record)))
                    )
                }
        } else {
            val cursorData: C = decodeCursor(paginationInfo.after, cClazz)
            val seekData: MutableList<Field<*>> = createSeek(cursorData)
            val filterData: Condition = filter(filter)
            val orderByData: List<SortField<*>> = orderBy(orderBy)
            Flux.from(
                query.where(filterData).orderBy(orderByData).seekAfter(*seekData.toTypedArray())
                    .limit(paginationInfo.first)
            )
                .map { record ->
                    CursorRecord(
                        recordToDto(record),
                        encodeCursor(createCursor(orderBy, recordToTypedRecord(record)))
                    )
                }
        }
    }

    @Transactional
    abstract fun getOne(id: ID): Mono<DTO>

}

interface FilterSupport<F> {
    fun filter(filter: F?): Condition = noCondition()
}

interface OrderBySupport<OB> {
    fun orderBy(orderBy: OB?): List<SortField<*>> = listOf()
}

interface CursorSupport<C : Any> {
    val objectMapper: ObjectMapper
}

fun <C : Any> CursorSupport<C>.decodeCursor(cursor: String, clazz: Class<C>): C {
    return objectMapper.readValue(Base64.getUrlDecoder().decode(cursor), clazz)
}

fun <C : Any> CursorSupport<C>.encodeCursor(cursor: C): String {
    return Base64.getUrlEncoder().encodeToString(objectMapper.writeValueAsBytes(cursor))
}