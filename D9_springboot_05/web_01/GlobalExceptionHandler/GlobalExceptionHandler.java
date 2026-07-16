package com.cds.javaweb.web_01.GlobalExceptionHandler;

import com.cds.javaweb.web_01.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public Result handleException(Exception e) {
        log.error("服务器发生异常:", e);
        return Result.error("出错了，服务器发生异常");
    }

}
