package com.ttymonkey.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ttymonkey.backend.models.Message
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

    override fun handle(session: WebSocketSession): Mono<Void> {
        sessions.add(session)
        val joinMessage = Message(2, "New user joined")
        broadcast(joinMessage, session)

        val closed = session.receive()
            .doOnNext { msg -> broadcast(Message(1, msg.payloadAsText), session) }
            .doFinally { signalType ->
                if (signalType == SignalType.ON_COMPLETE) {
                    sessions.remove(session)
                    val disconnectMessage = Message(2, "User disconnected")
                    broadcast(disconnectMessage, session)
                }
            }
            .then()

        return closed
    }

    private fun broadcast(message: Message, sender: WebSocketSession) {
        val jsonMessage = objectMapper.writeValueAsString(message)
        sessions.forEach { session ->
            session.send(Mono.just(session.textMessage(jsonMessage))).subscribe()
        }
    }
}
