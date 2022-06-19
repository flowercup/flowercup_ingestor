package io.github.flowercup.ingestor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Clock
import java.time.Instant

@SpringBootApplication
class IngestorApplication

fun main(args: Array<String>) {
	runApplication<IngestorApplication>(*args)
}