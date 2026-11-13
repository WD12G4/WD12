package net.ent.etnc.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Intercepteur de poignée de main (Handshake) WebSocket pour authentifier les agents de supervision.
 * Vérifie la présence et la validité du secret partagé transmis dans l'en-tête X-Agent-Secret.
 */
@Slf4j
@Component
public class AgentHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AGENT_SECRET_HEADER = "X-Agent-Secret";

    private final String expectedSecret;

    public AgentHandshakeInterceptor(@Value("${app.agent.secret:wd12-supervision-secret-change-in-prod}") String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String secret = request.getHeaders().getFirst(AGENT_SECRET_HEADER);

        // Repli facultatif via paramètre de requête si l'en-tête n'est pas fourni
        if (secret == null && request.getURI().getRawQuery() != null) {
            for (String param : request.getURI().getRawQuery().split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && ("secret".equalsIgnoreCase(pair[0]) || "agentSecret".equalsIgnoreCase(pair[0]))) {
                    secret = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (secret == null || !MessageDigest.isEqual(expectedSecret.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Tentative de connexion WebSocket agent refusée : secret absent ou invalide [remoteAddress={}]",
                    request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Aucune action post-handshake requise
    }
}
