package com.mszlu.rpc.compress;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.VarHandle;
import java.util.ServiceLoader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class GzipCompress implements Compress {
    @Override
    public String name() {
        return CompressTypeEnum.GZIP.getName();
    }

    @Override
    public byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("传入待压缩的数据为空");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            gzip.write(bytes);
            gzip.flush();
            gzip.finish();
            return os.toByteArray();
        } catch (IOException e) {
            throw new MsRpcException("压缩数据出错", e);
        }

    }

    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("传入的解压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            byte[] buffer = new byte[1024 * 4];
            int n;
            while ((n = gzipInputStream.read(buffer)) > -1) {
                os.write(buffer, 0, n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new MsRpcException("解压缩数据出错", e);
        }
    }

}
