package top.stocktv.ws;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ws服务端，提供ws服务
 */
@Log4j2
@Component
// TODO: 2025/3/11 在此设置作为ws服务端的地址
@ServerEndpoint("/websocket/{uid}")
public class MyWebSocketServer {
    /**
     * 静态变量，用来记录当前在线连接数，线程安全的类。
     */
    private static final AtomicInteger onlineSessionClientCount = new AtomicInteger(0);
    /**
     * 存放所有在线的客户端
     */
    private static final Map<String, Session> onlineSessionClientMap = new ConcurrentHashMap<>();
    /**
     * 连接uid和连接会话
     */
    private String uid;
    private Session session;


    /**
     * 连接建立成功调用的方法。由前端<code>new WebSocket</code>触发
     *
     * @param uid     每次页面建立连接时传入到服务端的id，比如用户id等。可以自定义。
     * @param session 与某个客户端的连接会话，需要通过它来给客户端发送消息
     */
    @OnOpen
    public void onOpen(@PathParam("uid") String uid, Session session) {
        log.info("连接建立中 ==> session_id = {}， sid = {}", session.getId(), uid);
        onlineSessionClientMap.put(uid, session);
        //在线数加1
        onlineSessionClientCount.incrementAndGet();
        this.uid = uid;
        this.session = session;
        sendToOne(uid, "连接成功");
        log.info("连接建立成功，当前在线数为：{} ==> 开始监听新连接：session_id = {}， sid = {},。", onlineSessionClientCount, session.getId(), uid);
    }

    /**
     * 连接关闭调用的方法。由前端<code>socket.close()</code>触发
     *
     * @param uid
     * @param session
     */
    @OnClose
    public void onClose(@PathParam("uid") String uid, Session session) {
        // 从 Map中移除
        onlineSessionClientMap.remove(uid);

        // 在线数减1
        onlineSessionClientCount.decrementAndGet();
        log.info("连接关闭成功，当前在线数为：{} ==> 关闭该连接信息：session_id = {}， sid = {},。", onlineSessionClientCount, session.getId(), uid);
    }

    /**
     * 发生错误调用的方法
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误，错误信息为：" + error.getMessage());
    }


    // 会话消息队列（无锁结构）
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> sessionQueues = new ConcurrentHashMap<>();

    // 会话处理状态标记（新增关键变量）
    private final ConcurrentHashMap<String, AtomicBoolean> processingFlags = new ConcurrentHashMap<>();

    // 工作线程池
    private final ExecutorService executor = Executors.newWorkStealingPool();


    /**
     * 极速群发消息
     */
    public void sendToAllTurbo(String message) {
        // 消息过滤
        if ("heart".equals(message) || StringUtils.isEmpty(message)) {
            return;
        }
        // TODO 可加入处理消息的业务代码
        onlineSessionClientMap.forEach((sessionId, session) -> {
            if (session != null && session.isOpen()) {
                // 1. 消息快速入队
                ConcurrentLinkedQueue<String> queue = sessionQueues.computeIfAbsent(
                        sessionId,
                        k -> new ConcurrentLinkedQueue<>()
                );
                queue.offer(message);

                // 2. CAS 操作触发处理（关键点）
                AtomicBoolean flag = processingFlags.computeIfAbsent(
                        sessionId,
                        k -> new AtomicBoolean(false)
                );

                if (flag.compareAndSet(false, true)) {
                    executor.submit(() -> processQueueTurbo(sessionId, session));
                }
            }
        });
    }

    /**
     * 涡轮处理队列
     */
    private void processQueueTurbo(String sessionId, Session session) {
        ConcurrentLinkedQueue<String> queue = sessionQueues.get(sessionId);
        if (queue == null || !session.isOpen()) {
            cleanupSession(sessionId);
            return;
        }

        try {
            while (true) {
                // 批量获取消息（提升吞吐量）
                StringBuilder batch = new StringBuilder();
                // 可调整批量大小
                for (int i = 0; i < 100; i++) {
                    String msg = queue.poll();
                    if (msg == null) {
                        break;
                    }
                    batch.append(msg).append("\n");
                }

                if (batch.length() == 0) {
                    break;
                }

                // 异步发送（非阻塞）
                session.getAsyncRemote().sendText(batch.toString(), result -> {
                    if (!result.isOK()) {
                        log.error("发送失败: ", result.getException());
                    }
                });
            }
        } finally {
            // 重置处理标志并检查残留消息
            processingFlags.getOrDefault(sessionId, new AtomicBoolean(false)).set(false);
            if (!queue.isEmpty()) {
                executor.submit(() -> processQueueTurbo(sessionId, session));
            }
        }
    }

    /**
     * 清理无效会话
     */
    private void cleanupSession(String sessionId) {
        sessionQueues.remove(sessionId);
        processingFlags.remove(sessionId);
    }


    /**
     * 指定发送消息
     *
     * @param toUid
     * @param message
     */
    public void sendToOne(String toUid, String message) {
        /*
         * 判断发送者是否在线
         */
        Session toSession = onlineSessionClientMap.get(toUid);
        if (toSession == null) {
            log.error("服务端给客户端发送消息 ==> toSid = {} 不存在, message = {}", toUid, message);
            return;
        }
        // 异步发送
        log.info("服务端给客户端发送消息 ==> toSid = {}, message = {}", toUid, message);
        toSession.getAsyncRemote().sendText(message);
    }

}
