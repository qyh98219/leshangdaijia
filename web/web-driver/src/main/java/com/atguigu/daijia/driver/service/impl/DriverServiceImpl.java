package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String login(String code) {
        Result<Long> loginResult = driverInfoFeignClient.login(code);
        if(!loginResult.getCode().equals(HttpStatus.SC_OK)){
          throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        Long driverId = loginResult.getData();
        if(Objects.isNull(driverId)){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,driverId.toString(), RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        if(Objects.isNull(driverId)){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        Result<DriverLoginVo> loginVoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);
        if(!loginVoResult.getCode().equals(HttpStatus.SC_OK)){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        DriverLoginVo driverLoginVo = loginVoResult.getData();
        if(Objects.isNull(driverLoginVo)){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        return driverInfoFeignClient.getDriverAuthInfo(driverId).getData();
    }
}
