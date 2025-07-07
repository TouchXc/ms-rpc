package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.constants.MsRpcConstants;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.netty.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.ServiceLoader;

/**
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 */
public class MsRpcDecoder extends LengthFieldBasedFrameDecoder {

    public MsRpcDecoder() {
        this(8*1024*1024,5,4,-9,0);
    }

    /**
     *
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5
     * @param lengthFieldLength 消息长度的大小  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿值 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,我们这为0
     */
    public MsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }


    protected Object decode(ChannelHandlerContext ctx, ByteBuf in)throws Exception{
        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf frame) {
            if(frame.readableBytes()< MsRpcConstants.TOTAL_LENGTH){
                throw new MsRpcException("数据长度不符,格式有误");
            }
            return decodeFrame(frame);
        }
        return decode;
    }

    private Object decodeFrame(ByteBuf in){
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

        MsMessage msMessage = MsMessage.builder()
                .messageType(messageType)
                .requestId(requestId)
                .codec(codecType)
                .compress(compressType)
                .build();

        if (bodyLength > 0){
            //有数据,读取body的数据
            byte[] bodyData = new byte[bodyLength];
            in.readBytes(bodyData);
            //解压缩 使用gzip
            Compress compress = loadCompress(compressType);
            bodyData = compress.decompress(bodyData);
            // 反序列化
            Serializer serializer = loadSerializer(codecType);
            if (MessageTypeEnum.REQUEST.getCode()==codecType) {
                Object object = serializer.deserialize(bodyData, MsRequest.class);
                if (object instanceof MsRequest msRequest) {
                    msMessage.setData(msRequest);
                }
            } else if (MessageTypeEnum.RESPONSE.getCode()==codecType) {
                Object object = serializer.deserialize(bodyData, MsResponse.class);
                if (object instanceof MsResponse<?> msResponse) {
                    msMessage.setData(msResponse);
                }
            }
        }
        return msMessage;
    }

    private Serializer loadSerializer(byte codec) {
        String name = SerializationTypeEnum.getName(codec);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name().equals(name)) {
                return serializer;
            }
        }
        throw new MsRpcException("无对应序列化类型");
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

    private Compress loadCompress(byte compressType) {
        String name = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> compressServiceLoader = ServiceLoader.load(Compress.class);
        for (Compress compress : compressServiceLoader) {
            if (compress.name().equals(name)) {
                return compress;
            }
        }
        throw new MsRpcException("没有找到对应的压缩方式");
    }
}
