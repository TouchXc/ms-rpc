package com.mszlu.rpc.netty.handler.server;

import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MsNettyServerHandler extends ChannelInboundHandlerAdapter {

    private final MsRequestHandler msRequestHandler;

    public MsNettyServerHandler() {
        this.msRequestHandler = SingletonFactory.getInstance(MsRequestHandler.class);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //这里接收到请求的信息，然后根据请求，找到对应的服务提供者，调用，获取结果，然后返回
        //消费方 会启动一个客户端，用户接收返回的数据
        try {
            if (msg instanceof MsMessage msMessage) {
                Object data = msMessage.getData();
                if (data instanceof MsRequest msRequest) {
                    Object handler = msRequestHandler.handle(msRequest);
                    msMessage.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    if (ctx.channel().isActive()&&ctx.channel().isWritable()) {
                        MsResponse<Object> msResponse = MsResponse.success(handler, msRequest.getRequestId());
                        msMessage.setData(msResponse);
                    }else{
                        MsResponse<Object> msResponse = MsResponse.fail("请求失败，失败id："+msMessage.getRequestId());
                        msMessage.setData(msResponse);
                    }
                    log.info("服务端收到数据，并处理完成{}:",msMessage);
                    // 写完数据后关闭通道
                    ctx.writeAndFlush(msMessage).addListener(ChannelFutureListener.CLOSE);
                }

            }
        }catch (Exception e){
            throw new MsRpcException("数据读取异常",e);
        }finally {
            //释放 以防内存泄露
            ReferenceCountUtil.release(msg);
        }
    }
}
