package top.stocktv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import top.stocktv.ws.MyWebSocketClient;

/**
 * ws配置文件
 */
@Configuration
public class WebSocketConfig {

    @Value("${ws.url}")
    private String wsUrl;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public StandardWebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean
    public WebSocketConnectionManager webSocketConnectionManager(StandardWebSocketClient client, WebSocketHandler handler) {
        // 在此设置连接到ws服务端地址
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, wsUrl);
        manager.start();
        return manager;
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new MyWebSocketClient();
    }
}
