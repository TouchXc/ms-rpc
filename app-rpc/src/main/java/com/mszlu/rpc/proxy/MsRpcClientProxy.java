package com.mszlu.rpc.proxy;


import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.message.MsRequest;
import lombok.Getter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中
// 当我们通过动态代理对象调用一个方法时候
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
@Getter
public class MsRpcClientProxy implements InvocationHandler {

    private MsReference msReference;

    public MsRpcClientProxy() {
    }

    public MsRpcClientProxy(MsReference msReference) {
        this.msReference = msReference;
    }

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("rpc的代理实现类 调用了...");
        // 构建请求数据
        MsRequest request = MsRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .version(msReference.version())
                .build();
        //创建Netty客户端
        String host = msReference.host();
        int port = msReference.port();

        return null;
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);

    }

}
