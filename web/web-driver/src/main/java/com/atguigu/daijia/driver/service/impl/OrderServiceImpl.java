package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("driverOrderService")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId);
        return orderStatusResult.getData();
    }
}
