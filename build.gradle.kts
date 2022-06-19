import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Logging

plugins {
	id("com.netflix.dgs.codegen") version "5.1.17"
	id("org.springframework.boot") version "2.7.0"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("org.flywaydb.flyway") version "8.5.13"
	id("nu.studer.jooq") version "7.1.1"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "io.github.flowercup"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
	schemaPaths = mutableListOf("${projectDir}/src/main/resources/schema") // List of directories containing schema files
	packageName = "io.github.flowercup.ingestor" // The package name to use to generate sources
	typeMapping = mutableMapOf(
		"UUID" to "java.util.UUID",
	)
}

dependencies {
	implementation("org.flywaydb:flyway-core:8.5.13")
	implementation("io.r2dbc:r2dbc-pool")
	implementation("org.postgresql:postgresql:42.4.0")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("com.graphql-java-kickstart:playground-spring-boot-starter:11.1.0")
	implementation("org.springframework.data:spring-data-r2dbc")
	implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:latest.release"))
	implementation("com.netflix.graphql.dgs:graphql-dgs-webflux-starter:5.0.1")
	implementation("com.netflix.graphql.dgs:graphql-dgs-extended-scalars:5.0.1")
	jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.amqp:spring-rabbit-test")
	implementation("org.postgresql:r2dbc-postgresql:0.9.1.RELEASE")
	jooqGenerator("org.postgresql:postgresql:42.4.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

flyway {
	this.url = "jdbc:postgresql://localhost:8001/postgres"
	this.user = "postgres"
	this.password = "postgres"
	this.schemas = arrayOf("public")
}

jooq {
	version.set("3.16.6")
	edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)
	configurations {
		create("main") {  // name of the jOOQ configuration
			generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

			jooqConfiguration.apply {
				logging = Logging.WARN
				jdbc.apply {
					driver = "org.postgresql.Driver"
					url = "jdbc:postgresql://localhost:8001/postgres"
					user = "postgres"
					password = "postgres"
				}
				generator.apply {
					name = "org.jooq.codegen.KotlinGenerator"
					database.apply {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
					}
					generate.apply {
						isDeprecated = false
						isRecords = true
						isImmutablePojos = true
						isFluentSetters = true
						isDaos = true
					}
					target.apply {
						packageName = "io.github.flowercup.ingestor.db"
						directory = "build/generated-src/jooq/main"  // default (can be omitted)
					}
					strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
				}
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
