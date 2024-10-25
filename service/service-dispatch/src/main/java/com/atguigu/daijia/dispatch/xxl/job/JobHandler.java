package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author qyh
 * @version 1.0
 * @className JobHandler
 * @description TODO
 * @date 2024/10/14 10:31
 **/
@Slf4j
@Component
public class JobHandler {
    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;
    @Autowired
    private NewOrderService newOrderService;

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler(){
        //记录任务调度日志
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        try{
            //执行搜索附件司机的任务
            newOrderService.executeTask(XxlJobHelper.getJobId());
            //成功状态
            xxlJobLog.setStatus(1);
        }catch (Exception e){
            //失败状态
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        }finally {
            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes(endTime-startTime);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
