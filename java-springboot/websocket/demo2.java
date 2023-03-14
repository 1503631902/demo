package com.mr.operation.controller.socket;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket 服务端
 */
@ServerEndpoint("/webSocket/{cid}")
public class WebSocketServer1 {

    /**
     * 静态变量，用来记录当前在线连接数
     */
    private static int onlineCount = 0;

    /**
     * concurrent 包的线程安全 Set，用来存放每个客户端对应的 WebSocketServer 对象
     */
    private static CopyOnWriteArraySet<WebSocketServer1> webSocketSet = new CopyOnWriteArraySet<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 客户端 ID
     */
    private String cid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("cid") String cid) {
        this.session = session;
        // 将当前 WebSocket 对象加入到 Set 中
        webSocketSet.add(this);
        // 在线连接数加 1
        addOnlineCount();
        System.out.println("有新窗口开始监听：" + cid + "，当前在线人数为：" + getOnlineCount());
        this.cid = cid;
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            System.out.println("websocket IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 将当前 WebSocket 对象从 Set 中删除
        webSocketSet.remove(this);
        // 在线连接数减 1
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为：" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端 " + cid + " 的消息：" + message);
        // 群发消息
        for (WebSocketServer1 item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 向客户端发送消息
     *
     * @param message 消息内容
     * @throws IOException
     */
    private void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 获取当前在线人数
     *
     * @return 在线人数
     */
    private static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 在线人数加 1
     */
    private static synchronized void addOnlineCount() {
        WebSocketServer1.onlineCount++;
    }

    /**
     * 在线人数减 1
     */
    private static synchronized void subOnlineCount() {
        WebSocketServer1.onlineCount--;
    }
}

