package com.mszlu.rpc.netty;

import com.mszlu.rpc.message.MsRequest;

public interface MsClient {

    Object sendRequest(MsRequest msRequest);
}
