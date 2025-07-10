package com.mszlu.rpc.netty.handler.client;

import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class MsNettyClientHandler extends ChannelInboundHandlerAdapter {

    private final UnprocessRequests unprocessRequests;

    public MsNettyClientHandler() {
        this.unprocessRequests = SingletonFactory.getInstance(UnprocessRequests.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof MsMessage msMessage) {
                byte messageType = msMessage.getMessageType();
                // 接收到服务端发送的response，代表请求结束
                if (messageType == MessageTypeEnum.RESPONSE.getCode()) {
                    MsResponse<Object> data = (MsResponse<Object>)msMessage.getData();
                    unprocessRequests.complete(data);
                }
            }
        } finally {
            // 清除ByteBuf 避免内存泄露
            ReferenceCountUtil.release(msg);
        }
    }
}
