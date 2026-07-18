package com.cds.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
//@Aspect
@Component
public class RecordTimeAspect {
    @Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
    public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {
        Long begin = System.currentTimeMillis();

        Object result = pjp.proceed();

        Long end = System.currentTimeMillis();
        log.info("方法 {} 执行耗时：{} ms",pjp.getSignature(),end - begin);
        return result;
    }
}
