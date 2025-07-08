package com.mszlu.rpc.netty;

import com.mszlu.rpc.message.MsRequest;

import java.util.Objects;

public interface MsClient {

    Object sendRequest(MsRequest msRequest,String host,int port);
}
