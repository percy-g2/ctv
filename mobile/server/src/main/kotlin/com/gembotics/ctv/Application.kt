package com.gembotics.ctv

import com.gembotics.ctv.routes.simpleRoutes
import com.gembotics.ctv.routes.vaultRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val SERVER_PORT = 8080

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Install ContentNegotiation plugin for JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Enable CORS for Android app
    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        anyHost()
    }
    
    routing {
        get("/") {
            call.respond(mapOf("message" to "CTV Playground API", "version" to "1.0.0"))
        }
        
        simpleRoutes()
        vaultRoutes()
    }
}