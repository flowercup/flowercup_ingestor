package io.github.flowercup.ingestor.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun Long.toOffsetDatetime(): OffsetDateTime {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}

fun Long.toMillisFromPicos(): Long {
    val stringRepresentation = this.toString()
    return stringRepresentation.substring(stringRepresentation.length - 3, stringRepresentation.length).toLong()
}

fun OffsetDateTime.toLong(): Long {
    return this.toInstant().toEpochMilli()
}