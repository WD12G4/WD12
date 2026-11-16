package net.ent.etnc.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Configuration du point d'entrée WebSocket pour les agents.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentWebSocketHandler agentWebSocketHandler;
    private final AgentHandshakeInterceptor agentHandshakeInterceptor;

    @Autowired
    public WebSocketConfig(final AgentWebSocketHandler agentWebSocketHandler, final AgentHandshakeInterceptor agentHandshakeInterceptor) {
        this.agentWebSocketHandler = agentWebSocketHandler;
        this.agentHandshakeInterceptor = agentHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketHandler, "/ws/agent").addInterceptors(agentHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * Configure le conteneur WebSocket Tomcat sous-jacent (Point A1).
     * Par défaut Tomcat limite les messages texte à 8 Ko, ce qui provoque la coupure de la connexion
     * dès qu'un agent transmet la liste des processus applicatifs avec icônes (50 Ko à 300 Ko).
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(10 * 1024 * 1024); // 10 Mo
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024); // 10 Mo
        container.setMaxSessionIdleTimeout(60000L); // 60s
        return container;
    }
}
