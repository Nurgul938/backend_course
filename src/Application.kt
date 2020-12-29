package com.bankBackend
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import config.DatabaseFactory
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import repo.*
import rest.restWorker

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@KtorExperimentalAPI
@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        json()
    }

    DatabaseFactory.init()

    install(AutoHeadResponse)

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    restWorker(
        RepoDSL(workersTable),
        Worker.serializer(),
        RepoDSL(drugsTable),
        Drug.serializer(),
        RepoDSL(cartsTable),
        Cart.serializer()
    )

    transaction {
        SchemaUtils.create(workersTable)
        SchemaUtils.create(drugsTable)
        SchemaUtils.create(cartsTable)

    }

    routing {
        get("/api") {
            call.respondText("Version: 0.0.1", contentType = ContentType.Text.Plain)
        }
    }
}

