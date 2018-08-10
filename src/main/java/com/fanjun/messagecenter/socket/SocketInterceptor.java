package com.fanjun.messagecenter.socket;

import java.util.List;

/**
 * socket服务拦截器
 */
public interface SocketInterceptor {
    /**
     * 收到服务器推送的数据
     *
     * @param msg 收到的数据包
     * @return 推送数据集合
     */
    List<ReceiveMsg> receiveServerMsg(String msg);

    /**
     * 心跳包
     *
     * @return 发送的心跳数据包
     */
    String heartbeat();

    /**
     * socket连接已断开
     *
     * @param exception 程序内部错误异常或网络断开异常
     */
    void connectionInterrupt(Exception exception);

    /**
     * 连接成功
     */
    void connectionSuccess();
}
