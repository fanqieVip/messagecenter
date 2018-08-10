package com.fanjun.messagecenter.socket;

import android.support.annotation.NonNull;

public final class ReceiveMsg {
    /**
     * 消息标识
     */
    private String tag;
    /**
     * 数据包
     */
    private Object obj;
    private ReceiveMsg() {
    }
    private ReceiveMsg(String tag, Object obj) {
        this.tag = tag;
        this.obj = obj;
    }
    /**
     * @param tag 消息标识
     * @param obj 数据包
     * @return ReceiveMsg
     */
    public static ReceiveMsg ini(@NonNull String tag, @NonNull Object obj){
        return new ReceiveMsg(tag, obj);
    };
    public ReceiveMsg tag(@NonNull String tag){
        this.tag = tag;
        return this;
    }
    public ReceiveMsg obj(@NonNull Object obj){
        this.obj = obj;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public Object getObj() {
        return obj;
    }
}
