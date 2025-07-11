package com.mszlu.rpc.consumer.config;

import com.mszlu.rpc.annontation.EnableHttpClient;
import com.mszlu.rpc.annontation.EnableRpc;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableHttpClient(basePackage = "com.mszlu.rpc.consumer")
@EnableRpc
public class RpcConfig {
}