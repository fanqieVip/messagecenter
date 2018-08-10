package com.fanjun.messagecenter.socket;

import com.fanjun.messagecenter.MessageCenter;
import com.fanjun.messagecenter.Msg;
import com.google.gson.Gson;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 客户端处理器
 */
public class ClientHandler extends SimpleChannelInboundHandler<Object> {
	private ChannelHandlerContext ctx;
	private SocketInterceptor socketInterceptor;
	private Gson gson = new Gson();

	public ClientHandler(SocketInterceptor socketInterceptor) {
		super();
		this.socketInterceptor = socketInterceptor;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg){
		//处理服务数据，注意有黏包情况
		ByteBuf result = (ByteBuf) msg;
		byte[] result1 = new byte[result.readableBytes()];
		result.readBytes(result1);
		try {
			if (socketInterceptor != null){
				List<ReceiveMsg> receiveMsgs = socketInterceptor.receiveServerMsg(new String(result1, "utf-8"));
				if (receiveMsgs != null){
					for(ReceiveMsg receiveMsg: receiveMsgs){
						Msg message = new Msg();
						message.setDestination(Msg.APP_INSIDE);
						message.setContent(receiveMsg.getObj());
						message.setTag(receiveMsg.getTag());
						MessageCenter.sendMessage(message);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		result.release();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {}

	@Override
	public void channelActive(final ChannelHandlerContext ctx){
		this.ctx = ctx;
		if (socketInterceptor != null){
			socketInterceptor.connectionSuccess();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		this.ctx = null;
		// 当出现异常就关闭连接
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx){
		this.ctx = null;
		try {
			super.channelInactive(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (socketInterceptor != null){
			socketInterceptor.connectionInterrupt(new Exception("连接已断开"));
		}
	}
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.WRITER_IDLE) {
				if (socketInterceptor != null){
					String heartbeat = socketInterceptor.heartbeat();
					if (heartbeat!=null){
						ByteBuf buf = Unpooled.copiedBuffer(heartbeat.getBytes());
						ctx.channel().writeAndFlush(buf);
					}
				}
			}
		}
	}
	public boolean sendMessage(Msg msg) {
		if (ctx == null) {
			return false;
		}else{
			try {
				ByteBuf buf = Unpooled.copiedBuffer(gson.toJson(msg.getContent()).getBytes());
				ctx.channel().writeAndFlush(buf);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
}
