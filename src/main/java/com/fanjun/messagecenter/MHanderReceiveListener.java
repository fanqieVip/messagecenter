package com.fanjun.messagecenter;


/**
 * 收到消息处理回调
 * @author Administrator
 *
 */
public interface MHanderReceiveListener {
	/**
	 * 收到消息
	 * @param message 消息包裹
	 */
	abstract void receivedMsg(Msg message);
}
