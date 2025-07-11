package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MsRpcConstants;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.netty.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

public class MsRpcEncoder extends MessageToByteEncoder<MsMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MsMessage msg, ByteBuf out) throws Exception {
        final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);
        // 4B  magic number（魔法数）
        out.writeBytes(MsRpcConstants.MAGIC_NUMBER);
        // 1B version（版本）
        out.writeByte(MsRpcConstants.VERSION);
        // 4B full length（消息长度）空出四个字节预留
        out.writerIndex(out.writerIndex()+4);
        // 1B messageType（消息类型）
        byte messageType = msg.getMessageType();
        out.writeByte(messageType);
        // 1B codec（序列化类型）
        out.writeByte(msg.getCodec());
        // 1B compress（压缩类型）
        out.writeByte(msg.getCompress());
        // 4B  requestId（请求的Id） 原子操作 线程安全 相对加锁 效率高
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());
        byte[] bytes = null;
        // 消息头长度16
        int fullLength = MsRpcConstants.TOTAL_LENGTH;
        // 序列化数据
        Serializer serializer = loadSerializer(msg.getCodec());
        bytes = serializer.serialize(msg.getData());
        // 压缩数据
        Compress compress = loadCompress(msg.getCompress());
        bytes = compress.compress(bytes);
        // 消息长度 = 请求头+消息体长度
        fullLength += bytes.length;

        out.writeBytes(bytes);
        int writerIndex = out.writerIndex();
        // 写入消息长度参数
        out.writerIndex(writerIndex-fullLength+MsRpcConstants.MAGIC_NUMBER.length+MsRpcConstants.VERSION);
        out.writeInt(fullLength);
        out.writerIndex(writerIndex);
    }

    private Serializer loadSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name().equals(serializerName)) {
                return serializer;
            }
        }
        throw new MsRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if (compress.name().equals(compressName)) {
                return compress;
            }
        }
        throw new MsRpcException("无对应的压缩类型");
    }
}
