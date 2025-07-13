package com.mszlu.rpc.register.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

public class NacosTemplate {

    private ConfigService configService;//主要用作配置方面的管理功能
    private NamingService namingService;//主要用作服务方面的管理功能

    public NacosTemplate(){
    }

    public void init(String nacosHost, int nacosPort) {
        try {
//            configService = NacosFactory.createConfigService("localhost:8848");
//            this.namingService = NacosFactory.createNamingService("localhost:8848");
            namingService = NacosFactory.createNamingService(nacosHost+":"+nacosPort);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    // 注册服务
    public void registerServer(Instance instance)throws Exception{
        namingService.registerInstance(instance.getServiceName(), instance);
    }

    // 注册服务
    public void registerServer(String groupName, Instance instance)throws Exception{
        namingService.registerInstance(instance.getServiceName(),groupName, instance);
    }

    // 删除服务
    public void deleteServer(Instance instance)throws Exception{
        namingService.deregisterInstance(instance.getServiceName(), instance.getIp(),instance);
    }

    // 随机全部 拿到全部实例后，我们可以按照自己的负载均衡算法进行调用。类似于springCloud的ribbon。
    public List<Instance> getAllServer(String serverName)throws Exception{
        return namingService.getAllInstances(serverName);
    }

    //根据负载均衡算法获取一个健康的实例
    public Instance getOneHealthyInstance(String serverName,String groupName)throws Exception{
        return namingService.selectOneHealthyInstance(serverName,groupName);
    }


}
