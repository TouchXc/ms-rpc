package com.mszlu.rpc.netty.serialize;

import com.mszlu.rpc.constants.SerializationTypeEnum;
import io.netty.util.Constant;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器
 */
public class ProtostuffSerializer implements Serializer {
    /**
     * 避免每次序列化都重新申请Buffer空间,用来暂时存放对象序列化之后的数据
     * LinkedBuffer 是一个可复用的、链式的字节缓冲区，用于在序列化过程中临时缓存数据，避免频繁创建新的字节数组，减少 GC 压力
     */
    private final LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    /**
     * 缓存类对应的Schema，由于构造schema需要获得对象的类和字段信息，会用到反射机制
     * 这是一个很耗时的过程，因此进行缓存很有必要，下次遇到相同的类直接从缓存中get就行了
     */
    private Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    private final String SERIALIZATION_TYPE = SerializationTypeEnum.Proto_stuff.getName();

    @Override
    public String name() {
        return this.SERIALIZATION_TYPE;
    }

    @Override
    public byte[] serialize(Object obj) {
        Class<?> clazz = obj.getClass();
        Schema schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        }finally {
            buffer.clear();
        }
        return data;
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        Schema schema = getSchema(clazz);
        Object obj = schema.newMessage();
        //反序列化操作，将字节数组转换为对应的对象
        ProtostuffIOUtil.mergeFrom(bytes,obj,schema);
        return obj;
    }

    /**
     * @description 获取Schema
     * @param clazz
     * @return [io.protostuff.Schema]
     */
    private Schema<?> getSchema(Class<?> clazz) {
        //首先尝试从Map缓存中获取类对应的schema
        Schema<?> schema = schemaCache.get(clazz);
        if (Objects.isNull(schema)) {
            //RuntimeSchema将schema的创建过程封装了起来
            //线程安全 懒加载
            schema = RuntimeSchema.getSchema(clazz);
        }
        return schema;
    }
}
