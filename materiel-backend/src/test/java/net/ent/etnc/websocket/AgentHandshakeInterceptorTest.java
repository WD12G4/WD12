package net.ent.etnc.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentHandshakeInterceptorTest {

    private static final String VALID_SECRET = "super-secret-token-123";

    private AgentHandshakeInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;

    @BeforeEach
    void setUp() {
        interceptor = new AgentHandshakeInterceptor(VALID_SECRET);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
    }

    @Test
    @DisplayName("beforeHandshake autorise la connexion lorsque l'en-tête X-Agent-Secret est valide")
    void beforeHandshake_ValidHeader_ReturnsTrue() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(AgentHandshakeInterceptor.AGENT_SECRET_HEADER, List.of(VALID_SECRET));
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isTrue();
        verify(response, never()).setStatusCode(any());
    }

    @Test
    @DisplayName("beforeHandshake refuse la connexion avec 401 lorsque le secret est absent")
    void beforeHandshake_MissingSecret_ReturnsFalseAndUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/agent"));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("beforeHandshake refuse la connexion avec 401 lorsque le secret est incorrect")
    void beforeHandshake_InvalidSecret_ReturnsFalseAndUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(AgentHandshakeInterceptor.AGENT_SECRET_HEADER, List.of("wrong-secret"));
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("beforeHandshake accepte le secret transmis via paramètre de requête")
    void beforeHandshake_ValidQueryParam_ReturnsTrue() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/agent?secret=" + VALID_SECRET));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isTrue();
        verify(response, never()).setStatusCode(any());
    }
}
