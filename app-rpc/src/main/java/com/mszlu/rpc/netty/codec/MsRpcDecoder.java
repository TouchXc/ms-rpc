package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MsRpcConstants;
import com.mszlu.rpc.exception.MsRpcException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MsRpcDecoder extends LengthFieldBasedFrameDecoder {

    public MsRpcDecoder() {
        this(8*1024*1024,5,4,-9,0);
    }

    public MsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }


    protected Object decode(ChannelHandlerContext ctx, ByteBuf in)throws Exception{
        // TODO 实现
        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decode;
            if(frame.readableBytes()< MsRpcConstants.TOTAL_LENGTH){
                throw new MsRpcException("数据长度不符,格式有误");
            }
            return decodeFrame(frame);
        }
        return decode;
    }

    private Object decodeFrame(ByteBuf in)throws Exception{
        //顺序读取
        //1. 先读取魔法数，确定是我们自定义的协议
        checkMagicNumber(in);
        //2. 检查版本
        checkVersion(in);
        //3.数据长度
        int fullLength = in.readInt();
        //4.messageType 消息类型
        byte messageType = in.readByte();
        //5.序列化类型
        byte codecType = in.readByte();
        //6.压缩类型
        byte compressType = in.readByte();
        //7. 请求id
        int requestId = in.readInt();
        //8. 读取数据
        int bodyLength = fullLength - MsRpcConstants.TOTAL_LENGTH;
        if (bodyLength > 0){
            //有数据,读取body的数据
            byte[] bodyData = new byte[bodyLength];
            in.readBytes(bodyData);
            //解压缩 使用gzip
            String compressName = CompressTypeEnum.getName(compress);

        }
        return null;
    }

    private void checkVersion(ByteBuf in) {
        byte b = in.readByte();
        if (b != MsRpcConstants.VERSION){
            throw new MsRpcException("未知的version");
        }
    }

    private void checkMagicNumber(ByteBuf in) {
        byte[] tmp = new byte[MsRpcConstants.MAGIC_NUMBER.length];
        in.readBytes(tmp);
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i] != MsRpcConstants.MAGIC_NUMBER[i]) {
                throw new MsRpcException("未知的magic number");
            }
        }
    }
}
