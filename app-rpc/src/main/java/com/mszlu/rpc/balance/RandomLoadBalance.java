package com.mszlu.rpc.balance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.Set;


@Slf4j
public class RandomLoadBalance implements LoadBalance {
    @Override
    public String name() {
        return "randomLoadBalance";
    }

    @Override
    public InetSocketAddress loadBalance(Set<String> services) {
        if (services.size() <= 0){
            return null;
        }
        Random random = new Random();
        int nextInt = random.nextInt(0, services.size());
        Optional<String> optional = services.stream().skip(nextInt).findFirst();
        if (optional.isPresent()){
            String ipAndPort = optional.get();
            String[] split = ipAndPort.split(":");
            log.info("应用了随机算法负载均衡器...");
            return new InetSocketAddress(split[0],Integer.parseInt(split[1]));
        }
        return null;
    }
}
