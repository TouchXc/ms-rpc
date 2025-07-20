package com.mszlu.rpc.spring;


import com.mszlu.rpc.annontation.EnableRpc;
import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyClient;
import com.mszlu.rpc.proxy.MsRpcClientProxy;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bean初始化前后处理
 */
@Component
@Slf4j
public class MsRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {

    final private MsServiceProvider msServiceProvider;
    final private NettyClient nettyClient;
    private MsRpcConfig msRpcConfig;
    private final NacosTemplate nacosTemplate;


    public MsRpcSpringBeanPostProcessor() {
        //通过单例工厂获取服务提供者类
        this.msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
        this.nettyClient = SingletonFactory.getInstance(NettyClient.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    //bean初始化方法前被调用
    @Override
    @SneakyThrows
    public Object postProcessBeforeInitialization(Object bean, String beanName){
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if (enableRpc!=null) {
            if (msRpcConfig==null) {
                log.info("EnableRpc 会先于所有的Bean实例之前初始化");
                MsRpcConfig msRpcConfig = new MsRpcConfig();
                msRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                msRpcConfig.setNacosPort(enableRpc.nacosPort());
                msRpcConfig.setProviderPort(enableRpc.serverPort());
                msRpcConfig.setNacosHost(enableRpc.nacosHost());
                msServiceProvider.init(msRpcConfig);
                // 根据nacos配置进行初始化
                nacosTemplate.init(msRpcConfig.getNacosHost(), msRpcConfig.getNacosPort());
                // 客户端配置
                nettyClient.setMsRpcConfig(msRpcConfig);
            }
        }
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
                MsRpcClientProxy msRpcClientProxy = new MsRpcClientProxy(msReference, nettyClient);
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

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
            // 加载类扫描器
            try {
                Class<?> scannerClass = ClassUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner", MsRpcSpringBeanPostProcessor.class.getClassLoader());
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
                        .newInstance(new Object[]{beanDefinitionRegistry, true});
                // 加载过滤器
                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter", MsRpcSpringBeanPostProcessor.class.getClassLoader());
                Object filter = filterClass.getConstructor(Class.class).newInstance(EnableRpc.class);
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter", ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", MsRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke(scanner, filter);
                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner,new Object[]{"com.mszlu.rpc.annontation"});
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;

    }
}
