package com.mszlu.rpc.utils;

import com.mszlu.rpc.balance.LoadBalance;
import com.mszlu.rpc.exception.MsRpcException;

import java.util.ServiceLoader;

public class SPIUtils {

    public static LoadBalance loadBalance(String name) {
        ServiceLoader<LoadBalance> load = ServiceLoader.load(LoadBalance.class);
        for (LoadBalance loadBalance : load) {
            if (loadBalance.name().equals(name)) {
                return loadBalance;
            }
        }
        throw new MsRpcException("无对应的负载均衡器");
    }
}