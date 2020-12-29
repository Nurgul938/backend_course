package rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import repo.*

fun Application.restWorker(
    workerRepo: Repo<Worker>,
    workerSerializer: KSerializer<Worker>,
    drugRepo: Repo<Drug>,
    drugSerializer: KSerializer<Drug>,
    cartRepo: Repo<Cart>,
    cartSerializer: KSerializer<Cart>
) {
    routing {
        route("/worker") {
            post {
                parseBody(workerSerializer)?.let { elem ->
                    if (workerRepo.read().filter { it.workername == elem.workername }.isEmpty()) {
                        if (workerRepo.create(elem)) {
                            val user = workerRepo.read().find { it.workername == elem.workername }!!
                            call.respond(user)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                       HttpStatusCode.Conflict
                    }
                } ?: HttpStatusCode.BadRequest
            }
        }
        route("/worker/{id}") {
            get {
                call.respond(
                    parseId()?.let { id ->
                        workerRepo.read(id) ?: HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
            put {
                parseBody(workerSerializer)?.let { elem ->
                    parseId()?.let { id ->
                        if (workerRepo.update(id, elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    }
                } ?: HttpStatusCode.BadRequest
            }
            delete {
                parseId()?.let { id ->
                    if (workerRepo.delete(id))
                        HttpStatusCode.OK
                    else
                        HttpStatusCode.NotFound
                } ?: HttpStatusCode.BadRequest
            }
        }

        route("/worker/drugs") {
            post {
                parseBody(drugSerializer)?.let { elem ->
                    if (drugRepo.create(elem)) {
                        HttpStatusCode.OK
                        val drug = drugRepo.read().find { it.title == elem.title }!!
                        call.respond(drug)
                    }
                    else
                        HttpStatusCode.NotFound
                } ?: HttpStatusCode.BadRequest
            }
            get {
                val drugs = drugRepo.read()
                if (drugs.isNotEmpty()) {
                    call.respond(drugs)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        route("/worker/{id}/drugs") {
            get {
                val drugs = drugRepo.read().filter { it.workerId == parseId() }
                if (drugs.isNotEmpty()) {
                    call.respond(drugs)
                } else {
                    call.respond(listOf<Drug>())
                }
            }
        }

        route("/worker/drugs/{id}") {
            get {
                parseId()?.let { id ->
                    drugRepo.read(id)?.let { elem ->
                        call.respond(elem)
                    } ?: HttpStatusCode.NotFound
                } ?: HttpStatusCode.BadRequest
            }
            put {
                parseBody(drugSerializer)?.let { elem ->
                    parseId()?.let { id ->
                        if (drugRepo.update(id, elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    }
                } ?: HttpStatusCode.BadRequest
            }
            delete {
                parseId()?.let { id ->
                    if (drugRepo.delete(id))
                        HttpStatusCode.OK
                    else
                        HttpStatusCode.NotFound
                } ?: HttpStatusCode.BadRequest
            }
        }

        route("/worker/drugs/id/cart") {//добавляем долг
            post {
                parseBody(drugSerializer)?.let { elem ->
                    if (drugRepo.create(elem)) {
                        HttpStatusCode.OK
                        val drug = drugRepo.read().find { it.title == elem.title }!!
                        call.respond(drug)
                    }
                    else
                        HttpStatusCode.NotFound
                } ?: HttpStatusCode.BadRequest
            }

            route("/worker/drugs/cart") {
                post {
                    parseBody(cartSerializer)?.let { elem ->
                        if(cartRepo.create(elem))
                            call.respond(HttpStatusCode.OK)
                        else {
                            call.respond(HttpStatusCode.BadRequest, "Препарат в корзине")
                            }
                        }
                }
            }

            route("/worker/{id}/drugs/cart") {
                get {
                    val workerCarts = drugRepo.read().filter { it.workerId == parseId() }
                    val Carts = cartRepo.read()
                    val carts = arrayListOf<Cart>()
                    for (drug in workerCarts) {
                        val myDebts = Carts.filter { it.workerId == drug.id }
                        for (cart in myDebts) {
                            carts.add(cart)
                        }
                    }
                    call.respond(carts)
                }
            }
        }



    }
}


fun PipelineContext<Unit, ApplicationCall>.parseId(id: String = "id") =
    call.parameters[id]?.toIntOrNull()

fun PipelineContext<Unit, ApplicationCall>.drugId(id: String = "drugId") =
    call.parameters[id]?.toIntOrNull()

suspend fun <T> PipelineContext<Unit, ApplicationCall>.parseBody(
    serializer: KSerializer<T>
) =
    try {
        Json.decodeFromString(
            serializer,
            call.receive()
        )
    } catch (e: Throwable) {
        null
    }
