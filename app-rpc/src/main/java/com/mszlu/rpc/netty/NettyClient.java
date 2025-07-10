package com.mszlu.rpc.netty;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import com.mszlu.rpc.netty.handler.client.MsNettyClientHandler;
import com.mszlu.rpc.netty.handler.client.UnprocessRequests;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyClient implements MsClient{

    private final Bootstrap bootstrap;
    private final UnprocessRequests unprocessedRequests;

    public NettyClient() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessRequests.class);
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast("decoder",new MsRpcDecoder());
                        socketChannel.pipeline().addLast("encoder",new MsRpcEncoder());
                        socketChannel.pipeline().addLast("handler",new MsNettyClientHandler());
                    }
                });

    }


    @Override
    public Object sendRequest(MsRequest msRequest, String host, int port) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host,port);
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    //代表连接成功，将channel放入任务中
                    completableFuture.complete(channelFuture.channel());
                }else {
                    throw new MsRpcException("连接服务器失败");
                }
            }
        });

        CompletableFuture<MsResponse<Object>> resultFuture = new CompletableFuture<>();
        try {
            Channel channel = completableFuture.get();
            if (channel.isActive()) {
                unprocessedRequests.put(msRequest.getRequestId(),resultFuture);
                MsMessage msMessage = MsMessage.builder()
                        .messageType(MessageTypeEnum.REQUEST.getCode())
                        .codec(SerializationTypeEnum.Proto_stuff.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .data(msRequest)
                        .build();
                channel.writeAndFlush(msMessage).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()){
                            //任务完成
                            log.info("发送数据成功:{}",msMessage);
                        }else{
                            //发送数据失败
                            channelFuture.channel().close();
                            //任务标识为完成 有异常
                            resultFuture.completeExceptionally(channelFuture.cause());
                            log.info("发送数据失败:",channelFuture.cause());
                        }
                    }
                });

            }

        } catch (InterruptedException | ExecutionException e) {
            throw new MsRpcException("获取channel失败",e);
        }
        return resultFuture;
    }
}
