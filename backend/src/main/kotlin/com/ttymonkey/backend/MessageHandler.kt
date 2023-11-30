package com.ttymonkey.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ttymonkey.backend.models.Message
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import java.util.concurrent.ConcurrentHashMap

@Component
class MessageHandler : WebSocketHandler {
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    private val objectMapper = jacksonObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> = mono {
        sessions.add(session)
        val joinMessage = Message(2, "New user joined")
        broadcast(joinMessage)

        session.receive()
            .doOnNext { msg -> broadcast(Message(1, msg.payloadAsText)) }
            .doFinally { signalType ->
                if (signalType == SignalType.ON_COMPLETE) {
                    sessions.remove(session)
                    val disconnectMessage = Message(2, "User disconnected")
                    broadcast(disconnectMessage)
                }
            }
            .then()
            .awaitFirstOrNull()
    }

    private fun broadcast(message: Message) {
        val jsonMessage = objectMapper.writeValueAsString(message)
        sessions.forEach { session ->
            session.send(Mono.just(session.textMessage(jsonMessage))).subscribe()
        }
    }
}
