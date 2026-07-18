package com.cds.controller;


import com.cds.pojo.Result;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Slf4j
@RestController
public class UploadController {

    @PostConstruct
    public void init() {
        log.info(">>> UploadController bean 已注册! <<<");
    }

    @GetMapping("/upload")
    public Result testGet() {
        return Result.success("GET /upload 正常");
    }

    @PostMapping("/upload")
    public Result upload(String name, Integer age, MultipartFile file) throws Exception {
        log.info("接收参数： {},{},{}", name, age, file);
        //获取文件名
        String originalFilename = file.getOriginalFilename();

        //新的文件名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;

        //保存文件
        file.transferTo(new File("D:\\SpringBoot文件上传\\" + fileName));

        return Result.success();
    }
}
