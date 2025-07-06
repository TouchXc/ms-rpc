package com.mszlu.rpc.bean;

import com.mszlu.rpc.annontation.MsHttpClient;
import com.mszlu.rpc.proxy.MsHttpClientProxy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;


//FactoryBean是一个工厂Bean，可以生成某一个类型Bean实例，它最大的一个作用是：可以让我们自定义Bean的创建过程。
@Setter
@Getter
public class MsHttpClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;
    //返回的对象实例
    @Override
    public T getObject() throws Exception {
        return new MsHttpClientProxy().getProxy(interfaceClass);
    }
    //Bean的类型
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

}
