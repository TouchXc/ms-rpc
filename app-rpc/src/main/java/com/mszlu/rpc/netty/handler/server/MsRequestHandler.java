package com.mszlu.rpc.netty.handler.server;

import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.server.MsServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MsRequestHandler {

    private final MsServiceProvider msServiceProvider;

    public MsRequestHandler(){
        this.msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
    }

    public Object handle(MsRequest msRequest) {
        String interfaceName = msRequest.getInterfaceName();
        String version = msRequest.getVersion();
        String serviceName = interfaceName + version;
        Object service = msServiceProvider.getService(serviceName);
        try {
            Method method = service.getClass().getMethod(msRequest.getMethodName(), msRequest.getParamTypes());
            return method.invoke(service, msRequest.getParameters());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new MsRpcException("服务调用出现问题:"+e.getMessage(),e);
        }
    }
}
