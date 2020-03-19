package com.fanjun.messagecenter.socket;
/**
 * 连接状态
 */
public enum ConnetState {
    //连接中
    CONNECTING,
    //已断开
    INTERRUPT,
    //连接成功
    SUCCESS,
    //已取消
    CANCEL
}
