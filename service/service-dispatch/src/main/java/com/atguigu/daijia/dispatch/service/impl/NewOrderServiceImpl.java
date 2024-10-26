package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    @Autowired
    private XxlJobClient xxlJobClient;
    @Autowired
    private OrderJobMapper orderJobMapper;
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;


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

    @Override
    public void executeTask(long jobId) {
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getJobId, jobId);
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (Objects.isNull(orderJob)){
            //任务没有创建
            return;
        }
        //查询订单状态
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(orderJob.getParameter(), NewOrderTaskVo.class);
        Integer status = orderInfoFeignClient.getOrderStatus(newOrderTaskVo.getOrderId()).getData();
        if(!Objects.equals(status, OrderStatus.WAITING_ACCEPT.getStatus())){
            //停止任务调度
            xxlJobClient.stopJob(jobId);
            return;
        }
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        List<NearByDriverVo> driverList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();
        //遍历司机集合，为每个司机建立队列
        driverList.forEach(item -> {
            //记录司机id，防止重复推送订单信息
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, item.getDriverId());
            if(Boolean.FALSE.equals(isMember)){
                //将订单推送给多个满足条件的司机
                redisTemplate.opsForSet().add(repeatKey, item.getDriverId());
                //设置过期时间15分钟
                redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
                //将订单信息保存到司机临时队列
                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(item.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+item.getDriverId();
                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
                redisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
            }
        });
    }
}
