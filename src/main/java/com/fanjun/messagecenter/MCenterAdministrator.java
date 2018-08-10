package com.fanjun.messagecenter;

import android.util.Log;

import com.fanjun.messagecenter.socket.Client;
import com.fanjun.messagecenter.socket.SocketInterceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * 包裹室管理员
 * 消息中心管理员
 * 1.收准取件包裹
 * 2.收准寄出包裹
 * 3.派发准取件包裹
 * 4.派发准寄出包裹
 * @author Administrator
 *
 */
public class MCenterAdministrator extends Thread{
	private BlockingQueue<Msg> messages;
	private Map<String, List<MessageHandler>> messageHanlders;
	private Client client;
	
	public MCenterAdministrator(BlockingQueue<Msg> messages, Map<String, List<MessageHandler>> messageHanlders) {
		super();
		this.messages = messages;
		this.messageHanlders = messageHanlders;
	}

	@Override
	public void run() {
		dispathMessage();
	}

	/**
	 * 连接socket服务器
	 * @param host socket服务器地址
	 * @param port socket服务器端口
	 * @param socketInterceptor 拦截器
	 */
	public void connectServer(String host, int port, SocketInterceptor socketInterceptor){
		if (client != null && client.isAlive()){
			client.disConnect();
		}
		client = new Client(host, port);
		client.setSocketInterceptor(socketInterceptor);
		client.start();
	}

	/**
	 * 断开socket连接
	 */
	public void disConnectServer(){
		if (client != null){
			client.disConnect();
			client = null;
		}
	}
	/**
	 * 分发消息
	 */
	private void dispathMessage(){
		while (true) {
			try {
				Msg message = messages.take();
				if(message.getDestination() == 0){
					sendAppInSide(message);
				}else{
					sendAppOutSide(message);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 发消息
	 * @param message 包裹
	 */
	public void sendMessage(Msg message) {
		try {
			this.messages.offer(message, 2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 发往应用外
	 * @param message 消息包裹
	 */
	private void sendAppOutSide(Msg message) {
		if (this.client == null) {
			Log.e("LiteHttp","还没有连接上socket服务器！");
			return;
		}
		if (this.client.getClientHandler() != null) {
			notifyMessageState(message, MHanderSendListener.ACTIVE);
			if(this.client.getClientHandler().sendMessage(message)){
				notifyMessageState(message, MHanderSendListener.SUCCESS);
			}else{
				notifyMessageState(message, MHanderSendListener.FAIL);
			}
		}
	}
	/**
	 * 发往应用内
	 * @param message 消息包裹
	 */
	private void sendAppInSide(Msg message) {
		List<MessageHandler> handlers = this.messageHanlders.get(message.getTag());
		if(handlers != null){
			for (MessageHandler handler : handlers) {
				if (handler.getMHanderReceiveListener() != null) {
					handler.getMHanderReceiveListener().receivedMsg(message);
				}
			}
		}
	}
	/**
     * 通知消息订阅者消息发送状态
     * @param msg 消息包裹
     * @param state 发送状态回执
     */
    private void notifyMessageState(Msg msg, int state){
    	List<MessageHandler> handlers = MessageCenter.getMessageHanlders().get(msg.getTag());
    	if (handlers == null) {
			return;
		}
    	for (MessageHandler handler: handlers) {
    		if (handler.getMHanderSendListener() != null) {
    			handler.getMHanderSendListener().sendMsgReturn(msg, state);
			}
		}
    }
}
