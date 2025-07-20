package com.mszlu.rpc.balance;


import java.net.InetSocketAddress;
import java.util.Set;

//负载均衡器接口
public interface LoadBalance {

    String name();

    InetSocketAddress loadBalance(Set<String> services);

}
