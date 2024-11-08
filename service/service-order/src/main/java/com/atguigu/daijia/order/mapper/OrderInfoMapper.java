package com.atguigu.daijia.order.mapper;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 分页查询订单
     * @param pageParam
     * @param customerId
     * @return
     */
    IPage<OrderInfo> selectCustomerOrderPage(@Param("pageParam") Page<OrderInfo> pageParam, @Param("customerId") Long customerId);
}
