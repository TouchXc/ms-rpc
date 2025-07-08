package com.mszlu.rpc.compress;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;

import java.util.ServiceLoader;

public interface Compress {
    /**
     * 压缩方法名称
     * @return
     */
     String name();

    /**
     * 压缩
     * @param bytes
     * @return
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压缩
     * @param bytes
     * @return
     */
    byte[] decompress(byte[] bytes);


    default Compress loadCompress(byte compressType) {
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