package com.fanjun.messagecenter.socket;

import android.os.Looper;
import android.text.TextUtils;

import com.fanjun.messagecenter.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ConnectionAssistant connectionAssistant;
    private android.os.Handler handler;
    private boolean hasConnected;

    public ClientHandler(SocketInterceptor socketInterceptor, ConnectionAssistant connectionAssistant) {
        super();
        this.connectionAssistant = connectionAssistant;
        this.socketInterceptor = socketInterceptor;
        this.handler = new android.os.Handler(Looper.getMainLooper());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //处理服务数据，注意有黏包情况
        final ByteBuf result = (ByteBuf) msg;
        final byte[] result1 = new byte[result.readableBytes()];
        result.readBytes(result1);
        if (socketInterceptor != null) {
            try {
                //开始符
                final String START_TAG = socketInterceptor.getStartTag();
                //结束符
                final String END_TAG = socketInterceptor.getEndTag();
                //socket收到的源数据
                final String sourceData = new String(result1, "utf-8");
                //如果开始结束符均不为空则认为需处理分包
                if (!TextUtils.isEmpty(START_TAG) && !TextUtils.isEmpty(END_TAG)) {
                    //新数据拼接老的黏包，注意拼接数据，老的在前面
                    String newSourceData = socketInterceptor.getCutData() + sourceData;
                    //查找新数据源需要立即分发的数据，一组START_TAG + END_TAG构成一个分发数据，但要注意处理中间值为空的情况，在极端情况可能出现无效数据，注意处理异常
                    final List<String> handDatas = findHandData(newSourceData, START_TAG, END_TAG);
                    //整理出最新的粘包数据
                    socketInterceptor.setCutData(sortCutData(newSourceData, START_TAG, END_TAG));
                    //分发到业务层
                    handler.postAtFrontOfQueue(new Runnable() {
                        @Override
                        public void run() {
                            for (String handData : handDatas) {
                                socketInterceptor.receiveServerMsg(handData);
                            }
                        }
                    });
                } else {
                    handler.postAtFrontOfQueue(new Runnable() {
                        @Override
                        public void run() {
                            //分发到业务层
                            socketInterceptor.receiveServerMsg(sourceData);
                        }
                    });
                }
                result.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    result.release();
                } catch (Exception e1) {
                }
            }

        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        this.ctx = ctx;
        if (connectionAssistant != null && !hasConnected) {
            connectionAssistant.connectionSuccess();
        }
        hasConnected = true;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.ctx = null;
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
        hasConnected = false;
        if (connectionAssistant != null) {
            connectionAssistant.connectionInterrupt(cause == null ? new Exception(new Throwable("未知异常断开socket")) : new Exception(cause));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.ctx = null;
        try {
            super.channelInactive(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                if (socketInterceptor != null) {
                    String heartbeat = socketInterceptor.heartbeat();
                    String START_TAG = socketInterceptor.getStartTag();
                    String END_TAG = socketInterceptor.getEndTag();
                    if (!TextUtils.isEmpty(heartbeat)){
                        ByteBuf buf = null;
                        if (!TextUtils.isEmpty(START_TAG) && !TextUtils.isEmpty(END_TAG)){
                            StringBuilder sb = new StringBuilder();
                            sb.append(START_TAG).append(heartbeat).append(END_TAG);
                            buf = Unpooled.copiedBuffer(sb.toString().getBytes());
                        }else{
                            buf = Unpooled.copiedBuffer(heartbeat.getBytes());
                        }
                        ctx.channel().writeAndFlush(buf);
                    }
                }
            }
        }
    }

    public boolean sendMessage(Msg msg) {
        if (ctx == null) {
            return false;
        } else {
            try {
                if (socketInterceptor != null) {
                    String sendMsgStr = socketInterceptor.packageMsg(msg.getContent());
                    String START_TAG = socketInterceptor.getStartTag();
                    String END_TAG = socketInterceptor.getEndTag();
                    if (!TextUtils.isEmpty(sendMsgStr)){
                        ByteBuf buf = null;
                        if (!TextUtils.isEmpty(START_TAG) && !TextUtils.isEmpty(END_TAG)){
                            StringBuilder sb = new StringBuilder();
                            sb.append(START_TAG).append(sendMsgStr).append(END_TAG);
                            buf = Unpooled.copiedBuffer(sb.toString().getBytes());
                        }else{
                            buf = Unpooled.copiedBuffer(sendMsgStr.getBytes());
                        }
                        ctx.channel().writeAndFlush(buf);
                    }
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 查找符合开始和结束标识的字符串
     * 如“startTag你好endTagstartTag朋友endTag”，得到“你好、朋友”
     *
     * @param sourceData
     * @param startTag
     * @param endTag
     * @return
     */
    public static List<String> findHandData(String sourceData, String startTag, String endTag) {
        List<String> handData = new ArrayList<>();
        String regex = startTag + ".*?" + endTag;
        String tempSourceData = sourceData;
        String data = null;
        while (true) {
            if (data != null) {
                tempSourceData = tempSourceData.substring(tempSourceData.indexOf(data) + data.length(), tempSourceData.length());
            }
            data = filerStartEnd(tempSourceData, regex);
            if (data != null) {
                //如果存在多个startTag开始符异常数据时，以最接近endTag结束符号的startTag开始符号为准
                if (data.indexOf(startTag) < data.lastIndexOf(startTag)) {
                    String tempData = data.substring(data.lastIndexOf(startTag), data.length());
                    handData.add(tempData);
                } else {
                    handData.add(data);
                }

            } else {
                break;
            }
        }
        return handData;
    }

    /**
     * 递归取出符合首位符号规则的字符串
     *
     * @param sourceData
     * @param regex
     * @return
     */
    public static String filerStartEnd(String sourceData, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sourceData);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    /**
     * 整理粘包数据
     *
     * @param sourceData
     * @param startTag
     * @param endTag
     * @return
     */
    public static String sortCutData(String sourceData, String startTag, String endTag) {
        String newSourceData = sourceData;
        int startTagLastPosition = newSourceData.lastIndexOf(startTag);
        int endTagLastPosition = newSourceData.lastIndexOf(endTag);
        if (startTagLastPosition >= 0) {
            //如果存在startTag标识，但不存在endTag标识，则将startTag标识以后的作为粘包数据
            if (endTagLastPosition < 0) {
                newSourceData = newSourceData.substring(startTagLastPosition, newSourceData.length());
            } else {
                //如果存在startTag/endTag标识，但startTag在endTag后，则以startTag标识以后的作为粘包数据
                if (endTagLastPosition < startTagLastPosition) {
                    newSourceData = newSourceData.substring(startTagLastPosition, newSourceData.length());
                } else {
                    //如果存在startTag/endTag标识，但startTag在endTag前，则后续数据可能为不完整的粘包数据
                    newSourceData = newSourceData.substring(endTagLastPosition, newSourceData.length());
                }
            }
        }
        return newSourceData;
    }
}
