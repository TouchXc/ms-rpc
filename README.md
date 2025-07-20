<h1 align="center">手写RPC框架</h1>
<p align="center">
  <img src="https://img.shields.io/github/languages/code-size/TouchXc/ms-rpc" alt="code size"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/github/languages/count/TouchXc/ms-rpc" alt="languages"/>
  <img src="https://img.shields.io/badge/Java-17-blue" alt="Java"/>
  <img src="https://img.shields.io/github/last-commit/TouchXc/ms-rpc" alt="last commit"/><br>
  <img src="https://img.shields.io/badge/Author-TouchXc-orange" alt="Author" />
</p>
<hr>

## 描述    
本项目来自【码神之路】手写RPC框架项目，借助Netty、Nacos等工具实现了下面的功能：

- 服务端服务注册与服务发现（Nacos）
- 客户端代理模式、序列化与反序列化机制（自定义消息格式、编解码器）
- 服务端与客户端网络通信：Netty框架NIO、channel连接缓存
- 心跳检测、负载均衡、请求重试



