package com.mszlu.rpc.factory;


import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.handler.server.MsNettyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;


public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast("decoder",new MsRpcDecoder());
        channel.pipeline().addLast("encoder",new MsRpcEncoder());
        channel.pipeline ().addLast(eventExecutors,"handler",new MsNettyServerHandler());
    }
}
