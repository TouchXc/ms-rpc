package com.mszlu.rpc.consumer.controller;


import com.mszlu.rpc.consumer.rpc.GoodsHttpRpc;
import com.mszlu.rpc.provider.service.modal.Goods;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("consumer")
public class ConsumerController {

    @Resource
    private GoodsHttpRpc goodsHttpRpc;

    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsHttpRpc.findGoods(id);
    }
}
