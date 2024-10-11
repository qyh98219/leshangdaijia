package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    @Autowired
    private XxlJobClient xxlJobClient;
    @Autowired
    private OrderJobMapper orderJobMapper;


    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        //判断当前订单是否开启调度
        Long orderId = newOrderTaskVo.getOrderId();
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getOrderId, orderId);
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        //没有开启过
        if(Objects.isNull(orderJob)){
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "", "0 0/1 * * * ?", "新订单任务,订单id："+newOrderTaskVo.getOrderId());

            OrderJob job = new OrderJob();
            job.setOrderId(orderId);
            job.setJobId(jobId);
            job.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(job);
            return jobId;
        }
        return null;
    }
}
