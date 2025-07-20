package com.mszlu.rpc.balance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class RoundRobinLoadBalance implements LoadBalance {

     Integer pos = 0;

     public RoundRobinLoadBalance(){
     }

    @Override
    public String name() {
        return "roundRobinLoadBalance";
    }

    @Override
    public InetSocketAddress loadBalance(Set<String> services) {
        if (services.size() <= 0){
            return null;
        }
        List<String> list = new ArrayList<>(services);
        System.out.println(pos+".....");
        synchronized (pos){
            if (pos >= services.size()){
                pos = 0;
            }
            String ipAndPort = list.get(pos);
            pos++;
            String[] split = ipAndPort.split(":");
            log.info("应用了轮询算法负载均衡器...");

            return new InetSocketAddress(split[0],Integer.parseInt(split[1]));
        }
    }
}