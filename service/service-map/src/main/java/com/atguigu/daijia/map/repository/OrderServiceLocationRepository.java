package com.atguigu.daijia.map.repository;

import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderServiceLocationRepository extends MongoRepository<OrderServiceLocation, String> {
    /**
     * 根据订单id查找保存的经纬度信息
     * @param orderId
     * @return
     */
    List<OrderServiceLocation> findOrderIdOrderByCreateTimeAsc(Long orderId);
}
