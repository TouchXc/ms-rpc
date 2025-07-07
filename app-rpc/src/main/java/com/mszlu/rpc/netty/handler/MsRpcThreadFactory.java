package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.server.MsServiceProvider;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// 实现ThreadFactory 作用是用于自定义线程的创建方式
public class MsRpcThreadFactory implements ThreadFactory {

    private MsServiceProvider msServiceProvider;

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    private final ThreadGroup threadGroup;

    public MsRpcThreadFactory(MsServiceProvider msServiceProvider) {
        this.msServiceProvider = msServiceProvider;
        // 直接使用当前线程的线程组（替代 SecurityManager 逻辑）
        this.threadGroup = Thread.currentThread().getThreadGroup();
        this.namePrefix = "ms-rpc-" + poolNumber.getAndIncrement() + "-thread-";
    }

    //创建的线程以“N-thread-M”命名，N是该工厂的序号，M是线程号
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(this.threadGroup, runnable,
                this.namePrefix + this.threadNumber.getAndIncrement(), 0);
        // 设置守护线程
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
