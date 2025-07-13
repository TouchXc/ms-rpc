package com.mszlu.rpc.server;


import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyServer;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MsServiceProvider {

    private MsRpcConfig msRpcConfig;
    private final Map<String, Object> serviceMap;
    private final NacosTemplate nacosTemplate;

    public MsServiceProvider() {
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        //发布的服务 都在这里
        this.serviceMap = new ConcurrentHashMap<>();
    }

    public void publishService(MsService msService, Object service) {
        registerService(msService, service);
        //检测到有服务发布的注解，启动NettyServer
        NettyServer nettyService = SingletonFactory.getInstance(NettyServer.class);
        nettyService.setMsServiceProvider(this);
        if (!nettyService.isRunning()) {
            nettyService.run();
        }
    }

    private void registerService(MsService msService, Object service) {
        //service要进行注册, 先创建一个map进行存储
        String serviceName = service.getClass().getInterfaces()[0].getCanonicalName() + msService.version();
        this.serviceMap.put(serviceName, service);
        // 同步注册到Nacos中
        if (msRpcConfig==null) {
            throw new MsRpcException("必须开启EnableRpc");
        }
        try {
            Instance instance = new Instance();
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            instance.setPort(msRpcConfig.getProviderPort());
            instance.setClusterName("ms-rpc-service-provider");
            instance.setServiceName(serviceName);
            nacosTemplate.registerServer(msRpcConfig.getNacosGroup(),instance);
        } catch (Exception e) {
            log.error("nacos 注册服务失败:",e);
        }
        log.info("发现服务{}并注册",serviceName);
    }

    public Object getService(String serviceName) {
        return this.serviceMap.get(serviceName);
    }

    public MsRpcConfig getMsRpcConfig() {
        return msRpcConfig;
    }

    public void init(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;
        nacosTemplate.init(msRpcConfig.getNacosHost(),msRpcConfig.getNacosPort());
    }
}
