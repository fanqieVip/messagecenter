package com.fanjun.messagecenter;


import android.support.annotation.NonNull;

/**
 * 消息订阅器
 * @author Administrator
 *
 */
public class MessageHandler {
	/**
	 * 消息订阅id
	 */
	private String subscribeId;
	private Object object;
	private MHanderReceiveListener mHanderReceiveListener;
	private MHanderSendListener mHanderSendListener;
	
	public MessageHandler(@NonNull String subscribeId, MHanderReceiveListener mHanderReceiveListener, MHanderSendListener mHanderSendListener) {
		super();
		this.subscribeId = subscribeId;
		this.mHanderReceiveListener = mHanderReceiveListener;
		this.mHanderSendListener = mHanderSendListener;
	}
	public MessageHandler(@NonNull String subscribeId, MHanderReceiveListener mHanderReceiveListener) {
		super();
		this.subscribeId = subscribeId;
		this.mHanderReceiveListener = mHanderReceiveListener;
	}
	public MessageHandler(@NonNull String subscribeId, MHanderSendListener mHanderSendListener) {
		super();
		this.subscribeId = subscribeId;
		this.mHanderSendListener = mHanderSendListener;
	}
	public String getSubscribeId() {
		return subscribeId;
	}
	public void setSubscribeId(String subscribeId) {
		this.subscribeId = subscribeId;
	}
	public MHanderReceiveListener getMHanderReceiveListener() {
		return mHanderReceiveListener;
	}
	public void setMHanderReceiveListener(MHanderReceiveListener messageHanderListener) {
		this.mHanderReceiveListener = messageHanderListener;
	}
	public MHanderSendListener getMHanderSendListener() {
		return mHanderSendListener;
	}
	public void setMHanderSendListener(MHanderSendListener mHanderSendListener) {
		this.mHanderSendListener = mHanderSendListener;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(@NonNull Object object) {
		this.object = object;
	}
}

