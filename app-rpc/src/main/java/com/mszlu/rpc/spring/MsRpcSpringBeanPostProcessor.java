package com.mszlu.rpc.spring;


import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.proxy.MsRpcClientProxy;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Bean初始化前后处理
 */
@Component
@Slf4j
public class MsRpcSpringBeanPostProcessor implements BeanPostProcessor {

    final private MsServiceProvider msServiceProvider;

    public MsRpcSpringBeanPostProcessor() {
        //通过单例工厂获取服务提供者类
        this.msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
    }

    //bean初始化方法前被调用
    @Override
    @SneakyThrows
    public Object postProcessBeforeInitialization(Object bean, String beanName){
        return bean;
    }


    //bean初始化方法调用后被调用
    @SneakyThrows
    public Object postProcessAfterInitialization(Object bean, String beanName){
        //判断bean上是否有MsService注解 有则将其发布为服务
        if (bean.getClass().isAnnotationPresent(MsService.class)) {
            MsService msService = bean.getClass().getAnnotation(MsService.class);
            //发布服务，如果netty服务未启动进行启动
            msServiceProvider.publishService(msService,bean);
        }
        //判断bean里面的字段有没有加@MsRefrence注解 有则识别并生成代理实现类，发起网络请求
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            MsReference msReference = declaredField.getAnnotation(MsReference.class);
            if (msReference != null) {
                //代理实现类，调用方法的时候 会触发invoke方法，在其中实现网络调用
                MsRpcClientProxy msRpcClientProxy = new MsRpcClientProxy(msReference);
                Object proxy = msRpcClientProxy.getProxy(declaredField.getType());
                try {
                    //当isAccessible()的结果是false时不允许通过反射访问该字段
                    declaredField.setAccessible(true);
                    declaredField.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessException(e.getMessage());
                }
            }
        }
        return bean;
    }
}
