package com.atguigu.daijia.driver.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author qyh
 * @version 1.0
 * @className TencentCloudProperties
 * @description TODO
 * @date 2024/8/22 10:20
 **/
@Data
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudProperties {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucketPrivate;
    private String persionGroupId;
}
