package com.mszlu.rpc.netty.handler.client;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.constants.MsRpcConstants;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent stateEvent) {
            IdleState state = stateEvent.state();
            if (state==IdleState.WRITER_IDLE) {
                log.info("3s未收到写请求，发起心跳,地址：{}", ctx.channel().remoteAddress());
                MsMessage rpcMessage = new MsMessage();
                rpcMessage.setCodec(SerializationTypeEnum.Proto_stuff.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(MsRpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(MsRpcConstants.PING);
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }

        }else {
            super.channelRead(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //代表通道已连接
        //表示channel活着
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //代表连接关闭了
        log.info("服务端连接关闭:{}",ctx.channel().remoteAddress());
        //需要将缓存清除掉
        //标识channel不活着
        ctx.fireChannelInactive();
    }
}
