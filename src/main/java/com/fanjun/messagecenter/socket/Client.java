package com.fanjun.messagecenter.socket;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 客户端
 */
public class Client extends Thread {
    private ClientHandler clientHandler;
    private String host;
    private int port;
    EventLoopGroup workerGroup;
    SocketInterceptor socketInterceptor;
    ConnectionAssistant connectionAssistant;

    public Client(String host, int port) {
        super();
        this.host = host;
        this.port = port;
        workerGroup = new NioEventLoopGroup();
    }

    public void setSocketInterceptor(SocketInterceptor socketInterceptor) {
        this.socketInterceptor = socketInterceptor;
    }

    public void setConnectionAssistant(ConnectionAssistant connectionAssistant) {
        this.connectionAssistant = connectionAssistant;
    }

    @Override
    public void run() {
        super.run();
        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 主动断开连接
     */
    public void disConnect() {
        workerGroup.shutdownGracefully();
        if (this.isAlive() || !this.isInterrupted()) {
            this.interrupt();
        }
    }

    /**
     * 连接服务器
     *
     * @throws Exception
     */
    private void connect() throws Exception {
        try {
            final Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.SO_TIMEOUT, 3000);
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator());
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    //心跳
                    ch.pipeline().addLast(new IdleStateHandler(10, 10, 20, TimeUnit.SECONDS));
                    //处理器
                    clientHandler = new ClientHandler(socketInterceptor, connectionAssistant);
                    ch.pipeline().addLast(clientHandler);
                }
            });
            //开启客户端
            final ChannelFuture future = b.connect(host, port).sync();
            //等待直到连接断开
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            if (connectionAssistant != null) {
                connectionAssistant.connectionInterrupt(e);
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }
}
