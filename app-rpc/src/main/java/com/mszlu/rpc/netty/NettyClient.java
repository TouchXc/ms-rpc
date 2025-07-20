package com.mszlu.rpc.netty;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.balance.LoadBalance;
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
import com.mszlu.rpc.netty.handler.client.ChannelCache;
import com.mszlu.rpc.netty.handler.client.MsNettyClientHandler;
import com.mszlu.rpc.netty.handler.client.UnprocessRequests;
import com.mszlu.rpc.netty.handler.idle.ConnectionWatchDog;
import com.mszlu.rpc.netty.timer.UpdateNacosServiceTask;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.utils.SPIUtils;
import com.sun.source.tree.NewClassTree;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.concurrent.CompleteFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient implements MsClient{

    @Getter
    private MsRpcConfig msRpcConfig;
    private final Bootstrap bootstrap;
    private final UnprocessRequests unprocessedRequests;
    private final NacosTemplate nacosTemplate;
    private final EventLoopGroup eventLoopGroup;
    private final ChannelCache channelCache;

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    // 用于缓存服务名称
    private static final Set<String> SERVICE = new CopyOnWriteArraySet<>();

    private String currentIpAndPort;
    // 定时任务器
    protected HashedWheelTimer serviceWheelTimer;
    // 负载均衡加载器
    protected LoadBalance loadBalance;

    public NettyClient() {
        this.channelCache = SingletonFactory.getInstance(ChannelCache.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
//                .handler(new ChannelInitializer<SocketChannel>() {
//                    @Override
//                    protected void initChannel(SocketChannel socketChannel) throws Exception {
//                        // 保活机制 3s
//                        socketChannel.pipeline().addLast(new IdleStateHandler(0,3,0, TimeUnit.SECONDS));
//                        socketChannel.pipeline().addLast("decoder",new MsRpcDecoder());
//                        socketChannel.pipeline().addLast("encoder",new MsRpcEncoder());
//                        socketChannel.pipeline().addLast("handler",new MsNettyClientHandler());
//                    }
//                });

    }


    @Override
    public Object sendRequest(MsRequest msRequest) {
        String serviceName = msRequest.getInterfaceName() + msRequest.getVersion();
        InetSocketAddress inetSocketAddress = null;
        // 优先获取缓存中的服务信息
        if (!SERVICE.isEmpty()) {
            inetSocketAddress = loadBalance.loadBalance(SERVICE);
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
            ipAndPort = ip + ":" + port;
            inetSocketAddress = new InetSocketAddress(ip,port);
            SERVICE.add(ipAndPort);
            this.currentIpAndPort = ipAndPort;
            // 设置定时任务
            if (serviceWheelTimer==null) {
                serviceWheelTimer = new HashedWheelTimer();
                serviceWheelTimer.newTimeout(
                        new UpdateNacosServiceTask(msRequest,this.msRpcConfig,SERVICE),
                        10,
                        TimeUnit.SECONDS);
            }

        }

        final String finalIpAndPort = ipAndPort;
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();

        ConnectionWatchDog watchDog = new ConnectionWatchDog(true,this.bootstrap,timer,inetSocketAddress,completableFuture) {
            @Override
            public void clear(InetSocketAddress inetSocketAddress) {
                SERVICE.remove(inetSocketAddress.getHostName()+":"+inetSocketAddress.getPort());
                channelCache.remove(inetSocketAddress);
                log.info("重连失败 清除缓存");
            }

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(0,3,0, TimeUnit.SECONDS),
                        new MsRpcDecoder(),
                        new MsRpcEncoder(),
                        new MsNettyClientHandler()
                };
            }
        };

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(watchDog.handlers());
            }
        });

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
        //            Channel channel = completableFuture.get();
        Channel channel = getChannel(inetSocketAddress,completableFuture);
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

        return resultFuture;
    }

    @SneakyThrows
    private Channel doConnect(InetSocketAddress inetSocketAddress, CompletableFuture<Channel> completableFuture,String finalIpAndPort) {
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
        return completableFuture.get();
    }

    @SneakyThrows
    private Channel getChannel(InetSocketAddress inetSocketAddress,CompletableFuture<Channel> channelCompletableFuture) {
        Channel channel = channelCache.get(inetSocketAddress);
        if (channel ==null) {
            // 直接连接
            channel = doConnect(inetSocketAddress,channelCompletableFuture,currentIpAndPort);
            channelCache.set(inetSocketAddress,channel);
            return channel;
        }else {
            log.info("当前使用从缓存中取出的channel");
            return channelCache.get(inetSocketAddress);
        }
    }

    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;
        this.loadBalance = SPIUtils.loadBalance(msRpcConfig.getLoadBalance());
    }

}
