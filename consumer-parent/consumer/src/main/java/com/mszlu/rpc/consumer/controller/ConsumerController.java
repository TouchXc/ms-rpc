package com.mszlu.rpc.consumer.controller;


import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.provider.service.GoodsService;
import com.mszlu.rpc.provider.service.modal.Goods;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    /**
     * Http实现方式
     */
//    @Resource
//    private GoodsHttpRpc goodsHttpRpc;
//
//    @GetMapping("/find/{id}")
//    public Goods find(@PathVariable Long id){
//        return goodsHttpRpc.findGoods(id);
//    }

    /**
     * TCP实现方式
     */
    @MsReference(host = "localhost", port = 13567)
    private GoodsService goodsService;

    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsService.findGoods(id);
    }

}
