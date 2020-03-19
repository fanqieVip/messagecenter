package com.fanjun.messagecenter.socket;
/**
 * 连接状态
 */
public interface ConnectStateListener {
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
