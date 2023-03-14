package com.mr.operation.controller.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@Service
@ServerEndpoint("/api/websocket/{sid}")
public class WebSocketServer {

    /** 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。 */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    /** concurrent 包的线程安全 Set，用来存放每个客户端对应的 WebSocket 对象。 */
    private static final Map<String, Set<WebSocketServer>> sidWebSocketSetMap = new ConcurrentHashMap<>();

    /** 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。 */
    private Session session;
    private String sid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        this.session = session;
        this.sid = sid;
        // 加入 set 中
        sidWebSocketSetMap.computeIfAbsent(sid, key -> new CopyOnWriteArraySet<>()).add(this);
        // 在线数加1
        addOnlineCount();
        try {
            sendMessage("conn_success");
            log.info("有新客户端开始监听,sid={},当前在线人数为:{}", sid, getOnlineCount());
        } catch (IOException e) {
            log.error("websocket IO Exception", e);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        sidWebSocketSetMap.computeIfPresent(sid, (key, value) -> {
            value.remove(this);
            if (value.isEmpty()) {
                sidWebSocketSetMap.remove(key);
            }
            return value;
        });
        subOnlineCount();
        releaseResource();
    }

    private void releaseResource() {
        log.info("有一连接关闭！当前在线人数为{}", getOnlineCount());
        // 处理资源释放和业务逻辑
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自客户端 sid={} 的信息:{}", sid, message);
        // 群发消息
        Set<WebSocketServer> webSocketSet = sidWebSocketSetMap.get(sid);
        if (webSocketSet != null) {
            webSocketSet.forEach(webSocketServer -> {
                try {
                    webSocketServer.sendMessage("客户端 " + sid + "发布消息：" + message);
                } catch (IOException e) {
                    log.error("消息发送失败", e);
                }
            });
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("{}客户端发生错误", session.getBasicRemote(), error);
    }

    public static void sendMessage(String message, String sid) throws IOException {
        log.info("推送消息到客户端 {}，推送内容:{}", sid, message);

        Set<WebSocketServer> webSocketSet = sidWebSocketSetMap.get(sid);
        if (webSocketSet != null) {
            webSocketSet.forEach(webSocketServer -> {
                try {
                    webSocketServer.sendMessage(message);
                } catch (IOException e) {
                    log.error("消息发送失败", e);
                }
            });
        }
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static int getOnlineCount() {
        return onlineCount.get();
    }

    public static void addOnlineCount() {
        onlineCount.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineCount.decrementAndGet();
    }
}
