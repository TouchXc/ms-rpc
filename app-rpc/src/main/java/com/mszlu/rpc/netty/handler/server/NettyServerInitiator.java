package com.mszlu.rpc.netty.handler.server;


import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;


public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    protected void initChannel(SocketChannel ch) throws Exception {
        // TCP保活机制参数 10s
        ch.pipeline().addLast(new IdleStateHandler(10,0,0, TimeUnit.SECONDS));
        // 解码器
        ch.pipeline().addLast("decoder",new MsRpcDecoder());
        // 编码器
        ch.pipeline().addLast("encoder",new MsRpcEncoder());
        // 业务处理
        ch.pipeline().addLast(this.eventExecutors,"handler",new MsNettyServerHandler());
    }
}
