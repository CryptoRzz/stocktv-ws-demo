package top.stocktv.ws;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ws客户端，用来连接中转服务
 */
@Log4j2
public class MyWebSocketClient extends TextWebSocketHandler {

    private boolean isConnect = false;

    private final Timer timer = new Timer();

    @Resource
    private MyWebSocketServer myWebSocketServer;

    //心跳线程
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("准备建立连接");
        isConnect = true;
        // 发送心跳
        startHeartbeat(session);
        session.sendMessage(new TextMessage("客户端初次握手消息"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("发送消息: {}", payload);
        // 在此作为ws服务端转发该消息
        myWebSocketServer.sendToAllTurbo(payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("ws连接关闭， 状态为:{} ", status);
        //停止心跳任务
        stopHeartbeat();
        //重连机制
        reconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("ws连接错误: {}", exception.getMessage());
        // 停止心跳任务
        stopHeartbeat();
        // 重连机制
        reconnect();
    }


    private void startHeartbeat(WebSocketSession session) {
        if (isConnect) {
            scheduler.scheduleAtFixedRate(() -> {
                if (isConnect && session.isOpen()) {
                    try {
                        // 发送心跳（心跳内容可自定义）
                        session.sendMessage(new TextMessage("heart"));
                    } catch (IOException e) {
                        log.error("发送心跳失败：{}", e.getMessage());
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
        }
    }

    // TODO: 当前代码会无限重连，后续可实现一个不会导致无限递归的逻辑，比如使用重试计数器或指数退避策略。
    private void stopHeartbeat() {
        scheduler.shutdownNow();
        // 重新初始化scheduler以便后续使用
        scheduler = Executors.newScheduledThreadPool(1);
    }

    private void reconnect() {
        if (!this.isConnect) {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // 重新连接
                        MyWebSocketClient.this.reconnect();
                    } catch (Exception e) {
                        log.error("重连失败：{}", e.getMessage());
                    }
                }
            }, 5000);
        }
    }

}
