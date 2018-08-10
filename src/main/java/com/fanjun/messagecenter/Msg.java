package com.fanjun.messagecenter;

/**
 * 包裹
 * 存放消息
 * @author Administrator
 *
 */
public final class Msg {
	/**
	 *  destination标识
	 *  APP_INSIDE：应用内
	 */
	public static final int APP_INSIDE = 0;
	/**
	 *  destination标识
	 *  APP_OUTSIDE：应用外
	 */
	public static final int APP_OUTSIDE = 1;
	/**
	 * 发往目的地
	 * 0:应用内 
	 * 1:应用外
	 */
	private int destination;
	/**
	 * 消息订阅id
	 */
	private String tag;
	/**
	 * 消息内容
	 */
	private Object content;
	public static Msg ini(String tag, Object content){
		Msg msg = new Msg();
		msg.setTag(tag);
		msg.setContent(content);
		return msg;
	}

	/**
	 * 送往目的地
	 * @param destination APP_INSIDE OR APP_OUTSIDE
	 * @return Msg
	 */
	public Msg destination(int destination){
		this.setDestination(destination);
		return this;
	}

	/**
	 * 设置标识
	 * @param tag 标识
	 * @return Msg Msg
	 */
	public Msg tag(String tag) {
		this.tag = tag;
		return this;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public Object getContent() {
		return content;
	}
	public void setContent(Object content) {
		this.content = content;
	}
	public int getDestination() {
		return destination;
	}
	public void setDestination(int destination) {
		this.destination = destination;
	}
	
}
