package com.atguigu.daijia.order.mapper;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.vo.order.OrderListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 分页查询乘客订单
     * @param pageParam
     * @param customerId
     * @return
     */
    IPage<OrderInfo> selectCustomerOrderPage(@Param("pageParam") Page<OrderInfo> pageParam, @Param("customerId") Long customerId);

    /**
     * 分页查询司机订单
     * @param pageParam
     * @param driverId
     * @return
     */
    IPage<OrderListVo> selectDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);
}
