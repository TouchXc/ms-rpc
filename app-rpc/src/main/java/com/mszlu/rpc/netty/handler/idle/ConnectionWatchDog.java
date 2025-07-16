package com.mszlu.rpc.netty.handler.idle;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.VolatileImage;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
@Slf4j
public abstract class ConnectionWatchDog extends ChannelInboundHandlerAdapter implements
        TimerTask, ChannelHandlerHolder,CacheClearHandler{

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final InetSocketAddress inetSocketAddress;

    private volatile boolean reconnect = true;
    private int attempts;

    private final CompletableFuture<Channel> completableFuture;

    public ConnectionWatchDog(boolean reconnect, Bootstrap bootstrap, Timer timer, InetSocketAddress inetSocketAddress, CompletableFuture<Channel> completableFuture) {
        this.reconnect = reconnect;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.inetSocketAddress = inetSocketAddress;
        this.completableFuture = completableFuture;
    }


    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        attempts = 0;
        ctx.fireChannelActive();
    }


    /**
     * 链路关闭时，尝试去重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(reconnect){
            log.info("链接关闭，将进行重连");
            if (attempts < 12) {
                attempts++;
                log.info("重连次数:{}",attempts);
            }else{
                //不在重连了
                reconnect = false;
                //连接失败 从 缓存中 去除
                clear(inetSocketAddress);
            }
            //重连的间隔时间会越来越长
            int timeout = 2 << attempts;
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future;
        synchronized (this.bootstrap) {
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(inetSocketAddress);
        }
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()){
                    //代表连接成功，将channel放入任务中
                    completableFuture.complete(f.channel());
                }else {
                    completableFuture.completeExceptionally(future.cause());
                    //尝试重连
                    f.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }
}
