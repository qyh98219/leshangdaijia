package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point,
                updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION,driverId.toString());
        return true;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue());

        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, Metrics.KILOMETERS);
        Circle circle = new Circle(point,distance);

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending();


        GeoResults<RedisGeoCommands.GeoLocation<String>> radiusResult = redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = radiusResult.getContent();
        List<NearByDriverVo> list = new ArrayList<>();

        if(!content.isEmpty()){
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : content) {
                Long driverId = Long.parseLong(item.getContent().getName());
                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();

                //判断订单里程
                BigDecimal orderDistance = driverSet.getOrderDistance();
                if (orderDistance.doubleValue() != 0 && orderDistance.subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                //判断接单里程
                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                BigDecimal currentDistance = BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);

                if (acceptDistance.doubleValue() != 0 && acceptDistance.subtract(currentDistance).doubleValue() < 0) {
                    continue;
                }

                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }
        }

        return list;
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        redisTemplate.opsForValue().set(RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId(), orderLocationVo);
        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return (OrderLocationVo) redisTemplate.opsForValue().get(RedisConstant.UPDATE_ORDER_LOCATION + orderId);
    }
}
