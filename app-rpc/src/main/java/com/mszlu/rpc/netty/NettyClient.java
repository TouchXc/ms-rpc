package com.mszlu.rpc.netty;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.MsRpcConfig;
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
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.sun.source.tree.NewClassTree;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient implements MsClient{

    @Setter
    @Getter
    private MsRpcConfig msRpcConfig;
    private final Bootstrap bootstrap;
    private final UnprocessRequests unprocessedRequests;
    private final NacosTemplate nacosTemplate;
    private final EventLoopGroup eventLoopGroup;
    // 用于缓存服务名称
    private static final Set<String> SERVICE = new CopyOnWriteArraySet<>();

    public NettyClient() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // 保活机制 3s
                        socketChannel.pipeline().addLast(new IdleStateHandler(0,3,0, TimeUnit.SECONDS));
                        socketChannel.pipeline().addLast("decoder",new MsRpcDecoder());
                        socketChannel.pipeline().addLast("encoder",new MsRpcEncoder());
                        socketChannel.pipeline().addLast("handler",new MsNettyClientHandler());
                    }
                });

    }


    @Override
    public Object sendRequest(MsRequest msRequest) {
        String serviceName = msRequest.getInterfaceName() + msRequest.getVersion();
        InetSocketAddress inetSocketAddress = null;
        // 优先获取缓存中的服务信息

        if (!SERVICE.isEmpty()) {
            //随机获取一个服务
            Optional<String> oneServiceOptional = SERVICE.stream().skip(SERVICE.size() - 1).findFirst();
            if (oneServiceOptional.isPresent()) {
                String serviceInfo = oneServiceOptional.get();
                String[] ipAndPort = serviceInfo.split(":");
                inetSocketAddress = new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            }
        }
        // 无缓存获取 直接走Nacos获取
        String ipAndPort = null;
        if (inetSocketAddress==null) {
            Instance oneHealthyInstance = null;
            try {
                oneHealthyInstance = nacosTemplate.getOneHealthyInstance(serviceName, msRpcConfig.getNacosGroup());
            } catch (Exception e) {
                log.error("获取nacos实例失败");
//                return resultFuture.completeExceptionally(e);
                throw new MsRpcException("没有获取到可用的服务提供者");
            }
            // 缓存服务实例信息
            String ip = oneHealthyInstance.getIp();
            int port = oneHealthyInstance.getPort();
            SERVICE.add(ip + ":" + port);
            ipAndPort = ip + ":" + port;
            inetSocketAddress = new InetSocketAddress(ip,port);
        }
        final String finalIpAndPort = ipAndPort;
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                //代表连接成功，将channel放入任务中
                completableFuture.complete(channelFuture.channel());
            }else {
                SERVICE.remove(finalIpAndPort);
                completableFuture.completeExceptionally(channelFuture.cause());
                throw new MsRpcException("连接服务器失败");
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
                channel.writeAndFlush(msMessage).addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()){
                        //任务完成
                        log.info("发送数据成功:{}",msMessage);
                    }else{
                        //发送数据失败
                        channelFuture.channel().close();
                        //任务标识为完成 有异常
                        resultFuture.completeExceptionally(channelFuture.cause());
                        log.error("发送数据失败:",channelFuture.cause());
                    }
                });

            }

        } catch (InterruptedException | ExecutionException e) {
            throw new MsRpcException("获取channel失败",e);
        }
        return resultFuture;
    }

}
