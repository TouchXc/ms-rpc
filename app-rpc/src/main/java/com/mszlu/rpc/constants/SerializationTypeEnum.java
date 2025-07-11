package com.mszlu.rpc.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    Proto_stuff((byte) 0x01, "proto_stuff");

    private final byte code;

    private final String name;

    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}