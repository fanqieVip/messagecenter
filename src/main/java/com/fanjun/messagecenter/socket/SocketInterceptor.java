package com.fanjun.messagecenter.socket;

import android.support.annotation.NonNull;

import java.sql.Connection;
import java.util.List;

/**
 * socket服务拦截器
 */
public abstract class SocketInterceptor {
    //缓存的粘包数据
    private String cutData = null;

    /**
     * 收到服务器推送的数据
     *
     * @param msg 收到的数据包
     */
    public abstract void receiveServerMsg(String msg);

    /**
     * 心跳包
     *
     * @return 发送的心跳数据包
     */
    public String heartbeat() {
        return null;
    }

    /**
     * 把发送socket的对象包装成字符串
     *
     * @param msgObj 发送socket的obj对象
     * @return 最终发送的socket的字符串
     */
    public abstract String packageMsg(Object msgObj);

    /**
     * 返回分包开始标识符
     *
     * @return
     */
    public String getStartTag() {
        return null;
    }

    /**
     * 返回分包结束标识符
     *
     * @return
     */
    public String getEndTag() {
        return null;
    }

    protected String getCutData() {
        return cutData == null ? "" : cutData;
    }

    protected void setCutData(String cutData) {
        this.cutData = cutData;
    }

    /**
     * 连接状态
     */
    public void connectState(ConnetState connetState, Exception e) {
    }

    ;
}
