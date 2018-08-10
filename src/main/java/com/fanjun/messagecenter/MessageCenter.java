package com.fanjun.messagecenter;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.fanjun.messagecenter.annotion.MHanderReceiveTag;
import com.fanjun.messagecenter.annotion.MHanderSendTag;
import com.fanjun.messagecenter.socket.SocketInterceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 包裹室
 * 消息中心，负责存放准寄出和准取件的包裹
 * @author Administrator
 * 
 */
public class MessageCenter {
	private static MessageCenter messageCenter;
	/**
	 * 存放包裹
	 */
	private BlockingQueue<Msg> messages;
	/**
	 * 负责存放包裹管理员
	 */
	private MCenterAdministrator centerAdministrator;
	/**
	 * 客户
	 */
	private Map<String, List<MessageHandler>> messageHanlders;
	/**
	 * Main线程通讯器
	 */
	private android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
	/**
	 * socket拦截器
	 */
	private SocketInterceptor socketInterceptor;
	/**
	 * socket地址
	 */
	private String host;
	/**
	 * socket端口
	 */
	private int port;

	private MessageCenter(){
		this.messages = new LinkedBlockingQueue<Msg>();
		this.messageHanlders = new HashMap<String, List<MessageHandler>>();
		this.centerAdministrator = new MCenterAdministrator(messages, messageHanlders);
		this.handler = new android.os.Handler(Looper.getMainLooper());
	}

	/**
	 * 绑定socket拦截器
	 * @param socketInterceptor 拦截器
	 * @return MessageCenter
	 */
	public MessageCenter socketInterceptor(SocketInterceptor socketInterceptor) {
		if (this.socketInterceptor == null){
			this.socketInterceptor = socketInterceptor;
		}
		return this;
	}

	/**
	 * socket地址
	 * @param host socket地址
	 * @return MessageCenter
	 */
	public MessageCenter host(String host){
		if (this.host == null){
			this.host = host;
		}
		return this;
	}

	/**
	 * socket端口
	 * @param port socket端口
	 * @return MessageCenter
	 */
	public MessageCenter port(int port){
		if (this.port <= 0){
			this.port = port;
		}
		return this;
	}
	/**
	 * 启动socket连接
	 */
	public static void connectSocket(){
		if (messageCenter != null){
			if (!TextUtils.isEmpty(messageCenter.host) && messageCenter.port!=0){
				messageCenter.centerAdministrator.connectServer(
						messageCenter.host,
						messageCenter.port,
						messageCenter.socketInterceptor);
			}
		}
	}
	/**
	 * 断开socket连接
	 */
	public static void disConnectSocket(){
		if (messageCenter != null){
			messageCenter.centerAdministrator.disConnectServer();
		}
	}

	/**
	 * 启动消息中心
	 * 同时连接socket服务器
	 * @return MessageCenter
	 */
	public static MessageCenter create() {
		if (messageCenter == null){
			messageCenter = new MessageCenter();
		}
		if (!messageCenter.centerAdministrator.isAlive() ){
			messageCenter.centerAdministrator.start();
		}
		return messageCenter;
	}
	/**
	 * 发出消息
	 * @param message 消息包裹
	 */
	public static void sendMessage(Msg message) {
		if (messageCenter != null){
			messageCenter.centerAdministrator.sendMessage(message);
		}
	}
	/**
	 * 注册客户
	 * @param handler 观察者
	 */
	public static void registMessageHandler(MessageHandler handler) {
		if (messageCenter != null){
			List<MessageHandler> handlers = messageCenter.messageHanlders.get(handler.getSubscribeId());
			if (handlers == null) {
				handlers = new ArrayList<MessageHandler>();
				messageCenter.messageHanlders.put(handler.getSubscribeId(), handlers);
			}
			handlers.add(handler);
		}
	}

	/**
	 * 注册客户
	 * 用注解方式注入handler
	 * @param object 订阅消息的java对象
	 */
	public static void registMessageHandler(final Object object){
		if (messageCenter != null){
			String tag;
			Method[] methods = object.getClass().getDeclaredMethods();
			for (final Method method : methods){
				method.setAccessible(true);
				if (method.isAnnotationPresent(MHanderReceiveTag.class)) {
					tag = method.getAnnotation(MHanderReceiveTag.class).value();
					MessageHandler messageHandler = new MessageHandler(tag, new MHanderReceiveListener() {
						@Override
						public void receivedMsg(final Msg message) {
							messageCenter.handler.postAtFrontOfQueue(new Runnable() {
								@Override
								public void run() {
									try {
										if (message.getContent()==null){
											method.invoke(object);
										}else{
											method.invoke(object, message.getContent());
										}
									}catch (Exception e){
										e.printStackTrace();
										Log.d("MessageCenter.class", object.getClass().getName()+"注入Hander失败！");
									}
								}
							});
						}
					});
					messageHandler.setObject(object);
					MessageCenter.registMessageHandler(messageHandler);
				}else if (method.isAnnotationPresent(MHanderSendTag.class)){
					tag = method.getAnnotation(MHanderSendTag.class).value();
					MessageHandler messageHandler = new MessageHandler(tag, new MHanderSendListener() {
						@Override
						public void sendMsgReturn(final Msg message, final int sendState) {
							messageCenter.handler.postAtFrontOfQueue(new Runnable() {
								@Override
								public void run() {
									try {
										if (message.getContent()==null){
											method.invoke(object, sendState);
										}else{
											method.invoke(object, message.getContent(), sendState);
										}
									}catch (Exception e){
										e.printStackTrace();
										Log.d("MessageCenter.class", object.getClass().getName()+"注入Hander失败！");
									}
								}
							});
						}
					});
					messageHandler.setObject(object);
					MessageCenter.registMessageHandler(messageHandler);
				}
			}
		}

	}
	/**
	 * 注销客户
	 * @param handler 观察者
	 */
	public static void unRegistMessageHandler(MessageHandler handler) {
		if (messageCenter != null){
			List<MessageHandler> handlers = messageCenter.messageHanlders.get(handler.getSubscribeId());
			if (handlers != null) {
				messageCenter.messageHanlders.get(handler.getSubscribeId()).remove(handler);
			}
		}
	}

	/**
	 * 注销客户
	 * @param obj 订阅消息的java对象
	 * @param tags 消息标识
	 */
	public synchronized static void unRegistMessageHandler(@NonNull Object obj, String... tags) {
		if (messageCenter != null){
			for(String tag : tags){
				List<MessageHandler> handlers = messageCenter.messageHanlders.get(tag);
				if (handlers != null) {
					List<MessageHandler> tempList = new ArrayList<>();
					for(MessageHandler messageHandler : handlers){
						if (messageHandler.getObject()!=null){
							if (messageHandler.getObject() == obj){
								tempList.add(messageHandler);
							}
						}
					}
					messageCenter.messageHanlders.get(tag).removeAll(tempList);
				}
			}
		}
	}

	/**
	 * 获取所有的订阅处理器
	 * @return 返回以消息为标识的观察者集合
	 */
	public static Map<String, List<MessageHandler>> getMessageHanlders() {
		if (messageCenter != null){
			return messageCenter.messageHanlders;
		}else {
			return new HashMap<>();
		}
	}
	
}
