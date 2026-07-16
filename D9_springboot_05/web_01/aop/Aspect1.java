package com.cds.javaweb.web_01.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
//@Aspect
@Component
public class Aspect1 {
    @Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
    public void before(JoinPoint joinPoint){
        log.info("before型通知.....");

        //获取目标对象
        Object target = joinPoint.getTarget();
        log.info("目标对象：{}",target);

        //获取目标类
        String className = target.getClass().getName();
        log.info("目标类：{}",className);

        //获取目标方法
        String methodName = joinPoint.getSignature().getName();
        log.info("目标方法：{}",methodName);

        //获取目标方法参数
        Object[] args = joinPoint.getArgs();
        log.info("目标方法参数：{}", Arrays.toString(args));

    }
}
