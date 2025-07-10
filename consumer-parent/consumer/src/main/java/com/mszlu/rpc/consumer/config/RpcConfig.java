package com.mszlu.rpc.consumer.config;

import com.mszlu.rpc.annontation.EnableHttpClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableHttpClient(basePackage = "com.mszlu.rpc.consumer")
public class RpcConfig {
}