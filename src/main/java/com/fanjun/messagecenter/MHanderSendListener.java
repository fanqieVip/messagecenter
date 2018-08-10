package com.fanjun.messagecenter;


/**
 * 收到发送消息的状态回调
 * @author Administrator
 *
 */
public interface MHanderSendListener {
	/**
	 * 发送中
	 */
	public static final int ACTIVE = 0;
	/**
	 * 发送成功
	 */
	public static final int SUCCESS = 1;
	/**
	 * 发送失败
	 */
	public static final int FAIL = 2;
	
	/**
	 * 消息发送状态回执
	 * @param message 消息包裹
	 * @param sendState 回执状态
	 */
	public void sendMsgReturn(Msg message, int sendState);
}
