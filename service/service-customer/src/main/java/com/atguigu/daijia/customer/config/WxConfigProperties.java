package com.atguigu.daijia.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author qyh
 * @version 1.0
 * @className WxConfigProperties
 * @description TODO
 * @date 2024/8/12 14:57
 **/
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxConfigProperties {
    private String appId;
    private String secret;
}
