package com.cds.aop;

import com.cds.mapper.OperateLogMapper;
import com.cds.pojo.OperateLog;
import com.cds.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 * 拦截所有标注了 @Log 注解的方法，自动记录操作日志到数据库
 */
@Slf4j
@Aspect
@Component
public class OperateLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Around("@annotation(com.cds.anno.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(CurrentHolder.getCurrentUserId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));

        Object result;
        try {
            result = joinPoint.proceed();
            operateLog.setReturnValue(result != null ? result.toString() : "");
        } catch (Throwable e) {
            operateLog.setCostTime(System.currentTimeMillis() - begin);
            operateLog.setReturnValue("异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            try {
                operateLogMapper.insert(operateLog);
            } catch (Exception ex) {
                log.warn("操作日志入库失败: {}", ex.getMessage());
            }
            throw e;
        }

        operateLog.setCostTime(System.currentTimeMillis() - begin);
        try {
            operateLogMapper.insert(operateLog);
        } catch (Exception ex) {
            log.warn("操作日志入库失败: {}", ex.getMessage());
        }

        return result;
    }
}
